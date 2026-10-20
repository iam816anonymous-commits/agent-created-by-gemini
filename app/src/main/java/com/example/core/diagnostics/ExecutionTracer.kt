package com.example.core.diagnostics

import org.json.JSONArray
import org.json.JSONObject

data class TraceEvent(
    val stepIndex: Int,
    val timestampMs: Long = System.currentTimeMillis(),
    val actionName: String,
    val targetDescription: String,
    val preStateHash: Long,
    val postStateHash: Long,
    val durationMs: Long,
    val isAncestorFallback: Boolean = false,
    val isInterruptionEncountered: Boolean = false,
    val status: String, // "SUCCESS", "FAILED", "ROLLED_BACK", "RECOVERED"
    val notes: String = ""
) {
    val stateMutated: Boolean get() = preStateHash != 0L && postStateHash != 0L && preStateHash != postStateHash

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("stepIndex", stepIndex)
            put("timestampMs", timestampMs)
            put("actionName", actionName)
            put("targetDescription", targetDescription)
            put("preStateHash", preStateHash)
            put("postStateHash", postStateHash)
            put("durationMs", durationMs)
            put("isAncestorFallback", isAncestorFallback)
            put("isInterruptionEncountered", isInterruptionEncountered)
            put("status", status)
            put("notes", notes)
            put("stateMutated", stateMutated)
        }
    }
}

data class TraceMetrics(
    val totalSteps: Int,
    val successfulSteps: Int,
    val failedSteps: Int,
    val rollbacksTriggered: Int,
    val ancestorFallbacksUsed: Int,
    val interruptionsDismissed: Int,
    val averageStepDurationMs: Long,
    val successRate: Float
)

/**
 * Studied from DroidRun (mobilerun) & testing repository:
 * Structured execution trace journal and multi-step diagnostic reporter.
 *
 * Provides granular post-mortem timelines for offline verification, regression benchmarking,
 * and remote tele-operation diagnostics on Android 8.1 / 4 GB RAM.
 *
 * Memory bounded: capacity 64 events (< 15 KB on heap).
 */
class ExecutionTracer(private val capacity: Int = 64) {

    private val traceLog = ArrayDeque<TraceEvent>(capacity)

    @Synchronized
    fun recordEvent(event: TraceEvent) {
        if (traceLog.size >= capacity) {
            traceLog.removeFirst()
        }
        traceLog.addLast(event)
    }

    @Synchronized
    fun getEvents(): List<TraceEvent> = traceLog.toList()

    @Synchronized
    fun clear() {
        traceLog.clear()
    }

    @Synchronized
    fun computeMetrics(): TraceMetrics {
        if (traceLog.isEmpty()) {
            return TraceMetrics(0, 0, 0, 0, 0, 0, 0L, 0f)
        }

        val total = traceLog.size
        val successes = traceLog.count { it.status == "SUCCESS" || it.status == "RECOVERED" }
        val failures = traceLog.count { it.status == "FAILED" }
        val rollbacks = traceLog.count { it.status == "ROLLED_BACK" }
        val ancestorFallbacks = traceLog.count { it.isAncestorFallback }
        val interruptions = traceLog.count { it.isInterruptionEncountered }
        val avgDuration = traceLog.map { it.durationMs }.average().toLong()
        val successRate = (successes.toFloat() / total.toFloat()) * 100f

        return TraceMetrics(
            totalSteps = total,
            successfulSteps = successes,
            failedSteps = failures,
            rollbacksTriggered = rollbacks,
            ancestorFallbacksUsed = ancestorFallbacks,
            interruptionsDismissed = interruptions,
            averageStepDurationMs = avgDuration,
            successRate = successRate
        )
    }

    @Synchronized
    fun exportJson(): String {
        val array = JSONArray()
        for (event in traceLog) {
            array.put(event.toJson())
        }
        return array.toString(2)
    }

    @Synchronized
    fun generateMarkdownReport(): String {
        val metrics = computeMetrics()
        val sb = StringBuilder()
        sb.append("### LocalAgent Execution Trace Report\n")
        sb.append("- **Total Steps:** ${metrics.totalSteps}\n")
        sb.append("- **Success Rate:** ${"%.1f".format(metrics.successRate)}%\n")
        sb.append("- **Avg Duration:** ${metrics.averageStepDurationMs} ms\n")
        sb.append("- **Ancestor Fallbacks:** ${metrics.ancestorFallbacksUsed}\n")
        sb.append("- **Rollbacks:** ${metrics.rollbacksTriggered}\n\n")

        sb.append("| Step | Action | Target | Pre/Post Hash | Duration | Status | Notes |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n")

        for (e in traceLog) {
            val hashDelta = if (e.preStateHash != e.postStateHash) "MUTATED" else "STAGNANT"
            sb.append("| #${e.stepIndex} | `${e.actionName}` | ${e.targetDescription.take(20)} | $hashDelta | ${e.durationMs}ms | **${e.status}** | ${e.notes.take(30)} |\n")
        }

        return sb.toString()
    }
}
