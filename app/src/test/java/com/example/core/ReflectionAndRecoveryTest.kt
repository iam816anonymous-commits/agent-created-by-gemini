package com.example.core

import com.example.core.engine.AutonomousRecoveryEngine
import com.example.core.model.RectData
import com.example.core.model.TargetSelector
import com.example.core.model.TaskCommand
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.reflection.InterruptionDetector
import com.example.core.reflection.InterruptionType
import com.example.core.reflection.StateSignatureHasher
import com.example.core.resolver.TargetResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionAndRecoveryTest {

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
    fun testInterruptionDetector_detectsPermissionControllerDialog() {
        val allowButton = createNode(
            id = "btn_allow",
            className = "android.widget.Button",
            text = "While using the app",
            resourceId = "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
            bounds = RectData(100, 500, 400, 600),
            isClickable = true
        )

        val dialogRoot = createNode(
            id = "dialog_root",
            className = "android.widget.LinearLayout",
            text = "Allow LocalAgent to access device location?",
            bounds = RectData(50, 400, 450, 800),
            children = listOf(allowButton)
        )

        val snapshot = UiSnapshot(
            packageName = "com.google.android.permissioncontroller",
            rootNode = dialogRoot,
            totalNodeCount = 2,
            isLive = true
        )

        val assessment = InterruptionDetector.assessSnapshot(snapshot)

        assertTrue("Should detect interruption in permission controller", assessment.isInterrupted)
        assertEquals("Should be PERMISSION_DIALOG type", InterruptionType.PERMISSION_DIALOG, assessment.type)
        assertNotNull("Should provide dismiss command", assessment.dismissCommand)

        val dismissCmd = assessment.dismissCommand as TaskCommand.ClickElement
        assertEquals("Target should click 'While using the app'", "While using the app", dismissCmd.selector.text)
    }

    @Test
    fun testInterruptionDetector_normalScreenHasNoInterruption() {
        val normalRoot = createNode(
            id = "calc_root",
            className = "android.widget.FrameLayout",
            text = "Calculator",
            bounds = RectData(0, 0, 1080, 2400)
        )

        val snapshot = UiSnapshot(
            packageName = "com.google.android.calculator",
            rootNode = normalRoot,
            totalNodeCount = 1,
            isLive = true
        )

        val assessment = InterruptionDetector.assessSnapshot(snapshot)
        assertFalse("Normal calculator screen should not be flagged as interrupted", assessment.isInterrupted)
        assertEquals(InterruptionType.NONE, assessment.type)
    }

    @Test
    fun testStateSignatureHasher_detectsIdenticalStateStagnation() {
        val node1 = createNode(
            id = "item1",
            className = "android.widget.TextView",
            text = "First item",
            resourceId = "com.test.app:id/title",
            bounds = RectData(0, 0, 200, 50)
        )
        val snapshot1 = UiSnapshot(
            packageName = "com.test.app",
            rootNode = node1,
            totalNodeCount = 1,
            isLive = true
        )

        val snapshot2 = UiSnapshot(
            packageName = "com.test.app",
            rootNode = node1,
            totalNodeCount = 1,
            isLive = true
        )

        val sig1 = StateSignatureHasher.computeSignature(snapshot1)
        val sig2 = StateSignatureHasher.computeSignature(snapshot2)

        assertEquals("Identical layout must have identical hash", sig1.hash, sig2.hash)
        assertTrue("Identical signatures must be flagged as stagnant", sig1.isStagnantWith(sig2))
    }

    @Test
    fun testStateSignatureHasher_detectsProgressWhenNodesMutate() {
        val node1 = createNode(
            id = "item1",
            className = "android.widget.TextView",
            text = "First item",
            bounds = RectData(0, 0, 200, 50)
        )
        val node2 = createNode(
            id = "item2",
            className = "android.widget.TextView",
            text = "Second item (added after scroll)",
            bounds = RectData(0, 50, 200, 100)
        )
        val snapshotPre = UiSnapshot(
            packageName = "com.test.app",
            rootNode = node1,
            totalNodeCount = 1,
            isLive = true
        )
        val snapshotPost = UiSnapshot(
            packageName = "com.test.app",
            rootNode = createNode("root", "FrameLayout", children = listOf(node1, node2)),
            totalNodeCount = 2,
            isLive = true
        )

        val sigPre = StateSignatureHasher.computeSignature(snapshotPre)
        val sigPost = StateSignatureHasher.computeSignature(snapshotPost)

        assertFalse("Signatures must differ when new elements appear", sigPre.isStagnantWith(sigPost))
    }

    @Test
    fun testTargetResolver_clickableAncestorPropagation() {
        val targetResolver = TargetResolver()

        // Inner non-clickable text node
        val textNode = createNode(
            id = "inner_text",
            className = "android.widget.TextView",
            text = "Submit Order",
            bounds = RectData(50, 50, 250, 100),
            isClickable = false
        )

        // Enclosing clickable button container
        val buttonContainer = createNode(
            id = "btn_container",
            className = "com.google.android.material.button.MaterialButton",
            bounds = RectData(40, 40, 260, 110),
            isClickable = true,
            children = listOf(textNode)
        )

        val snapshot = UiSnapshot(
            packageName = "com.example.store",
            rootNode = buttonContainer,
            totalNodeCount = 2,
            isLive = true
        )

        val result = targetResolver.resolve(
            selector = TargetSelector(text = "Submit Order"),
            snapshot = snapshot
        )

        assertTrue("Resolution should succeed", result is TargetResolver.ResolutionResult.Success)
        val success = result as TargetResolver.ResolutionResult.Success
        val match = success.match

        assertTrue("Should propagate to clickable ancestor", match.isAncestorFallback)
        assertEquals("Resolved node should be the button container", "btn_container", match.node.id)
        assertEquals("Coordinates should match inner text center", 150, match.clickCoordinates?.first)
        assertEquals("Coordinates should match inner text center", 75, match.clickCoordinates?.second)
    }

    @Test
    fun testAutonomousRecoveryEngine_prioritizesInterruptionBypass() = runBlocking {
        val recoveryEngine = AutonomousRecoveryEngine()

        val allowButton = createNode(
            id = "btn_allow",
            className = "android.widget.Button",
            text = "While using the app",
            isClickable = true
        )
        val interruptedSnapshot = UiSnapshot(
            packageName = "com.google.android.permissioncontroller",
            rootNode = allowButton,
            totalNodeCount = 1,
            isLive = true
        )

        val failedAction = TaskCommand.ClickElement(
            selector = TargetSelector(text = "Calculate"),
            targetLabel = "Calculate Button"
        )

        val plan = recoveryEngine.formulateRecoveryPlan(
            failedCommand = failedAction,
            failureReason = "Target not found in active window",
            preSnapshot = interruptedSnapshot,
            durableStorage = null
        )

        assertTrue("Strategy name should mention Interruption Bypass", plan.strategyName.contains("Interruption Bypass"))
        assertEquals("First step must dismiss/grant interruption", 3, plan.alternativeCommands.size)
        assertTrue(plan.alternativeCommands[0] is TaskCommand.ClickElement)
        assertEquals(failedAction, plan.alternativeCommands[2])
    }
}
