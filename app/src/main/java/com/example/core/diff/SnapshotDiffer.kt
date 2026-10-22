package com.example.core.diff

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lightweight snapshot capturing the exact Accessibility node hierarchy signature.
 * Uses 64-bit FNV-1a hashing for O(1) state delta detection on Android 8.1 / Helio P23.
 */
data class ViewNodeSnapshot(
    val packageName: String,
    val totalNodeCount: Int,
    val fnv1aHash: Long,
    val nodesSummary: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dual View-Tree Comparison & Live Console Mutation Engine.
 * Captures pre- and post-action AccessibilityNodeInfo hierarchies, computes 64-bit FNV-1a
 * state signatures, and highlights mutations without allocating bulky intermediate objects.
 */
object SnapshotDiffer {

    private const val FNV_64_OFFSET_BASIS = -3750763034362895579L // 14695981039346656037L unsigned
    private const val FNV_64_PRIME = 1099511628211L

    /**
     * Traverses AccessibilityNodeInfo tree and generates a 64-bit FNV-1a signature.
     */
    fun captureSnapshot(rootNode: AccessibilityNodeInfo?, packageName: String): ViewNodeSnapshot {
        var count = 0
        val summaries = mutableListOf<String>()
        var hash = FNV_64_OFFSET_BASIS

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            count++
            val text = node.text?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val entry = "$viewId:$text:$desc"
            summaries.add(entry)

            for (char in entry) {
                hash = hash xor char.code.toLong()
                hash *= FNV_64_PRIME
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                try {
                    traverse(node.getChild(i))
                } catch (_: Exception) {
                    // Safe handling for recycled nodes
                }
            }
        }

        traverse(rootNode)
        return ViewNodeSnapshot(packageName, count, hash, summaries)
    }

    /**
     * Compares pre- and post-action snapshots and returns detailed delta analytics.
     */
    fun compareSnapshots(pre: ViewNodeSnapshot, post: ViewNodeSnapshot): String {
        val added = post.nodesSummary.toSet() subtract pre.nodesSummary.toSet()
        val removed = pre.nodesSummary.toSet() subtract post.nodesSummary.toSet()
        val isMatch = pre.fnv1aHash == post.fnv1aHash

        return "Mutations Detected -> Added: ${added.size}, Removed: ${removed.size}, FNV Match: $isMatch, PreHash: 0x${java.lang.Long.toHexString(pre.fnv1aHash)}, PostHash: 0x${java.lang.Long.toHexString(post.fnv1aHash)}"
    }

    /**
     * Rapid boolean check if the screen mutated following an action.
     */
    fun hasUiMutated(pre: ViewNodeSnapshot, post: ViewNodeSnapshot): Boolean {
        return pre.fnv1aHash != post.fnv1aHash || pre.packageName != post.packageName
    }
}
