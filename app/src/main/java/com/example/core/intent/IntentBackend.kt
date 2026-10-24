package com.example.core.intent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

data class IntentActionStep(
    val id: String,
    val action: String,
    val target: String,
    val parameters: Map<String, String> = emptyMap()
)

data class IntentPlanResult(
    val sourceTier: Int,
    val backendName: String,
    val confidence: Float,
    val latencyMs: Long,
    val steps: List<IntentActionStep>,
    val isOfflineDegraded: Boolean = false,
    val provenance: String
)

/**
 * Common IntentBackend Interface for multi-tier autonomy.
 */
interface IntentBackend {
    val name: String
    val tier: Int
    suspend fun isAvailable(context: Context): Boolean
    suspend fun plan(context: Context, query: String, activePackage: String?): IntentPlanResult?
}

/**
 * Hierarchical Intent Router:
 * Tier 1: Deterministic Fast-Path (<5ms)
 * Tier 2: Local SLM via llama.cpp JNI
 * Tier 3: Keystore-backed Gemini Cloud HTTPS
 * Tier 4: Honest degraded offline state (no fabricated plans)
 */
class HierarchicalIntentRouter(
    private val fastPathBackend: IntentBackend,
    private val localSlmBackend: IntentBackend,
    private val cloudGeminiBackend: IntentBackend
) {
    companion object {
        private const val TAG = "HierarchicalRouter"

        fun isNetworkConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    suspend fun route(context: Context, query: String, activePackage: String?): IntentPlanResult {
        val start = System.currentTimeMillis()

        // Tier 1: Fast-Path
        if (fastPathBackend.isAvailable(context)) {
            val res = fastPathBackend.plan(context, query, activePackage)
            if (res != null && res.confidence >= 0.85f) {
                return res
            }
        }

        // Tier 2: Local SLM
        if (localSlmBackend.isAvailable(context)) {
            val res = localSlmBackend.plan(context, query, activePackage)
            if (res != null && res.confidence >= 0.70f) {
                return res
            }
        }

        // Tier 3: Cloud Gemini (Honest connectivity check)
        if (isNetworkConnected(context) && cloudGeminiBackend.isAvailable(context)) {
            val res = cloudGeminiBackend.plan(context, query, activePackage)
            if (res != null) {
                return res
            }
        }

        // Tier 4: Honest degraded offline state (0% hallucination)
        val elapsed = System.currentTimeMillis() - start
        Log.w(TAG, "All inference backends exhausted for query '$query'. Returning honest degraded offline fallback.")
        return IntentPlanResult(
            sourceTier = 4,
            backendName = "HonestOfflineFallback",
            confidence = 0.0f,
            latencyMs = elapsed,
            steps = emptyList(),
            isOfflineDegraded = true,
            provenance = "OFFLINE_UNREACHABLE_NO_FABRICATION"
        )
    }
}
