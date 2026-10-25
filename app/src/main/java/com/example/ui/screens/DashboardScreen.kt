package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.AgentEngineState
import com.example.core.model.DeviceCapabilities
import com.example.core.model.ExecutionStatus
import com.example.ui.AgentTab
import com.example.ui.AgentUiState
import com.example.ui.AgentViewModel
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.StatusFailure
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusTimeout
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: AgentViewModel,
    uiState: AgentUiState,
    engineState: AgentEngineState,
    capabilities: DeviceCapabilities
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRunning = engineState.status == ExecutionStatus.ROUTING ||
            engineState.status == ExecutionStatus.PARSING ||
            engineState.status == ExecutionStatus.RESOLVING_TARGET ||
            engineState.status == ExecutionStatus.EXECUTING_ACTION ||
            engineState.status == ExecutionStatus.WAITING_OBSERVATION ||
            engineState.status == ExecutionStatus.VERIFYING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 100% On-Device Offline Status Pill / Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("offline_status_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalWifiOff,
                        contentDescription = "Offline Mode",
                        tint = CyberCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% On-Device • Zero Cloud Calls • Airplane Ready",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = CyberCyanPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { viewModel.setSelectedTab(AgentTab.DEVICE) }
                ) {
                    Text(
                        text = "Matrix",
                        color = CyberCyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Accessibility Service Alert or Banner
        if (!capabilities.isAccessibilityEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("accessibility_warning_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = StatusWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accessibility Service Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To automate physical UI gestures and observe live windows on your TECNO phone, enable 'LocalAgent Automation Service' in system settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary),
                            modifier = Modifier.weight(1f).testTag("enable_accessibility_button")
                        ) {
                            Text("Open Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.refreshCapabilities() },
                            modifier = Modifier.testTag("refresh_capabilities_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = StatusSuccess.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("service_active_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accessibility Engine Active & Ready",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusSuccess
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Live Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Autonomous Living Entity & Physical Kill Switch Card
        val killSwitchCount by viewModel.killSwitchTriggeredCount.collectAsState()
        val lastKillReason by viewModel.lastKillSwitchReason.collectAsState()

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StatusFailure.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .testTag("autonomous_entity_kill_switch_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (capabilities.isAccessibilityEnabled) StatusSuccess else StatusWarning)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTONOMOUS LIVING ENTITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyanPrimary,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusFailure.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusFailure.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "KILL SWITCH ARMED",
                            color = StatusFailure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Full Device Control Active • Physical Abort Button:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Press your phone's physical VOLUME UP button TWICE in rapid succession (<650ms) from ANY screen or app to instantly halt and abort all autonomous agent actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                if (killSwitchCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Triggered: $killSwitchCount times. Last: ${lastKillReason ?: "Volume Up Double-Press"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = StatusFailure
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerEmergencyKillSwitch("Simulated Physical Volume-Up Kill Switch Test")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFailure),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusFailure.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_kill_switch_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusFailure)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Kill Switch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isRunning) {
                        Button(
                            onClick = { viewModel.triggerEmergencyKillSwitch("Manual Abort Button") },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusFailure, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ABORT NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Execution Status Dashboard Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .testTag("status_dashboard_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTONOMOUS CONTROLLER",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyanPrimary,
                        letterSpacing = 1.5.sp
                    )
                    StatusChip(status = engineState.status)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = engineState.statusMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (engineState.activeBackendName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyanPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "PLAN ROUTER: Tier ${engineState.activeTier ?: 1} (${engineState.activeBackendName})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = CyberCyanPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Step ${engineState.currentStepIndex} of ${engineState.totalSteps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = engineState.currentCommandName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyanPrimary
                        )
                    }
                }
            }
        }

        // Active Timer Countdown Card
        if (engineState.activeTimerCountdownSec != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCyanPrimary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CyberCyanPrimary, RoundedCornerShape(18.dp))
                    .testTag("active_timer_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Timer Running",
                        tint = CyberCyanPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE TIMER COUNTDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${engineState.activeTimerCountdownSec}s remaining",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (engineState.activeTimerCountdownSec == 0) StatusSuccess else StatusRunning
                    ) {
                        Text(
                            text = if (engineState.activeTimerCountdownSec == 0) "ALERT TRIGGERED!" else "RUNNING",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Researched & Scraped Intelligence Card (e.g. New Telugu Movies)
        engineState.scrapedResearchResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyanPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .testTag("scraped_research_results_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SCRAPED WEB INTELLIGENCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyanPrimary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Query: '${result.query}'",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${result.extractedCount} Items Scraped",
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = result.rawSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        result.hits.forEachIndexed { idx, hit ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${idx + 1}. ${hit.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyanPrimary
                                    )
                                    if (hit.url.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = hit.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (hit.snippet.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = hit.snippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Natural Language Task Input Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Natural Language Task Input",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a task. LocalAgent normalizes it, inspects accessible UI trees, resolves targets, executes actions, and verifies outcomes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.taskInput,
                    onValueChange = { viewModel.setTaskInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_field"),
                    placeholder = { Text("e.g. Launch Calculator and tap 7") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK DEMO PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip("turn on wifi") { viewModel.setTaskInput(it) }
                    PresetChip("turn off wifi") { viewModel.setTaskInput(it) }
                    PresetChip("turn on bluetooth") { viewModel.setTaskInput(it) }
                    PresetChip("turn off bluetooth") { viewModel.setTaskInput(it) }
                    PresetChip("start timer 5 minutes") { viewModel.setTaskInput(it) }
                    PresetChip("open google and search for new telugu movies") { viewModel.setTaskInput(it) }
                    PresetChip("Launch Calculator and tap 7") { viewModel.setTaskInput(it) }
                    PresetChip("Calculate 7 + 5 =") { viewModel.setTaskInput(it) }
                    PresetChip("Open Settings and scroll down") { viewModel.setTaskInput(it) }
                    PresetChip("vibrate") { viewModel.setTaskInput(it) }
                    PresetChip("turn on flashlight") { viewModel.setTaskInput(it) }
                    PresetChip("turn off flashlight") { viewModel.setTaskInput(it) }
                    PresetChip("play beep") { viewModel.setTaskInput(it) }
                    PresetChip("Open Navigation Drawer") { viewModel.setTaskInput(it) }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.stopTask() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusFailure),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("stop_task_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Execution", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startTask(runAsBenchmark = false) },
                            enabled = capabilities.isAccessibilityEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyanPrimary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("start_live_task_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Task", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.startTask(runAsBenchmark = true) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("start_benchmark_button")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = "Harness")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Harness", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Quick Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.captureFreshObservation() }
                    .testTag("inspect_live_ui_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Inspect UI",
                        tint = CyberCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inspect UI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live window nodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setSelectedTab(AgentTab.TIMELINE) }
                    .testTag("view_timeline_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Timeline",
                        tint = CyberCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${engineState.steps.size} step(s) logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        viewModel.setSelectedTab(AgentTab.OBSERVATION)
                        viewModel.setObservationSubTab(com.example.ui.ObservationSubTab.DUAL_TREE_DIFF)
                    }
                    .testTag("view_trees_diff_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "2 View Trees Diff",
                        tint = CyberCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2 View Trees Diff",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Before/after node delta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setSelectedTab(AgentTab.DEVICE) }
                    .testTag("hardware_actuators_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Actuator Deck",
                        tint = CyberCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Actuator Deck",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Haptics, torch, audio, HW",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ExecutionStatus) {
    val (color, text) = when (status) {
        ExecutionStatus.IDLE -> Pair(MaterialTheme.colorScheme.outline, "IDLE")
        ExecutionStatus.ROUTING -> Pair(CyberCyanPrimary, "ROUTING")
        ExecutionStatus.PARSING -> Pair(CyberCyanPrimary, "PARSING")
        ExecutionStatus.RESOLVING_TARGET -> Pair(StatusRunning, "RESOLVING")
        ExecutionStatus.EXECUTING_ACTION -> Pair(StatusRunning, "EXECUTING")
        ExecutionStatus.WAITING_OBSERVATION -> Pair(StatusRunning, "OBSERVING")
        ExecutionStatus.VERIFYING -> Pair(StatusWarning, "VERIFYING")
        ExecutionStatus.COMPLETED_SUCCESS -> Pair(StatusSuccess, "SUCCESS")
        ExecutionStatus.COMPLETED_FAILURE -> Pair(StatusFailure, "FAILED")
        ExecutionStatus.ABORTED_BY_USER -> Pair(StatusWarning, "ABORTED")
        ExecutionStatus.TIMEOUT -> Pair(StatusTimeout, "TIMEOUT")
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PresetChip(label: String, onClick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick(label) }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
