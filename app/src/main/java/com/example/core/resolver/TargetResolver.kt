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
            return ResolutionResult.Failure(
                "Target '${selector.toReadableDescription()}' not found in ${allNodes.size} accessible nodes (active package: ${snapshot.packageName})"
            )
        }

        // Sort descending by score
        scoredMatches.sortByDescending { it.score }
        val top = scoredMatches.first()

        return ResolutionResult.Success(
            match = top,
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
