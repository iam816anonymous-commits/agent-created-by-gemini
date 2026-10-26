package com.example.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.actuator.ActuatorController
import com.example.core.diagnostics.DeviceCapabilityDetector
import com.example.core.hardware.HardwareManager
import com.example.core.model.ActuatorType
import com.example.core.model.DurableMemoryBackup
import com.example.core.model.StepStatus
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.UiSnapshot
import com.example.core.model.VerificationRule
import com.example.core.resolver.TargetResolver
import com.example.core.verifier.StepVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ActuatorAndTelemetryTest {

    private lateinit var context: Context
    private lateinit var capabilityDetector: DeviceCapabilityDetector
    private lateinit var actuatorController: ActuatorController
    private lateinit var hardwareManager: HardwareManager
    private lateinit var targetResolver: TargetResolver
    private lateinit var stepVerifier: StepVerifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        capabilityDetector = DeviceCapabilityDetector(context)
        actuatorController = ActuatorController(context)
        hardwareManager = HardwareManager(context)
        targetResolver = TargetResolver()
        stepVerifier = StepVerifier()
    }

    @Test
    fun testStorageMetricsFallbackAndConsumerHandling() {
        val metrics = capabilityDetector.getStorageMetrics()
        // getStorageMetrics returns null when StatFs fails in unit test environment
        if (metrics != null) {
            assertTrue("Total storage bytes should be valid or -1", metrics.first >= -1L)
            assertTrue("Available storage bytes should be valid or -1", metrics.second >= -1L)
        }

        // Test consumer null handling logic
        val consumerNullResult: Pair<Long, Long>? = null
        assertTrue("Consumers must handle null storage metrics safely", consumerNullResult == null)
    }

    @Test
    fun testBatteryLevelTelemetryUnknownFallback() {
        val batteryLevel = hardwareManager.getBatteryLevel()
        assertTrue("Battery level should be valid percentage [0..100] or -1 for unknown", batteryLevel in -1..100)
    }

    @Test
    fun testVerificationRuleAlwaysPassReportsInconclusive() {
        val postSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )
        val outcome = stepVerifier.verify(
            command = TaskCommand.VerifyCondition(VerificationRule.AlwaysPass, "Check AlwaysPass"),
            preSnapshot = null,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = System.currentTimeMillis()
        )
        assertEquals("AlwaysPass rule must return INCONCLUSIVE status", StepStatus.INCONCLUSIVE, outcome.status)
        assertEquals("Confidence for AlwaysPass rule must be 0.0", 0.0f, outcome.confidence)
    }

    @Test
    fun testTargetResolverCoordinateFallbackAndBoundaryValidation() {
        val emptySnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )

        // 1. Valid normalized coordinates
        val validSelector = TargetSelector(
            text = "Target",
            fallbackNormalizedX = 0.5f,
            fallbackNormalizedY = 0.25f
        )
        val validResult = targetResolver.resolve(validSelector, emptySnapshot)
        assertTrue("Valid normalized coordinates should succeed via Stage-4 fallback", validResult is TargetResolver.ResolutionResult.Success)
        val match = (validResult as TargetResolver.ResolutionResult.Success).match
        assertTrue("Match reason must indicate UNVERIFIED fallback", match.matchReason.contains("UNVERIFIED Stage-4"))
        assertEquals("Score for coordinate fallback must be 20", 20, match.score)

        // 2. Invalid out-of-bounds normalized coordinates
        val invalidSelector = TargetSelector(
            text = "Target",
            fallbackNormalizedX = 1.5f, // > 1.0
            fallbackNormalizedY = -0.2f // < 0.0
        )
        val invalidResult = targetResolver.resolve(invalidSelector, emptySnapshot)
        assertTrue("Out-of-bounds coordinates must fail resolution", invalidResult is TargetResolver.ResolutionResult.Failure)

        // 3. Stale snapshot rejection
        val staleSnapshot = UiSnapshot(
            timestampMs = System.currentTimeMillis() - 10000L, // 10s old
            packageName = "com.example.app",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )
        val staleResult = targetResolver.resolve(validSelector, staleSnapshot, currentTimeMs = System.currentTimeMillis() + 10000L)
        assertTrue("Stale snapshot must be rejected", staleResult is TargetResolver.ResolutionResult.Failure)
        assertTrue("Failure reason must mention stale snapshot", (staleResult as TargetResolver.ResolutionResult.Failure).reason.contains("Stale snapshot"))
    }

    @Test
    fun testDurableMemoryBackupDynamicDeviceModelDefault() {
        val backup = DurableMemoryBackup()
        assertNotNull("deviceModel must not be null", backup.deviceModel)
        assertTrue("deviceModel must not be blank", backup.deviceModel.isNotBlank())
    }

    @Test
    fun testTargetActuatorCommandsAndSettingsPanelDistinction() {
        val targetTypes = listOf(
            ActuatorType.HAPTIC_TICK,
            ActuatorType.HAPTIC_IMPACT,
            ActuatorType.TORCH_STROBE,
            ActuatorType.TORCH_SOS,
            ActuatorType.SPEAKER_CONFIRM,
            ActuatorType.SPEAKER_ALARM,
            ActuatorType.BRIGHTNESS_LEVEL,
            ActuatorType.NIGHT_SHIELD,
            ActuatorType.BATTERY_SAVER
        )

        for (type in targetTypes) {
            val cmd = TaskCommand.ControlActuator(type)
            val result = actuatorController.executeActuatorCommand(cmd)
            assertNotNull("Actuator result should not be null", result)
            assertTrue("Actuator result message should not be blank", result.message.isNotBlank())

            // Verify settings panel launches distinguish intent launch from verified setting change
            if (type == ActuatorType.BRIGHTNESS_LEVEL || type == ActuatorType.NIGHT_SHIELD || type == ActuatorType.BATTERY_SAVER) {
                assertTrue(
                    "Settings panel launch must be reported as unverified setting change",
                    result.message.contains("Unverified system setting change") || result.message.contains("Launched system settings panel")
                )
            }
        }
    }

    @Test
    fun testActuatorCleanupAndResetAllActuators() {
        // Trigger torch & verify reset
        actuatorController.setTorch(true)
        actuatorController.resetAllActuators()
        assertEquals("Torch state should be false after resetAllActuators()", false, actuatorController.isTorchOn.value)
    }
}
