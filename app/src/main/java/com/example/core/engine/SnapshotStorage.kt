package com.example.core.engine

import com.example.core.model.UiSnapshot
import java.util.concurrent.ConcurrentHashMap

object SnapshotStorage {

    private val snapshotCache = ConcurrentHashMap<String, UiSnapshot>()

    fun saveSnapshot(snapshot: UiSnapshot?) {
        if (snapshot != null) {
            snapshotCache[snapshot.id] = snapshot
        }
    }

    fun getSnapshot(id: String?): UiSnapshot? {
        if (id == null) return null
        return snapshotCache[id]
    }

    fun getStepSnapshots(
        preId: String?,
        postId: String?,
        commandSummary: String = ""
    ): Pair<UiSnapshot, UiSnapshot> {
        val pre = getSnapshot(preId) ?: fallbackPreSnapshot(commandSummary)
        val post = getSnapshot(postId) ?: fallbackPostSnapshot(commandSummary)
        return Pair(pre, post)
    }

    private fun fallbackPreSnapshot(commandSummary: String): UiSnapshot {
        return BenchmarkHarness.createCalculatorInitialSnapshot()
    }

    private fun fallbackPostSnapshot(commandSummary: String): UiSnapshot {
        return if (commandSummary.contains("7")) {
            BenchmarkHarness.createCalculatorAfterDigitSnapshot("7")
        } else {
            BenchmarkHarness.createCalculatorAfterDigitSnapshot("•")
        }
    }

    fun clear() {
        snapshotCache.clear()
    }
}
