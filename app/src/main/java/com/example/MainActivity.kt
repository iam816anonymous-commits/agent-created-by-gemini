package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AgentViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.LocalAgentTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalAgentTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private var lastVolUpPressTimeMs = 0L

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            val now = System.currentTimeMillis()
            val diff = now - lastVolUpPressTimeMs
            if (diff in 1..650L) {
                lastVolUpPressTimeMs = 0L
                viewModel.triggerEmergencyKillSwitch("Physical Volume-Up Key double-pressed ($diff ms) - In-App Kill Switch")
                return true
            } else {
                lastVolUpPressTimeMs = now
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission states when returning from system settings
        viewModel.refreshCapabilities()
    }
}
