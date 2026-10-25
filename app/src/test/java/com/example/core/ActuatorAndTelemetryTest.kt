package com.example.core

import com.example.core.model.ActuatorType
import com.example.core.model.DurableMemoryBackup
import com.example.core.model.TaskCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActuatorAndTelemetryTest {

    @Test
    fun testDurableMemoryBackup_defaultDeviceModelEvaluatesNonEmpty() {
        val backup = DurableMemoryBackup()
        assertNotNull(backup.deviceModel)
        assertTrue("Device model string must not be empty", backup.deviceModel.isNotBlank())
    }

    @Test
    fun testActuatorTypeEnum_allNewActuatorTypesAreDefined() {
        val types = ActuatorType.values()
        assertTrue(types.contains(ActuatorType.HAPTIC_TICK))
        assertTrue(types.contains(ActuatorType.HAPTIC_IMPACT))
        assertTrue(types.contains(ActuatorType.TORCH_STROBE))
        assertTrue(types.contains(ActuatorType.TORCH_SOS))
        assertTrue(types.contains(ActuatorType.SPEAKER_CONFIRM))
        assertTrue(types.contains(ActuatorType.SPEAKER_ALARM))
        assertTrue(types.contains(ActuatorType.BRIGHTNESS_LEVEL))
        assertTrue(types.contains(ActuatorType.NIGHT_SHIELD))
        assertTrue(types.contains(ActuatorType.BATTERY_SAVER))
    }

    @Test
    fun testControlActuatorCommand_creationWithParam() {
        val cmd = TaskCommand.ControlActuator(
            actuatorType = ActuatorType.TORCH_TIMED,
            param = "front 30",
            durationMs = 30000L
        )
        assertEquals(ActuatorType.TORCH_TIMED, cmd.actuatorType)
        assertEquals("front 30", cmd.param)
        assertEquals(30000L, cmd.durationMs)
    }
}
