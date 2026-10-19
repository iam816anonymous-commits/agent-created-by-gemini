package com.example.core

import com.example.core.model.RectData
import com.example.core.model.ScrollDirection
import com.example.core.model.StepStatus
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.verifier.StepVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepVerifierTest {

    private val verifier = StepVerifier(maxStalenessMs = 4000L)

    private fun createNode(
        id: String,
        className: String,
        text: String? = null,
        resourceId: String? = null,
        bounds: RectData = RectData(0, 0, 100, 100),
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        isFocused: Boolean = false,
        isScrollable: Boolean = false,
        children: List<UiNode> = emptyList()
    ): UiNode = UiNode(
        id = id,
        packageName = "com.test.app",
        className = className,
        text = text,
        contentDescription = null,
        resourceId = resourceId,
        bounds = bounds,
        isClickable = isClickable,
        isEditable = isEditable,
        isEnabled = true,
        isFocused = isFocused,
        isScrollable = isScrollable,
        isVisibleToUser = true,
        depth = 0,
        children = children
    )

    @Test
    fun testStaleObservation_rejectedWhenTimestampPriorToDispatch() {
        val command = TaskCommand.LaunchApp("com.google.android.calculator", "Calculator")
        val preSnapshot = UiSnapshot(
            packageName = "com.home.launcher",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 10,
            isLive = true,
            monotonicTimestampMs = 1000L,
            generationId = 1L
        )

        // Snapshot captured at 1050ms, but action dispatched at 1200ms
        val stalePostSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 15,
            isLive = true,
            monotonicTimestampMs = 1050L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = stalePostSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        assertEquals("Stale snapshot captured before dispatch MUST be INCONCLUSIVE",
            StepStatus.INCONCLUSIVE, outcome.status)
        assertTrue(outcome.reason.contains("Stale observation"))
    }

    @Test
    fun testStaleObservation_rejectedWhenGenerationDoesNotAdvance() {
        val command = TaskCommand.LaunchApp("com.google.android.calculator", "Calculator")
        val preSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 10,
            isLive = true,
            monotonicTimestampMs = 1000L,
            generationId = 5L
        )

        // Monotonic clock is after dispatch, but generationId is still 5L (same tree buffer reused)
        val sameGenSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "FrameLayout"),
            totalNodeCount = 10,
            isLive = true,
            monotonicTimestampMs = 1250L,
            generationId = 5L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = sameGenSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        assertEquals(StepStatus.INCONCLUSIVE, outcome.status)
        assertTrue(outcome.reason.contains("Stale generation"))
    }

    @Test
    fun testTextEntry_rejectsUnrelatedLabelSubstringMatch() {
        val command = TaskCommand.EnterText(
            selector = TargetSelector(resourceId = "com.example.app:id/notes_field"),
            textToEnter = "Meeting at 5"
        )

        val preSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = createNode("root", "FrameLayout", children = listOf(
                // Non-editable title label that coincidentally contains "Meeting at 5"
                createNode("label1", "android.widget.TextView", text = "Old Meeting at 5 PM notice", isEditable = false),
                // The actual targeted note field which is empty
                createNode("input1", "android.widget.EditText", text = "", resourceId = "com.example.app:id/notes_field", isEditable = true)
            )),
            totalNodeCount = 3,
            isLive = true,
            monotonicTimestampMs = 1000L,
            generationId = 1L
        )

        // Post-snapshot where target field is still empty, but unrelated label still exists
        val postSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = createNode("root", "FrameLayout", children = listOf(
                createNode("label1", "android.widget.TextView", text = "Old Meeting at 5 PM notice", isEditable = false),
                createNode("input1", "android.widget.EditText", text = "", resourceId = "com.example.app:id/notes_field", isEditable = true)
            )),
            totalNodeCount = 3,
            isLive = true,
            monotonicTimestampMs = 1250L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        // Whole-tree scan would have falsely passed because label1 contains "Meeting at 5"!
        // The targeted verifier MUST return FAILURE because the actual notes_field has empty text.
        assertEquals("Target editable field missing text must result in FAILURE, not false positive from unrelated label",
            StepStatus.FAILURE, outcome.status)
    }

    @Test
    fun testTextEntry_verifiesTargetEditableFieldCorrectly() {
        val command = TaskCommand.EnterText(
            selector = TargetSelector(resourceId = "com.example.app:id/notes_field"),
            textToEnter = "Buy groceries"
        )

        val postSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = createNode("root", "FrameLayout", children = listOf(
                createNode("input1", "android.widget.EditText", text = "Buy groceries", resourceId = "com.example.app:id/notes_field", isEditable = true, isFocused = true)
            )),
            totalNodeCount = 2,
            isLive = true,
            monotonicTimestampMs = 1250L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = null,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        assertEquals(StepStatus.SUCCESS, outcome.status)
        assertEquals("Buy groceries", outcome.postValue)
        assertEquals(1.0f, outcome.confidence, 0.01f)
    }

    @Test
    fun testClickVerification_calculatorFormulaChangeVerified() {
        val command = TaskCommand.ClickElement(
            selector = TargetSelector(text = "7", resourceId = "com.google.android.calculator:id/digit_7"),
            targetLabel = "Digit 7"
        )

        val preSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "ViewGroup", children = listOf(
                createNode("formula", "android.widget.TextView", text = "", resourceId = "com.google.android.calculator:id/formula"),
                createNode("btn7", "android.widget.Button", text = "7", resourceId = "com.google.android.calculator:id/digit_7")
            )),
            totalNodeCount = 3,
            isLive = true,
            monotonicTimestampMs = 1000L,
            generationId = 1L
        )

        val postSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = createNode("root", "ViewGroup", children = listOf(
                createNode("formula", "android.widget.TextView", text = "7", resourceId = "com.google.android.calculator:id/formula"),
                createNode("btn7", "android.widget.Button", text = "7", resourceId = "com.google.android.calculator:id/digit_7")
            )),
            totalNodeCount = 3,
            isLive = true,
            monotonicTimestampMs = 1250L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        assertEquals(StepStatus.SUCCESS, outcome.status)
        assertEquals("7", outcome.postValue)
        assertEquals(1.0f, outcome.confidence, 0.01f)
    }

    @Test
    fun testClickVerification_ambiguousOutcomeReturnsInconclusiveToAvoidDoubleTap() {
        val command = TaskCommand.ClickElement(
            selector = TargetSelector(text = "Submit"),
            targetLabel = "Submit Button"
        )

        val preSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = createNode("root", "ViewGroup", children = listOf(
                createNode("btn", "android.widget.Button", text = "Submit")
            )),
            totalNodeCount = 2,
            isLive = true,
            monotonicTimestampMs = 1000L,
            generationId = 1L
        )

        // Post snapshot: hierarchy unchanged, button still there, no formula or dialog mutation
        val postSnapshot = UiSnapshot(
            packageName = "com.example.app",
            rootNode = createNode("root", "ViewGroup", children = listOf(
                createNode("btn", "android.widget.Button", text = "Submit")
            )),
            totalNodeCount = 2,
            isLive = true,
            monotonicTimestampMs = 1250L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = preSnapshot,
            postSnapshot = postSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        // Must NOT return SUCCESS (which risks thinking it succeeded) and must NOT immediately double-tap
        assertEquals("Ambiguous click outcome MUST return INCONCLUSIVE",
            StepStatus.INCONCLUSIVE, outcome.status)
    }

    @Test
    fun testAppLaunch_requiresInteractiveRootWindow() {
        val command = TaskCommand.LaunchApp("com.google.android.calculator", "Calculator")

        // Post-snapshot has the right package name, but null root node (transitory system window / black frame)
        val nonInteractiveSnapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true,
            isInteractiveRoot = false,
            monotonicTimestampMs = 1250L,
            generationId = 2L
        )

        val outcome = verifier.verify(
            command = command,
            preSnapshot = null,
            postSnapshot = nonInteractiveSnapshot,
            actionDispatchedTimestampMs = 1200L,
            currentTimeMs = 1300L
        )

        assertEquals(StepStatus.INCONCLUSIVE, outcome.status)
        assertTrue(outcome.reason.contains("does not have an active, interactive window root"))
    }
}
