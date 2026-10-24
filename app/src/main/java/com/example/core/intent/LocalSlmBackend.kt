package com.example.core.intent

import android.content.Context
import com.example.core.slm.LlamaAndroidAdapter
import com.example.core.slm.LlamaNativeBridge
import com.example.core.slm.LocalGbnfGrammar
import com.example.core.slm.LocalSlmProfile
import com.example.core.slm.ModelDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Tier 2 On-Device Small Language Model Backend.
 * Executes on-demand, zero-leak inference via llama.cpp JNI using quantized GGUF weights.
 */
class LocalSlmBackend(
    private val preferredProfile: LocalSlmProfile = LocalSlmProfile.SMOLLM2_360M_Q4_K_M,
    private val llamaAdapter: LlamaAndroidAdapter = LlamaAndroidAdapter()
) : IntentBackend {

    override val name: String = "LocalSlmInferenceEngine"
    override val tier: Int = 2

    override suspend fun isAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!LlamaNativeBridge.isNativeLibraryLoaded) return@withContext false
        val downloadManager = ModelDownloadManager(context)
        downloadManager.isModelDownloaded(preferredProfile)
    }

    override suspend fun plan(context: Context, query: String, activePackage: String?): IntentPlanResult? = withContext(Dispatchers.Default) {
        val start = System.currentTimeMillis()
        if (!isAvailable(context)) return@withContext null

        val downloadManager = ModelDownloadManager(context)
        val modelDir = downloadManager.getModelsDirectory()

        try {
            val session = llamaAdapter.loadModelSession(
                profile = preferredProfile,
                contextTokens = 1024,
                modelDir = modelDir
            )

            session.use { activeSession ->
                val prompt = "User Task: $query\nActive Package: ${activePackage ?: "HomeScreen"}\nGenerate JSON plan:"
                val tokens = llamaAdapter.completeStream(activeSession, prompt).toList()
                val rawOutput = tokens.joinToString("").trim()

                val latency = System.currentTimeMillis() - start

                if (rawOutput.startsWith("{") && rawOutput.endsWith("}")) {
                    val json = JSONObject(rawOutput)
                    val planArray = json.optJSONArray("plan") ?: return@withContext null
                    val steps = mutableListOf<IntentActionStep>()

                    for (i in 0 until planArray.length()) {
                        val stepObj = planArray.getJSONObject(i)
                        steps.add(
                            IntentActionStep(
                                id = "slm_step_$i",
                                action = stepObj.optString("action", "TAP"),
                                target = stepObj.optString("target", ""),
                                parameters = mapOf("raw" to stepObj.toString())
                            )
                        )
                    }

                    if (steps.isNotEmpty()) {
                        return@withContext IntentPlanResult(
                            sourceTier = 2,
                            backendName = "${preferredProfile.modelName} (llama-android)",
                            confidence = 0.85f,
                            latencyMs = latency,
                            steps = steps,
                            isOfflineDegraded = false,
                            provenance = "ON_DEVICE_GGUF_INFERENCE"
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Log and return null so the router can gracefully advance to the next tier
        }
        null
    }
}
