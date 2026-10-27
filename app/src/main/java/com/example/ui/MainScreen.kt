package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.ExecutionStatus
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ObservationScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.StatusFailure
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AgentViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val capabilities by viewModel.deviceCapabilities.collectAsStateWithLifecycle()
    val storageMetrics by viewModel.storageMetrics.collectAsStateWithLifecycle()
    val executions by viewModel.allExecutions.collectAsStateWithLifecycle()
    val logs by viewModel.recentLogs.collectAsStateWithLifecycle()

    val savedTasks by viewModel.allSynthesizedTasks.collectAsStateWithLifecycle()
    val skills by viewModel.allSkills.collectAsStateWithLifecycle()

    // Handle back press to return to Dashboard from sub-tabs
    BackHandler(enabled = uiState.selectedTab != AgentTab.DASHBOARD) {
        viewModel.setSelectedTab(AgentTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulse status indicator
                        val pulseColor = when (engineState.status) {
                            ExecutionStatus.IDLE -> if (capabilities.isAccessibilityEnabled) StatusSuccess else StatusWarning
                            ExecutionStatus.ROUTING,
                            ExecutionStatus.PARSING,
                            ExecutionStatus.RESOLVING_TARGET,
                            ExecutionStatus.EXECUTING_ACTION,
                            ExecutionStatus.WAITING_OBSERVATION,
                            ExecutionStatus.VERIFYING -> StatusRunning
                            ExecutionStatus.COMPLETED_SUCCESS -> StatusSuccess
                            ExecutionStatus.COMPLETED_FAILURE -> StatusFailure
                            ExecutionStatus.ABORTED_BY_USER -> StatusWarning
                            ExecutionStatus.TIMEOUT -> StatusFailure
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pulseColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LocalAgent",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${capabilities.manufacturer} ${capabilities.model}".trim().ifBlank { "Unknown device" },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyanPrimary,
                            modifier = Modifier
                                .background(CyberCyanPrimary.copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusFailure.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusFailure.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                viewModel.triggerEmergencyKillSwitch("User Tapped Kill Switch Button in Top Bar")
                            }
                            .testTag("kill_switch_header_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusFailure)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KILL SWITCH [VOL UP x2]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = StatusFailure
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.DASHBOARD,
                    onClick = { viewModel.setSelectedTab(AgentTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Agent") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.RECORDER,
                    onClick = { viewModel.setSelectedTab(AgentTab.RECORDER) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Recorder") },
                    label = { Text("Record") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_recorder")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.OBSERVATION,
                    onClick = { viewModel.setSelectedTab(AgentTab.OBSERVATION) },
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "Observation") },
                    label = { Text("Observe") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_observation")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.TIMELINE,
                    onClick = { viewModel.setSelectedTab(AgentTab.TIMELINE) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline") },
                    label = { Text("Timeline") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_timeline")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.HISTORY,
                    onClick = { viewModel.setSelectedTab(AgentTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_history")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AgentTab.DEVICE,
                    onClick = { viewModel.setSelectedTab(AgentTab.DEVICE) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Device") },
                    label = { Text("Device") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_device")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = uiState.selectedTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    AgentTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        engineState = engineState,
                        capabilities = capabilities
                    )
                    AgentTab.RECORDER -> com.example.ui.screens.RecorderScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        savedTasks = savedTasks,
                        skills = skills
                    )
                    AgentTab.OBSERVATION -> ObservationScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        engineState = engineState
                    )
                    AgentTab.TIMELINE -> TimelineScreen(
                        viewModel = viewModel,
                        engineState = engineState
                    )
                    AgentTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        executions = executions,
                        logs = logs
                    )
                    AgentTab.DEVICE -> DeviceScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        capabilities = capabilities,
                        storageMetrics = storageMetrics
                    )
                }
            }
        }
    }
}
