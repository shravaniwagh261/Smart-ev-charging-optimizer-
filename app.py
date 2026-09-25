"""
Edge AI Based Smart EV Charging Station Optimizer
Main Streamlit Application Dashboard
"""

import streamlit as st
import pandas as pd
import numpy as np
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime

from src.data_generator import generate_ev_dataset, get_grid_state
from src.optimizer import schedule_ev_charging
from src.ml_model import train_or_load_model
from src.utils import generate_alerts

st.set_page_config(
    page_title="Smart EV Charging Station Optimizer",
    page_icon="⚡",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom Styling
st.markdown("""
<style>
    .metric-card {
        background-color: #1E293B;
        border-radius: 10px;
        padding: 16px;
        border-left: 5px solid #10B981;
    }
    .badge-critical { background-color: #EF4444; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
    .badge-warning { background-color: #F59E0B; color: black; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
    .badge-info { background-color: #3B82F6; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
    .badge-normal { background-color: #10B981; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
</style>
""", unsafe_allow_html=True)

st.title("⚡ Smart EV Charging Station Optimizer")
st.caption("Edge AI & IoT Powered Grid Load Management & Priority Scheduling System (Synthetic Data Academic Demo)")

# ----------------- SIDEBAR CONTROLS -----------------
st.sidebar.header("🛠️ Station & Grid Configuration")

num_evs = st.sidebar.slider("Number of Connected EVs", min_value=3, max_value=16, value=8, step=1)
station_capacity = st.sidebar.slider("Station Power Capacity (kW)", min_value=100.0, max_value=500.0, value=250.0, step=25.0)
num_chargers = st.sidebar.slider("Available Charger Bays", min_value=2, max_value=10, value=6, step=1)
grid_load_pct = st.sidebar.slider("Current Regional Grid Load (%)", min_value=20.0, max_value=95.0, value=68.0, step=1.0)
max_charger_power = st.sidebar.select_slider(
    "Max Charger Power (kW)",
    options=[22.0, 50.0, 75.0, 100.0, 150.0, 250.0],
    value=100.0
)

col_b1, col_b2 = st.sidebar.columns(2)
with col_b1:
    gen_data_btn = st.button("Generate EV Data", use_container_width=True)
with col_b2:
    reset_btn = st.button("Reset Simulation", use_container_width=True)

retrain_btn = st.sidebar.button("Train / Retrain AI Model", use_container_width=True)
optimize_btn = st.sidebar.button("Generate Optimized Schedule", type="primary", use_container_width=True)

# ----------------- SESSION STATE -----------------
if "ev_data" not in st.session_state or gen_data_btn or reset_btn:
    st.session_state.ev_data = generate_ev_dataset(
        num_evs=num_evs,
        station_capacity_kw=station_capacity,
        max_charger_power=max_charger_power
    )

if "model" not in st.session_state or retrain_btn:
    st.session_state.model = train_or_load_model(retrain=retrain_btn)
    if retrain_btn:
        st.sidebar.success("AI Model successfully retrained on synthetic dataset!")

# Grid state
grid_state = get_grid_state(
    station_capacity_kw=station_capacity,
    base_grid_load_pct=grid_load_pct,
    total_chargers=num_chargers
)

# Run optimization
scheduled_evs, schedule_summary = schedule_ev_charging(st.session_state.ev_data, grid_state)
alerts = generate_alerts(st.session_state.ev_data, grid_state, scheduled_evs)

# ----------------- TOP METRIC CARDS -----------------
total_energy_req = round(sum(ev["energy_required_kwh"] for ev in scheduled_evs), 1)
pred_peak_load = round(min(station_capacity, schedule_summary["allocated_power_kw"] * 1.08), 1)

c1, c2, c3, c4, c5, c6 = st.columns(6)
c1.metric("Available Power", f"{grid_state['available_grid_power_kw']} kW", delta=f"{round(station_capacity - grid_state['available_grid_power_kw'], 1)} kW Throttled")
c2.metric("Current Grid Load", f"{grid_state['current_grid_load_pct']}%", delta="Normal" if grid_state['current_grid_load_pct'] < 75 else "High Stress", delta_color="inverse")
c3.metric("Active Charging EVs", f"{schedule_summary['total_active_charging']} / {num_evs}")
c4.metric("Available Chargers", f"{schedule_summary['available_chargers_remaining']} / {num_chargers}")
c5.metric("Total Energy Req.", f"{total_energy_req} kWh")
c6.metric("Predicted Peak Load", f"{pred_peak_load} kW", delta=f"Cap: {station_capacity} kW")

# ----------------- ALERTS SECTION -----------------
st.write("---")
st.subheader("🚨 Real-Time Station & Grid Alerts")
alert_cols = st.columns(len(alerts))
for i, alert in enumerate(alerts):
    lvl = alert["level"]
    with alert_cols[i % len(alert_cols)]:
        if lvl == "CRITICAL":
            st.error(f"**{alert['title']}**\n\n{alert['message']}")
        elif lvl == "WARNING" or lvl == "ALERT":
            st.warning(f"**{alert['title']}**\n\n{alert['message']}")
        else:
            st.info(f"**{alert['title']}**\n\n{alert['message']}")

# ----------------- TABS FOR COMPREHENSIVE SECTIONS -----------------
tab1, tab2, tab3, tab4, tab5 = st.tabs([
    "📊 Live IoT & Schedule",
    "🧠 AI Charging Priority",
    "📈 ML Duration Prediction",
    "⚡ Grid & Energy Analytics",
    "📋 Raw Dataset & Export"
])

with tab1:
    col_sched, col_iot = st.columns([3, 2])
    with col_sched:
        st.markdown("### D. Recommended Charging Schedule")
        st.caption("Grid-constrained Knapsack scheduling ensures total active load ≤ available station power limit.")
        
        sched_df = pd.DataFrame(scheduled_evs)[[
            "ev_id", "model_name", "assigned_charger", "allocated_power_kw",
            "start_time", "est_completion_time", "schedule_status", "priority_score"
        ]]
        st.dataframe(sched_df, use_container_width=True, hide_index=True)
        
        st.progress(
            min(1.0, schedule_summary["allocated_power_kw"] / max(1.0, grid_state["available_grid_power_kw"])),
            text=f"Station Power Utilization: {schedule_summary['allocated_power_kw']} kW / {grid_state['available_grid_power_kw']} kW ({schedule_summary['power_utilization_pct']}%)"
        )
        
    with col_iot:
        st.markdown("### A. Live IoT Sensor Telemetry")
        st.caption("Real-time telemetry stream from smart charger meters (synthetic).")
        iot_df = pd.DataFrame(scheduled_evs)[[
            "ev_id", "battery_pct", "voltage_v", "current_a", "energy_required_kwh"
        ]]
        st.dataframe(iot_df, use_container_width=True, hide_index=True)

with tab2:
    st.markdown("### C. AI Multi-Attribute Charging Priority Scores")
    st.caption("Priority formulated from Battery Urgency (45%), Departure Urgency (35%), and Energy Demand (20%).")
    
    p_df = pd.DataFrame(scheduled_evs).sort_values("priority_score", ascending=True)
    fig_priority = px.bar(
        p_df,
        x="priority_score",
        y="ev_id",
        orientation="h",
        color="schedule_status",
        color_discrete_map={"Charging": "#10B981", "Queued": "#F59E0B"},
        title="EV Charging Urgency Scores vs Assigned Status",
        labels={"priority_score": "Priority Score (0-100)", "ev_id": "Vehicle ID"}
    )
    st.plotly_chart(fig_priority, use_container_width=True)

with tab3:
    st.markdown("### E. Machine Learning Charging-Time Prediction")
    st.caption("Trained Random Forest Regression Model on synthetic charging cycles.")
    
    # Evaluate model
    test_samples = []
    for ev in scheduled_evs:
        test_samples.append({
            "ev_id": ev["ev_id"],
            "battery_pct": ev["battery_pct"],
            "target_battery_pct": ev["target_battery_pct"],
            "battery_capacity_kwh": ev["battery_capacity_kwh"],
            "max_charging_power_kw": ev["max_charging_power_kw"],
            "energy_required_kwh": ev["energy_required_kwh"],
            "grid_load_pct": grid_state["current_grid_load_pct"],
            "actual_duration_min": ev.get("est_duration_min", 45) + np.random.uniform(-3, 3)
        })
    metrics = st.session_state.model.evaluate(test_samples)
    
    m1, m2, m3 = st.columns(3)
    m1.metric("Mean Absolute Error (MAE)", f"{metrics['mae']} min")
    m2.metric("Root Mean Squared Error (RMSE)", f"{metrics['rmse']} min")
    m3.metric("R² Score", f"{metrics['r2']}")
    
    comp_df = pd.DataFrame(metrics["comparison"])
    fig_ml = px.scatter(
        comp_df,
        x="actual",
        y="predicted",
        text="ev_id",
        title="Actual vs Predicted Charging Duration (Minutes)",
        labels={"actual": "Ground Truth Duration (min)", "predicted": "ML Predicted Duration (min)"},
        color_discrete_sequence=["#3B82F6"]
    )
    # Perfect prediction line
    min_v = min(comp_df["actual"].min(), comp_df["predicted"].min())
    max_v = max(comp_df["actual"].max(), comp_df["predicted"].max())
    fig_ml.add_trace(go.Scatter(x=[min_v, max_v], y=[min_v, max_v], mode='lines', name='Ideal Fit (y=x)', line=dict(dash='dash', color='red')))
    st.plotly_chart(fig_ml, use_container_width=True)

with tab4:
    st.markdown("### F & G. Grid Load & Energy Consumption Analytics")
    col_g1, col_g2 = st.columns(2)
    
    with col_g1:
        # 24 hour grid load curve
        hours = [f"{h:02d}:00" for h in range(24)]
        synthetic_grid_profile = [35, 30, 28, 25, 27, 34, 48, 62, 75, 78, 82, 85, 80, 77, 73, 76, 88, 92, 86, 78, 68, 55, 45, 38]
        fig_grid = px.line(
            x=hours,
            y=synthetic_grid_profile,
            title="Regional Grid Demand Curve (24-Hour Profile)",
            labels={"x": "Hour of Day", "y": "Grid Stress (%)"},
            color_discrete_sequence=["#EF4444"]
        )
        fig_grid.add_hline(y=grid_state["current_grid_load_pct"], line_dash="dash", line_color="orange", annotation_text=f"Current Load ({grid_state['current_grid_load_pct']}%)")
        st.plotly_chart(fig_grid, use_container_width=True)
        
    with col_g2:
        # Energy required per EV
        fig_energy = px.bar(
            pd.DataFrame(scheduled_evs),
            x="ev_id",
            y="energy_required_kwh",
            color="model_name",
            title="Energy Required per Vehicle (kWh)",
            labels={"energy_required_kwh": "Energy Needed (kWh)", "ev_id": "Vehicle"}
        )
        st.plotly_chart(fig_energy, use_container_width=True)

with tab5:
    st.subheader("Synthetic Dataset Explorer")
    st.caption("All records in this dataset are synthetically generated for engineering internship demonstration.")
    st.dataframe(pd.DataFrame(scheduled_evs), use_container_width=True)
    csv = pd.DataFrame(scheduled_evs).to_csv(index=False)
    st.download_button("Download Current Simulation CSV", data=csv, file_name="ev_simulation_data.csv", mime="text/csv")
