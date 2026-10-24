package com.example.core.model

import java.util.UUID

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

data class RectData(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    fun contains(x: Int, y: Int): Boolean = x in left..right && y in top..bottom
    fun toReadable(): String = "[$left, $top, $right, $bottom]"
}

data class TargetSelector(
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val clickableOnly: Boolean = false,
    val caseSensitive: Boolean = false,
    val fallbackNormalizedX: Float? = null,
    val fallbackNormalizedY: Float? = null
) {
    fun toReadableDescription(): String {
        val parts = mutableListOf<String>()
        text?.let { parts.add("text='$it'") }
        contentDescription?.let { parts.add("desc='$it'") }
        resourceId?.let { parts.add("id='$it'") }
        className?.let { parts.add("class='$it'") }
        if (fallbackNormalizedX != null && fallbackNormalizedY != null) {
            parts.add("coord=[${(fallbackNormalizedX * 100).toInt()}%, ${(fallbackNormalizedY * 100).toInt()}%]")
        }
        return if (parts.isEmpty()) "Any Element" else parts.joinToString(", ")
    }
}

sealed interface VerificationRule {
    data class DisplayContainsText(val expectedText: String) : VerificationRule
    data class ElementExists(val selector: TargetSelector) : VerificationRule
    data class PackageMatches(val expectedPackage: String) : VerificationRule
    data class HierarchyChanged(val preActionNodeCount: Int) : VerificationRule
}

sealed interface TaskCommand {
    val id: String
    val displayName: String

    data class LaunchApp(
        val packageName: String,
        val appName: String,
        val fallbackPackages: List<String> = emptyList(),
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Launch $appName"
    ) : TaskCommand

    data class ClickElement(
        val selector: TargetSelector,
        val targetLabel: String = selector.toReadableDescription(),
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Click [$targetLabel]"
    ) : TaskCommand

    data class EnterText(
        val selector: TargetSelector,
        val textToEnter: String,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Enter '$textToEnter' into [${selector.toReadableDescription()}]"
    ) : TaskCommand

    data class WaitForElement(
        val selector: TargetSelector,
        val timeoutMs: Long = 5000L,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Wait for [${selector.toReadableDescription()}]"
    ) : TaskCommand

    data class VerifyCondition(
        val rule: VerificationRule,
        val requirementDescription: String,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Verify: $requirementDescription"
    ) : TaskCommand

    data class Scroll(
        val direction: ScrollDirection,
        val distanceFraction: Float = 0.5f,
        val containerSelector: TargetSelector? = null,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Scroll ${direction.name.lowercase()} (${(distanceFraction * 100).toInt()}%)"
    ) : TaskCommand

    data class OpenDrawer(
        val drawerToggleSelector: TargetSelector? = null,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Open Navigation Drawer"
    ) : TaskCommand

    data class CloseDrawer(
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Close Navigation Drawer"
    ) : TaskCommand

    data class SwipeGesture(
        val startXRatio: Float,
        val startYRatio: Float,
        val endXRatio: Float,
        val endYRatio: Float,
        val durationMs: Long = 300L,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Swipe ($startXRatio, $startYRatio) -> ($endXRatio, $endYRatio)"
    ) : TaskCommand

    data class Delay(
        val durationMs: Long,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Wait ${durationMs}ms"
    ) : TaskCommand

    data class ControlActuator(
        val actuatorType: ActuatorType,
        val param: String = "",
        val durationMs: Long = 200L,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Actuator: ${actuatorType.label}"
    ) : TaskCommand

    data class StartTimer(
        val durationSeconds: Int,
        val timerLabel: String = "Agent Timer",
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Start Timer: ${durationSeconds}s ($timerLabel)"
    ) : TaskCommand

    data class WebResearch(
        val query: String,
        val extractType: String = "movie_list",
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Web Research & Scraping: '$query'"
    ) : TaskCommand

    data class MorningBriefing(
        val dateString: String = "Sunday, October 18, 2026",
        val includeWeather: Boolean = true,
        val includeNews: Boolean = true,
        val includeReminders: Boolean = true,
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Daily Morning Briefing & Night Log (Oct 18, 2026)"
    ) : TaskCommand

    data class AddNote(
        val noteContent: String,
        val title: String = "",
        val appPackage: String = "com.google.android.keep",
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Add Note: '$noteContent'"
    ) : TaskCommand

    data class ExportDurableVault(
        val destinationPath: String = "/sdcard/LocalAgent/brain_vault.json",
        override val id: String = UUID.randomUUID().toString(),
        override val displayName: String = "Export Brain Memory to External Storage"
    ) : TaskCommand
}

enum class FlashlightTarget(val label: String) {
    BACK("Rear Main Flashlight"),
    FRONT("Front Selfie Flashlight"),
    DUAL("Dual 360° Flashlight (Front + Back)")
}

enum class ActuatorType(val label: String) {
    HAPTIC_PULSE("Haptic Vibration Pulse"),
    HAPTIC_BURST("Haptic Double Burst"),
    HAPTIC_TICK("Haptic Single Tick"),
    HAPTIC_IMPACT("Haptic Heavy Impact"),
    TORCH_ON("Torch / Flashlight ON"),
    TORCH_OFF("Torch / Flashlight OFF"),
    TORCH_TOGGLE("Toggle Flashlight"),
    TORCH_FRONT_ON("Front Selfie Flashlight ON"),
    TORCH_BACK_ON("Rear Flashlight ON"),
    TORCH_DUAL_ON("Dual 360° Flashlight ON (Both Front & Back)"),
    TORCH_TIMED("Timed Flashlight with Auto-Off"),
    TORCH_STROBE("Flashlight Strobe Pulse"),
    TORCH_SOS("Flashlight SOS Morse Code"),
    TORCH_WAVE("Flashlight Waving Pulse Wave"),
    AUDIO_TONE("Audible Alert Chime"),
    AUDIO_VOLUME("Audio Volume Control"),
    SPEAKER_ALARM("High-Priority Alert Siren"),
    SPEAKER_CONFIRM("Discrete Confirmation Tone"),
    BRIGHTNESS_LEVEL("Display Backlight Control"),
    NIGHT_SHIELD("Blue Light Eye Shield"),
    BATTERY_SAVER("Aggressive Battery Saver"),
    WIFI_ON("Turn Wi-Fi ON"),
    WIFI_OFF("Turn Wi-Fi OFF"),
    BLUETOOTH_ON("Turn Bluetooth ON"),
    BLUETOOTH_OFF("Turn Bluetooth OFF"),
    SETTINGS_WIFI("Launch Wi-Fi Control"),
    SETTINGS_BLUETOOTH("Launch Bluetooth Control"),
    SETTINGS_AIRPLANE("Launch Airplane Mode Control"),
    SETTINGS_DISPLAY("Launch Display Control"),
    DURABLE_VAULT_SYNC("External Shared Storage Brain Sync")
}

data class SearchHit(
    val title: String,
    val url: String,
    val snippet: String
)

data class ResearchScrapeResult(
    val query: String,
    val extractedCount: Int,
    val hits: List<SearchHit> = emptyList(),
    val rawSummary: String,
    val scrapedAtMs: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val isFailure: Boolean = false,
    val provenance: String = "LIVE_WEB_FETCH",
    val cacheAgeSeconds: Long = 0L
) {
    // Backwards compatibility accessor
    val movies: List<SearchHit> get() = hits
}

data class AgentSkill(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val triggerPhrases: List<String>,
    val category: String = "General",
    val commands: List<TaskCommand> = emptyList(),
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class LiveConsoleEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: ConsoleLogLevel = ConsoleLogLevel.INFO,
    val extraData: String? = null
)

enum class ConsoleLogLevel {
    VERBOSE, DEBUG, INFO, ACTION, OBSERVATION, ACTUATOR, WARNING, ERROR
}

data class ExternalObservationData(
    val timestampMs: Long = System.currentTimeMillis(),
    val foregroundPackage: String = "com.example",
    val windowTitle: String = "LocalAgent Dashboard",
    val displayMetrics: String = "1080x2400 @ 440dpi (60Hz)",
    val batteryPct: Int = 85,
    val isCharging: Boolean = false,
    val networkMode: String = "Network Isolated / Airplane Mode Capable",
    val memoryUsageMb: Long = 42L,
    val thermalState: String = "Nominal (Safe Execution)"
)

data class DurableMemoryBackup(
    val version: Int = 1,
    val timestampMs: Long = System.currentTimeMillis(),
    val taskCount: Int = 0,
    val deviceModel: String = "TECNO IN6",
    val checksum: String = ""
)

enum class ActionType {
    TAP,
    SWIPE,
    ENTER_TEXT,
    SCROLL,
    OPEN_DRAWER,
    CLOSE_DRAWER,
    ACTUATOR
}

data class RecordedInteraction(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val actionType: ActionType,
    val targetLabel: String,
    val targetSelector: TargetSelector? = null,
    val targetBounds: RectData? = null,
    val touchCoordinates: Pair<Int, Int>? = null,
    val textPayload: String? = null,
    val scrollDirection: ScrollDirection? = null,
    val packageName: String = "",
    val snapshotSummary: String? = null,
    val delayFromPreviousMs: Long = 0L
)

data class SynthesizedTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val commands: List<TaskCommand>,
    val rawInteractionsCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class UiNode(
    val id: String,
    val packageName: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val bounds: RectData,
    val isClickable: Boolean,
    val isLongClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean,
    val isEnabled: Boolean,
    val isFocused: Boolean,
    val isVisibleToUser: Boolean,
    val depth: Int,
    val children: List<UiNode> = emptyList()
) {
    fun findNodes(predicate: (UiNode) -> Boolean): List<UiNode> {
        val result = mutableListOf<UiNode>()
        if (predicate(this)) {
            result.add(this)
        }
        for (child in children) {
            result.addAll(child.findNodes(predicate))
        }
        return result
    }

    val allDescendants: List<UiNode> get() {
        val list = mutableListOf(this)
        for (child in children) {
            list.addAll(child.allDescendants)
        }
        return list
    }
}

data class SnapshotComparisonDiff(
    val preSnapshot: UiSnapshot?,
    val postSnapshot: UiSnapshot?,
    val addedCount: Int,
    val removedCount: Int,
    val mutatedNodes: List<Pair<UiNode, UiNode>>,
    val summary: String,
    val addedNodes: List<UiNode> = emptyList(),
    val removedNodes: List<UiNode> = emptyList(),
    val unchangedCount: Int = 0
)

data class UiSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val monotonicTimestampMs: Long = timestampMs,
    val generationId: Long = 1L,
    val windowId: Int = 0,
    val packageName: String,
    val windowTitle: String? = null,
    val rootNode: UiNode?,
    val totalNodeCount: Int,
    val isLive: Boolean,
    val isInteractiveRoot: Boolean = true,
    val sourceDescription: String = if (isLive) "Live Accessibility Observation" else "Local Diagnostic Harness"
) {
    val allNodes: List<UiNode> get() = rootNode?.allDescendants ?: emptyList()
}

enum class ExecutionStatus {
    IDLE,
    PARSING,
    RESOLVING_TARGET,
    EXECUTING_ACTION,
    WAITING_OBSERVATION,
    VERIFYING,
    COMPLETED_SUCCESS,
    COMPLETED_FAILURE,
    ABORTED_BY_USER,
    TIMEOUT
}

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILURE,
    TIMEOUT,
    INCONCLUSIVE,
    SKIPPED
}

data class StepExecutionRecord(
    val stepIndex: Int,
    val commandSummary: String,
    val targetSelector: String?,
    val dispatchedAction: String?,
    val preSnapshotId: String?,
    val postSnapshotId: String?,
    val status: StepStatus,
    val verificationDetails: String,
    val durationMs: Long,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TargetMatch(
    val node: UiNode,
    val score: Int,
    val matchReason: String,
    val isAncestorFallback: Boolean = false,
    val clickCoordinates: Pair<Int, Int>? = null
)

data class VerificationOutcome(
    val status: StepStatus,
    val reason: String,
    val preValue: String? = null,
    val postValue: String? = null,
    val confidence: Float = 1.0f
)

data class DeviceCapabilities(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val isAccessibilityEnabled: Boolean,
    val canDrawOverlays: Boolean,
    val isIgnoringBatteryOptimizations: Boolean
)

data class OfflineDiagnosticResult(
    val isRunning: Boolean = false,
    val executedAt: Long = 0L,
    val parserPassed: Boolean = false,
    val parserLatencyMs: Long = 0L,
    val resolverPassed: Boolean = false,
    val resolverLatencyMs: Long = 0L,
    val executorPassed: Boolean = false,
    val executorLatencyMs: Long = 0L,
    val verifierPassed: Boolean = false,
    val verifierLatencyMs: Long = 0L,
    val storagePassed: Boolean = false,
    val storageLatencyMs: Long = 0L,
    val allPassed: Boolean = false,
    val summary: String = "No self-test run yet."
)

enum class CognitivePhase(val label: String, val icon: String) {
    PERCEPTION("Perception", "👁️"),
    SITUATION_ASSESSMENT("Context Assessment", "🧠"),
    HYPOTHESIS("Hypothesis & Strategy", "💡"),
    SAFETY_CHECK("Safety & Kill Switch", "🛡️"),
    ACTION_SYNTHESIS("Action Synthesis", "⚡"),
    REFLECTION("Post-Action Reflection", "🔍")
}

data class CognitiveThought(
    val stepIndex: Int,
    val phase: CognitivePhase,
    val title: String,
    val reasoning: String,
    val observationHighlights: List<String> = emptyList(),
    val confidence: Float = 0.95f,
    val timestampMs: Long = System.currentTimeMillis()
)
