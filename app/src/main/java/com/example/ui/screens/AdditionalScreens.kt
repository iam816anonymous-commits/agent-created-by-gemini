package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val isRecording by viewModel.isRecording.collectAsState()
    val recordedEvents by viewModel.recordedEvents.collectAsState()
    val preview by viewModel.synthesizedTaskPreview.collectAsState()

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
                Text(
                    text = "Recorder: ${if (isRecording) "ACTIVE" else "STOPPED"} • ${recordedEvents.size} interaction(s)",
                    fontSize = 12.sp,
                    color = if (isRecording) CyberCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.actionFeedback?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startVisualRecording() }, enabled = !isRecording) {
                        Text("Start Recording")
                    }
                    OutlinedButton(onClick = { viewModel.stopVisualRecording() }, enabled = isRecording) {
                        Text("Stop")
                    }
                    OutlinedButton(onClick = { viewModel.clearRecording() }, enabled = recordedEvents.isNotEmpty() || preview != null) {
                        Text("Clear")
                    }
                }
                preview?.let { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${task.commands.size} executable command(s)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.saveSynthesizedTask() }, enabled = task.commands.isNotEmpty()) {
                                    Text("Save Skill")
                                }
                                Button(onClick = { viewModel.executeSynthesizedPreview() }, enabled = task.commands.isNotEmpty()) {
                                    Text("Save & Run")
                                }
                            }
                        }
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
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = skill.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = skill.description, fontSize = 11.sp, color = Color.Gray)
                    OutlinedButton(
                        onClick = { viewModel.executeSkill(skill) },
                        enabled = skill.commandsPayload.isNotBlank() || skill.name.isNotBlank()
                    ) {
                        Text("Run Skill")
                    }
                }
            }
        }

        Text("Saved Recordings (${savedTasks.size})", fontWeight = FontWeight.Bold, color = Color.White)
        savedTasks.forEach { task ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(task.description, fontSize = 11.sp, color = Color.Gray)
                    OutlinedButton(onClick = { viewModel.executeSynthesizedTask(task) }) {
                        Text("Run Recording")
                    }
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

        val snapshot = engineState.latestObservation
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Latest observation", fontWeight = FontWeight.Bold, color = CyberCyanPrimary)
                if (snapshot == null) {
                    Text("No observation captured yet.", color = Color.LightGray, fontSize = 12.sp)
                } else {
                    Text("Source: ${snapshot.sourceDescription}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Package: ${snapshot.packageName}", color = Color.White, fontSize = 12.sp)
                    Text("Nodes: ${snapshot.totalNodeCount} • Live: ${snapshot.isLive}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Captured: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(snapshot.timestampMs))}", color = Color.Gray, fontSize = 11.sp)
                    snapshot.allNodes.take(40).forEach { node ->
                        Text(
                            text = "${node.text?.takeIf { it.isNotBlank() } ?: node.contentDescription?.takeIf { it.isNotBlank() } ?: node.className} • ${if (node.isClickable) "clickable" else "read-only"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
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
                    text = "Step index: ${engineState.currentStepIndex}/${engineState.totalSteps}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(engineState.statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                engineState.failureReason?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            }
        }
        engineState.steps.forEach { step ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${step.stepIndex}. ${step.commandSummary}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(step.status.name, fontSize = 11.sp, color = when (step.status) {
                        com.example.core.model.StepStatus.SUCCESS -> StatusSuccess
                        com.example.core.model.StepStatus.FAILURE,
                        com.example.core.model.StepStatus.TIMEOUT -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
                    Text(step.verificationDetails, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${step.durationMs} ms", fontSize = 10.sp, color = Color.Gray)
                }
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
    val selectedSteps by viewModel.selectedExecutionSteps.collectAsState()

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
        uiState.actionFeedback?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (executions.isEmpty()) {
            Text("No saved executions yet.", fontSize = 12.sp, color = Color.LightGray)
        }
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
                        Text(
                            text = record.status,
                            color = when {
                                record.status.contains("SUCCESS", ignoreCase = true) -> StatusSuccess
                                record.status.contains("FAIL", ignoreCase = true) ||
                                    record.status.contains("TIMEOUT", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = record.failureReason ?: if (record.status.contains("SUCCESS", ignoreCase = true)) {
                            "Completed: ${record.successfulSteps}/${record.totalSteps} steps"
                        } else {
                            "No failure details recorded."
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    OutlinedButton(onClick = { viewModel.loadExecutionSteps(record.id) }) {
                        Text(if (uiState.selectedExecutionId == record.id) "Steps Selected" else "View Steps")
                    }
                }
            }
        }
        if (uiState.selectedExecutionId != null) {
            Text("Stored steps for selected execution", fontWeight = FontWeight.Bold, color = CyberCyanPrimary)
            selectedSteps.forEach { step ->
                Text(
                    "${step.stepIndex}. ${step.commandSummary} — ${step.status}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "HARDWARE ACTIONS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                uiState.actionFeedback?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.toggleFlashlight() }, modifier = Modifier.weight(1f)) {
                        Text("Toggle Flashlight")
                    }
                    OutlinedButton(onClick = { viewModel.triggerHapticPulse() }, modifier = Modifier.weight(1f)) {
                        Text("Vibrate")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.triggerHapticBurst() }, modifier = Modifier.weight(1f)) {
                        Text("Vibration Burst")
                    }
                    OutlinedButton(onClick = { viewModel.triggerAudioTone() }, modifier = Modifier.weight(1f)) {
                        Text("Play Tone")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.toggleWifi() }, modifier = Modifier.weight(1f)) {
                        Text("Wi-Fi Controls")
                    }
                    OutlinedButton(onClick = { viewModel.toggleBluetooth() }, modifier = Modifier.weight(1f)) {
                        Text("Bluetooth Controls")
                    }
                }
                OutlinedButton(onClick = { viewModel.refreshCapabilities() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Refresh Device State")
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
