package com.example.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import com.example.core.model.DeviceCapabilities
import com.example.service.LocalAgentAccessibilityService

class DeviceCapabilityDetector(private val context: Context) {

    fun detectCapabilities(): DeviceCapabilities {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        val isServiceActive = LocalAgentAccessibilityService.isServiceConnected.value || isAccessibilityServiceEnabledInSettings()

        val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        return DeviceCapabilities(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            isAccessibilityEnabled = isServiceActive,
            canDrawOverlays = canDrawOverlays,
            isIgnoringBatteryOptimizations = isIgnoringBattery
        )
    }

    private fun isAccessibilityServiceEnabledInSettings(): Boolean {
        val expectedServiceName = "${context.packageName}/${LocalAgentAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(expectedServiceName) || enabledServices.contains(LocalAgentAccessibilityService::class.java.simpleName)
    }

    fun getStorageMetrics(): Pair<Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val available = stat.availableBytes
            Pair(total, available)
        } catch (e: Exception) {
            Pair(64L * 1024 * 1024 * 1024, 32L * 1024 * 1024 * 1024)
        }
    }
}
