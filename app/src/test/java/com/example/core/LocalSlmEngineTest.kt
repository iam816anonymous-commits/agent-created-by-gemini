package com.example.core

import com.example.core.slm.LocalGbnfGrammar
import com.example.core.slm.LocalSlmIntentEngine
import com.example.core.slm.LocalSlmProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LocalSlmEngineTest {

    private lateinit var slmEngine: LocalSlmIntentEngine

    @Before
    fun setUp() {
        slmEngine = LocalSlmIntentEngine()
    }

    @Test
    fun testDeterministicFastPathInterception() = runBlocking {
        val prompt = "turn on flashlight for 1 minute"
        assertTrue("Flashlight command must qualify for fast-path", slmEngine.isDeterministicFastPathEligible(prompt))

        val result = slmEngine.extractIntentOnDemand(prompt)
        assertTrue("Fast path must be flagged as used", result.usedDeterministicFastPath)
        assertEquals("RAM delta must be exactly 0 for deterministic fast path", 0L, result.ramDeltaMb)
        assertTrue("Memory must be guaranteed returned to kernel", result.memoryReturnedToKernel)
        assertFalse("Model must not remain resident in memory after fast path", slmEngine.isModelLoaded.value)
    }

    @Test
    fun testOnDemandZeroLeakLifecycleSmolLM2() = runBlocking {
        val complexPrompt = "Search for local movie times and take a note of tickets"
        assertFalse("Complex prompt should not use simple fast path", slmEngine.isDeterministicFastPathEligible(complexPrompt))

        val result = slmEngine.extractIntentOnDemand(complexPrompt, LocalSlmProfile.SMOLLM2_360M_Q4_K_M)
        assertNotNull(result)
        assertEquals(390L, result.ramDeltaMb)
        assertTrue("Result must confirm grammar validation", result.grammarValidated)
        assertTrue("Memory must be reported as freed back to Linux kernel", result.memoryReturnedToKernel)

        // Strict verification: model must NOT be kept resident in RAM
        assertFalse("Zero-leak verification: model must be unloaded immediately after inference", slmEngine.isModelLoaded.value)
    }

    @Test
    fun testQwenStructuredToolCalling() = runBlocking {
        val complexPrompt = "Open calculator and calculate 15 * 8 then copy"
        val result = slmEngine.extractIntentOnDemand(complexPrompt, LocalSlmProfile.QWEN2_5_0_5B_Q4_K_M)

        assertTrue(result.grammarValidated)
        assertTrue(result.modelUsed.contains("Qwen2.5-0.5B"))
        assertEquals(490L, result.ramDeltaMb)
        assertFalse("Qwen model must be unloaded post-inference", slmEngine.isModelLoaded.value)
    }

    @Test
    fun testProhibited2BPlusModelRejectionGate() = runBlocking {
        val prohibitedProfile = LocalSlmProfile.PROHIBITED_2B_PLUS
        assertFalse("2B+ models must be flagged unsafe for 4GB device", prohibitedProfile.isSafeFor4GbDevice)
        assertFalse("canLoadModel must reject 2B+ models", slmEngine.canLoadModel(prohibitedProfile))

        try {
            slmEngine.loadModel(prohibitedProfile)
            fail("Loading a 2B+ model must throw IllegalArgumentException to prevent Android LMK crash")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("exceeds the 500 MB safe budget") == true)
        }
    }

    @Test
    fun testGbnfGrammarJsonValidation() {
        val validJson = """
            {
              "intent": "LAUNCH_AND_NOTE",
              "confidence": 0.98,
              "plan": [
                {"step": 1, "action": "LAUNCH_APP", "target": "com.google.android.keep"},
                {"step": 2, "action": "TYPE_TEXT", "target": "note_body", "text": "Groceries"}
              ]
            }
        """.trimIndent()
        assertTrue("Valid JSON conforming to GBNF must pass validation", LocalGbnfGrammar.validateStructuredPlanJson(validJson))

        val invalidJson = "I will now open the calculator for you: [step 1: open]"
        assertFalse("Free-form text without GBNF JSON structure must be rejected", LocalGbnfGrammar.validateStructuredPlanJson(invalidJson))
    }
}
