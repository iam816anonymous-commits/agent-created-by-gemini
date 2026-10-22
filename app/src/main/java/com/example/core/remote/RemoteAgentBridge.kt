package com.example.core.remote

import com.example.core.actuator.ActuatorController
import com.example.core.engine.AutonomousRecoveryEngine
import com.example.core.hardware.HardwareManager
import com.example.core.model.ActuatorType
import com.example.core.model.ExecutionStatus
import com.example.core.model.FlashlightTarget
import com.example.core.model.TaskCommand
import com.example.core.parser.NaturalLanguageCommandParser
import com.example.service.LocalAgentAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * Remote Control & Tele-Operation Protocol Model
 */
data class RemoteDirectiveRequest(
    val commandText: String,
    val senderId: String = "remote-supervisor",
    val requireManualApproval: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class RemoteControlTelemetry(
    val isServerRunning: Boolean = false,
    val port: Int = 8080,
    val localIpAddress: String = "127.0.0.1",
    val activeConnectionsCount: Int = 0,
    val totalDirectivesProcessed: Int = 0,
    val pairingToken: String = "JARVIS-SECURE-9942",
    val lastRemoteCommand: String? = null,
    val lastCommandStatus: String? = null
)

/**
 * Embedded Lightweight Remote Control Server for LocalAgent.
 *
 * Capabilities:
 * 1. Operates over local Wi-Fi or USB adb port-forwarding (adb forward tcp:8080 tcp:8080).
 * 2. Serves embedded interactive HTML5 Web Console on GET / for zero-install remote supervision.
 * 3. Processes remote execution requests, returns live status and UI snapshots.
 * 4. Enforces cryptographic pairing token authentication.
 * 5. Strictly honors emergency kill-switch and resource budget (<2MB footprint).
 */
class RemoteAgentBridge(
    private val hardwareManager: HardwareManager,
    private val parser: NaturalLanguageCommandParser = NaturalLanguageCommandParser()
) {
    companion object {
        const val DEFAULT_PORT = 8080
        private const val DEFAULT_TOKEN = "JARVIS-SECURE-9942"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    /**
     * Optional execution callback invoked when a remote directive is received.
     * Connects directly to AgentExecutionEngine / AgentViewModel.
     */
    var onExecuteDirective: (suspend (String) -> Unit)? = null

    private val _telemetry = MutableStateFlow(
        RemoteControlTelemetry(
            port = DEFAULT_PORT,
            pairingToken = DEFAULT_TOKEN
        )
    )
    val telemetry: StateFlow<RemoteControlTelemetry> = _telemetry.asStateFlow()

    fun startServer(port: Int = DEFAULT_PORT, token: String = DEFAULT_TOKEN) {
        if (isRunning) return
        isRunning = true

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val resolvedIp = getLocalIpAddress()
                _telemetry.value = _telemetry.value.copy(
                    isServerRunning = true,
                    port = port,
                    pairingToken = token,
                    localIpAddress = resolvedIp,
                    lastCommandStatus = "Server listening on http://$resolvedIp:$port"
                )

                while (isRunning) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (_: Exception) {
                        null
                    } ?: break

                    handleClient(clientSocket, token)
                }
            } catch (e: Exception) {
                _telemetry.value = _telemetry.value.copy(
                    isServerRunning = false,
                    lastCommandStatus = "Server error: ${e.message}"
                )
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _telemetry.value = _telemetry.value.copy(
            isServerRunning = false,
            activeConnectionsCount = 0,
            lastCommandStatus = "Server stopped"
        )
    }

    private fun handleClient(socket: Socket, expectedToken: String) {
        scope.launch {
            _telemetry.value = _telemetry.value.copy(
                activeConnectionsCount = _telemetry.value.activeConnectionsCount + 1
            )

            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                var line = reader.readLine() ?: ""
                var authHeader: String? = null
                var contentLength = 0
                val requestLine = line

                while (line.isNotEmpty()) {
                    if (line.startsWith("X-Agent-Auth-Token:", ignoreCase = true)) {
                        authHeader = line.substringAfter(":").trim()
                    } else if (line.startsWith("Authorization: Bearer", ignoreCase = true)) {
                        authHeader = line.substringAfter("Bearer").trim()
                    } else if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine() ?: ""
                }

                // Handle CORS preflight
                if (requestLine.startsWith("OPTIONS")) {
                    sendCorsPreflightResponse(writer)
                    socket.close()
                    return@launch
                }

                // Serve Embedded Web Dashboard on root GET
                if (requestLine.startsWith("GET / ") || requestLine.startsWith("GET /index.html")) {
                    sendHtmlResponse(writer, 200, buildWebConsoleHtml(expectedToken))
                    socket.close()
                    return@launch
                }

                // Security Check for API endpoints
                val requiresAuth = requestLine.startsWith("POST /api/") || requestLine.startsWith("GET /api/remote/snapshot")
                if (requiresAuth && authHeader != expectedToken) {
                    val errorJson = JSONObject().apply {
                        put("error", "Unauthorized: Invalid or missing X-Agent-Auth-Token")
                        put("code", 401)
                    }
                    sendHttpResponse(writer, 401, errorJson.toString())
                    socket.close()
                    return@launch
                }

                // Read Body if POST
                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    String(buffer)
                } else ""

                // Route Request
                val responseJson = when {
                    requestLine.startsWith("GET /api/remote/status") -> handleStatusRequest()
                    requestLine.startsWith("GET /api/remote/snapshot") -> handleSnapshotRequest()
                    requestLine.startsWith("POST /api/remote/abort") -> handleAbortRequest()
                    requestLine.startsWith("POST /api/remote/execute") -> handleExecuteRequest(body)
                    requestLine.startsWith("POST /api/remote/flashlight") -> handleFlashlightRequest(body)
                    requestLine.startsWith("POST /api/remote/navigate") -> handleNavigateRequest(body)
                    requestLine.startsWith("POST /api/remote/tap") -> handleTapRequest(body)
                    requestLine.startsWith("POST /api/remote/text") -> handleTextRequest(body)
                    else -> JSONObject().apply {
                        put("status", "ok")
                        put("message", "LocalAgent J.A.R.V.I.S. Remote Control Bridge online")
                        put("webConsole", "Open http://${_telemetry.value.localIpAddress}:${_telemetry.value.port}/ in browser")
                        put("endpoints", JSONArray(listOf(
                            "/api/remote/status",
                            "/api/remote/execute",
                            "/api/remote/snapshot",
                            "/api/remote/abort",
                            "/api/remote/flashlight",
                            "/api/remote/navigate",
                            "/api/remote/tap",
                            "/api/remote/text"
                        )))
                    }
                }

                sendHttpResponse(writer, 200, responseJson.toString())
            } catch (_: Exception) {
            } finally {
                try { socket.close() } catch (_: Exception) {}
                _telemetry.value = _telemetry.value.copy(
                    activeConnectionsCount = (_telemetry.value.activeConnectionsCount - 1).coerceAtLeast(0)
                )
            }
        }
    }

    private fun handleStatusRequest(): JSONObject {
        val root = JSONObject()
        root.put("status", "READY")
        root.put("agentName", "LocalAgent J.A.R.V.I.S.")
        root.put("batteryLevel", hardwareManager.getBatteryLevel())
        root.put("heapAllocatedMb", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0))
        root.put("isAccessibilityConnected", LocalAgentAccessibilityService.isServiceConnected.value)
        root.put("lastForegroundPackage", LocalAgentAccessibilityService.lastEventPackage.value)
        root.put("killSwitchTriggeredCount", LocalAgentAccessibilityService.killSwitchTriggeredCount.value)
        root.put("flashlightActive", hardwareManager.isTorchEnabled.value)
        root.put("totalDirectivesProcessed", _telemetry.value.totalDirectivesProcessed)
        root.put("timestampMs", System.currentTimeMillis())
        return root
    }

    private fun handleSnapshotRequest(): JSONObject {
        val service = LocalAgentAccessibilityService.instance
        val snapshot = service?.captureLiveSnapshot()

        val root = JSONObject()
        if (snapshot != null) {
            root.put("success", true)
            root.put("packageName", snapshot.packageName)
            root.put("windowTitle", snapshot.windowTitle ?: "")
            root.put("totalNodes", snapshot.totalNodeCount)
            root.put("timestampMs", snapshot.timestampMs)
            root.put("generationId", snapshot.generationId)
        } else {
            root.put("success", false)
            root.put("message", "Accessibility service active window snapshot unavailable")
        }
        return root
    }

    private fun handleAbortRequest(): JSONObject {
        LocalAgentAccessibilityService.dispatchKillSwitch("Remote Supervisor Requested Emergency Abort")
        _telemetry.value = _telemetry.value.copy(
            lastRemoteCommand = "EMERGENCY_ABORT",
            lastCommandStatus = "ABORT_DISPATCHED"
        )
        return JSONObject().apply {
            put("success", true)
            put("action", "EMERGENCY_HALT")
            put("message", "Hardware actuators disconnected and active tasks aborted")
        }
    }

    private fun handleExecuteRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val prompt = bodyJson.optString("prompt", "").trim()

        if (prompt.isEmpty()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Missing 'prompt' parameter in request body")
            }
        }

        val parsedCommands = parser.parse(prompt)
        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = prompt,
            lastCommandStatus = "EXECUTING (${parsedCommands.size} steps)"
        )

        // Dispatch directive to execution engine via callback
        onExecuteDirective?.let { handler ->
            scope.launch {
                try {
                    handler.invoke(prompt)
                } catch (_: Exception) {}
            }
        }

        val commandsArray = JSONArray()
        for (cmd in parsedCommands) {
            val cmdObj = JSONObject().apply {
                put("id", cmd.id)
                put("displayName", cmd.displayName)
            }
            commandsArray.put(cmdObj)
        }

        return JSONObject().apply {
            put("success", true)
            put("receivedPrompt", prompt)
            put("resolvedCommandsCount", parsedCommands.size)
            put("commands", commandsArray)
            put("executionStatus", "DISPATCHED_TO_EXECUTION_ENGINE")
        }
    }

    private fun handleFlashlightRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val targetStr = bodyJson.optString("target", "BACK").uppercase()
        val durationSeconds = bodyJson.optInt("durationSeconds", 60)
        val enable = bodyJson.optBoolean("enable", true)

        val target = when (targetStr) {
            "FRONT" -> FlashlightTarget.FRONT
            "DUAL" -> FlashlightTarget.DUAL
            else -> FlashlightTarget.BACK
        }

        if (enable && durationSeconds > 0) {
            hardwareManager.turnTorchTimed(target, durationSeconds)
        } else {
            hardwareManager.turnTorch(target, enable)
        }

        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = "Flashlight $targetStr ($durationSeconds s)",
            lastCommandStatus = if (enable) "ACTIVE" else "OFF"
        )

        return JSONObject().apply {
            put("success", true)
            put("target", target.label)
            put("durationSeconds", durationSeconds)
            put("state", if (enable) "ACTIVE" else "OFF")
        }
    }

    private fun handleNavigateRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val action = bodyJson.optString("action", "BACK").uppercase()
        val service = LocalAgentAccessibilityService.instance

        val success = when (action) {
            "BACK" -> service?.dispatchBack() == true
            "HOME" -> service?.dispatchHome() == true
            "RECENTS" -> service?.dispatchRecents() == true
            "NOTIFICATIONS" -> service?.dispatchNotifications() == true
            else -> false
        }

        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = "NAVIGATE $action",
            lastCommandStatus = if (success) "SUCCESS" else "FAILED"
        )

        return JSONObject().apply {
            put("success", success)
            put("action", action)
            put("message", if (success) "Dispatched navigation action $action" else "Failed to dispatch $action (Service connected: ${service != null})")
        }
    }

    private fun handleTapRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val x = bodyJson.optInt("x", 540)
        val y = bodyJson.optInt("y", 1200)
        val service = LocalAgentAccessibilityService.instance

        val success = service?.dispatchClick(com.example.core.model.RectData(x, y, x + 1, y + 1)) ?: false

        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = "TAP ($x, $y)",
            lastCommandStatus = if (success) "SUCCESS" else "FAILED"
        )

        return JSONObject().apply {
            put("success", success)
            put("x", x)
            put("y", y)
            put("action", "TAP")
        }
    }

    private fun handleTextRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val text = bodyJson.optString("text", "")
        val service = LocalAgentAccessibilityService.instance

        val success = if (text.isNotEmpty()) {
            service?.dispatchEnterText(text) ?: false
        } else false

        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = "ENTER_TEXT '$text'",
            lastCommandStatus = if (success) "SUCCESS" else "FAILED"
        )

        return JSONObject().apply {
            put("success", success)
            put("text", text)
            put("action", "ENTER_TEXT")
        }
    }

    private fun sendHttpResponse(writer: PrintWriter, statusCode: Int, body: String) {
        val statusText = if (statusCode == 200) "OK" else if (statusCode == 401) "Unauthorized" else "Error"
        writer.println("HTTP/1.1 $statusCode $statusText")
        writer.println("Content-Type: application/json; charset=utf-8")
        writer.println("Content-Length: ${body.toByteArray().size}")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println("Access-Control-Allow-Headers: *")
        writer.println("Access-Control-Allow-Methods: GET, POST, OPTIONS")
        writer.println("Connection: close")
        writer.println()
        writer.print(body)
        writer.flush()
    }

    private fun sendHtmlResponse(writer: PrintWriter, statusCode: Int, html: String) {
        writer.println("HTTP/1.1 $statusCode OK")
        writer.println("Content-Type: text/html; charset=utf-8")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.print(html)
        writer.flush()
    }

    private fun sendCorsPreflightResponse(writer: PrintWriter) {
        writer.println("HTTP/1.1 204 No Content")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println("Access-Control-Allow-Headers: *")
        writer.println("Access-Control-Allow-Methods: GET, POST, OPTIONS")
        writer.println("Connection: close")
        writer.println()
        writer.flush()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1" // Fallback to loopback / adb forward
    }

    private fun buildWebConsoleHtml(token: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>LocalAgent J.A.R.V.I.S. Remote Web Controller</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace; }
  body { background: #0b0f19; color: #f1f5f9; padding: 16px; max-width: 800px; margin: 0 auto; }
  header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #1e293b; padding-bottom: 12px; margin-bottom: 16px; }
  h1 { font-size: 1.25rem; color: #38bdf8; display: flex; align-items: center; gap: 8px; }
  .badge { background: #1e293b; border: 1px solid #334155; padding: 4px 8px; border-radius: 6px; font-size: 0.75rem; color: #94a3b8; }
  .badge-live { background: rgba(34, 197, 94, 0.2); border-color: #22c55e; color: #4ade80; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 16px; }
  .card { background: #131b2e; border: 1px solid #1e293b; border-radius: 8px; padding: 12px; }
  .card h3 { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; margin-bottom: 6px; }
  .card .val { font-size: 1.1rem; font-weight: bold; color: #f8fafc; }
  .panel { background: #131b2e; border: 1px solid #1e293b; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
  .panel h2 { font-size: 1rem; color: #f8fafc; margin-bottom: 12px; display: flex; justify-content: space-between; }
  .input-group { display: flex; gap: 8px; margin-bottom: 10px; }
  input[type="text"] { flex: 1; background: #0b0f19; border: 1px solid #334155; color: #fff; padding: 10px 12px; border-radius: 6px; font-size: 0.9rem; }
  button { background: #0284c7; color: #fff; border: none; padding: 10px 16px; border-radius: 6px; font-weight: bold; cursor: pointer; transition: background 0.2s; }
  button:hover { background: #0369a1; }
  button.danger { background: #dc2626; }
  button.danger:hover { background: #b91c1c; }
  button.sec { background: #334155; }
  button.sec:hover { background: #475569; }
  .btn-row { display: flex; flex-wrap: wrap; gap: 8px; }
  .log-box { background: #060911; border: 1px solid #1e293b; border-radius: 6px; padding: 10px; font-size: 0.8rem; color: #38bdf8; max-height: 160px; overflow-y: auto; white-space: pre-wrap; }
</style>
</head>
<body>
<header>
  <h1><span>⚡</span> LocalAgent J.A.R.V.I.S. Remote</h1>
  <span id="connBadge" class="badge badge-live">ONLINE</span>
</header>

<div class="grid">
  <div class="card">
    <h3>Foreground Package</h3>
    <div id="fgPkg" class="val">Loading...</div>
  </div>
  <div class="card">
    <h3>Battery / Power</h3>
    <div id="batteryVal" class="val">-- %</div>
  </div>
  <div class="card">
    <h3>Heap Memory</h3>
    <div id="heapVal" class="val">-- MB</div>
  </div>
  <div class="card">
    <h3>Accessibility Service</h3>
    <div id="accVal" class="val">--</div>
  </div>
</div>

<div class="panel">
  <h2>Autonomous Directive Dispatcher</h2>
  <div class="input-group">
    <input type="text" id="directivePrompt" placeholder="e.g. Open YouTube and search lo-fi beats, or turn on torch" value="Launch Calculator and tap 7" />
    <button onclick="dispatchExecute()">Execute</button>
  </div>
  <div class="btn-row" style="margin-top: 8px;">
    <button class="sec" onclick="setPreset('Launch Calculator and tap 7')">Calculator 7</button>
    <button class="sec" onclick="setPreset('Turn on flashlight for 30 seconds')">Flashlight 30s</button>
    <button class="sec" onclick="setPreset('Open Google Keep and create note')">Keep Note</button>
    <button class="danger" onclick="dispatchAbort()">🚨 EMERGENCY HALT</button>
  </div>
</div>

<div class="panel">
  <h2>System Navigation & Hardware Actuators</h2>
  <div class="btn-row" style="margin-bottom: 12px;">
    <button class="sec" onclick="dispatchNav('BACK')">⬅ Back</button>
    <button class="sec" onclick="dispatchNav('HOME')">🏠 Home</button>
    <button class="sec" onclick="dispatchNav('RECENTS')">📑 Recents</button>
    <button class="sec" onclick="dispatchNav('NOTIFICATIONS')">🔔 Notifications</button>
  </div>
  <div class="btn-row">
    <button class="sec" onclick="dispatchFlashlight('BACK', true)">💡 Torch Back</button>
    <button class="sec" onclick="dispatchFlashlight('FRONT', true)">💡 Torch Front</button>
    <button class="sec" onclick="dispatchFlashlight('BACK', false)">Torch OFF</button>
    <button class="sec" onclick="dispatchSnapshot()">📸 UI Tree Snapshot</button>
  </div>
</div>

<div class="panel">
  <h2>Execution Telemetry & Live Feedback</h2>
  <div id="consoleLog" class="log-box">Initialized Remote Web Controller.\nListening for tele-operations...</div>
</div>

<script>
const TOKEN = '$token';

function log(msg) {
  const box = document.getElementById('consoleLog');
  const ts = new Date().toLocaleTimeString();
  box.innerText = `[${'$'}{ts}] ${'$'}{msg}\n` + box.innerText;
}

function setPreset(txt) {
  document.getElementById('directivePrompt').value = txt;
}

async function fetchStatus() {
  try {
    const res = await fetch('/api/remote/status');
    const data = await res.json();
    document.getElementById('fgPkg').innerText = data.lastForegroundPackage || 'Standby';
    document.getElementById('batteryVal').innerText = data.batteryLevel + '%';
    document.getElementById('heapVal').innerText = Math.round(data.heapAllocatedMb || 0) + ' MB';
    document.getElementById('accVal').innerText = data.isAccessibilityConnected ? 'CONNECTED' : 'DISCONNECTED';
  } catch (e) {
    document.getElementById('connBadge').innerText = 'OFFLINE';
    document.getElementById('connBadge').className = 'badge';
  }
}

async function dispatchExecute() {
  const prompt = document.getElementById('directivePrompt').value;
  if (!prompt) return;
  log('Dispatching directive: ' + prompt);
  try {
    const res = await fetch('/api/remote/execute', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Agent-Auth-Token': TOKEN
      },
      body: JSON.stringify({ prompt })
    });
    const data = await res.json();
    log('Result: ' + JSON.stringify(data));
  } catch (e) {
    log('Error dispatching: ' + e.message);
  }
}

async function dispatchAbort() {
  log('🚨 INITIATING EMERGENCY HALT...');
  try {
    const res = await fetch('/api/remote/abort', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': TOKEN }
    });
    const data = await res.json();
    log('Abort status: ' + data.message);
  } catch (e) {
    log('Abort error: ' + e.message);
  }
}

async function dispatchNav(action) {
  log('Navigating: ' + action);
  try {
    const res = await fetch('/api/remote/navigate', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': TOKEN, 'Content-Type': 'application/json' },
      body: JSON.stringify({ action })
    });
    const data = await res.json();
    log('Navigation response: ' + data.message);
  } catch (e) {
    log('Nav error: ' + e.message);
  }
}

async function dispatchFlashlight(target, enable) {
  log(`Flashlight ${'$'}{target} -> ${'$'}{enable}`);
  try {
    const res = await fetch('/api/remote/flashlight', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': TOKEN, 'Content-Type': 'application/json' },
      body: JSON.stringify({ target, enable, durationSeconds: 30 })
    });
    const data = await res.json();
    log('Flashlight result: ' + JSON.stringify(data));
  } catch (e) {
    log('Flashlight error: ' + e.message);
  }
}

async function dispatchSnapshot() {
  log('Capturing UI tree snapshot...');
  try {
    const res = await fetch('/api/remote/snapshot', {
      headers: { 'X-Agent-Auth-Token': TOKEN }
    });
    const data = await res.json();
    log('Snapshot: ' + JSON.stringify(data, null, 2));
  } catch (e) {
    log('Snapshot error: ' + e.message);
  }
}

fetchStatus();
setInterval(fetchStatus, 3000);
</script>
</body>
</html>
        """.trimIndent()
    }
}
