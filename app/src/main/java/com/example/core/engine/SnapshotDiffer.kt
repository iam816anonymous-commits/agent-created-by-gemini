package com.example.core.engine

import com.example.core.model.SnapshotComparisonDiff
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot

object SnapshotDiffer {

    fun computeDiff(pre: UiSnapshot?, post: UiSnapshot?): SnapshotComparisonDiff {
        if (pre == null && post == null) {
            return SnapshotComparisonDiff(
                preSnapshot = null,
                postSnapshot = null,
                addedCount = 0,
                removedCount = 0,
                mutatedNodes = emptyList(),
                summary = "No snapshots available for comparison"
            )
        }

        if (pre == null) {
            val postNodes = post?.allNodes ?: emptyList()
            return SnapshotComparisonDiff(
                preSnapshot = null,
                postSnapshot = post,
                addedCount = postNodes.size,
                removedCount = 0,
                mutatedNodes = emptyList(),
                summary = "Initial observation captured with ${postNodes.size} UI elements"
            )
        }

        if (post == null) {
            val preNodes = pre.allNodes
            return SnapshotComparisonDiff(
                preSnapshot = pre,
                postSnapshot = null,
                addedCount = 0,
                removedCount = preNodes.size,
                mutatedNodes = emptyList(),
                summary = "Post-action observation unavailable (${preNodes.size} prior nodes)"
            )
        }

        val preMap = pre.allNodes.associateBy { it.resourceId ?: "${it.className}_${it.bounds.left}_${it.bounds.top}" }
        val postMap = post.allNodes.associateBy { it.resourceId ?: "${it.className}_${it.bounds.left}_${it.bounds.top}" }

        val addedNodes = mutableListOf<UiNode>()
        val removedNodes = mutableListOf<UiNode>()
        val mutated = mutableListOf<Pair<UiNode, UiNode>>()
        var unchangedCount = 0

        for ((key, postNode) in postMap) {
            val preNode = preMap[key]
            if (preNode == null) {
                addedNodes.add(postNode)
            } else if (preNode.text != postNode.text || preNode.contentDescription != postNode.contentDescription || preNode.isFocused != postNode.isFocused || preNode.isEnabled != postNode.isEnabled) {
                mutated.add(Pair(preNode, postNode))
            } else {
                unchangedCount++
            }
        }

        for ((key, preNode) in preMap) {
            if (!postMap.containsKey(key)) {
                removedNodes.add(preNode)
            }
        }

        val addedCount = addedNodes.size
        val removedCount = removedNodes.size

        val summaryParts = mutableListOf<String>()
        if (pre.packageName != post.packageName) {
            summaryParts.add("Active package shifted from '${pre.packageName}' to '${post.packageName}'")
        }
        if (mutated.isNotEmpty()) {
            val firstMutation = mutated.first()
            summaryParts.add("Element '${firstMutation.second.resourceId ?: firstMutation.second.className.substringAfterLast(".")}' mutated ('${firstMutation.first.text ?: ""}' -> '${firstMutation.second.text ?: ""}')")
        }
        if (addedCount > 0 || removedCount > 0) {
            summaryParts.add("$addedCount element(s) added, $removedCount removed")
        }
        if (summaryParts.isEmpty()) {
            summaryParts.add("UI hierarchy layout and values preserved without mutations")
        }

        return SnapshotComparisonDiff(
            preSnapshot = pre,
            postSnapshot = post,
            addedCount = addedCount,
            removedCount = removedCount,
            mutatedNodes = mutated,
            summary = summaryParts.joinToString(" • "),
            addedNodes = addedNodes,
            removedNodes = removedNodes,
            unchangedCount = unchangedCount
        )
    }
}
