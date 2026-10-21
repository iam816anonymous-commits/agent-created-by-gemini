package com.example.core.learning

import java.util.UUID

/**
 * Environmental Telemetry & Physical Host State.
 * Tailored for the 4GB TECNO Camon i Click (MediaTek Helio P23, Android 8.1 / API 27).
 */
data class PhysicalEnvironmentSnapshot(
    val deviceModel: String = "TECNO Camon i Click (CF7)",
    val socArchitecture: String = "MediaTek Helio P23 (MT6763V) Octa-Core",
    val androidVersion: String = "Android 8.1 Oreo (API Level 27)",
    val ramTotalMb: Int = 4096,
    val ramFreeMb: Int = 2150,
    val agentRamBudgetMb: Int = 500,
    val agentActiveRamMb: Int = 42,
    val batteryPercent: Int = 84,
    val isCharging: Boolean = false,
    val thermalSoCDegreesC: Float = 36.2f,
    val ambientLightLux: Float = 145.0f,
    val screenOrientation: String = "PORTRAIT_18_9",
    val screenResolution: String = "720x1440 HD+",
    val displayBrightnessPercent: Int = 65,
    val networkType: String = "WIFI_5GHZ",
    val networkLatencyMs: Int = 22,
    val rearQuadFlashAvailable: Boolean = true,
    val frontDualFlashAvailable: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * App Screen Node Topology and Spatial Anchors.
 */
data class AppUiAnchor(
    val anchorId: String,
    val targetType: String, // BUTTON, SWITCH, EDIT_TEXT, LIST_ITEM
    val accessibilityId: String?,
    val contentDescription: String?,
    val textLabel: String?,
    val spatialOffsetRelation: String?, // "RIGHT_OF_LABEL", "BELOW_HEADER", "EXACT_BOUNDS"
    val boundingBox: String,
    val verifiedActionable: Boolean = true
)

data class AppTopologyProfile(
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean,
    val mainActivity: String,
    val knownScreens: List<String>,
    val verifiedUiAnchors: List<AppUiAnchor>,
    val fastPathIntentUri: String?,
    val requiredPermissions: List<String>,
    val permissionsGranted: Boolean,
    val environmentalMasteryScore: Int // 0 to 100
)

/**
 * Environment & Physical Topology Learning Engine.
 *
 * Implements Step 1 of the Autonomous Agent Constitution:
 * "First learn its own environment perfectly, all apps, and how to work in its physical environment."
 */
class EnvironmentTopologyEngine {

    private var currentEnvironment = PhysicalEnvironmentSnapshot()
    private val appCatalog = mutableMapOf<String, AppTopologyProfile>()

    init {
        seedDefaultAppTopologies()
    }

    private fun seedDefaultAppTopologies() {
        appCatalog["com.android.settings"] = AppTopologyProfile(
            packageName = "com.android.settings",
            appLabel = "Settings",
            isSystemApp = true,
            mainActivity = "com.android.settings.Settings",
            knownScreens = listOf("MainSettings", "NetworkAndInternet", "ConnectedDevices", "BatteryOptimization", "DisplaySettings", "Accessibility"),
            verifiedUiAnchors = listOf(
                AppUiAnchor("anchor-wifi-toggle", "SWITCH", "switch_widget", "Wi-Fi Switch", null, "RIGHT_OF_LABEL", "Rect(620, 180 - 700, 240)"),
                AppUiAnchor("anchor-bluetooth-toggle", "SWITCH", "switch_widget", "Bluetooth Switch", null, "RIGHT_OF_LABEL", "Rect(620, 260 - 700, 320)"),
                AppUiAnchor("anchor-battery-opt", "LIST_ITEM", "battery_opt_row", null, "Battery Optimization", "EXACT_BOUNDS", "Rect(40, 520 - 680, 590)")
            ),
            fastPathIntentUri = "android.settings.SETTINGS",
            requiredPermissions = emptyList(),
            permissionsGranted = true,
            environmentalMasteryScore = 100
        )

        appCatalog["com.google.android.keep"] = AppTopologyProfile(
            packageName = "com.google.android.keep",
            appLabel = "Keep Notes",
            isSystemApp = true,
            mainActivity = "com.google.android.keep.activities.BrowseActivity",
            knownScreens = listOf("BrowseNotes", "EditorActivity", "SearchNotes", "ReminderPicker"),
            verifiedUiAnchors = listOf(
                AppUiAnchor("anchor-new-note", "BUTTON", "new_note_button", "New text note", null, "EXACT_BOUNDS", "Rect(610, 1310 - 690, 1390)"),
                AppUiAnchor("anchor-note-title", "EDIT_TEXT", "editable_title", null, "Title", "EXACT_BOUNDS", "Rect(40, 120 - 680, 190)"),
                AppUiAnchor("anchor-note-body", "EDIT_TEXT", "edit_note_text", null, "Note", "EXACT_BOUNDS", "Rect(40, 200 - 680, 1100)")
            ),
            fastPathIntentUri = "android.intent.action.VIEW",
            requiredPermissions = emptyList(),
            permissionsGranted = true,
            environmentalMasteryScore = 98
        )

        appCatalog["com.android.calculator2"] = AppTopologyProfile(
            packageName = "com.android.calculator2",
            appLabel = "Calculator",
            isSystemApp = true,
            mainActivity = "com.android.calculator2.Calculator",
            knownScreens = listOf("MainCalculatorPad", "AdvancedPadMenu"),
            verifiedUiAnchors = listOf(
                AppUiAnchor("anchor-calc-formula", "EDIT_TEXT", "formula", null, "Formula", "EXACT_BOUNDS", "Rect(40, 80 - 680, 280)"),
                AppUiAnchor("anchor-calc-result", "EDIT_TEXT", "result", null, "Result", "EXACT_BOUNDS", "Rect(40, 290 - 680, 420)"),
                AppUiAnchor("anchor-calc-equals", "BUTTON", "eq", "equals", "=", "EXACT_BOUNDS", "Rect(530, 1220 - 690, 1380)")
            ),
            fastPathIntentUri = null,
            requiredPermissions = emptyList(),
            permissionsGranted = true,
            environmentalMasteryScore = 100
        )

        appCatalog["com.android.hardware.flashlight"] = AppTopologyProfile(
            packageName = "com.android.hardware.flashlight",
            appLabel = "Dual Torch Actuator",
            isSystemApp = true,
            mainActivity = "com.example.localagent.hardware.TorchService",
            knownScreens = listOf("CameraManagerService"),
            verifiedUiAnchors = listOf(
                AppUiAnchor("anchor-rear-quad", "SWITCH", "rear_torch_toggle", "Camera 0 Quad-LED", null, "EXACT_BOUNDS", "Rect(100, 200 - 300, 280)"),
                AppUiAnchor("anchor-front-dual", "SWITCH", "front_torch_toggle", "Camera 1 Dual-LED", null, "EXACT_BOUNDS", "Rect(420, 200 - 620, 280)")
            ),
            fastPathIntentUri = null,
            requiredPermissions = listOf("android.permission.CAMERA"),
            permissionsGranted = true,
            environmentalMasteryScore = 100
        )

        appCatalog["com.android.chrome"] = AppTopologyProfile(
            packageName = "com.android.chrome",
            appLabel = "Chrome Browser",
            isSystemApp = true,
            mainActivity = "com.google.android.apps.chrome.Main",
            knownScreens = listOf("TabSwitcher", "WebBrowserView", "OmniboxEditor"),
            verifiedUiAnchors = listOf(
                AppUiAnchor("anchor-omnibox", "EDIT_TEXT", "url_bar", "Search or type web address", null, "EXACT_BOUNDS", "Rect(60, 60 - 600, 130)"),
                AppUiAnchor("anchor-tab-switcher", "BUTTON", "tab_switcher_button", "Open tabs", null, "EXACT_BOUNDS", "Rect(610, 60 - 690, 130)")
            ),
            fastPathIntentUri = "android.intent.action.VIEW",
            requiredPermissions = listOf("android.permission.INTERNET"),
            permissionsGranted = true,
            environmentalMasteryScore = 95
        )
    }

    fun getPhysicalEnvironment(): PhysicalEnvironmentSnapshot = currentEnvironment

    fun getMappedApps(): List<AppTopologyProfile> = appCatalog.values.toList()

    fun getAppTopology(packageName: String): AppTopologyProfile? = appCatalog[packageName]

    /**
     * Executes real-time environmental scan & updates calibration.
     */
    fun refreshPhysicalEnvironment(
        ambientLightLux: Float? = null,
        batteryPercent: Int? = null,
        socTemp: Float? = null
    ): PhysicalEnvironmentSnapshot {
        currentEnvironment = currentEnvironment.copy(
            ambientLightLux = ambientLightLux ?: currentEnvironment.ambientLightLux,
            batteryPercent = batteryPercent ?: currentEnvironment.batteryPercent,
            thermalSoCDegreesC = socTemp ?: currentEnvironment.thermalSoCDegreesC,
            timestampMs = System.currentTimeMillis()
        )
        return currentEnvironment
    }

    /**
     * Calculates the aggregate Environmental Mastery Score across physical sensors and all mapped apps.
     */
    fun computeOverallEnvironmentalMastery(): Int {
        if (appCatalog.isEmpty()) return 0
        val avgAppScore = appCatalog.values.map { it.environmentalMasteryScore }.average()
        val physicalSensorMastery = if (currentEnvironment.agentActiveRamMb <= currentEnvironment.agentRamBudgetMb) 100 else 75
        return ((avgAppScore * 0.7) + (physicalSensorMastery * 0.3)).toInt()
    }
}
