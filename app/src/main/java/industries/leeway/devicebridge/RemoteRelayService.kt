package industries.leeway.devicebridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.min

class RemoteRelayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var connecting = false
    private var reconnectDelayMs = 1_000L
    private val seenCommandIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val workstationKeeperTick = object : Runnable {
        override fun run() {
            WorkstationKeeper.ensure(this@RemoteRelayService)
            handler.postDelayed(this, WORKSTATION_KEEPER_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startAsForeground("Connecting to LeeWay relay")
        val workstation = WorkstationKeeper.ensure(this)
        ReceiptStore.record(
            this,
            "workstation.background.ensure",
            if (workstation.optBoolean("ok")) "PASS" else "BLOCKED",
            workstation.optString("state")
        )
        handler.postDelayed(workstationKeeperTick, WORKSTATION_KEEPER_INTERVAL_MS)
        FloatingAgentLeeOverlay.attach(this)
        connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!RemoteRelayState.enabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        WorkstationKeeper.ensure(this)
        if (webSocket == null && !connecting) connect()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "service stopped")
        webSocket = null
        connecting = false
        RemoteRelayState.setConnection(this, false, "SERVICE_STOPPED")
        client.dispatcher.executorService.shutdown()
        FloatingAgentLeeOverlay.detach(this)
        super.onDestroy()
    }

    private fun connect() {
        if (!RemoteRelayState.enabled(this) || connecting || webSocket != null) return
        connecting = true
        RemoteRelayState.setConnection(this, false, "FETCHING_PAGES_RELAY_CONFIG")

        val request = Request.Builder()
            .url(RemoteRelayState.CONFIG_URL)
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                openRelay(RemoteRelayState.relayUrl(this@RemoteRelayService))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val configured = runCatching {
                        JSONObject(it.body?.string().orEmpty())
                            .optString("relayUrl")
                            .takeIf { url -> url.startsWith("wss://") }
                    }.getOrNull()
                    if (!configured.isNullOrBlank()) {
                        RemoteRelayState.setRelayUrl(this@RemoteRelayService, configured)
                    }
                }
                openRelay(RemoteRelayState.relayUrl(this@RemoteRelayService))
            }
        })
    }

    private fun openRelay(relayUrl: String) {
        if (!RemoteRelayState.enabled(this)) {
            connecting = false
            return
        }

        val request = Request.Builder().url(relayUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                connecting = false
                val identity = DeviceIdentity.ensure(this@RemoteRelayService)
                val hello = JSONObject().apply {
                    put("type", "hello")
                    put("role", "phone")
                    put("deviceId", identity.optString("deviceId"))
                    put("token", BridgeSecret.ensure(this@RemoteRelayService))
                }
                ws.send(hello.toString())
                RemoteRelayState.setConnection(
                    this@RemoteRelayService,
                    false,
                    "SOCKET_OPEN_AUTH_PENDING"
                )
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(ws, text)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (webSocket === ws) webSocket = null
                connecting = false
                RemoteRelayState.setConnection(
                    this@RemoteRelayService,
                    false,
                    "CLOSED:$code"
                )
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (webSocket === ws) webSocket = null
                connecting = false
                RemoteRelayState.setConnection(
                    this@RemoteRelayService,
                    false,
                    "FAILURE:" + t.javaClass.simpleName
                )
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(ws: WebSocket, text: String) {
        val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (msg.optString("type")) {
            "hello-ack" -> {
                reconnectDelayMs = 1_000L
                RemoteRelayState.setConnection(this, true, "AUTHENTICATED")
                updateNotification("LeeWay remote bridge connected")
                ReceiptStore.record(
                    this,
                    "device.remote.connect",
                    "PASS",
                    "Permanent relay authenticated"
                )
            }
            "command" -> executeRemoteCommand(ws, msg)
            "pong" -> RemoteRelayState.setConnection(this, true, "PONG")
            "error" -> {
                RemoteRelayState.setConnection(
                    this,
                    false,
                    "RELAY_ERROR:" + msg.optString("error")
                )
            }
        }
    }

    private fun executeRemoteCommand(ws: WebSocket, command: JSONObject) {
        Thread {
            val id = command.optString("id")
            val capability = command.optString("capability")
            val args = command.optJSONObject("arguments") ?: JSONObject()
            val firstSeen = id.isNotBlank() && seenCommandIds.add(id)
            val result = RemoteCommandRouter.execute(this, id, capability, args, firstSeen)
            val ok = result.optBoolean("ok", false)

            ReceiptStore.record(
                this,
                capability.ifBlank { "device.remote.command" },
                if (ok) "PASS" else "BLOCKED",
                "remote command id=" + id
            )

            val envelope = JSONObject().apply {
                put("type", "result")
                put("id", id)
                put("ok", ok)
                if (ok) {
                    put("result", result.opt("result"))
                    put("capability", result.optString("capability"))
                } else {
                    put("error", result.optString("error", "REMOTE_COMMAND_FAILED"))
                    put("capability", capability)
                }
            }
            ws.send(envelope.toString())
        }.start()
    }

    private fun scheduleReconnect() {
        if (!RemoteRelayState.enabled(this)) return
        val delay = reconnectDelayMs
        reconnectDelayMs = min(reconnectDelayMs * 2, 30_000L)
        handler.postDelayed({
            if (webSocket == null && !connecting) connect()
        }, delay)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LeeWay Remote Bridge",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the owner-authorized Device Bridge reachable"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun notification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("LeeWay Device Bridge")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    private fun startAsForeground(text: String) {
        val note = notification(text)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                note,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(NOTIFICATION_ID, note)
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    companion object {
        private const val CHANNEL_ID = "leeway_remote_bridge"
        private const val NOTIFICATION_ID = 5323
        private const val WORKSTATION_KEEPER_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            RemoteRelayState.setEnabled(context, true)
            val intent = Intent(context, RemoteRelayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            RemoteRelayState.setEnabled(context, false)
            RemoteRelayState.setConnection(context, false, "OWNER_DISABLED")
            context.stopService(Intent(context, RemoteRelayService::class.java))
        }
    }
}
