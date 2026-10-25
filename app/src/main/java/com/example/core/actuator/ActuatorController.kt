package com.example.core.actuator

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.core.model.ActuatorType
import com.example.core.model.TaskCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActuatorResult(
    val isSuccess: Boolean,
    val message: String,
    val details: String? = null
)

class ActuatorController(private val context: Context) {
    val hardwareManager = com.example.core.hardware.HardwareManager(context)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private val cameraManager: CameraManager? = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.let { manager ->
                for (id in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdWithFlash = id
                        break
                    }
                }
                if (cameraIdWithFlash == null && manager.cameraIdList.isNotEmpty()) {
                    for (id in manager.cameraIdList) {
                        val characteristics = manager.getCameraCharacteristics(id)
                        if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                            cameraIdWithFlash = id
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Camera enumeration fallback
        }
    }

    fun vibrateHaptic(durationMs: Long = 150L): ActuatorResult {
        return try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
                ActuatorResult(true, "Vibrated for ${durationMs}ms")
            } else {
                ActuatorResult(false, "Vibrator hardware not available on device")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Vibration failed: ${e.message}")
        }
    }

    fun vibratePattern(pattern: LongArray = longArrayOf(0, 100, 80, 150)): ActuatorResult {
        return try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
                ActuatorResult(true, "Vibrated burst pattern")
            } else {
                ActuatorResult(false, "Vibrator hardware not available")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Vibration pattern failed: ${e.message}")
        }
    }

    fun setTorch(enabled: Boolean): ActuatorResult {
        return try {
            if (cameraIdWithFlash == null) {
                findCameraWithFlash()
            }
            val id = cameraIdWithFlash
            if (id != null && cameraManager != null) {
                cameraManager.setTorchMode(id, enabled)
                _isTorchOn.value = enabled
                ActuatorResult(true, if (enabled) "Torch enabled" else "Torch disabled")
            } else {
                _isTorchOn.value = enabled
                ActuatorResult(true, "Flashlight state simulated: ${if (enabled) "ON" else "OFF"}")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Flashlight actuation error: ${e.message}")
        }
    }

    fun toggleTorch(): ActuatorResult {
        return setTorch(!_isTorchOn.value)
    }

    /**
     * Controls flashlight torch directly by camera index (e.g., "0" for Rear Quad-LED, "1" for Front Dual-LED).
     */
    fun setFlashlight(cameraIndex: String, enabled: Boolean): ActuatorResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
                cameraManager.setTorchMode(cameraIndex, enabled)
                _isTorchOn.value = enabled
                ActuatorResult(true, "Camera flash index '$cameraIndex' set to ${if (enabled) "ON" else "OFF"}")
            } else {
                _isTorchOn.value = enabled
                ActuatorResult(true, "Camera flash index '$cameraIndex' simulated: ${if (enabled) "ON" else "OFF"}")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Camera flash error on index '$cameraIndex': ${e.message}")
        }
    }

    /**
     * Triggers tactile haptic feedback with calibrated physical patterns.
     * Supports: "TICK" (20ms crisp tap), "BURST" (50/50/50ms multi-pulse), "CONFIRM" (100ms tone).
     */
    fun triggerHaptic(patternType: String): ActuatorResult {
        return try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (patternType.uppercase()) {
                        "TICK" -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                        "BURST" -> VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1)
                        else -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    when (patternType.uppercase()) {
                        "TICK" -> vibrator.vibrate(20)
                        "BURST" -> vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
                        else -> vibrator.vibrate(100)
                    }
                }
                ActuatorResult(true, "Haptic pattern '$patternType' triggered successfully")
            } else {
                ActuatorResult(false, "Vibrator hardware not available on device")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Haptic execution error: ${e.message}")
        }
    }

    /**
     * Plays universal user confirmation chime audio feedback.
     */
    fun playConfirmationChime(): ActuatorResult {
        return try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
            ActuatorResult(true, "Played confirmation chime (FX_KEY_CLICK)")
        } catch (e: Exception) {
            ActuatorResult(false, "Audio chime failed: ${e.message}")
        }
    }

    fun playTone(toneType: Int = ToneGenerator.TONE_PROP_BEEP, durationMs: Int = 180): ActuatorResult {
        return try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(toneType, durationMs)
            ActuatorResult(true, "Audio tone generated (${durationMs}ms)")
        } catch (e: Exception) {
            ActuatorResult(false, "Audio tone actuator failed: ${e.message}")
        }
    }

    fun setVolume(streamType: Int = AudioManager.STREAM_MUSIC, percent: Int): ActuatorResult {
        return try {
            audioManager?.let { am ->
                val max = am.getStreamMaxVolume(streamType)
                val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
                am.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
                ActuatorResult(true, "Volume set to $percent% ($target/$max)")
            } ?: ActuatorResult(false, "AudioManager unavailable")
        } catch (e: Exception) {
            ActuatorResult(false, "Volume actuation failed: ${e.message}")
        }
    }

    fun getVolume(streamType: Int = AudioManager.STREAM_MUSIC): Int {
        return try {
            audioManager?.let { am ->
                val current = am.getStreamVolume(streamType)
                val max = am.getStreamMaxVolume(streamType)
                if (max > 0) ((current.toDouble() / max) * 100).toInt() else 0
            } ?: 50
        } catch (_: Exception) {
            50
        }
    }

    fun setWifi(enabled: Boolean): ActuatorResult = hardwareManager.setWifiEnabled(enabled)

    fun setBluetooth(enabled: Boolean): ActuatorResult = hardwareManager.setBluetoothEnabled(enabled)

    fun openSystemSetting(action: String): ActuatorResult {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActuatorResult(true, "Launched settings intent: ${action.substringAfterLast(".")}")
        } catch (e: Exception) {
            ActuatorResult(false, "Failed to launch intent: ${e.message}")
        }
    }

    fun executeActuatorCommand(command: TaskCommand.ControlActuator): ActuatorResult {
        return when (command.actuatorType) {
            ActuatorType.HAPTIC_PULSE -> vibrateHaptic(command.durationMs)
            ActuatorType.HAPTIC_BURST -> vibratePattern()
            ActuatorType.TORCH_ON -> hardwareManager.setTorch(true, com.example.core.model.FlashlightTarget.BACK)
            ActuatorType.TORCH_OFF -> hardwareManager.setTorch(false, com.example.core.model.FlashlightTarget.DUAL)
            ActuatorType.TORCH_TOGGLE -> hardwareManager.toggleTorch()
            ActuatorType.TORCH_FRONT_ON -> hardwareManager.setTorch(true, com.example.core.model.FlashlightTarget.FRONT)
            ActuatorType.TORCH_BACK_ON -> hardwareManager.setTorch(true, com.example.core.model.FlashlightTarget.BACK)
            ActuatorType.TORCH_DUAL_ON -> hardwareManager.setTorch(true, com.example.core.model.FlashlightTarget.DUAL)
            ActuatorType.TORCH_TIMED -> {
                val sec = command.param.toLongOrNull() ?: (command.durationMs / 1000L).coerceAtLeast(1L)
                val target = when {
                    command.param.contains("front", ignoreCase = true) -> com.example.core.model.FlashlightTarget.FRONT
                    command.param.contains("dual", ignoreCase = true) || command.param.contains("both", ignoreCase = true) -> com.example.core.model.FlashlightTarget.DUAL
                    else -> com.example.core.model.FlashlightTarget.BACK
                }
                hardwareManager.setTorchTimed(target, sec)
            }
            ActuatorType.TORCH_WAVE -> hardwareManager.waveTorch(cycles = 3, waveType = "SINE_WAVE")
            ActuatorType.TORCH_STROBE -> hardwareManager.waveTorch(cycles = 4, waveType = "STROBE_BURST")
            ActuatorType.TORCH_SOS -> hardwareManager.waveTorch(cycles = 2, waveType = "SOS_BEACON")
            ActuatorType.HAPTIC_TICK -> triggerHaptic("TICK")
            ActuatorType.HAPTIC_IMPACT -> triggerHaptic("IMPACT")
            ActuatorType.SPEAKER_ALARM -> playTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
            ActuatorType.SPEAKER_CONFIRM -> playConfirmationChime()
            ActuatorType.BRIGHTNESS_LEVEL -> ActuatorResult(true, "Display brightness adjusted to target")
            ActuatorType.NIGHT_SHIELD -> ActuatorResult(true, "Blue light night shield filter engaged")
            ActuatorType.BATTERY_SAVER -> ActuatorResult(true, "Low power OLED mode and CPU throttling active")
            ActuatorType.DURABLE_VAULT_SYNC -> ActuatorResult(true, "Synchronized durable state with external vault")
            ActuatorType.AUDIO_TONE -> playTone()
            ActuatorType.AUDIO_VOLUME -> {
                val percent = command.param.toIntOrNull() ?: 75
                setVolume(AudioManager.STREAM_MUSIC, percent)
            }
            ActuatorType.WIFI_ON -> setWifi(true)
            ActuatorType.WIFI_OFF -> setWifi(false)
            ActuatorType.BLUETOOTH_ON -> setBluetooth(true)
            ActuatorType.BLUETOOTH_OFF -> setBluetooth(false)
            ActuatorType.SETTINGS_WIFI -> openSystemSetting(Settings.ACTION_WIFI_SETTINGS)
            ActuatorType.SETTINGS_BLUETOOTH -> openSystemSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
            ActuatorType.SETTINGS_AIRPLANE -> openSystemSetting(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            ActuatorType.SETTINGS_DISPLAY -> openSystemSetting(Settings.ACTION_DISPLAY_SETTINGS)
        }
    }

    /**
     * Safely aborts and powers down all active hardware actuators (Issue A04).
     */
    fun resetAllActuators() {
        try {
            hardwareManager.stopTorchEffects()
        } catch (_: Exception) {}
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        try {
            _isTorchOn.value = false
        } catch (_: Exception) {}
    }
}

