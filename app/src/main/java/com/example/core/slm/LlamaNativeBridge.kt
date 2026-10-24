package com.example.core.slm

import android.util.Log

/**
 * JNI Native Bridge Interface to llama.cpp (libllama-bridge.so).
 *
 * Implements real native bindings for GGUF model initialization,
 * GBNF grammar compilation, and constrained token generation.
 */
object LlamaNativeBridge {
    private const val TAG = "LlamaNativeBridge"

    @Volatile
    var isNativeLibraryLoaded = false
        private set

    init {
        try {
            System.loadLibrary("llama-bridge")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Native library libllama-bridge.so loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLibraryLoaded = false
            Log.w(TAG, "Native library libllama-bridge.so not found on device: ${e.message}")
        }
    }

    @JvmStatic
    external fun nativeInitContext(modelPath: String, nThreads: Int, nCtx: Int): Long
    @JvmStatic
    external fun nativeCompileGbnf(grammarStr: String): Long
    @JvmStatic
    external fun nativeCompleteConstrained(handle: Long, grammarHandle: Long, prompt: String): String
    @JvmStatic
    external fun nativeFreeContext(handle: Long)
    @JvmStatic
    external fun nativeFreeGrammar(grammarHandle: Long)
}
