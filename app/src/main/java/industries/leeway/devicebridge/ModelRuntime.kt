package industries.leeway.devicebridge

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ModelRuntime {
    private val inferenceGuard = InferenceGuard()
    const val MODEL_ID = "litert-community/SmolLM2-360M-Instruct"
    const val MODEL_FILE = "SmolLM2_360M_instruct.litertlm"
    const val MODEL_SIZE_BYTES = 373719040L
    const val MODEL_SHA256 = "8e2834da211b439751af968ed650febdde5a8cb8d88bc6c1a3059f049caa5c2e"
    const val MODEL_URL = "https://huggingface.co/litert-community/SmolLM2-360M-Instruct/resolve/main/SmolLM2_360M_instruct.litertlm?download=true"

    private fun modelDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(context: Context): File = File(modelDir(context), MODEL_FILE)

    fun status(context: Context): JSONObject {
        val file = modelFile(context)
        val exists = file.isFile
        val size = if (exists) file.length() else 0L
        val hash = if (exists && size == MODEL_SIZE_BYTES) sha256(file) else null
        val verified = exists && size == MODEL_SIZE_BYTES && hash == MODEL_SHA256
        return JSONObject().apply {
            put("modelId", MODEL_ID)
            put("format", "litertlm")
            put("installed", exists)
            put("sizeBytes", size)
            put("expectedSizeBytes", MODEL_SIZE_BYTES)
            put("sha256", hash)
            put("expectedSha256", MODEL_SHA256)
            put("verified", verified)
            put("runtime", "LiteRT-LM")
            put("backend", "CPU")
            put("authority", "PHONE_LOCAL_MODEL")
            put("busy", inferenceGuard.isBusy())
        }
    }

    fun download(context: Context, progress: (Long, Long) -> Unit): JSONObject {
        val dir = modelDir(context)
        val finalFile = modelFile(context)
        val partFile = File(dir, MODEL_FILE + ".part")
        if (partFile.exists()) partFile.delete()

        val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.setRequestProperty("User-Agent", "LeeWay-Device-Bridge/0.5.0")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("MODEL_DOWNLOAD_HTTP_" + connection.responseCode)
        }

        val expectedLength = connection.contentLengthLong
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L

        connection.inputStream.use { input ->
            FileOutputStream(partFile).use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    total += read
                    progress(total, if (expectedLength > 0) expectedLength else MODEL_SIZE_BYTES)
                }
                output.fd.sync()
            }
        }
        connection.disconnect()

        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        if (total != MODEL_SIZE_BYTES) {
            partFile.delete()
            throw IllegalStateException("MODEL_SIZE_MISMATCH:" + total)
        }
        if (actualHash != MODEL_SHA256) {
            partFile.delete()
            throw IllegalStateException("MODEL_HASH_MISMATCH")
        }

        if (finalFile.exists()) finalFile.delete()
        if (!partFile.renameTo(finalFile)) {
            partFile.copyTo(finalFile, overwrite = true)
            partFile.delete()
        }

        ReceiptStore.record(
            context,
            "model.install",
            "PASS",
            "model=" + MODEL_ID + " bytes=" + total + " sha256=" + actualHash
        )
        return status(context)
    }

    fun generate(context: Context, prompt: String): JSONObject =
        generateBounded(context, prompt, 512, 90000L, 16384)

    fun generateConversation(context: Context, prompt: String, creatorContext: String = ""): JSONObject =
        generateBounded(context, prompt, 128, 45000L, 4096, ConversationPrompt.systemInstruction(creatorContext))

    private fun generateBounded(context: Context, prompt: String, maxOutputTokens: Int, timeoutMs: Long, maxPromptChars: Int, systemInstruction: String? = null): JSONObject {
        fun failed(error: String, elapsed: Long = 0L): JSONObject {
            ReceiptStore.record(context, "model.inference", "BLOCKED", "error=$error elapsedMs=$elapsed")
            return JSONObject().put("ok", false).put("error", error).put("elapsedMs", elapsed)
                .put("modelId", MODEL_ID).put("maxOutputTokens", maxOutputTokens)
                .put("generationTimeoutMs", timeoutMs)
        }
        if (prompt.isBlank() || prompt.length > maxPromptChars) return failed("MODEL_PROMPT_LIMIT")
        if (!inferenceGuard.tryAcquire()) return failed("MODEL_BUSY")
        val started = System.currentTimeMillis()
        try {
            val file = modelFile(context)
            val st = status(context)
            if (!st.optBoolean("verified")) {
                return JSONObject().apply {
                    put("ok", false)
                    put("error", "MODEL_NOT_VERIFIED")
                    put("status", st)
                }
            }

            val output = StringBuilder()
            runBlocking {
                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                Engine(config).use { engine ->
                    engine.initialize()
                    engine.createConversation(ConversationConfig(
                        systemInstruction = systemInstruction?.let { Contents.of(it) },
                        maxOutputToken = maxOutputTokens,
                        automaticToolCalling = false
                    )).use { conversation ->
                        try {
                            withTimeout(timeoutMs) {
                                conversation.sendMessageAsync(prompt, maxOutputToken = maxOutputTokens).collect { token ->
                                    output.append(token)
                                }
                            }
                        } catch (timeout: TimeoutCancellationException) {
                            // Explicit native cancellation precedes conversation/engine cleanup.
                            // Initialization and cleanup are synchronous native operations, not hard-deadline guarantees.
                            conversation.cancelProcess()
                            throw timeout
                        }
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - started
            if (output.isBlank()) return failed("MODEL_EMPTY_RESPONSE", elapsed)
            val result = JSONObject().apply {
                put("ok", true)
                put("modelId", MODEL_ID)
                put("prompt", prompt)
                put("response", output.toString())
                put("elapsedMs", elapsed)
                put("authority", "PHONE_LOCAL_MODEL")
                put("maxOutputTokens", maxOutputTokens)
                put("generationTimeoutMs", timeoutMs)
            }
            ReceiptStore.record(
                context,
                "model.inference",
                "PASS",
                "model=" + MODEL_ID + " elapsedMs=" + elapsed
            )
            return result
        } catch (_: TimeoutCancellationException) {
            return failed("MODEL_TIMEOUT", System.currentTimeMillis() - started)
        } catch (_: Exception) {
            return failed("MODEL_INFERENCE_FAILED", System.currentTimeMillis() - started)
        } finally {
            inferenceGuard.release()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
