package industries.leeway.devicebridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RemoteCommandRouter {
    private val remoteQualified = setOf(
        "device.health", "device.info", "device.capabilities",
        "device.bluetooth.list-bonded", "device.network.discover", "device.receipts",
        "device.screen.capture", "device.ui.snapshot", "device.ui.back", "device.ui.home", "device.ui.recents",
        "device.ui.tap", "device.ui.swipe", "device.ui.text",
        "device.apps.list", "device.apps.launch", "device.apps.install.status", "device.apps.install",
        "device.media.scan", "device.media.delete.request",
        "model.status", "model.install", "model.inference", "voice.status", "voice.speak", "agent.chat"
    )

    fun execute(context: Context, commandId: String, capability: String, arguments: JSONObject, firstSeen: Boolean, conversationRequest: Boolean = false): JSONObject {
        val governance = LocalAuthority.agentAccessEnabled(context)
        val supported = capability in remoteQualified
        val prompt = if (capability == "agent.chat") ConversationPrompt.userRequest(
            if (arguments.has("userRequest")) arguments.optString("userRequest") else null,
            arguments.optString("prompt")
        ) else arguments.optString("prompt").trim()
        val text = arguments.optString("text").trim()
        val capabilityPrecondition = when (capability) {
            "model.inference", "agent.chat" -> prompt.isNotEmpty()
            "voice.speak" -> text.isNotEmpty()
            "device.ui.tap" -> arguments.has("x") && arguments.has("y")
            "device.ui.swipe" -> arguments.has("x1") && arguments.has("y1") &&
                arguments.has("x2") && arguments.has("y2")
            "device.ui.text" -> text.isNotEmpty()
            "device.apps.launch" -> arguments.optString("packageName").isNotBlank()
            "device.media.delete.request" -> arguments.optJSONArray("uris")?.length()?.let { it > 0 } == true
            "device.apps.install" -> arguments.optString("url").startsWith("https://") &&
                arguments.optString("sha256").matches(Regex("^[A-Fa-f0-9]{64}$"))
            else -> true
        }
        val gate = FormulaF8Gate.evaluate(
            trigger = true,
            governance = governance,
            conditions = listOf(
                commandId.isNotBlank(), capability.isNotBlank(), supported,
                firstSeen, capabilityPrecondition
            )
        )
        if (!gate.optBoolean("fire")) return JSONObject().apply {
            put("ok", false); put("error", "LOCAL_ELIGIBILITY_HOLD"); put("capability", capability); put("gate", gate); put("formulaAuthority", FormulaF8Gate.FORMULA_AUTHORITY); put("canonicalFormulaState", "NOT_EXECUTED")
        }
        return try {
            val value = when (capability) {
                "device.health" -> health(context)
                "device.info" -> DevicePassport.capture(context)
                "device.capabilities" -> capabilities(context)
                "device.bluetooth.list-bonded" -> BluetoothProvider.snapshot(context)
                "device.network.discover" -> NetworkDiscoveryProvider.discover(context)
                "device.receipts" -> JSONObject().put("receipts", ReceiptStore.list(context))
                "device.screen.capture" -> DeviceOperatorAccessibilityService.captureScreen()
                "device.ui.snapshot" -> DeviceOperatorAccessibilityService.snapshot()
                "device.ui.back" -> DeviceOperatorAccessibilityService.global("back")
                "device.ui.home" -> DeviceOperatorAccessibilityService.global("home")
                "device.ui.recents" -> DeviceOperatorAccessibilityService.global("recents")
                "device.ui.tap" -> DeviceOperatorAccessibilityService.tap(
                    arguments.getDouble("x").toFloat(),
                    arguments.getDouble("y").toFloat()
                )
                "device.ui.swipe" -> DeviceOperatorAccessibilityService.swipe(
                    arguments.getDouble("x1").toFloat(),
                    arguments.getDouble("y1").toFloat(),
                    arguments.getDouble("x2").toFloat(),
                    arguments.getDouble("y2").toFloat(),
                    arguments.optLong("durationMs", 300L)
                )
                "device.ui.text" -> DeviceOperatorAccessibilityService.setFocusedText(text)
                "device.apps.list" -> AppOperator.listLaunchable(context)
                "device.apps.launch" -> AppOperator.launch(context, arguments.getString("packageName"))
                "device.apps.install.status" -> PackageInstallBroker.status(context)
                "device.apps.install" -> PackageInstallBroker.installFromUrl(
                    context,
                    arguments.getString("url"),
                    arguments.getString("sha256")
                )
                "device.media.scan" -> MediaOperator.scan(context, arguments.optInt("maxItems", 500))
                "device.media.delete.request" -> {
                    val raw = arguments.getJSONArray("uris")
                    val uris = (0 until raw.length()).map { raw.getString(it) }
                    MediaOperator.requestDelete(context, uris)
                }
                "model.status" -> ModelRuntime.status(context)
                "model.install" -> ModelRuntime.download(context) { _, _ -> }
                "model.inference" -> if (conversationRequest) ModelRuntime.generateConversation(context, prompt) else ModelRuntime.generate(context, prompt)
                "voice.status" -> VoiceRuntime.initialize(context)
                "voice.speak" -> VoiceRuntime.speak(context, text)
                "agent.chat" -> chat(context, prompt, arguments.optBoolean("speak", true), arguments.optString("creatorContext"))
                else -> JSONObject().put("error", "CAPABILITY_NOT_REMOTE_QUALIFIED")
            }
            val valueOk = !value.has("ok") || value.optBoolean("ok")
            JSONObject().apply {
                put("ok", valueOk)
                put("capability", capability)
                put("gate", gate); put("formulaAuthority", FormulaF8Gate.FORMULA_AUTHORITY); put("canonicalFormulaState", "NOT_EXECUTED")
                put("result", value)
                if (!valueOk) put("error", value.optString("error", "CAPABILITY_EXECUTION_FAILED"))
            }
        } catch (e: Exception) {
            JSONObject().apply { put("ok", false); put("capability", capability); put("gate", gate); put("formulaAuthority", FormulaF8Gate.FORMULA_AUTHORITY); put("canonicalFormulaState", "NOT_EXECUTED"); put("error", e.message ?: e.javaClass.simpleName) }
        }
    }

    private fun chat(context: Context, prompt: String, speak: Boolean, creatorContext: String): JSONObject {
        if (CreatorIdentityReply.matches(prompt)) {
            val profile = runCatching {
                context.assets.open("leeway-creator-profile.json").bufferedReader().use { JSONObject(it.readText()) }
            }.getOrNull()
            val response = CreatorIdentityReply.fromProfile(profile)
            val voice = if (speak) VoiceRuntime.speak(context, response)
                else JSONObject().put("ok", true).put("spoken", false)
            ReceiptStore.record(context, "agent.chat", if (voice.optBoolean("ok")) "PASS" else "FAIL",
                "source=USER_AUTHORIZED_CREATOR_PROFILE modelExecuted=false speak=$speak")
            return JSONObject().put("ok", true).put("prompt", prompt).put("response", response)
                .put("modelExecuted", false).put("modelId", JSONObject.NULL).put("elapsedMs", 0)
                .put("voice", voice).put("authority", "USER_AUTHORIZED_CREATOR_PROFILE")
                .put("promptContract", "EXACT_PROFILE_LOOKUP_V1").put("canonicalFormulaState", "NOT_EXECUTED")
        }
        val generated = ModelRuntime.generateConversation(context, prompt, creatorContext)
        if (!generated.optBoolean("ok")) return generated
        val response = generated.optString("response")
        val voice = if (speak) VoiceRuntime.speak(context, response)
            else JSONObject().put("ok", true).put("spoken", false)
        ReceiptStore.record(context, "agent.chat", if (voice.optBoolean("ok")) "PASS" else "FAIL",
            "model=" + generated.optString("modelId") + " speak=" + speak)
        return JSONObject().apply {
            put("ok", true); put("prompt", prompt); put("response", response)
            put("modelId", generated.optString("modelId")); put("elapsedMs", generated.optLong("elapsedMs"))
            put("voice", voice); put("authority", "PHONE_LOCAL_AGENT_CHAT")
            put("promptContract", "SEPARATE_SYSTEM_AND_USER_V1")
            put("canonicalFormulaState", "NOT_EXECUTED")
        }
    }

    private fun health(context: Context): JSONObject = JSONObject().apply {
        put("remote", RemoteRelayState.status(context)); put("model", ModelRuntime.status(context))
        put("voice", VoiceRuntime.status(context)); put("agentAccessEnabled", LocalAuthority.agentAccessEnabled(context))
        put("authority", "PHONE_LOCAL_RUNTIME")
    }

    internal fun qualifiedCapabilities(): JSONArray = JSONArray(remoteQualified.toList())

    private fun capabilities(context: Context): JSONObject {
        val passport = BootstrapStore.loadPassport(context) ?: DevicePassport.capture(context)
        return JSONObject().apply {
            put("capabilities", passport.optJSONArray("capabilityClaims"))
            put("remoteQualified", qualifiedCapabilities())
        }
    }
}
