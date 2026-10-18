package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 11 — Overlay and In-Context Interaction Service
 * Provides an optional, permission-guarded floating supervisor HUD.
 *
 * Guarantees:
 * 1. Checks Settings.canDrawOverlays(context) before attaching window.
 * 2. Provides floating status badge and tactile emergency stop button.
 * 3. Draggable on touch without obstructing foreground app interactions.
 * 4. Cannot bypass TaskApprovalGate execution approvals.
 * 5. Full lifecycle cleanup in onDestroy().
 */
class LocalAgentOverlayService : Service() {

    companion object {
        private const val TAG = "LocalAgentOverlay"

        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        private val _currentStatusText = MutableStateFlow("LocalAgent: IDLE")
        val currentStatusText: StateFlow<String> = _currentStatusText.asStateFlow()

        fun updateStatus(status: String) {
            _currentStatusText.value = status
        }

        fun startOverlay(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, LocalAgentOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, LocalAgentOverlayService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createFloatingHud()
        _isOverlayActive.value = true
    }

    private fun createFloatingHud() {
        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 120
        }

        // Programmatic HUD container with cyber aesthetic
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE030712")) // Black with 93% opacity
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#06B6D4")) // Cyan border
            }
            elevation = 16f
        }

        val statusLabel = TextView(this).apply {
            text = _currentStatusText.value
            setTextColor(Color.parseColor("#22D3EE")) // Cyan text
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 16, 0)
        }

        val stopButton = Button(this).apply {
            text = "STOP"
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444")) // Red button
                cornerRadius = 16f
            }
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                LocalAgentAccessibilityService.dispatchKillSwitch("Overlay Floating HUD Emergency Stop")
                _currentStatusText.value = "ABORTED BY USER"
            }
        }

        container.addView(statusLabel)
        container.addView(stopButton)

        // Draggable touch listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        wm.updateViewLayout(container, params)
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(container, params)
        overlayView = container

        // Collect status changes
        serviceScope.launch {
            _currentStatusText.collect { status ->
                statusLabel.text = status
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isOverlayActive.value = false
        serviceScope.cancel()
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
    }
}
