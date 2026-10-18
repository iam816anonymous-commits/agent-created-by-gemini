package com.example.core.recorder

import com.example.core.model.ActionType
import com.example.core.model.RecordedInteraction
import com.example.core.model.ScrollDirection
import com.example.core.model.TargetSelector
import com.example.core.model.UiNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class VisualInteractionRecorder {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedEvents = MutableStateFlow<List<RecordedInteraction>>(emptyList())
    val recordedEvents: StateFlow<List<RecordedInteraction>> = _recordedEvents.asStateFlow()

    private var recordingStartTimestampMs: Long = 0L
    private var lastEventTimestampMs: Long = 0L

    fun startRecording() {
        val now = System.currentTimeMillis()
        recordingStartTimestampMs = now
        lastEventTimestampMs = now
        _recordedEvents.value = emptyList()
        _isRecording.value = true
    }

    fun stopRecording(): List<RecordedInteraction> {
        _isRecording.value = false
        return _recordedEvents.value
    }

    fun recordTap(
        node: UiNode?,
        coordinates: Pair<Int, Int>? = null,
        packageName: String = node?.packageName ?: ""
    ) {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val delayMs = if (lastEventTimestampMs > 0) now - lastEventTimestampMs else 0
        lastEventTimestampMs = now

        val selector = node?.let {
            TargetSelector(
                text = it.text,
                contentDescription = it.contentDescription,
                resourceId = it.resourceId,
                className = it.className.substringAfterLast(".")
            )
        }

        val label = node?.text ?: node?.contentDescription ?: node?.resourceId?.substringAfterLast("/") ?: "Tap at (${coordinates?.first ?: 0}, ${coordinates?.second ?: 0})"

        val interaction = RecordedInteraction(
            id = UUID.randomUUID().toString(),
            timestampMs = now,
            actionType = ActionType.TAP,
            targetLabel = label,
            targetSelector = selector,
            targetBounds = node?.bounds,
            touchCoordinates = coordinates ?: node?.let { Pair(it.bounds.centerX, it.bounds.centerY) },
            packageName = packageName,
            snapshotSummary = "Target: $label",
            delayFromPreviousMs = delayMs
        )

        _recordedEvents.update { it + interaction }
    }

    fun recordTextEntry(
        text: String,
        targetField: UiNode? = null,
        packageName: String = targetField?.packageName ?: ""
    ) {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val delayMs = if (lastEventTimestampMs > 0) now - lastEventTimestampMs else 0
        lastEventTimestampMs = now

        val selector = targetField?.let {
            TargetSelector(
                text = it.text,
                contentDescription = it.contentDescription,
                resourceId = it.resourceId,
                className = "EditText"
            )
        }

        val label = "Enter '$text'"

        val interaction = RecordedInteraction(
            id = UUID.randomUUID().toString(),
            timestampMs = now,
            actionType = ActionType.ENTER_TEXT,
            targetLabel = label,
            targetSelector = selector,
            targetBounds = targetField?.bounds,
            textPayload = text,
            packageName = packageName,
            snapshotSummary = "Field: ${targetField?.resourceId ?: "Active Input"}",
            delayFromPreviousMs = delayMs
        )

        _recordedEvents.update { it + interaction }
    }

    fun recordScroll(
        direction: ScrollDirection,
        containerNode: UiNode? = null,
        packageName: String = containerNode?.packageName ?: ""
    ) {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val delayMs = if (lastEventTimestampMs > 0) now - lastEventTimestampMs else 0
        lastEventTimestampMs = now

        val selector = containerNode?.let {
            TargetSelector(
                resourceId = it.resourceId,
                className = it.className.substringAfterLast(".")
            )
        }

        val label = "Scroll ${direction.name}"

        val interaction = RecordedInteraction(
            id = UUID.randomUUID().toString(),
            timestampMs = now,
            actionType = ActionType.SCROLL,
            targetLabel = label,
            targetSelector = selector,
            targetBounds = containerNode?.bounds,
            scrollDirection = direction,
            packageName = packageName,
            snapshotSummary = "Container: ${containerNode?.resourceId ?: "Window"}",
            delayFromPreviousMs = delayMs
        )

        _recordedEvents.update { it + interaction }
    }

    fun recordSwipe(
        startXRatio: Float,
        startYRatio: Float,
        endXRatio: Float,
        endYRatio: Float,
        durationMs: Long = 300L,
        packageName: String = ""
    ) {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val delayMs = if (lastEventTimestampMs > 0) now - lastEventTimestampMs else 0
        lastEventTimestampMs = now

        val label = "Swipe (${(startXRatio * 100).toInt()}%, ${(startYRatio * 100).toInt()}%) -> (${(endXRatio * 100).toInt()}%, ${(endYRatio * 100).toInt()}%)"

        val interaction = RecordedInteraction(
            id = UUID.randomUUID().toString(),
            timestampMs = now,
            actionType = ActionType.SWIPE,
            targetLabel = label,
            packageName = packageName,
            delayFromPreviousMs = delayMs
        )

        _recordedEvents.update { it + interaction }
    }

    fun recordDrawerAction(isOpen: Boolean, packageName: String = "") {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val delayMs = if (lastEventTimestampMs > 0) now - lastEventTimestampMs else 0
        lastEventTimestampMs = now

        val interaction = RecordedInteraction(
            id = UUID.randomUUID().toString(),
            timestampMs = now,
            actionType = if (isOpen) ActionType.OPEN_DRAWER else ActionType.CLOSE_DRAWER,
            targetLabel = if (isOpen) "Open Navigation Drawer" else "Close Navigation Drawer",
            packageName = packageName,
            delayFromPreviousMs = delayMs
        )

        _recordedEvents.update { it + interaction }
    }

    fun clear() {
        _recordedEvents.value = emptyList()
    }
}
