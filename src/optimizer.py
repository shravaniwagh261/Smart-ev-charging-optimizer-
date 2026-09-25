"""
Charging Optimization Module
Computes multidimensional EV priority scores and dynamically schedules charging slots
while strictly enforcing grid constraints: Total Allocated Power <= Station Available Power.
"""

from datetime import datetime, timedelta

def calculate_priority_score(ev, grid_load_pct):
    """
    Computes priority score (0.0 to 100.0) based on:
    - Battery urgency (lower SOC -> higher urgency)
    - Departure urgency (less remaining time -> higher urgency)
    - Energy requirement ratio
    - Grid load sensitivity
    """
    battery_urgency = max(0.0, (100.0 - ev["battery_pct"]) / 100.0)
    
    stay_hours = max(0.2, ev["stay_duration_hours"])
    departure_urgency = min(1.0, 1.2 / (stay_hours + 0.2))
    
    energy_ratio = min(1.0, ev["energy_required_kwh"] / max(20.0, ev["battery_capacity_kwh"]))
    
    # Weighted multi-attribute score (Scaled to 0 - 100)
    w_battery = 45.0
    w_departure = 35.0
    w_energy = 20.0
    
    base_score = (battery_urgency * w_battery) + (departure_urgency * w_departure) + (energy_ratio * w_energy)
    
    # Modulate slightly if grid is extremely stressed
    if grid_load_pct > 80.0:
        base_score *= 0.95
        
    return round(base_score, 2)

def schedule_ev_charging(ev_list, grid_state):
    """
    Greedy Knapsack-style power allocation and charger slot assignment.
    Constraint 1: Active chargers <= Total physical chargers
    Constraint 2: Sum(Allocated Power) <= Available Grid Power
    """
    station_capacity = grid_state["available_grid_power_kw"]
    total_chargers = grid_state["total_chargers"]
    grid_load_pct = grid_state["current_grid_load_pct"]
    
    # 1. Calculate Priority Scores
    for ev in ev_list:
        ev["priority_score"] = calculate_priority_score(ev, grid_load_pct)
        
    # 2. Sort by descending priority score
    sorted_evs = sorted(ev_list, key=lambda x: x["priority_score"], reverse=True)
    
    allocated_power_sum = 0.0
    assigned_chargers_count = 0
    now = datetime.now()
    
    scheduled_results = []
    
    for idx, ev in enumerate(sorted_evs):
        requested_power = ev["max_charging_power_kw"]
        
        # Check if charger slot and power headroom are available
        can_charge_now = (assigned_chargers_count < total_chargers) and \
                         (allocated_power_sum + 10.0 <= station_capacity)
                         
        if can_charge_now:
            assigned_chargers_count += 1
            # Adjust power if near capacity limit
            power_to_give = min(requested_power, station_capacity - allocated_power_sum)
            allocated_power_sum += power_to_give
            
            # Charging duration in hours (considering efficiency ~92%)
            duration_hours = ev["energy_required_kwh"] / (max(5.0, power_to_give) * 0.92)
            duration_minutes = max(15, int(duration_hours * 60))
            
            start_time = now.strftime("%H:%M")
            comp_time = (now + timedelta(minutes=duration_minutes)).strftime("%H:%M")
            
            ev_copy = dict(ev)
            ev_copy["assigned_charger"] = f"Bay #{assigned_chargers_count}"
            ev_copy["allocated_power_kw"] = round(power_to_give, 1)
            ev_copy["start_time"] = start_time
            ev_copy["est_completion_time"] = comp_time
            ev_copy["est_duration_min"] = duration_minutes
            ev_copy["schedule_status"] = "Charging"
            scheduled_results.append(ev_copy)
        else:
            # Deferred / Queued in schedule
            duration_hours = ev["energy_required_kwh"] / (requested_power * 0.92)
            duration_minutes = max(15, int(duration_hours * 60))
            
            ev_copy = dict(ev)
            ev_copy["assigned_charger"] = "Queue"
            ev_copy["allocated_power_kw"] = 0.0
            ev_copy["start_time"] = "Deferred"
            ev_copy["est_completion_time"] = f"+{duration_minutes}m after slot frees"
            ev_copy["est_duration_min"] = duration_minutes
            ev_copy["schedule_status"] = "Queued"
            scheduled_results.append(ev_copy)
            
    summary = {
        "total_active_charging": assigned_chargers_count,
        "total_queued": len(ev_list) - assigned_chargers_count,
        "allocated_power_kw": round(allocated_power_sum, 1),
        "station_headroom_kw": round(max(0.0, station_capacity - allocated_power_sum), 1),
        "power_utilization_pct": round((allocated_power_sum / max(1.0, station_capacity)) * 100.0, 1),
        "available_chargers_remaining": total_chargers - assigned_chargers_count
    }
    
    return scheduled_results, summary
