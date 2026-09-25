package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber

data class VivaQA(
    val question: String,
    val answer: String
)

@Composable
fun InternshipVivaView(modifier: Modifier = Modifier) {
    val qas = listOf(
        VivaQA(
            question = "Q1: Why is Edge AI needed at an EV charging station instead of relying solely on the cloud?",
            answer = "Edge AI allows real-time local decision-making (<100ms response). If external cellular/internet connectivity drops, the station can still perform safety-critical power modulation, preventing transformer trip-outs and grid blackout penalties without cloud latency."
        ),
        VivaQA(
            question = "Q2: How does the optimizer ensure the local power grid is never overloaded?",
            answer = "The optimizer enforces a strict mathematical knapsack bound: Sum(Allocated Power) <= Available Grid Power. If high-priority vehicles plug in, power is either dynamically throttled across active ports or lower-priority vehicles are queued until headroom frees up."
        ),
        VivaQA(
            question = "Q3: Why is battery saturation (tapering above 80% SOC) crucial in duration prediction?",
            answer = "Lithium-ion cells transition from Constant Current (CC) to Constant Voltage (CV) mode near 80% SOC to prevent lithium plating and thermal degradation. Charging speed slows non-linearly. The ML regression model captures this exponential taper."
        ),
        VivaQA(
            question = "Q4: What are the three primary components of your multi-attribute priority score?",
            answer = "1) Battery Urgency (45%): Lower SOC = higher priority.\n2) Departure Urgency (35%): Shorter remaining stay = higher priority.\n3) Energy Demand Ratio (20%): Proportional energy needed relative to pack capacity."
        ),
        VivaQA(
            question = "Q5: How is synthetic IoT sensor data modeled?",
            answer = "Sensor data is generated based on standard EV pack physics (40 to 93.4 kWh), nominal bus voltages (390-415V DC), Ohm's law current calculation (I = P * 1000 / V), and Poisson-distributed arrival and departure windows."
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("internship_viva_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = EnergyAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Internship Defense & Viva Exam Prep",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Key theoretical questions and answers for defending this Edge AI Smart EV Charging Station project.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            qas.forEach { qa ->
                VivaQAItem(qa = qa)
            }
        }
    }
}

@Composable
fun VivaQAItem(qa: VivaQA) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = qa.question,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = qa.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
