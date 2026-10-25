package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.model.RectData
import com.example.core.model.ScrollDirection
import com.example.core.model.UiNode
import com.example.core.model.UiSnapshot
import com.example.core.recorder.VisualInteractionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LocalAgentAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance = this
        _isServiceConnected.value = true
        _lastStatusMessage.value = "Accessibility Service connected and ready"
        liveEventStream.tryEmit(
            com.example.core.model.LiveConsoleEntry(
                tag = "Service",
                message = "LocalAgent Accessibility Service connected",
                level = com.example.core.model.ConsoleLogLevel.INFO
            )
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: ""
        _lastEventPackage.value = pkg
        _lastEventTimeMs.value = System.currentTimeMillis()

        try {
            val typeStr = AccessibilityEvent.eventTypeToString(event.eventType)
            liveEventStream.tryEmit(
                com.example.core.model.LiveConsoleEntry(
                    tag = "A11yEvent",
                    message = "${typeStr.substringAfter("TYPE_")} from '$pkg'",
                    level = com.example.core.model.ConsoleLogLevel.OBSERVATION,
                    extraData = if (event.text.isNotEmpty()) event.text.joinToString("") else null
                )
            )
        } catch (_: Exception) {}

        // Hook into Visual Interaction Recorder if active
        val recorder = activeRecorder
        if (recorder != null && recorder.isRecording.value) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    val source = event.source
                    if (source != null) {
                        val bounds = Rect()
                        source.getBoundsInScreen(bounds)
                        val node = convertNode(source, 0)
                        recorder.recordTap(node, Pair(bounds.centerX(), bounds.centerY()), pkg)
                    }
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    val text = event.text.joinToString("")
                    if (text.isNotBlank()) {
                        val source = event.source
                        val node = source?.let { convertNode(it, 0) }
                        recorder.recordTextEntry(text, node, pkg)
                    }
                }
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    val dir = if (event.scrollY > 0) ScrollDirection.DOWN else ScrollDirection.UP
                    val source = event.source
                    val node = source?.let { convertNode(it, 0) }
                    recorder.recordScroll(dir, node, pkg)
                }
            }
        }
    }

    private var lastVolumeUpPressTimeMs: Long = 0L
    private val doubleClickWindowMs: Long = 650L

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            val diff = now - lastVolumeUpPressTimeMs
            if (diff in 1..doubleClickWindowMs) {
                lastVolumeUpPressTimeMs = 0L
                val reason = "Physical Volume-Up Key double-pressed ($diff ms) - Global Hardware Kill Switch"
                _killSwitchTriggeredCount.value += 1
                _lastKillSwitchReason.value = reason
                onKillSwitchTriggered?.invoke(reason)
                liveEventStream.tryEmit(
                    com.example.core.model.LiveConsoleEntry(
                        tag = "KillSwitch",
                        message = "EMERGENCY ABORT: $reason",
                        level = com.example.core.model.ConsoleLogLevel.ERROR
                    )
                )
                return true // Intercept key event so volume does not unintentionally change
            } else {
                lastVolumeUpPressTimeMs = now
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        _lastStatusMessage.value = "Accessibility Service interrupted"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_instance == this) {
            _instance = null
            _isServiceConnected.value = false
        }
    }

    fun captureLiveSnapshot(): UiSnapshot? {
        val root = rootInActiveWindow ?: run {
            val activeWindow = windows?.firstOrNull { it.isActive }
            activeWindow?.root
        }

        if (root == null) {
            return null
        }

        val packageName = root.packageName?.toString() ?: "unknown"
        val rootUiNode = convertNode(root, depth = 0)
        val totalCount = rootUiNode?.allDescendants?.size ?: 0

        return UiSnapshot(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            packageName = packageName,
            windowTitle = root.className?.toString(),
            rootNode = rootUiNode,
            totalNodeCount = totalCount,
            isLive = true,
            sourceDescription = "Live Accessibility Service (rootInActiveWindow)"
        )
    }

    private fun convertNode(node: AccessibilityNodeInfo, depth: Int): UiNode? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val childList = mutableListOf<UiNode>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val convertedChild = convertNode(child, depth + 1)
                if (convertedChild != null) {
                    childList.add(convertedChild)
                }
            }
        }

        return UiNode(
            id = UUID.randomUUID().toString(),
            packageName = node.packageName?.toString() ?: "",
            className = node.className?.toString() ?: "",
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            resourceId = node.viewIdResourceName,
            bounds = RectData(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isEnabled = node.isEnabled,
            isFocused = node.isFocused,
            isVisibleToUser = node.isVisibleToUser,
            depth = depth,
            children = childList
        )
    }

    fun dispatchClick(targetBounds: RectData): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val matchedNode = findClickableNodeAt(root, targetBounds.centerX, targetBounds.centerY)
            if (matchedNode != null && matchedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        return dispatchGestureClick(targetBounds.centerX.toFloat(), targetBounds.centerY.toFloat())
    }

    private fun findClickableNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findClickableNodeAt(child, x, y)
            if (found != null) return found
        }

        return if (node.isClickable) node else null
    }

    private fun dispatchGestureClick(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun dispatchEnterText(text: String, targetBounds: RectData? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        // Focus target if bounds are provided
        if (targetBounds != null) {
            val target = findClickableNodeAt(root, targetBounds.centerX, targetBounds.centerY)
            target?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: (if (targetBounds != null) findClickableNodeAt(root, targetBounds.centerX, targetBounds.centerY) else null)

        if (focused != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    fun dispatchScroll(direction: ScrollDirection, containerBounds: RectData? = null): Boolean {
        val root = rootInActiveWindow

        // Strategy 1: Perform ACTION_SCROLL on scrollable container if found
        if (root != null) {
            val scrollableNode = findScrollableNode(root, containerBounds)
            if (scrollableNode != null) {
                val action = when (direction) {
                    ScrollDirection.DOWN, ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    ScrollDirection.UP, ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                if (scrollableNode.performAction(action)) {
                    return true
                }
            }
        }

        // Strategy 2: Gesture swipe fallback
        val bounds = containerBounds ?: RectData(100, 400, 980, 1800)
        val cx = bounds.centerX.toFloat()
        val cy = bounds.centerY.toFloat()
        val halfH = (bounds.height * 0.35f)
        val halfW = (bounds.width * 0.35f)

        return when (direction) {
            ScrollDirection.DOWN -> dispatchSwipe(cx, cy + halfH, cx, cy - halfH, 300L)
            ScrollDirection.UP -> dispatchSwipe(cx, cy - halfH, cx, cy + halfH, 300L)
            ScrollDirection.LEFT -> dispatchSwipe(cx + halfW, cy, cx - halfW, cy, 300L)
            ScrollDirection.RIGHT -> dispatchSwipe(cx - halfW, cy, cx + halfW, cy, 300L)
        }
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo, bounds: RectData?): AccessibilityNodeInfo? {
        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)

        if (bounds != null && !Rect.intersects(nodeBounds, Rect(bounds.left, bounds.top, bounds.right, bounds.bottom))) {
            return null
        }

        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child, bounds)
            if (found != null) return found
        }
        return null
    }

    fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(100L, 1000L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun dispatchOpenDrawer(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            // Find hamburger or drawer toggle
            val drawerIcon = findDrawerButton(root)
            if (drawerIcon != null && drawerIcon.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        // Fallback: Edge swipe from left to right (x=10 -> x=650)
        return dispatchSwipe(10f, 900f, 650f, 900f, 350L)
    }

    fun dispatchCloseDrawer(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val closeIcon = findCloseOrScrim(root)
            if (closeIcon != null && closeIcon.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        // Fallback: Swipe right to left or back
        return dispatchSwipe(650f, 900f, 20f, 900f, 300L) || performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun attemptToggleSwitch(desiredState: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val switchNode = findSwitchNode(root, desiredState)
        return switchNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun findSwitchNode(node: AccessibilityNodeInfo, desiredState: Boolean): AccessibilityNodeInfo? {
        val cls = node.className?.toString() ?: ""
        val isSwitch = cls.contains("Switch") || cls.contains("ToggleButton") || node.isCheckable
        if (isSwitch) {
            val currentState = node.isChecked
            if (currentState != desiredState) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSwitchNode(child, desiredState)
            if (found != null) return found
        }
        return null
    }

    private fun findDrawerButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val resId = node.viewIdResourceName?.lowercase() ?: ""

        val isHamburger = desc.contains("navigation") || desc.contains("drawer") || desc.contains("menu") ||
                resId.contains("drawer") || resId.contains("menu") || resId.contains("toggle") ||
                text == "menu" || desc == "open"

        if (isHamburger && node.isClickable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findDrawerButton(child)
            if (found != null) return found
        }
        return null
    }

    private fun findCloseOrScrim(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val resId = node.viewIdResourceName?.lowercase() ?: ""
        if ((desc.contains("close") || resId.contains("scrim") || resId.contains("backdrop")) && node.isClickable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findCloseOrScrim(child)
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile
        private var _instance: LocalAgentAccessibilityService? = null
        val instance: LocalAgentAccessibilityService? get() = _instance

        var activeRecorder: VisualInteractionRecorder? = null

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _lastEventPackage = MutableStateFlow("")
        val lastEventPackage: StateFlow<String> = _lastEventPackage.asStateFlow()

        private val _lastEventTimeMs = MutableStateFlow(0L)
        val lastEventTimeMs: StateFlow<Long> = _lastEventTimeMs.asStateFlow()

        private val _lastStatusMessage = MutableStateFlow("Accessibility Service not running")
        val lastStatusMessage: StateFlow<String> = _lastStatusMessage.asStateFlow()

        var onKillSwitchTriggered: ((reason: String) -> Unit)? = null

        private val _killSwitchTriggeredCount = MutableStateFlow(0)
        val killSwitchTriggeredCount: StateFlow<Int> = _killSwitchTriggeredCount.asStateFlow()

        private val _lastKillSwitchReason = MutableStateFlow<String?>(null)
        val lastKillSwitchReason: StateFlow<String?> = _lastKillSwitchReason.asStateFlow()

        fun dispatchKillSwitch(reason: String = "Physical Abort Kill Switch Triggered") {
            _killSwitchTriggeredCount.value += 1
            _lastKillSwitchReason.value = reason
            onKillSwitchTriggered?.invoke(reason)
            liveEventStream.tryEmit(
                com.example.core.model.LiveConsoleEntry(
                    tag = "KillSwitch",
                    message = "EMERGENCY ABORT: $reason",
                    level = com.example.core.model.ConsoleLogLevel.ERROR
                )
            )
        }

        val liveEventStream = kotlinx.coroutines.flow.MutableSharedFlow<com.example.core.model.LiveConsoleEntry>(replay = 50, extraBufferCapacity = 100)
    }
}
