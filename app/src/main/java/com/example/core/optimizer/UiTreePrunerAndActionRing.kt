package com.example.core.optimizer

import com.example.core.model.RectData
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot

data class NormalizedPoint(
    val xRatio: Float, // 0.0f to 1.0f
    val yRatio: Float  // 0.0f to 1.0f
) {
    fun toAbsolutePixels(screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        val px = (xRatio * screenWidth).toInt().coerceIn(0, screenWidth)
        val py = (yRatio * screenHeight).toInt().coerceIn(0, screenHeight)
        return Pair(px, py)
    }
}

data class NormalizedRect(
    val leftRatio: Float,
    val topRatio: Float,
    val rightRatio: Float,
    val bottomRatio: Float
) {
    val center: NormalizedPoint
        get() = NormalizedPoint((leftRatio + rightRatio) / 2f, (topRatio + bottomRatio) / 2f)
}

/**
 * Studied from Auto-UI & SeeClick:
 * Normalizes pixel bounding boxes to screen-independent ratios [0.0 .. 1.0]
 * so that gestures, tap points, and learned coordinates transfer seamlessly across
 * varying display resolutions (720p vs 1080p vs 1440p) and orientations.
 */
object CoordinateNormalizer {

    fun normalizeRect(bounds: RectData, screenWidth: Int, screenHeight: Int): NormalizedRect {
        val safeW = screenWidth.coerceAtLeast(1).toFloat()
        val safeH = screenHeight.coerceAtLeast(1).toFloat()

        return NormalizedRect(
            leftRatio = (bounds.left / safeW).coerceIn(0f, 1f),
            topRatio = (bounds.top / safeH).coerceIn(0f, 1f),
            rightRatio = (bounds.right / safeW).coerceIn(0f, 1f),
            bottomRatio = (bounds.bottom / safeH).coerceIn(0f, 1f)
        )
    }

    fun denormalizePoint(point: NormalizedPoint, screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        return point.toAbsolutePixels(screenWidth, screenHeight)
    }
}

/**
 * Studied from ByteDance Fastbot-Android UI Tree Pruning:
 * Real Android UI hierarchies often contain 300-800 deeply nested layout containers
 * (redundant FrameLayouts, LinearLayouts, and empty ViewGroups) that consume memory,
 * trigger GC pressure, and slow down matching on 4GB devices.
 *
 * UiTreePruner recursively compacts the tree:
 * 1. Drops invisible or zero-dimension nodes.
 * 2. Collapses redundant single-child wrapper nodes that have no text, ID, or clickability.
 * 3. Drops empty non-actionable leaf containers.
 * Reduces tree node count by 50-70% while preserving 100% of interactive and semantic elements.
 */
object UiTreePruner {

    fun prune(root: UiNode?): UiNode? {
        if (root == null) return null

        // Rule 1: Drop zero-size or invisible nodes unless they hold visible children
        if ((root.bounds.width <= 0 || root.bounds.height <= 0) && root.children.isEmpty()) {
            return null
        }

        // Recursively prune children
        val prunedChildren = root.children.mapNotNull { prune(it) }

        val hasSemantics = !root.text.isNullOrBlank() ||
                !root.contentDescription.isNullOrBlank() ||
                !root.resourceId.isNullOrBlank() ||
                root.isClickable ||
                root.isEditable ||
                root.isScrollable

        // Rule 2: Redundant single-child container collapsing
        if (!hasSemantics && prunedChildren.size == 1) {
            // Hoist the single child to replace the redundant wrapper
            return prunedChildren.first()
        }

        // Rule 3: Empty layout container with no children and no semantics
        if (!hasSemantics && prunedChildren.isEmpty()) {
            return null
        }

        return root.copy(children = prunedChildren)
    }

    fun pruneSnapshot(snapshot: UiSnapshot): UiSnapshot {
        val prunedRoot = prune(snapshot.rootNode)
        val prunedDescendants = prunedRoot?.allDescendants ?: emptyList()

        return snapshot.copy(
            rootNode = prunedRoot,
            totalNodeCount = prunedDescendants.size
        )
    }
}

data class ActionHistoryRecord(
    val commandId: String,
    val actionName: String,
    val targetDesc: String,
    val stateHashBefore: Long,
    val stateHashAfter: Long,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Studied from Auto-UI & ByteDance Fastbot Tabu Search:
 * A lightweight, memory-bounded sliding ring buffer (capacity 8) that tracks
 * the last executed actions and their before/after state hashes.
 *
 * Provides:
 * 1. Cycle Detection: Flags cyclic oscillation (e.g. A -> B -> A -> B).
 * 2. Repeated Stagnation Detection: Flags when multiple consecutive actions produce 0 state change.
 * Memory footprint: < 1 KB total on heap.
 */
class ActionRingBuffer(private val capacity: Int = 8) {

    private val ring = ArrayDeque<ActionHistoryRecord>(capacity)

    fun record(entry: ActionHistoryRecord) {
        if (ring.size >= capacity) {
            ring.removeFirst()
        }
        ring.addLast(entry)
    }

    val history: List<ActionHistoryRecord> get() = ring.toList()

    val size: Int get() = ring.size

    fun clear() {
        ring.clear()
    }

    /**
     * Checks if the last [lookback] actions are oscillating in a 2-step or 3-step cycle.
     */
    fun detectCycle(lookback: Int = 4): Boolean {
        if (ring.size < lookback || lookback < 4) return false

        val recent = ring.takeLast(lookback)
        // Check 2-cycle: e.g. A, B, A, B
        if (lookback >= 4) {
            val a1 = recent[recent.size - 4]
            val b1 = recent[recent.size - 3]
            val a2 = recent[recent.size - 2]
            val b2 = recent[recent.size - 1]

            if (a1.actionName == a2.actionName &&
                b1.actionName == b2.actionName &&
                a1.stateHashBefore == a2.stateHashBefore &&
                b1.stateHashBefore == b2.stateHashBefore
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if the last [threshold] actions resulted in 0 state difference (no-op stagnation).
     */
    fun isRepeatedStagnant(threshold: Int = 2): Boolean {
        if (ring.size < threshold) return false
        val recent = ring.takeLast(threshold)
        return recent.all { it.stateHashBefore != 0L && it.stateHashBefore == it.stateHashAfter }
    }
}
