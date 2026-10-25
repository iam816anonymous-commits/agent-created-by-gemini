package com.example.core.resolver

import com.example.core.model.TargetMatch
import com.example.core.model.TargetSelector
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import java.util.Locale

class TargetResolver(
    private val maxSnapshotStalenessMs: Long = 4000L
) {
    sealed interface ResolutionResult {
        data class Success(val match: TargetMatch, val candidateCount: Int) : ResolutionResult
        data class Failure(val reason: String) : ResolutionResult
    }

    fun resolve(
        selector: TargetSelector,
        snapshot: UiSnapshot?,
        currentTimeMs: Long = System.currentTimeMillis()
    ): ResolutionResult {
        if (snapshot == null) {
            return ResolutionResult.Failure("No UI snapshot available for target resolution")
        }

        // Freshness verification rule
        val ageMs = currentTimeMs - snapshot.timestampMs
        if (ageMs > maxSnapshotStalenessMs) {
            return ResolutionResult.Failure("Stale snapshot rejected (age: ${ageMs}ms > max ${maxSnapshotStalenessMs}ms)")
        }

        val allNodes = snapshot.allNodes
        if (allNodes.isEmpty()) {
            if (selector.fallbackNormalizedX != null && selector.fallbackNormalizedY != null) {
                val screenW = 1080
                val screenH = 2400
                val px = (selector.fallbackNormalizedX * screenW).toInt()
                val py = (selector.fallbackNormalizedY * screenH).toInt()
                val fallbackNode = UiNode(
                    id = "coord_fallback_${px}_${py}",
                    packageName = snapshot.packageName,
                    className = "android.view.View",
                    text = selector.text,
                    contentDescription = selector.contentDescription,
                    resourceId = selector.resourceId,
                    bounds = com.example.core.model.RectData(px - 15, py - 15, px + 15, py + 15),
                    isClickable = true,
                    isEditable = false,
                    isEnabled = true,
                    isFocused = false,
                    isScrollable = false,
                    isVisibleToUser = true,
                    depth = 1
                )
                return ResolutionResult.Success(
                    match = TargetMatch(
                        node = fallbackNode,
                        score = 45,
                        matchReason = "Stage-4 Normalized Coordinate Fallback [${(selector.fallbackNormalizedX * 100).toInt()}%, ${(selector.fallbackNormalizedY * 100).toInt()}%]",
                        clickCoordinates = Pair(px, py)
                    ),
                    candidateCount = 1
                )
            }
            return ResolutionResult.Failure("Snapshot contains zero accessible nodes in package '${snapshot.packageName}'")
        }

        val scoredMatches = mutableListOf<TargetMatch>()

        for (node in allNodes) {
            val score = calculateScore(node, selector)
            if (score > 30) {
                val reason = buildMatchReason(node, selector, score)
                scoredMatches.add(TargetMatch(node = node, score = score, matchReason = reason))
            }
        }

        if (scoredMatches.isEmpty()) {
            // Studied from OpenGUI & AppAgent: Stage-4 Normalized Coordinate Fallback
            // If accessibility nodes are not exposed (custom canvas, Flutter, Webview),
            // resolve using normalized coordinate bounding box center.
            if (selector.fallbackNormalizedX != null && selector.fallbackNormalizedY != null) {
                val screenW = 1080
                val screenH = 2400
                val px = (selector.fallbackNormalizedX * screenW).toInt()
                val py = (selector.fallbackNormalizedY * screenH).toInt()
                val fallbackNode = UiNode(
                    id = "coord_fallback_${px}_${py}",
                    packageName = snapshot.packageName,
                    className = "android.view.View",
                    text = selector.text,
                    contentDescription = selector.contentDescription,
                    resourceId = selector.resourceId,
                    bounds = com.example.core.model.RectData(px - 15, py - 15, px + 15, py + 15),
                    isClickable = true,
                    isEditable = false,
                    isEnabled = true,
                    isFocused = false,
                    isScrollable = false,
                    isVisibleToUser = true,
                    depth = 1
                )
                return ResolutionResult.Success(
                    match = TargetMatch(
                        node = fallbackNode,
                        score = 45,
                        matchReason = "Stage-4 Normalized Coordinate Fallback [${(selector.fallbackNormalizedX * 100).toInt()}%, ${(selector.fallbackNormalizedY * 100).toInt()}%]",
                        clickCoordinates = Pair(px, py)
                    ),
                    candidateCount = 1
                )
            }

            return ResolutionResult.Failure(
                "Target '${selector.toReadableDescription()}' not found in ${allNodes.size} accessible nodes (active package: ${snapshot.packageName})"
            )
        }

        // Sort descending by score
        scoredMatches.sortByDescending { it.score }
        val top = scoredMatches.first()

        // Studied from Tencent AppAgent & DroidBot:
        // If top candidate is not explicitly clickable, propagate to enclosing clickable ancestor
        // or supply exact center coordinates for gesture dispatch.
        val resolvedMatch = if (!top.node.isClickable) {
            val clickableAncestor = allNodes.firstOrNull { ancestor ->
                ancestor.isClickable &&
                ancestor.bounds.left <= top.node.bounds.left &&
                ancestor.bounds.top <= top.node.bounds.top &&
                ancestor.bounds.right >= top.node.bounds.right &&
                ancestor.bounds.bottom >= top.node.bounds.bottom &&
                ancestor.id != top.node.id
            }
            if (clickableAncestor != null) {
                top.copy(
                    node = clickableAncestor,
                    isAncestorFallback = true,
                    clickCoordinates = Pair(top.node.bounds.centerX, top.node.bounds.centerY),
                    matchReason = "${top.matchReason} | Clickable Ancestor '${clickableAncestor.className}'"
                )
            } else {
                top.copy(
                    clickCoordinates = Pair(top.node.bounds.centerX, top.node.bounds.centerY),
                    matchReason = "${top.matchReason} | Center Coordinate Fallback [${top.node.bounds.centerX}, ${top.node.bounds.centerY}]"
                )
            }
        } else {
            top.copy(clickCoordinates = Pair(top.node.bounds.centerX, top.node.bounds.centerY))
        }

        return ResolutionResult.Success(
            match = resolvedMatch,
            candidateCount = scoredMatches.size
        )
    }

    private fun calculateScore(node: UiNode, selector: TargetSelector): Int {
        // Severe penalty for zero-size or off-screen elements
        if (node.bounds.width <= 0 || node.bounds.height <= 0) {
            return -100
        }
        if (!node.isVisibleToUser) {
            return -50
        }

        var score = 0
        val targetText = selector.text?.trim()
        val targetDesc = selector.contentDescription?.trim()
        val targetId = selector.resourceId?.trim()
        val targetClass = selector.className?.trim()

        val nodeText = node.text?.trim()
        val nodeDesc = node.contentDescription?.trim()
        val nodeId = node.resourceId?.trim()
        val nodeClass = node.className.trim()

        if (!targetText.isNullOrEmpty()) {
            when {
                nodeText.equals(targetText, ignoreCase = !selector.caseSensitive) -> {
                    score += 100
                }
                nodeDesc.equals(targetText, ignoreCase = !selector.caseSensitive) -> {
                    score += 90
                }
                nodeId != null && nodeId.contains(targetText, ignoreCase = true) -> {
                    score += 70
                }
                nodeText != null && nodeText.contains(targetText, ignoreCase = true) -> {
                    score += 60
                }
                nodeDesc != null && nodeDesc.contains(targetText, ignoreCase = true) -> {
                    score += 55
                }
            }
        }

        if (!targetDesc.isNullOrEmpty()) {
            when {
                nodeDesc.equals(targetDesc, ignoreCase = !selector.caseSensitive) -> {
                    score += 95
                }
                nodeDesc != null && nodeDesc.contains(targetDesc, ignoreCase = true) -> {
                    score += 65
                }
            }
        }

        if (!targetId.isNullOrEmpty() && nodeId != null) {
            if (nodeId.equals(targetId, ignoreCase = true) || nodeId.endsWith("/$targetId", ignoreCase = true)) {
                score += 85
            }
        }

        if (!targetClass.isNullOrEmpty() && nodeClass.endsWith(targetClass, ignoreCase = true)) {
            score += 15
        }

        if (selector.clickableOnly && !node.isClickable) {
            score -= 40
        } else if (node.isClickable) {
            score += 10
        }

        return score
    }

    private fun buildMatchReason(node: UiNode, selector: TargetSelector, score: Int): String {
        val parts = mutableListOf<String>()
        if (!selector.text.isNullOrEmpty() && node.text?.equals(selector.text, ignoreCase = true) == true) {
            parts.add("Exact text match ('${node.text}')")
        } else if (!selector.text.isNullOrEmpty() && node.contentDescription?.equals(selector.text, ignoreCase = true) == true) {
            parts.add("Exact contentDesc match ('${node.contentDescription}')")
        } else if (!node.resourceId.isNullOrEmpty()) {
            parts.add("ViewId '${node.resourceId}'")
        }
        parts.add("Score: $score")
        return parts.joinToString(" | ")
    }
}
