package com.example.core

import com.example.core.model.RectData
import com.example.core.model.TargetSelector
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.resolver.TargetResolver
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetResolverSafetyTest {
    private val resolver = TargetResolver()

    private fun node(text: String) = UiNode(
        id = "root",
        packageName = "com.example.test",
        className = "android.widget.FrameLayout",
        text = text,
        contentDescription = null,
        resourceId = null,
        bounds = RectData(0, 0, 1080, 2400),
        isClickable = false,
        isEditable = false,
        isEnabled = true,
        isFocused = false,
        isVisibleToUser = true,
        depth = 0
    )

    @Test
    fun coordinateFallbackWithEmptySnapshotIsRejected() {
        val snapshot = UiSnapshot(
            timestampMs = 1_000L,
            packageName = "com.example.test",
            rootNode = null,
            totalNodeCount = 0,
            isLive = true
        )
        val selector = TargetSelector(text = "Continue", fallbackNormalizedX = 0.5f, fallbackNormalizedY = 0.5f)

        val result = resolver.resolve(selector, snapshot, currentTimeMs = 1_000L)

        assertTrue("Empty observation must not manufacture a clickable target",
            result is TargetResolver.ResolutionResult.Failure)
        assertTrue((result as TargetResolver.ResolutionResult.Failure).reason.contains("coordinate-only"))
    }

    @Test
    fun coordinateFallbackWithNoSemanticMatchIsRejectedEvenWhenBoundsExist() {
        val snapshot = UiSnapshot(
            timestampMs = 1_000L,
            packageName = "com.example.test",
            rootNode = node("Welcome"),
            totalNodeCount = 1,
            isLive = true
        )
        val selector = TargetSelector(text = "Delete account", fallbackNormalizedX = 0.5f, fallbackNormalizedY = 0.5f)

        val result = resolver.resolve(selector, snapshot, currentTimeMs = 1_000L)

        assertTrue("A coordinate guess must not be returned as a successful semantic match",
            result is TargetResolver.ResolutionResult.Failure)
    }

}
