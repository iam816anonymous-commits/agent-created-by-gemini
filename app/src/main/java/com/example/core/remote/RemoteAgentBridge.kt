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
import java.security.MessageDigest
import java.security.SecureRandom
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
    val pairingToken: String = "",
    val lastRemoteCommand: String? = null,
    val lastCommandStatus: String? = null
)

/**
 * Hardened Embedded Lightweight Remote Control Server for LocalAgent.
 *
 * Security Protections Enforced:
 * 1. Zero hardcoded tokens. Generates an ephemeral 6-digit cryptographic PIN via SecureRandom
 *    on server startup, displayed ONLY on the physical device screen / local notification.
 * 2. Constant-time token verification using MessageDigest.isEqual to prevent timing attacks.
 * 3. Bounded request reader capped at MAX_REQUEST_BODY_BYTES (64KB) to prevent OOM on Helio P23.
 * 4. Strict CORS policy: drops wildcard '*', only permits localhost/paired loopback origin.
 * 5. Web Console on GET / never embeds authentication tokens in plaintext.
 */
class RemoteAgentBridge(
    private val hardwareManager: HardwareManager,
    private val parser: NaturalLanguageCommandParser = NaturalLanguageCommandParser()
) {
    companion object {
        const val DEFAULT_PORT = 8080
        const val MAX_REQUEST_BODY_BYTES = 65536 // 64 KB strict buffer cap
        const val MAX_FAILED_PIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes lockout after 5 failed tries
        const val MAX_HEADER_LINE_LENGTH = 4096 // 4 KB max per header line

        fun generateSecureSessionPin(): String {
            val random = SecureRandom()
            return String.format("%06d", random.nextInt(1_000_000))
        }

        fun constantTimeVerify(provided: String?, expected: String): Boolean {
            if (provided.isNullOrBlank() || expected.isBlank()) return false
            val a = provided.trim().toByteArray(Charsets.UTF_8)
            val b = expected.trim().toByteArray(Charsets.UTF_8)
            return MessageDigest.isEqual(a, b)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    // Brute-force protection state
    @Volatile private var failedAttemptsCount = 0
    @Volatile private var lockoutUntilMs = 0L

    fun isLockedOut(): Boolean = System.currentTimeMillis() < lockoutUntilMs

    fun getRemainingLockoutSeconds(): Long = ((lockoutUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)

    private fun recordFailedAttempt() {
        failedAttemptsCount++
        if (failedAttemptsCount >= MAX_FAILED_PIN_ATTEMPTS) {
            lockoutUntilMs = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            failedAttemptsCount = 0
        }
    }

    private fun recordSuccessfulAttempt() {
        failedAttemptsCount = 0
        lockoutUntilMs = 0L
    }

    /**
     * Bounded header line reader preventing unbounded memory allocation from malicious streams.
     */
    private fun readBoundedLine(reader: BufferedReader, maxLen: Int = MAX_HEADER_LINE_LENGTH): String? {
        val sb = StringBuilder()
        var charCode: Int
        while (reader.read().also { charCode = it } != -1) {
            val ch = charCode.toChar()
            if (ch == '\n') break
            if (ch != '\r') {
                if (sb.length >= maxLen) {
                    throw IllegalArgumentException("Header line exceeds maximum length of $maxLen characters")
                }
                sb.append(ch)
            }
        }
        return if (charCode == -1 && sb.isEmpty()) null else sb.toString()
    }

    /**
     * Optional execution callback invoked when a remote directive is received.
     * Connects directly to AgentExecutionEngine / AgentViewModel.
     */
    var onExecuteDirective: (suspend (String) -> Unit)? = null

    // Ephemeral pairing token generated dynamically per session - never shipped in code
    private var activePairingPin: String = generateSecureSessionPin()
    var onPinGeneratedListener: ((String) -> Unit)? = null

    private val _telemetry = MutableStateFlow(
        RemoteControlTelemetry(
            port = DEFAULT_PORT,
            pairingToken = "******" // Never expose raw token in public telemetry
        )
    )
    val telemetry: StateFlow<RemoteControlTelemetry> = _telemetry.asStateFlow()

    fun startServer(port: Int = DEFAULT_PORT, token: String? = null, loopbackOnly: Boolean = true): String {
        if (isRunning) return activePairingPin
        activePairingPin = token ?: generateSecureSessionPin()
        isRunning = true
        failedAttemptsCount = 0
        lockoutUntilMs = 0L
        onPinGeneratedListener?.invoke(activePairingPin)

        scope.launch {
            try {
                serverSocket = if (loopbackOnly) {
                    ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))
                } else {
                    ServerSocket(port)
                }
                val resolvedIp = if (loopbackOnly) "127.0.0.1" else getLocalIpAddress()
                _telemetry.value = _telemetry.value.copy(
                    isServerRunning = true,
                    port = port,
                    pairingToken = "******",
                    localIpAddress = resolvedIp,
                    lastCommandStatus = "Server listening on http://$resolvedIp:$port (Pair via adb forward tcp:$port tcp:$port)"
                )

                while (isRunning) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (_: Exception) {
                        null
                    } ?: break

                    handleClient(clientSocket, activePairingPin)
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
        return activePairingPin
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
                socket.soTimeout = 8000 // 8-second read timeout prevents hung socket starvation
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val requestLine = try {
                    readBoundedLine(reader)
                } catch (_: Exception) {
                    sendHttpResponse(writer, 431, "{\"error\":\"Request Line Too Large\"}", null)
                    socket.close()
                    return@launch
                } ?: return@launch

                var authHeader: String? = null
                var contentLength = 0
                var originHeader: String? = null
                var headerCount = 0

                try {
                    var line: String? = readBoundedLine(reader)
                    while (!line.isNullOrEmpty()) {
                        if (++headerCount > 64) {
                            sendHttpResponse(writer, 431, "{\"error\":\"Request Header Fields Too Large\"}", originHeader)
                            socket.close()
                            return@launch
                        }
                        if (line.startsWith("X-Agent-Auth-Token:", ignoreCase = true)) {
                            authHeader = line.substringAfter(":").trim()
                        } else if (line.startsWith("Authorization: Bearer", ignoreCase = true)) {
                            authHeader = line.substringAfter("Bearer").trim()
                        } else if (line.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                        } else if (line.startsWith("Origin:", ignoreCase = true)) {
                            originHeader = line.substringAfter(":").trim()
                        }
                        line = readBoundedLine(reader)
                    }
                } catch (e: Exception) {
                    sendHttpResponse(writer, 431, "{\"error\":\"Header line exceeds limit\"}", originHeader)
                    socket.close()
                    return@launch
                }

                // Handle CORS preflight
                if (requestLine.startsWith("OPTIONS")) {
                    sendCorsPreflightResponse(writer, originHeader)
                    socket.close()
                    return@launch
                }

                // Serve Embedded Web Dashboard on root GET (Never leak token in HTML)
                if (requestLine.startsWith("GET / ") || requestLine.startsWith("GET /index.html")) {
                    sendHtmlResponse(writer, 200, buildWebConsoleHtml())
                    socket.close()
                    return@launch
                }

                // Security Check for all API endpoints using constant-time comparison and lockout guard
                val requiresAuth = requestLine.contains("/api/remote/")
                if (requiresAuth) {
                    if (isLockedOut()) {
                        val remSec = getRemainingLockoutSeconds()
                        val errJson = JSONObject().apply {
                            put("error", "Too Many Requests: Brute-force lockout active. Try again in ${remSec}s.")
                            put("code", 429)
                            put("retryAfterSeconds", remSec)
                        }
                        sendHttpResponse(writer, 429, errJson.toString(), originHeader)
                        socket.close()
                        return@launch
                    }

                    if (!constantTimeVerify(authHeader, expectedToken)) {
                        recordFailedAttempt()
                        val remAttempts = (MAX_FAILED_PIN_ATTEMPTS - failedAttemptsCount).coerceAtLeast(0)
                        val errorJson = JSONObject().apply {
                            put("error", "Unauthorized: Invalid or missing pairing PIN. Please enter the 6-digit code shown on the Android device screen. Remaining attempts: $remAttempts.")
                            put("code", 401)
                            put("remainingAttempts", remAttempts)
                        }
                        sendHttpResponse(writer, 401, errorJson.toString(), originHeader)
                        socket.close()
                        return@launch
                    }

                    // Successful PIN authentication
                    recordSuccessfulAttempt()
                }

                // Safe bounded body reader (MAX_REQUEST_BODY_BYTES = 64KB cap)
                if (contentLength > MAX_REQUEST_BODY_BYTES) {
                    val errJson = JSONObject().apply {
                        put("error", "Payload Too Large: Content length ($contentLength bytes) exceeds 64KB Helio P23 memory cap")
                        put("code", 413)
                    }
                    sendHttpResponse(writer, 413, errJson.toString(), originHeader)
                    socket.close()
                    return@launch
                }

                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val read = reader.read(buffer, readTotal, contentLength - readTotal)
                        if (read == -1) break
                        readTotal += read
                    }
                    String(buffer, 0, readTotal)
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
                        put("security", "Pairing PIN required on all mutations. Wildcard CORS disabled. 64KB buffer cap active.")
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

                sendHttpResponse(writer, 200, responseJson.toString(), originHeader)
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
        root.put("bufferCapBytes", MAX_REQUEST_BODY_BYTES)
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

    private suspend fun handleExecuteRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val prompt = bodyJson.optString("prompt", "").trim()

        if (prompt.isEmpty()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Empty prompt provided")
            }
        }

        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = prompt,
            lastCommandStatus = "DISPATCHING"
        )

        val callback = onExecuteDirective
        return if (callback != null) {
            callback.invoke(prompt)
            JSONObject().apply {
                put("success", true)
                put("action", "DISPATCHED_TO_ENGINE")
                put("prompt", prompt)
            }
        } else {
            val parsed = parser.parse(prompt)
            JSONObject().apply {
                put("success", true)
                put("action", "PARSED_LOCAL")
                put("commandsCount", parsed.size)
                put("commands", JSONArray(parsed.map { it.displayName }))
            }
        }
    }

    private fun handleFlashlightRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val target = bodyJson.optString("target", "BACK")
        val enable = bodyJson.optBoolean("enable", true)
        val duration = bodyJson.optInt("durationSeconds", 30)

        val flashTarget = when (target.uppercase()) {
            "FRONT" -> FlashlightTarget.FRONT
            "DUAL" -> FlashlightTarget.DUAL
            else -> FlashlightTarget.BACK
        }

        val success = hardwareManager.setTorch(enable, flashTarget).isSuccess
        _telemetry.value = _telemetry.value.copy(
            totalDirectivesProcessed = _telemetry.value.totalDirectivesProcessed + 1,
            lastRemoteCommand = "FLASHLIGHT $target $enable (${duration}s)",
            lastCommandStatus = if (success) "SUCCESS" else "FAILED"
        )

        return JSONObject().apply {
            put("success", success)
            put("target", target)
            put("enabled", enable)
            put("durationSeconds", duration)
        }
    }

    private fun handleNavigateRequest(body: String): JSONObject {
        val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val action = bodyJson.optString("action", "BACK").uppercase()
        val service = LocalAgentAccessibilityService.instance

        val success = when (action) {
            "BACK" -> service?.dispatchBack() ?: false
            "HOME" -> service?.dispatchHome() ?: false
            "RECENTS" -> service?.dispatchRecents() ?: false
            "NOTIFICATIONS" -> service?.dispatchNotifications() ?: false
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

    fun isOriginAllowed(clientOrigin: String?): Boolean {
        if (clientOrigin.isNullOrBlank()) return false
        val uri = try { java.net.URI(clientOrigin) } catch (_: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host == "localhost" || host == "127.0.0.1"
    }

    private fun resolveAllowedOrigin(clientOrigin: String?): String {
        return if (isOriginAllowed(clientOrigin)) {
            clientOrigin!!
        } else {
            "http://127.0.0.1:${_telemetry.value.port}"
        }
    }

    private fun sendHttpResponse(writer: PrintWriter, statusCode: Int, body: String, origin: String?) {
        val statusText = when (statusCode) {
            200 -> "OK"
            401 -> "Unauthorized"
            413 -> "Payload Too Large"
            429 -> "Too Many Requests"
            431 -> "Request Header Fields Too Large"
            else -> "Error"
        }
        val allowedOrigin = resolveAllowedOrigin(origin)
        writer.println("HTTP/1.1 $statusCode $statusText")
        writer.println("Content-Type: application/json; charset=utf-8")
        writer.println("Content-Length: ${body.toByteArray().size}")
        writer.println("Access-Control-Allow-Origin: $allowedOrigin")
        writer.println("Access-Control-Allow-Headers: X-Agent-Auth-Token, Authorization, Content-Type")
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

    private fun sendCorsPreflightResponse(writer: PrintWriter, origin: String?) {
        val allowedOrigin = resolveAllowedOrigin(origin)
        writer.println("HTTP/1.1 204 No Content")
        writer.println("Access-Control-Allow-Origin: $allowedOrigin")
        writer.println("Access-Control-Allow-Headers: X-Agent-Auth-Token, Authorization, Content-Type")
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

    private fun buildWebConsoleHtml(): String {
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
  .card p { font-size: 1.1rem; font-weight: bold; color: #e2e8f0; }
  .panel { background: #131b2e; border: 1px solid #1e293b; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
  .panel h2 { font-size: 1rem; color: #94a3b8; margin-bottom: 12px; }
  .input-group { display: flex; gap: 8px; }
  input[type="text"], input[type="password"] { flex: 1; background: #0b0f19; border: 1px solid #334155; color: #f8fafc; padding: 10px 14px; border-radius: 6px; font-size: 0.9rem; }
  input:focus { outline: none; border-color: #38bdf8; }
  button { background: #0284c7; color: white; border: none; padding: 10px 18px; border-radius: 6px; font-weight: bold; cursor: pointer; font-size: 0.9rem; }
  button:hover { background: #0369a1; }
  button.danger { background: #dc2626; }
  button.danger:hover { background: #b91c1c; }
  button.sec { background: #334155; }
  button.sec:hover { background: #475569; }
  .btn-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
  .log-box { background: #000; border: 1px solid #1e293b; border-radius: 6px; padding: 12px; font-family: monospace; font-size: 0.8rem; color: #38bdf8; min-height: 120px; max-height: 250px; overflow-y: auto; white-space: pre-wrap; }
</style>
</head>
<body>
<header>
  <h1><span>🛡️</span> J.A.R.V.I.S. Local Control</h1>
  <div id="connBadge" class="badge badge-live">ONLINE</div>
</header>

<div class="panel" style="border: 1px solid #0284c7;">
  <h2 style="color: #38bdf8;">Device Pairing & Authorization</h2>
  <p style="font-size: 0.8rem; color: #94a3b8; margin-bottom: 8px;">Enter the 6-digit dynamic PIN currently shown on the phone screen / notification:</p>
  <div class="input-group">
    <input type="password" id="authTokenInput" placeholder="Enter 6-digit Device PIN" maxlength="32" />
    <button onclick="savePin()">Set PIN</button>
  </div>
  <div id="authStatusText" style="font-size: 0.75rem; color: #f59e0b; margin-top: 6px;">PIN is stored in local browser memory only.</div>
</div>

<div class="grid">
  <div class="card">
    <h3>Foreground Package</h3>
    <p id="fgPkg">Standby</p>
  </div>
  <div class="card">
    <h3>Battery</h3>
    <p id="batteryVal">--%</p>
  </div>
  <div class="card">
    <h3>Heap Memory</h3>
    <p id="heapVal">-- MB</p>
  </div>
  <div class="card">
    <h3>Accessibility Core</h3>
    <p id="accVal">--</p>
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
function getPin() {
  return document.getElementById('authTokenInput').value.trim() || sessionStorage.getItem('agent_pin') || '';
}

function savePin() {
  const pin = document.getElementById('authTokenInput').value.trim();
  if (pin) {
    sessionStorage.setItem('agent_pin', pin);
    document.getElementById('authStatusText').innerText = 'PIN configured for session requests.';
    document.getElementById('authStatusText').style.color = '#22c55e';
    log('Authentication PIN configured.');
  }
}

// Restore cached session PIN if present
const cachedPin = sessionStorage.getItem('agent_pin');
if (cachedPin) {
  document.getElementById('authTokenInput').value = cachedPin;
  document.getElementById('authStatusText').innerText = 'PIN active from current browser session.';
  document.getElementById('authStatusText').style.color = '#22c55e';
}

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
  const pin = getPin();
  if (!pin) {
    alert('Please enter the 6-digit Device Pairing PIN before executing directives.');
    return;
  }
  log('Dispatching directive: ' + prompt);
  try {
    const res = await fetch('/api/remote/execute', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Agent-Auth-Token': pin
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
  const pin = getPin();
  try {
    const res = await fetch('/api/remote/abort', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': pin }
    });
    const data = await res.json();
    log('Abort status: ' + data.message);
  } catch (e) {
    log('Abort error: ' + e.message);
  }
}

async function dispatchNav(action) {
  log('Navigating: ' + action);
  const pin = getPin();
  try {
    const res = await fetch('/api/remote/navigate', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': pin, 'Content-Type': 'application/json' },
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
  const pin = getPin();
  try {
    const res = await fetch('/api/remote/flashlight', {
      method: 'POST',
      headers: { 'X-Agent-Auth-Token': pin, 'Content-Type': 'application/json' },
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
  const pin = getPin();
  try {
    const res = await fetch('/api/remote/snapshot', {
      headers: { 'X-Agent-Auth-Token': pin }
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
