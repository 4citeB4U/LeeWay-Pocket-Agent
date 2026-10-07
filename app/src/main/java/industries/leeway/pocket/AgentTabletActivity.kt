/*
LEEWAY HEADER - DO NOT REMOVE
REGION: POCKET.WORKSTATION.VT
TAG: EXISTING.VT.EMBEDDED.PACKAGE.V1
5WH:
WHAT = Open the existing Agent VT bundle inside the same Agent Lee package
WHY = Replace the broken menu alert without a second launcher or workstation
WHO = LeeWay Industries / Agent Lee under Creator authority
WHERE = Internal, non-exported activity in Pocket's golden-package candidate
WHEN = 2026-10-06
HOW = Hash-bound packaged HTML at an isolated local HTTPS origin; Continuum mode adds only owner-bound GET reads
AUTHORIZED ROLES: OWNER_UI / LOCAL_PREVIEW / CONTINUUM_READ; no remote execution or agent-control grants
LICENSE: MIT; bundled workstation and HyperFrames retain their upstream licenses
*/
package industries.leeway.pocket

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.MessageDigest

class AgentTabletActivity : Activity() {
    private var web: WebView? = null
    private val host = "appassets.androidplatform.net"
    companion object { const val EXTRA_SURFACE = "leeway_surface" }
    private val continuumMode get() = intent.getStringExtra(EXTRA_SURFACE) == "continuum"
    private val entry get() = if (continuumMode) "/agent-vt/continuum/index.html" else "/agent-vt/standalone.html"
    @Volatile private var continuumAdmitted = false
    private var bundleResources = JSONObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 25, 31))
            setOnApplyWindowInsetsListener { view, insets ->
                // Keep toolbar and WebView controls clear of the system bars and cutout.
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                insets.inset(safe.left, safe.top, safe.right, safe.bottom)
            }
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolbar.addView(Button(this).apply {
            text = "Return to Agent Lee"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toolbar.addView(Button(this).apply {
            text = "Talk to Agent Lee"
            setOnClickListener { startActivity(PocketVoiceActivity.launchIntent(this@AgentTabletActivity)) }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (!continuumMode) toolbar.addView(Button(this).apply {
            text = "Continuum"
            setOnClickListener {
                startActivity(Intent(this@AgentTabletActivity, AgentTabletActivity::class.java)
                    .putExtra(EXTRA_SURFACE, "continuum"))
            }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layout.addView(toolbar)
        setContentView(layout)
        layout.requestApplyInsets()
        val payload = runCatching { verifiedPayload() }.getOrElse { error ->
            layout.addView(TextView(this).apply {
                text = "Agent VT is not admitted in this package.\n" +
                    (error.message ?: "Bundle verification failed") +
                    "\nThe existing Brain and owner data remain unchanged."
                setTextColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
            })
            return
        }
        continuumAdmitted = continuumMode
        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(24, 25, 31))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            }
            // Deliberately no addJavascriptInterface: compositions cannot acquire native authority.
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (continuumMode) {
                        if (continuumAdmitted && request.isForMainFrame && uri.scheme == "https" && uri.host == host &&
                            uri.port == -1 && uri.userInfo == null && uri.path == "/agent-vt/continuum/close" &&
                            uri.query == null && uri.fragment == null) {
                            runOnUiThread { finish() }
                            return true
                        }
                        // Only the admitted entry can become a document; stored data never becomes a WebView page.
                        return !(request.isForMainFrame && uri.scheme == "https" && uri.host == host &&
                            uri.port == -1 && uri.userInfo == null && uri.path == entry)
                    }
                    if (!request.isForMainFrame && uri.scheme in listOf("about", "blob", "data")) return false
                    return !(uri.scheme == "https" && uri.host == host &&
                        (uri.path == entry || bundleResources.has(resourceKey(uri.path ?: ""))))
                }
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val uri = request.url
                    if (continuumMode) {
                        if (!continuumAdmitted || uri.scheme != "https" || uri.host != host ||
                            uri.port != -1 || uri.userInfo != null) return blockedContinuumResource()
                        if ((uri.path ?: "").startsWith("/api/continuum/")) {
                            return AndroidContinuumReadAdapter.response(this@AgentTabletActivity, uri, request.method)
                        }
                    }
                    if (uri.scheme == "about" || uri.scheme == "blob" || uri.scheme == "data") return null
                    if (request.method == "GET" && uri.scheme == "https" && uri.host == host && uri.path == entry) {
                        return WebResourceResponse("text/html", "UTF-8", 200, "OK",
                            documentHeaders(),
                            ByteArrayInputStream(payload))
                    }
                    if (request.method == "GET" && uri.scheme == "https" && uri.host == host) {
                        val key = resourceKey(uri.path ?: "")
                        if (bundleResources.has(key) && (!continuumMode || key.startsWith("continuum/"))) {
                            val record = bundleResources.getJSONObject(key)
                            return runCatching {
                                WebResourceResponse(record.getString("mime"), "UTF-8",
                                    ByteArrayInputStream(readVerifiedResource(key, record.getString("sha256"))))
                            }.getOrElse {
                                WebResourceResponse("text/plain", "UTF-8", 409, "Conflict", emptyMap(),
                                    ByteArrayInputStream("VT_RESOURCE_HASH_FAILED".toByteArray(Charsets.UTF_8)))
                            }
                        }
                    }
                    val localApi = uri.scheme == "https" && uri.host == host && (uri.path ?: "").startsWith("/api/")
                    val state = if (localApi) "WORKSTATION_PROVIDER_UNBOUND" else "WORKSTATION_RESOURCE_NOT_ADMITTED"
                    return WebResourceResponse("application/json", "UTF-8", if (localApi) 503 else 403,
                        if (localApi) "Unavailable" else "Forbidden", mapOf("Cache-Control" to "no-store"),
                        ByteArrayInputStream("{\"state\":\"$state\",\"executed\":false}".toByteArray(Charsets.UTF_8)))
                }
            }
            loadUrl("https://$host$entry")
        }
        layout.addView(web, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun verifiedPayload(): ByteArray {
        val lock = JSONObject(assets.open("agent-vt/SOURCE.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(lock.getString("schema") == "leeway.embedded-vt.source.v1") { "VT_LOCK_SCHEMA_INVALID" }
        require(lock.getString("repository") == "4citeB4U/LeeWay-Digital-Workforce") { "VT_SOURCE_OWNER_INVALID" }
        require(lock.getString("sourceCommit").matches(Regex("[0-9a-f]{40}"))) { "VT_SOURCE_REVISION_REQUIRED" }
        val expected = lock.getString("sha256")
        require(expected.matches(Regex("[0-9a-f]{64}"))) { "VT_PAYLOAD_HASH_REQUIRED" }
        bundleResources = lock.optJSONObject("resources") ?: JSONObject()
        require(bundleResources.length() <= 128) { "VT_RESOURCE_LIMIT" }
        if (continuumMode) {
            require(bundleResources.has("continuum/index.html")) { "CONTINUUM_ASSET_NOT_ADMITTED" }
            val record = bundleResources.getJSONObject("continuum/index.html")
            require(record.getString("mime") == "text/html") { "CONTINUUM_ENTRY_MIME_INVALID" }
            return readVerifiedResource("continuum/index.html", record.getString("sha256"))
        }
        return readVerifiedResource("standalone.html", expected)
    }

    private fun blockedContinuumResource() = WebResourceResponse("application/json", "UTF-8", 403, "Forbidden",
        mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"),
        ByteArrayInputStream("{\"ok\":false,\"error\":\"CONTINUUM_TRUSTED_ASSET_REQUIRED\"}".toByteArray(Charsets.UTF_8)))

    private fun documentHeaders(): Map<String, String> {
        val headers = mutableMapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff")
        if (continuumMode) headers["Content-Security-Policy"] =
            "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; " +
            "object-src 'none'; frame-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"
        return headers
    }

    private fun resourceKey(uriPath: String): String {
        val relative = uriPath.removePrefix("/agent-vt/").trimStart('/')
        return if (relative.endsWith('/')) relative + "index.html" else relative
    }

    private fun readVerifiedResource(key: String, expected: String): ByteArray {
        require(key.matches(Regex("[A-Za-z0-9_./-]+")) && !key.split('/').any { it == ".." || it == "." }) { "VT_RESOURCE_PATH_INVALID" }
        require(expected.matches(Regex("[0-9a-f]{64}"))) { "VT_RESOURCE_HASH_INVALID" }
        val bytes = assets.open("agent-vt/" + key).use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= 16_777_216) { "VT_PAYLOAD_SIZE_INVALID" }
                out.write(buffer, 0, count)
            }
            require(total > 0) { "VT_PAYLOAD_EMPTY" }
            out.toByteArray()
        }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(actual == expected) { "VT_PAYLOAD_HASH_MISMATCH" }
        return bytes
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val surfaceChanged = continuumMode != (intent.getStringExtra(EXTRA_SURFACE) == "continuum")
        if (surfaceChanged) {
            continuumAdmitted = false
            web?.stopLoading()
        }
        setIntent(intent)
        if (surfaceChanged) recreate()
    }

    override fun onPause() { web?.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); web?.onResume() }
    override fun onDestroy() { continuumAdmitted = false; web?.stopLoading(); web?.destroy(); web = null; super.onDestroy() }
}
