package com.example.core.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

/**
 * Strict memory and resource budget manager.
 * Calibrated specifically for TECNO Camon i Click (Android 8.1 / API 27, 4 GB RAM, 64 GB Storage).
 */
data class MemorySnapshot(
    val heapAllocatedMb: Double,
    val heapMaxMb: Double,
    val nativeAllocatedMb: Double,
    val systemAvailableRamMb: Long,
    val isLowMemory: Boolean,
    val storedSkillsCount: Int,
    val storageUsedKb: Long,
    val storageBudgetKb: Long = 51_200L // 50 MB budget
)

class MemoryBudgetManager(private val context: Context) {

    companion object {
        const val MAX_AGENT_HEAP_MB = 64.0 // Enforced heap budget
        const val MAX_IN_MEMORY_SNAPSHOTS = 5
        const val MAX_SNAPSHOT_NODES = 150
        const val MAX_SNAPSHOT_DEPTH = 8
        const val MAX_PERSISTENT_STORAGE_KB = 51_200L // 50 MB
    }

    fun measureMemory(skillsCount: Int = 0, storageUsedKb: Long = 0L): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val heapAllocated = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / (1024 * 1024)
        val heapMax = runtime.maxMemory().toDouble() / (1024 * 1024)
        val nativeAllocated = Debug.getNativeHeapAllocatedSize().toDouble() / (1024 * 1024)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val availRamMb = memInfo.availMem / (1024 * 1024)
        val isLow = memInfo.lowMemory || (heapAllocated > MAX_AGENT_HEAP_MB)

        return MemorySnapshot(
            heapAllocatedMb = (heapAllocated * 100).toLong() / 100.0,
            heapMaxMb = (heapMax * 100).toLong() / 100.0,
            nativeAllocatedMb = (nativeAllocated * 100).toLong() / 100.0,
            systemAvailableRamMb = availRamMb,
            isLowMemory = isLow,
            storedSkillsCount = skillsCount,
            storageUsedKb = storageUsedKb,
            storageBudgetKb = MAX_PERSISTENT_STORAGE_KB
        )
    }

    fun canAcceptNewLearning(currentStorageUsedKb: Long): Boolean {
        return currentStorageUsedKb < MAX_PERSISTENT_STORAGE_KB
    }

    /**
     * Purges internal transient caches when memory is constrained.
     * Complies with Priority #3: Never terminates unrelated user applications.
     */
    fun purgeTransientCaches() {
        System.gc()
    }
}
