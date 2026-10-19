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
import com.example.core.model.AgentSkill
import com.example.core.model.DeviceCapabilities
import com.example.core.model.LiveConsoleEntry
import com.example.core.model.StepExecutionRecord
import com.example.core.model.SynthesizedTask
import com.example.ui.AgentState
import com.example.ui.AgentViewModel
import com.example.ui.EngineState
import com.example.ui.StorageMetrics
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.StatusSuccess

@Composable
fun RecorderScreen(
    viewModel: AgentViewModel,
    uiState: AgentState,
    savedTasks: List<SynthesizedTask>,
    skills: List<AgentSkill>,
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
                    Button(onClick = { viewModel.startRecording() }) {
                        Text("Start Recording")
                    }
                    OutlinedButton(onClick = { viewModel.stopRecording() }) {
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
    uiState: AgentState,
    engineState: EngineState,
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
                Button(onClick = { viewModel.captureLiveSnapshot() }) {
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
    engineState: EngineState,
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
    uiState: AgentState,
    executions: List<StepExecutionRecord>,
    logs: List<LiveConsoleEntry>,
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
                        Text(text = record.commandSummary, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${record.status}", color = StatusSuccess, fontSize = 11.sp)
                    }
                    Text(text = record.verificationDetails, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DeviceScreen(
    viewModel: AgentViewModel,
    uiState: AgentState,
    capabilities: DeviceCapabilities?,
    storageMetrics: StorageMetrics?,
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
                    Text("Database Records: ${st.databaseRecordsCount}", color = Color.White, fontSize = 12.sp)
                    Text("Storage Size: ${st.databaseSizeBytes} bytes", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}
