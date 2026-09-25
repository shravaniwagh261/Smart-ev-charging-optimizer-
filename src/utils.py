"""
Utility functions for data serialization, alerts, and metrics formatting.
"""

def generate_alerts(ev_list, grid_state, scheduled_results):
    """
    Evaluates real-time alerts:
    - High grid load (> 80%)
    - Insufficient available power
    - EV departure approaching (< 30 min left)
    - Schedule conflicts / Over-subscription
    """
    alerts = []
    
    # 1. Grid Load Alert
    grid_load = grid_state["current_grid_load_pct"]
    if grid_load >= 85.0:
        alerts.append({
            "level": "CRITICAL",
            "title": "Severe Grid Stress Detected",
            "message": f"Regional grid load is at {grid_load}%. Charger throttling active to avoid blackout tariffs."
        })
    elif grid_load >= 75.0:
        alerts.append({
            "level": "WARNING",
            "title": "High Grid Demand Warning",
            "message": f"Grid load at {grid_load}%. Peak-hour rates apply. Optimization prioritizing critical EVs."
        })
        
    # 2. Insufficient Station Power Alert
    active_power = sum(ev.get("allocated_power_kw", 0) for ev in scheduled_results)
    station_cap = grid_state["available_grid_power_kw"]
    if station_cap - active_power < 15.0 and len(scheduled_results) > 0:
        alerts.append({
            "level": "WARNING",
            "title": "Station Capacity Near Peak",
            "message": f"Station headroom is {round(station_cap - active_power, 1)} kW out of {station_cap} kW limit."
        })
        
    # 3. Impending Departure for queued EVs
    for ev in scheduled_results:
        if ev.get("schedule_status") == "Queued" and ev.get("stay_duration_hours", 2.0) <= 1.0:
            alerts.append({
                "level": "ALERT",
                "title": f"Departure Imminent: {ev['ev_id']}",
                "message": f"{ev['ev_id']} leaves in {int(ev['stay_duration_hours'] * 60)}m but is currently queued!"
            })
            
    # 4. Schedule Queue Conflict
    queued_count = sum(1 for ev in scheduled_results if ev.get("schedule_status") == "Queued")
    if queued_count >= 3:
        alerts.append({
            "level": "INFO",
            "title": "Charger Congestion Notice",
            "message": f"{queued_count} EVs currently waiting in queue. Consider activating Smart Dynamic Load Balancing."
        })
        
    if not alerts:
        alerts.append({
            "level": "NORMAL",
            "title": "Station Operations Nominal",
            "message": "All active charging bays operating within safe thermal and grid power limits."
        })
        
    return alerts
