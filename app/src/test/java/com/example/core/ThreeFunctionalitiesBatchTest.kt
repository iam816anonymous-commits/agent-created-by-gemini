package com.example.core

import com.example.core.model.ActionType
import com.example.core.model.RecordedInteraction
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.recorder.TaskSynthesizer
import com.example.core.resolver.TargetResolver
import com.example.core.slm.LocalSlmIntentEngine
import com.example.core.slm.LocalSlmProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThreeFunctionalitiesBatchTest {

    // =========================================================================
    // Functionality 1 Test: TaskSynthesizer Input Deduplication & Normalization
    // =========================================================================
    @Test
    fun testTaskSynthesizer_deduplicatesConsecutiveKeystrokes() {
        val synthesizer = TaskSynthesizer()

        val interactions = listOf(
            RecordedInteraction(actionType = ActionType.ENTER_TEXT, textPayload = "B", packageName = "com.google.android.keep"),
            RecordedInteraction(actionType = ActionType.ENTER_TEXT, textPayload = "Bu", packageName = "com.google.android.keep"),
            RecordedInteraction(actionType = ActionType.ENTER_TEXT, textPayload = "Buy milk", packageName = "com.google.android.keep")
        )

        val synthesized = synthesizer.synthesize(interactions, "Note Taking Macro")

        val textCommands = synthesized.commands.filterIsInstance<TaskCommand.EnterText>()
        assertEquals("Should collapse consecutive keystrokes into a single enter text command", 1, textCommands.size)
        assertEquals("Final text payload should be 'Buy milk'", "Buy milk", textCommands.first().textToEnter)
    }

    @Test
    fun testTaskSynthesizer_encodesAndDecodesNormalizedCoordinates() {
        val synthesizer = TaskSynthesizer()

        val tapInteraction = RecordedInteraction(
            actionType = ActionType.TAP,
            targetLabel = "Custom Canvas Button",
            touchCoordinates = Pair(540, 1200), // 50% X, 50% Y
            packageName = "com.example.game"
        )

        val synthesized = synthesizer.synthesize(listOf(tapInteraction), "Canvas Tap")
        val clickCmd = synthesized.commands.filterIsInstance<TaskCommand.ClickElement>().firstOrNull()
        assertNotNull(clickCmd)

        assertEquals(0.5f, clickCmd?.selector?.fallbackNormalizedX ?: 0f, 0.01f)
        assertEquals(0.5f, clickCmd?.selector?.fallbackNormalizedY ?: 0f, 0.01f)

        // Verify roundtrip encoding
        val encoded = synthesizer.encodeCommands(synthesized.commands)
        val decoded = synthesizer.decodeCommands(encoded)

        val decodedClick = decoded.filterIsInstance<TaskCommand.ClickElement>().firstOrNull()
        assertNotNull(decodedClick)
        assertEquals(0.5f, decodedClick?.selector?.fallbackNormalizedX ?: 0f, 0.01f)
        assertEquals(0.5f, decodedClick?.selector?.fallbackNormalizedY ?: 0f, 0.01f)
    }

    // =========================================================================
    // Functionality 2 Test: TargetResolver Stage-4 Normalized Coordinate Fallback
    // =========================================================================
    @Test
    fun testTargetResolver_stage4CoordinateFallbackWhenNodesUnexposed() {
        val resolver = TargetResolver()

        val snapshot = UiSnapshot(
            packageName = "com.custom.canvas.app",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )

        // Selector with normalized coordinates [0.5, 0.25]
        val selector = TargetSelector(
            text = "Game Start",
            fallbackNormalizedX = 0.5f,
            fallbackNormalizedY = 0.25f
        )

        val result = resolver.resolve(selector, snapshot)
        assertTrue("Should succeed via Stage-4 coordinate fallback", result is TargetResolver.ResolutionResult.Success)

        val success = result as TargetResolver.ResolutionResult.Success
        val match = success.match
        assertTrue("Match reason must mention Stage-4 fallback", match.matchReason.contains("Stage-4"))
        assertEquals(540, match.clickCoordinates?.first)
        assertEquals(600, match.clickCoordinates?.second)
    }

    // =========================================================================
    // Functionality 3 Test: LocalSlmIntentEngine On-Demand Memory Lifecycle
    // =========================================================================
    @Test
    fun testLocalSlmIntentEngine_onDemandLoadAndUnloadLifecycle() = runBlocking {
        val engine = LocalSlmIntentEngine()

        assertFalse("Model must be initially unloaded", engine.isModelLoaded.value)

        // Execute intent extraction on-demand
        val result = engine.extractIntentOnDemand(
            prompt = "Open calculator and multiply 5 by 6",
            profile = LocalSlmProfile.SMOLLM2_360M
        )

        assertNotNull(result)
        assertEquals("Calculator", result.targetApp)
        assertTrue(result.confidence >= 0.90f)

        // Verify that model was automatically unloaded immediately after inference to free memory
        assertFalse("Model must be freed back to OS pool immediately after inference", engine.isModelLoaded.value)
    }
}
