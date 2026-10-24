package com.example.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Dynamic API-Level Capability Registry supporting Android 8.0 through Android 16 (API 26–36+).
 */
object DeviceCapabilityRegistry {

    data class CapabilityProfile(
        val sdkInt: Int,
        val isLegacyServiceMode: Boolean,     // API 26–28
        val isScopedStorageEnforced: Boolean, // API 29+
        val requiresGranularMediaPermissions: Boolean, // API 33+
        val requiresPostNotifications: Boolean,        // API 33+
        val requiresExactAlarmCheck: Boolean,          // API 31+
        val supports16KbPageSize: Boolean,             // API 35+
        val isLowRamDevice: Boolean,
        val maxSafeHeapMb: Long
    )

    fun inspectDevice(context: Context): CapabilityProfile {
        val sdk = Build.VERSION.SDK_INT
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice ?: false

        return CapabilityProfile(
            sdkInt = sdk,
            isLegacyServiceMode = sdk in 26..28,
            isScopedStorageEnforced = sdk >= 29,
            requiresGranularMediaPermissions = sdk >= 33,
            requiresPostNotifications = sdk >= 33,
            requiresExactAlarmCheck = sdk >= 31,
            supports16KbPageSize = sdk >= 35,
            isLowRamDevice = isLowRam,
            maxSafeHeapMb = if (isLowRam) 48L else 128L
        )
    }

    fun isThermalThrottling(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val status = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            return status >= PowerManager.THERMAL_STATUS_SEVERE
        }
        return false
    }
}
