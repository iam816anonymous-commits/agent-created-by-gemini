package com.example.core

import com.example.core.model.RectData
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.optimizer.ActionHistoryRecord
import com.example.core.optimizer.ActionRingBuffer
import com.example.core.optimizer.CoordinateNormalizer
import com.example.core.optimizer.NormalizedPoint
import com.example.core.optimizer.UiTreePruner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizerAndPrunerTest {

    private fun createNode(
        id: String,
        className: String,
        text: String? = null,
        resourceId: String? = null,
        bounds: RectData = RectData(0, 0, 100, 100),
        isClickable: Boolean = false,
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
        isEditable = false,
        isEnabled = true,
        isFocused = false,
        isScrollable = false,
        isVisibleToUser = true,
        depth = 0,
        children = children
    )

    @Test
    fun testUiTreePruner_collapsesRedundantSingleChildWrappers() {
        // Deep redundant layout nesting: FrameLayout -> LinearLayout -> Button("Click Me")
        val targetButton = createNode(
            id = "btn_real",
            className = "android.widget.Button",
            text = "Click Me",
            resourceId = "com.test.app:id/submit",
            bounds = RectData(100, 200, 400, 300),
            isClickable = true
        )

        val redundantLinear = createNode(
            id = "linear_empty",
            className = "android.widget.LinearLayout",
            bounds = RectData(100, 200, 400, 300),
            children = listOf(targetButton)
        )

        val redundantFrame = createNode(
            id = "frame_empty",
            className = "android.widget.FrameLayout",
            bounds = RectData(0, 0, 1080, 2400),
            children = listOf(redundantLinear)
        )

        val pruned = UiTreePruner.prune(redundantFrame)
        assertNotNull(pruned)
        // Redundant FrameLayout and LinearLayout should collapse down directly to the target Button
        assertEquals("Redundant single-child containers must collapse directly to actionable child",
            "btn_real", pruned?.id)
        assertEquals("Click Me", pruned?.text)
        assertTrue(pruned?.isClickable == true)
    }

    @Test
    fun testUiTreePruner_prunesEmptyNonSemanticLeafContainers() {
        val emptyContainer = createNode(
            id = "useless_view",
            className = "android.view.View",
            bounds = RectData(0, 0, 100, 100),
            children = emptyList()
        )

        val pruned = UiTreePruner.prune(emptyContainer)
        assertEquals("Empty container with no text/ID/action must be pruned to null", null, pruned)
    }

    @Test
    fun testActionRingBuffer_detectsOscillationCycle() {
        val buffer = ActionRingBuffer(capacity = 8)

        // Simulate an alternating cycle: Tap Next -> Tap Back -> Tap Next -> Tap Back
        buffer.record(ActionHistoryRecord("c1", "TapNext", "Next Button", 1001L, 2002L))
        buffer.record(ActionHistoryRecord("c2", "TapBack", "Back Button", 2002L, 1001L))
        buffer.record(ActionHistoryRecord("c3", "TapNext", "Next Button", 1001L, 2002L))
        buffer.record(ActionHistoryRecord("c4", "TapBack", "Back Button", 2002L, 1001L))

        assertTrue("Should detect 2-step oscillation cycle", buffer.detectCycle(lookback = 4))
    }

    @Test
    fun testActionRingBuffer_detectsRepeatedNoOpStagnation() {
        val buffer = ActionRingBuffer(capacity = 8)

        // Consecutive actions where stateHashBefore == stateHashAfter (no-ops)
        buffer.record(ActionHistoryRecord("c1", "Click", "Dead Button", 5555L, 5555L))
        buffer.record(ActionHistoryRecord("c2", "Click", "Dead Button", 5555L, 5555L))

        assertTrue("Should detect repeated stagnation when hashes don't mutate", buffer.isRepeatedStagnant(threshold = 2))
    }

    @Test
    fun testCoordinateNormalizer_convertsAccuratelyAcrossResolutions() {
        val bounds = RectData(left = 270, top = 600, right = 810, bottom = 1200)
        val screenW = 1080
        val screenH = 2400

        val normalized = CoordinateNormalizer.normalizeRect(bounds, screenW, screenH)

        assertEquals(0.25f, normalized.leftRatio, 0.001f)
        assertEquals(0.25f, normalized.topRatio, 0.001f)
        assertEquals(0.75f, normalized.rightRatio, 0.001f)
        assertEquals(0.50f, normalized.bottomRatio, 0.001f)
        assertEquals(0.50f, normalized.center.xRatio, 0.001f)
        assertEquals(0.375f, normalized.center.yRatio, 0.001f)

        // Denormalize center point
        val (px, py) = CoordinateNormalizer.denormalizePoint(normalized.center, screenW, screenH)
        assertEquals(540, px)
        assertEquals(900, py)
    }
}
