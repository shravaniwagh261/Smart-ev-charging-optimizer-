"""
IoT Data Simulator Module
Generates realistic simulated telemetry and EV arrival characteristics.
NOTE: All generated data is synthetic/simulated for academic and prototyping purposes.
"""

import random
from datetime import datetime, timedelta

# Popular EV profiles for realistic synthetic generation
EV_MODELS = [
    {"model": "Tesla Model 3", "capacity": 60.0, "max_ac_kw": 11.0, "max_dc_kw": 170.0},
    {"model": "Tesla Model Y", "capacity": 75.0, "max_ac_kw": 11.0, "max_dc_kw": 250.0},
    {"model": "Hyundai Ioniq 5", "capacity": 77.4, "max_ac_kw": 11.0, "max_dc_kw": 220.0},
    {"model": "Kia EV6", "capacity": 77.4, "max_ac_kw": 11.0, "max_dc_kw": 230.0},
    {"model": "Ford Mustang Mach-E", "capacity": 88.0, "max_ac_kw": 11.0, "max_dc_kw": 150.0},
    {"model": "Volkswagen ID.4", "capacity": 77.0, "max_ac_kw": 11.0, "max_dc_kw": 135.0},
    {"model": "Nissan Leaf", "capacity": 40.0, "max_ac_kw": 6.6, "max_dc_kw": 50.0},
    {"model": "BYD Atto 3", "capacity": 60.5, "max_ac_kw": 11.0, "max_dc_kw": 88.0},
    {"model": "Porsche Taycan", "capacity": 93.4, "max_ac_kw": 22.0, "max_dc_kw": 270.0},
    {"model": "MG ZS EV", "capacity": 50.3, "max_ac_kw": 7.4, "max_dc_kw": 76.0}
]

def generate_ev_dataset(num_evs=8, station_capacity_kw=250.0, max_charger_power=50.0, base_time=None):
    """
    Generates synthetic EV telemetry entries for the optimizer.
    """
    if base_time is None:
        base_time = datetime.now().replace(minute=0, second=0, microsecond=0)
    
    ev_records = []
    
    for i in range(1, num_evs + 1):
        preset = random.choice(EV_MODELS)
        capacity = preset["capacity"]
        
        # State of Charge (SOC)
        initial_soc = round(random.uniform(12.0, 55.0), 1)
        target_soc = round(random.choice([80.0, 85.0, 90.0, 95.0, 100.0]), 1)
        if target_soc <= initial_soc:
            target_soc = min(100.0, initial_soc + 30.0)
            
        energy_required = round(((target_soc - initial_soc) / 100.0) * capacity, 2)
        
        # Timing
        arrival_delta_min = random.randint(-40, 20)
        arrival_time = base_time + timedelta(minutes=arrival_delta_min)
        
        stay_duration_hours = random.uniform(1.2, 5.0)
        departure_time = arrival_time + timedelta(hours=stay_duration_hours)
        
        # Hardware charging capability
        supported_power = min(max_charger_power, preset["max_dc_kw"])
        
        # IoT Electrical sensor readings
        nominal_voltage = round(random.uniform(390.0, 415.0), 1)
        # Power = V * I / 1000 => I = (P * 1000) / V
        nominal_current = round((supported_power * 1000.0) / nominal_voltage, 1)
        
        ev_records.append({
            "ev_id": f"EV-{100 + i}",
            "model_name": preset["model"],
            "battery_pct": initial_soc,
            "target_battery_pct": target_soc,
            "battery_capacity_kwh": capacity,
            "arrival_time": arrival_time.strftime("%H:%M"),
            "departure_time": departure_time.strftime("%H:%M"),
            "stay_duration_hours": round(stay_duration_hours, 2),
            "energy_required_kwh": energy_required,
            "max_charging_power_kw": supported_power,
            "voltage_v": nominal_voltage,
            "current_a": nominal_current,
            "charging_status": "Queued"
        })
        
    return ev_records

def get_grid_state(station_capacity_kw=250.0, base_grid_load_pct=65.0, total_chargers=6):
    """
    Simulates real-time grid conditions and available headroom.
    """
    # Fluctuate grid load realistically
    current_grid_load_pct = max(10.0, min(95.0, base_grid_load_pct + random.uniform(-4.0, 4.0)))
    current_grid_load_pct = round(current_grid_load_pct, 1)
    
    # Available station power throttled by grid load
    grid_throttle_factor = 1.0 - (max(0.0, current_grid_load_pct - 60.0) / 100.0)
    effective_station_power = round(station_capacity_kw * grid_throttle_factor, 1)
    
    return {
        "station_power_capacity_kw": station_capacity_kw,
        "current_grid_load_pct": current_grid_load_pct,
        "grid_throttle_factor": round(grid_throttle_factor, 2),
        "available_grid_power_kw": effective_station_power,
        "total_chargers": total_chargers
    }
