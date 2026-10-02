package industries.leeway.devicebridge

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

object LocalBridgeServer {
    const val PORT = 5323
    @Volatile private var server: ServerSocket? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var ownerBootstrapNonce: String? = null
    @Volatile private var ownerBootstrapExpiresAt: Long = 0L

    fun armOwnerBootstrap(context: Context, nonce: String): JSONObject {
        val clean = nonce.trim()
        val valid = clean.matches(Regex("^[A-Za-z0-9_-]{32,128}$"))
        if (!valid) {
            return JSONObject().put("ok", false).put("error", "INVALID_BOOTSTRAP_NONCE")
        }
        LocalAuthority.setAgentAccess(context, true)
        if (!isRunning()) start(context)
        ownerBootstrapNonce = clean
        ownerBootstrapExpiresAt = System.currentTimeMillis() + 120_000L
        ReceiptStore.record(
            context.applicationContext,
            "device.owner.bootstrap.arm",
            "PASS",
            "Loopback owner bootstrap armed for Termux"
        )
        return JSONObject().apply {
            put("ok", true)
            put("armed", true)
            put("expiresInMs", 120_000)
            put("bindAddress", "127.0.0.1")
            put("port", PORT)
        }
    }

    @Synchronized
    private fun consumeOwnerBootstrap(context: Context, nonce: String?): JSONObject {
        val expected = ownerBootstrapNonce
        val now = System.currentTimeMillis()
        if (expected.isNullOrBlank() || now > ownerBootstrapExpiresAt) {
            ownerBootstrapNonce = null
            ownerBootstrapExpiresAt = 0L
            return JSONObject().put("ok", false).put("error", "BOOTSTRAP_NOT_ARMED_OR_EXPIRED")
        }
        if (nonce.isNullOrBlank() || nonce != expected) {
            return JSONObject().put("ok", false).put("error", "BOOTSTRAP_NONCE_MISMATCH")
        }
        ownerBootstrapNonce = null
        ownerBootstrapExpiresAt = 0L
        val identity = DeviceIdentity.ensure(context)
        ReceiptStore.record(
            context.applicationContext,
            "device.owner.bootstrap.consume",
            "PASS",
            "Termux consumed one-time owner bootstrap"
        )
        return JSONObject().apply {
            put("ok", true)
            put("deviceId", identity.optString("deviceId"))
            put("pairingToken", BridgeSecret.ensure(context))
            put("relayUrl", RemoteRelayState.relayUrl(context))
            put("authority", "OWNER_LOCAL_LOOPBACK_BOOTSTRAP")
        }
    }

    fun isRunning(): Boolean = server?.isClosed == false

    fun start(context: Context): JSONObject {
        if (isRunning()) return status(context)
        val appContext = context.applicationContext
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT))
        server = socket
        worker = thread(name = "leeway-device-bridge", isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    handle(appContext, socket.accept())
                } catch (_: Exception) {
                    if (!socket.isClosed) {
                        ReceiptStore.record(appContext, "device.bridge.error", "FAIL", "Local bridge socket error")
                    }
                }
            }
        }
        BridgeSecret.ensure(appContext)
        ReceiptStore.record(appContext, "device.bridge.start", "PASS", "Loopback bridge started on 127.0.0.1:$PORT")
        return status(appContext)
    }

    fun stop(context: Context): JSONObject {
        try { server?.close() } catch (_: Exception) {}
        server = null
        worker = null
        ReceiptStore.record(context.applicationContext, "device.bridge.stop", "PASS", "Local bridge stopped")
        return status(context)
    }

    fun status(context: Context): JSONObject = JSONObject().apply {
        put("ok", true)
        put("running", isRunning())
        put("bindAddress", "127.0.0.1")
        put("port", PORT)
        put("agentAccessEnabled", LocalAuthority.agentAccessEnabled(context))
        put("appVersionName", BuildConfig.VERSION_NAME)
        put("appVersionCode", BuildConfig.VERSION_CODE)
        put("updateMetadataUrl", AgentLeeUpdate.METADATA_URL)
        put("transport", "LOCAL_LOOPBACK")
        put("authority", "PHONE_LOCAL_RUNTIME")
    }

    private fun handle(context: Context, socket: Socket) {
        socket.use { client ->
            client.soTimeout = 3000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0) ?: ""
            val target = parts.getOrNull(1) ?: "/"
            val path = target.substringBefore("?")
            val query = target.substringAfter("?", "")
            val queryParams = query.split("&")
                .mapNotNull { part ->
                    if (part.isBlank()) null
                    else {
                        val idx = part.indexOf("=")
                        if (idx <= 0) null
                        else URLDecoder.decode(part.substring(0, idx), "UTF-8") to
                            URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                    }
                }.toMap()
            var authorization: String? = null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Authorization:", ignoreCase = true)) {
                    authorization = line.substringAfter(":").trim()
                }
            }

            if (method != "GET") {
                respond(client, 405, JSONObject().put("ok", false).put("error", "METHOD_NOT_ALLOWED"))
                return
            }

            if (path == "/health") {
                respond(client, 200, status(context))
                return
            }

            if (path == "/owner-bootstrap") {
                val body = consumeOwnerBootstrap(context, queryParams["nonce"])
                respond(client, if (body.optBoolean("ok")) 200 else 401, body)
                return
            }

            if (!LocalAuthority.agentAccessEnabled(context)) {
                respond(client, 403, JSONObject().put("ok", false).put("error", "AGENT_ACCESS_DISABLED"))
                return
            }

            val bearer = authorization?.removePrefix("Bearer ")?.trim()
            if (!BridgeSecret.matches(context, bearer)) {
                respond(client, 401, JSONObject().put("ok", false).put("error", "UNAUTHORIZED"))
                return
            }

            val body = when (path) {
                "/passport" -> BootstrapStore.loadPassport(context) ?: DevicePassport.capture(context)
                "/capabilities" -> JSONObject().put(
                    "capabilities",
                    (BootstrapStore.loadPassport(context) ?: DevicePassport.capture(context))
                        .optJSONArray("capabilityClaims")
                )
                "/receipts" -> JSONObject().put("receipts", ReceiptStore.list(context))
                "/providers/bluetooth" -> BluetoothProvider.snapshot(context)
                "/bridge" -> status(context)
                else -> null
            }

            if (body == null) {
                respond(client, 404, JSONObject().put("ok", false).put("error", "NOT_FOUND"))
            } else {
                ReceiptStore.record(context, "device.bridge.read", "PASS", "GET $path")
                respond(client, 200, body)
            }
        }
    }

    private fun respond(socket: Socket, status: Int, body: JSONObject) {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Error"
        }
        val header = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n\r\n")
        }
        socket.getOutputStream().use { out ->
            out.write(header.toByteArray(Charsets.UTF_8))
            out.write(bytes)
            out.flush()
        }
    }
}
