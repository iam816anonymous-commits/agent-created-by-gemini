package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.AgentEngineState
import com.example.core.model.DeviceCapabilities
import com.example.core.model.LiveConsoleEntry
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.SkillEntity
import com.example.data.local.SynthesizedTaskEntity
import com.example.data.local.TaskExecutionEntity
import com.example.ui.AgentUiState
import com.example.ui.AgentViewModel
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.StatusSuccess

@Composable
fun RecorderScreen(
    viewModel: AgentViewModel,
    uiState: AgentUiState,
    savedTasks: List<SynthesizedTaskEntity>,
    skills: List<SkillEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "VISUAL INTERACTION RECORDER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Record manual user interactions on screen to automatically synthesize reusable task skills.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startVisualRecording() }) {
                        Text("Start Recording")
                    }
                    OutlinedButton(onClick = { viewModel.stopVisualRecording() }) {
                        Text("Stop")
                    }
                }
            }
        }

        Text(
            text = "Synthesized Skills (${skills.size})",
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        skills.forEach { skill ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = skill.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = skill.description, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ObservationScreen(
    viewModel: AgentViewModel,
    uiState: AgentUiState,
    engineState: AgentEngineState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACCESSIBILITY OBSERVATION ENGINE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Live window hierarchy inspection with stale observation rejection.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Button(onClick = { viewModel.captureFreshObservation() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Capture Fresh Snapshot")
                }
            }
        }
    }
}

@Composable
fun TimelineScreen(
    viewModel: AgentViewModel,
    engineState: AgentEngineState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cognitive Execution Timeline",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current State: ${engineState.status}",
                    fontWeight = FontWeight.Bold,
                    color = CyberCyanPrimary
                )
                Text(
                    text = "Step index: ${engineState.currentStepIndex}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: AgentViewModel,
    uiState: AgentUiState,
    executions: List<TaskExecutionEntity>,
    logs: List<DiagnosticLogEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Audit & Execution Records",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        executions.take(15).forEach { record ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = record.prompt, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = record.status, color = StatusSuccess, fontSize = 11.sp)
                    }
                    Text(text = record.failureReason ?: "Success (${record.successfulSteps}/${record.totalSteps} steps)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DeviceScreen(
    viewModel: AgentViewModel,
    uiState: AgentUiState,
    capabilities: DeviceCapabilities?,
    storageMetrics: Pair<Long, Long>?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEVICE TELEMETRY & HARDWARE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                capabilities?.let { cap ->
                    Text("Model: ${cap.manufacturer} ${cap.model}", color = Color.White, fontSize = 13.sp)
                    Text("Android: ${cap.androidVersion} (API ${cap.sdkInt})", color = Color.LightGray, fontSize = 12.sp)
                    Text("Total RAM: ${cap.totalRamBytes / (1024 * 1024)} MB", color = Color.LightGray, fontSize = 12.sp)
                    Text("Accessibility Service: ${if (cap.isAccessibilityEnabled) "ENABLED" else "DISABLED"}", color = CyberCyanPrimary, fontSize = 12.sp)
                }
            }
        }

        storageMetrics?.let { st ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ROOM & VAULT STORAGE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CyberCyanPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Total Storage: ${st.first / (1024 * 1024)} MB", color = Color.White, fontSize = 12.sp)
                    Text("Available Storage: ${st.second / (1024 * 1024)} MB", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}
