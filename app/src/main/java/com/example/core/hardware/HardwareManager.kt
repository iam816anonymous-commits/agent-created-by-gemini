package com.example.core.hardware

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.core.actuator.ActuatorResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveSensorTelemetry(
    val accelerometerX: Float = 0f,
    val accelerometerY: Float = 9.8f,
    val accelerometerZ: Float = 0f,
    val ambientLightLux: Float = 120f,
    val proximityDistanceCm: Float = 5.0f,
    val isProximityNear: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasLightSensor: Boolean = false,
    val hasProximitySensor: Boolean = false,
    val hasBarometer: Boolean = false,
    val pressureHpa: Float = 1013.25f,
    val lastUpdateMs: Long = System.currentTimeMillis()
)

class HardwareManager(private val context: Context) : SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default)

    // Hardware Managers
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val bluetoothAdapter: BluetoothAdapter? = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
    private val cameraManager: CameraManager? = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // StateFlows for UI reflection
    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isTorchEnabled = MutableStateFlow(false)
    val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    private val _liveSensors = MutableStateFlow(LiveSensorTelemetry())
    val liveSensors: StateFlow<LiveSensorTelemetry> = _liveSensors.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        refreshHardwareStates()
        findCameraWithFlash()
        registerStateReceivers()
        registerSensors()
    }

    fun refreshHardwareStates() {
        // Wi-Fi State
        try {
            val wifiState = wifiManager?.isWifiEnabled == true
            _isWifiEnabled.value = wifiState
        } catch (_: Exception) {}

        // Bluetooth State
        try {
            val btState = bluetoothAdapter?.isEnabled == true
            _isBluetoothEnabled.value = btState
        } catch (_: Exception) {}
    }

    private fun registerStateReceivers() {
        try {
            val filter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    when (intent?.action) {
                        WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                            _isWifiEnabled.value = (state == WifiManager.WIFI_STATE_ENABLED)
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                        }
                    }
                }
            }, filter)
        } catch (_: Exception) {}
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.let { manager ->
                for (id in manager.cameraIdList) {
                    val chars = manager.getCameraCharacteristics(id)
                    val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdWithFlash = id
                        break
                    }
                }
                if (cameraIdWithFlash == null && manager.cameraIdList.isNotEmpty()) {
                    for (id in manager.cameraIdList) {
                        val chars = manager.getCameraCharacteristics(id)
                        if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                            cameraIdWithFlash = id
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun registerSensors() {
        sensorManager?.let { sm ->
            val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val light = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            val baro = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)

            _liveSensors.value = _liveSensors.value.copy(
                hasAccelerometer = accel != null,
                hasLightSensor = light != null,
                hasProximitySensor = prox != null,
                hasBarometer = baro != null
            )

            accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            light?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            prox?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            baro?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                _liveSensors.value = _liveSensors.value.copy(
                    accelerometerX = event.values.getOrNull(0) ?: 0f,
                    accelerometerY = event.values.getOrNull(1) ?: 9.8f,
                    accelerometerZ = event.values.getOrNull(2) ?: 0f,
                    lastUpdateMs = System.currentTimeMillis()
                )
            }
            Sensor.TYPE_LIGHT -> {
                _liveSensors.value = _liveSensors.value.copy(
                    ambientLightLux = event.values.getOrNull(0) ?: 0f,
                    lastUpdateMs = System.currentTimeMillis()
                )
            }
            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values.getOrNull(0) ?: 5f
                _liveSensors.value = _liveSensors.value.copy(
                    proximityDistanceCm = dist,
                    isProximityNear = dist < 4f,
                    lastUpdateMs = System.currentTimeMillis()
                )
            }
            Sensor.TYPE_PRESSURE -> {
                _liveSensors.value = _liveSensors.value.copy(
                    pressureHpa = event.values.getOrNull(0) ?: 1013.25f,
                    lastUpdateMs = System.currentTimeMillis()
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ==========================================
    // HARDWARE WI-FI ACTUATION LOGIC
    // ==========================================
    @Suppress("DEPRECATION")
    fun setWifiEnabled(enabled: Boolean): ActuatorResult {
        val hasChangePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        var directSuccess = false

        if (wifiManager != null && hasChangePerm) {
            try {
                directSuccess = wifiManager.setWifiEnabled(enabled)
                if (directSuccess) {
                    _isWifiEnabled.value = enabled
                    return ActuatorResult(true, "Wi-Fi ${if (enabled) "turned ON" else "turned OFF"} via direct Hardware API")
                }
            } catch (e: Exception) {
                // Restricted on API 29+
            }
        }

        // Modern API fallback (API 29+ / Q)
        return try {
            val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            context.startActivity(panelIntent)
            _isWifiEnabled.value = enabled // optimistic update
            ActuatorResult(true, "Wi-Fi control panel launched (requested state: ${if (enabled) "ON" else "OFF"})")
        } catch (e: Exception) {
            openSystemSetting(Settings.ACTION_WIFI_SETTINGS)
            ActuatorResult(true, "Launched Wi-Fi system settings: ${e.message}")
        }
    }

    fun toggleWifi(): ActuatorResult {
        return setWifiEnabled(!_isWifiEnabled.value)
    }

    // ==========================================
    // HARDWARE BLUETOOTH ACTUATION LOGIC
    // ==========================================
    @Suppress("DEPRECATION")
    fun setBluetoothEnabled(enabled: Boolean): ActuatorResult {
        var directSuccess = false
        val hasAdminPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED

        if (bluetoothAdapter != null && hasAdminPerm) {
            try {
                directSuccess = if (enabled) bluetoothAdapter.enable() else bluetoothAdapter.disable()
                if (directSuccess) {
                    _isBluetoothEnabled.value = enabled
                    return ActuatorResult(true, "Bluetooth ${if (enabled) "turned ON" else "turned OFF"} via Hardware Adapter")
                }
            } catch (_: Exception) {
                // Restricted on modern API without system signature or BLUETOOTH_CONNECT
            }
        }

        return try {
            if (enabled) {
                val reqIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(reqIntent)
                    _isBluetoothEnabled.value = true
                    ActuatorResult(true, "Bluetooth enable authorization prompt triggered")
                } catch (_: Exception) {
                    openSystemSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
                }
            } else {
                openSystemSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
        } catch (e: Exception) {
            openSystemSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
            ActuatorResult(true, "Launched Bluetooth system settings: ${e.message}")
        }
    }

    fun toggleBluetooth(): ActuatorResult {
        return setBluetoothEnabled(!_isBluetoothEnabled.value)
    }

    // ==========================================
    // SENSORY & TACTILE ACTUATORS
    // ==========================================
    fun vibrateHaptic(durationMs: Long = 150L): ActuatorResult {
        return try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
                ActuatorResult(true, "Tactile actuator vibrated for ${durationMs}ms")
            } else {
                ActuatorResult(false, "Vibrator actuator unavailable on host")
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
                ActuatorResult(true, "Tactile burst sequence dispatched")
            } else {
                ActuatorResult(false, "Vibrator unavailable")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Vibration pattern error: ${e.message}")
        }
    }

    fun setTorch(enabled: Boolean): ActuatorResult {
        return try {
            if (cameraIdWithFlash == null) findCameraWithFlash()
            val id = cameraIdWithFlash
            if (id != null && cameraManager != null) {
                cameraManager.setTorchMode(id, enabled)
                _isTorchEnabled.value = enabled
                ActuatorResult(true, "LED Torch ${if (enabled) "ON" else "OFF"}")
            } else {
                _isTorchEnabled.value = enabled
                ActuatorResult(true, "LED Flashlight state simulated: ${if (enabled) "ON" else "OFF"}")
            }
        } catch (e: Exception) {
            ActuatorResult(false, "Torch actuation error: ${e.message}")
        }
    }

    fun toggleTorch(): ActuatorResult {
        return setTorch(!_isTorchEnabled.value)
    }

    fun playTone(toneType: Int = ToneGenerator.TONE_PROP_BEEP, durationMs: Int = 180): ActuatorResult {
        return try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(toneType, durationMs)
            ActuatorResult(true, "Audible chime signal dispatched (${durationMs}ms)")
        } catch (e: Exception) {
            ActuatorResult(false, "Tone actuator error: ${e.message}")
        }
    }

    fun setVolume(percent: Int): ActuatorResult {
        return try {
            audioManager?.let { am ->
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                ActuatorResult(true, "Audio stream set to $percent% ($target/$max)")
            } ?: ActuatorResult(false, "AudioManager unavailable")
        } catch (e: Exception) {
            ActuatorResult(false, "Volume setting failed: ${e.message}")
        }
    }

    fun openSystemSetting(action: String): ActuatorResult {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActuatorResult(true, "Launched intent: ${action.substringAfterLast(".")}")
        } catch (e: Exception) {
            ActuatorResult(false, "Intent launch failed: ${e.message}")
        }
    }
}
