/*
LEEWAY HEADER - DO NOT REMOVE
REGION: LEEWAY.CONTINUUM.POCKET.RETRIEVAL
TAG: EXISTING_OWNER_RECORD_EXTRACTIVE_RETRIEVAL
5WH:
WHAT = Retrieve bounded prior records through the same read adapter used by the Continuum viewer.
WHY = A phone Continuum query must use that phone's existing owner-bound records.
WHO = Agent Lee / the existing installation owner.
WHERE = Existing Pocket runtime; this protocol has no network, database, model or voice provider.
WHEN = An explicit search/find/retrieve Continuum request has a successfully retained current event ID.
HOW = Cut off the current event, verify original content bytes, quote bounded extracts with sources.
AUTHORIZED ROLES: OWNER_READ / RETRIEVE. Digests are source checks, not Formula or Veritas receipts.
LICENSE: MIT
*/
package industries.leeway.pocket

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

data class ContinuumReadResult(val status: Int, val headers: Map<String, String>, val bytes: ByteArray)
fun interface ContinuumReadApi {
    fun get(route: String, parameters: Map<String, String>): ContinuumReadResult
}

data class LocalContinuumReply(
    val displayText: String,
    val spokenText: String,
    val sourceEvidence: String,
    val sourceEvidenceSha256: String,
    val returnedMatches: Int,
    val checkedSources: Int
)

/** Pure protocol: the Android facade supplies only the existing local read adapter. */
class LocalContinuumRetrieval(private val api: ContinuumReadApi) {
    companion object {
        const val MAX_BYTES = 1_048_576
        const val MAX_HITS = 8
        const val MAX_EXTRACTS = 4
        const val MAX_EXTRACT_CHARS = 480
        const val MAX_SPOKEN_CHARS = 3_200
        private val intent = Regex(
            "\\b(?:(?:search|find|retrieve)\\s+(?:(?:in|my)\\s+){0,2}continuum\\b|continuum\\s+(?:search|find|retrieve)\\b)(?:\\s+for\\b)?\\s*[:,-]?\\s*([\\s\\S]*)$",
            RegexOption.IGNORE_CASE
        )

        /** Null means this is not a Continuum retrieval command; empty is an invalid local command. */
        fun query(request: String): String? {
            val text = request.trim()
            // The explicit marker owns even empty/multiline remainder; invalid local input never falls through to /turn.
            return intent.find(text)?.groupValues?.get(1)?.trim()
        }

        fun validateQuery(query: String) {
            require(query.length in 2..160 && query.none { it.code < 32 || it.code == 127 }) {
                "CONTINUUM_SEARCH_QUERY_INVALID"
            }
        }

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }

        private fun utf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()

        /** Display/speech excerpt only; hashes always cover the unchanged original bytes. */
        fun excerpt(text: String, query: String, maximum: Int = MAX_EXTRACT_CHARS): String {
            require(maximum in 32..MAX_EXTRACT_CHARS) { "CONTINUUM_EXCERPT_LIMIT_INVALID" }
            val clean = text.replace(Regex("[\\s\\p{Cc}]+"), " ").trim()
            if (clean.length <= maximum) return clean
            val match = clean.indexOf(query, ignoreCase = true)
            var start = if (match < 0) 0 else (match - maximum / 4).coerceAtLeast(0)
            if (start > 0 && clean[start].isLowSurrogate()) start++
            var end = (start + maximum - 2).coerceAtMost(clean.length)
            if (end < clean.length && end > start && clean[end - 1].isHighSurrogate()) end--
            return (if (start > 0) "…" else "") + clean.substring(start, end) +
                (if (end < clean.length) "…" else "")
        }

        private fun requiredString(json: JSONObject, key: String, maximum: Int = 512): String {
            val value = json.opt(key)
            check(value is String && value.isNotBlank() && value.length <= maximum && value.none { it.code < 32 }) {
                "CONTINUUM_SOURCE_FIELD_INVALID"
            }
            return value
        }
    }

    private data class Bound(val ownerId: String, val bodyId: String, val revision: Long)

    /** Run before the existing conversation writer so an absent store cannot be bootstrapped by a read request. */
    fun requireExistingStorage() { bound() }

    private fun json(route: String, parameters: Map<String, String> = emptyMap()): JSONObject {
        val result = api.get(route, parameters)
        check(result.bytes.size <= MAX_BYTES) { "CONTINUUM_RESPONSE_LIMIT" }
        val envelope = JSONObject(utf8(result.bytes))
        if (result.status != 200 || envelope.opt("ok") != true) {
            val rawCode = envelope.opt("error") as? String
            error(rawCode?.takeIf { Regex("[A-Z_]{3,100}").matches(it) } ?: "CONTINUUM_READ_BLOCKED")
        }
        return envelope.optJSONObject("result") ?: error("CONTINUUM_RESPONSE_INVALID")
    }

    private fun bound(): Bound {
        val status = json("status")
        val ownerId = requiredString(status, "ownerId")
        val bodyId = requiredString(status, "bodyId")
        val revision = status.optLong("bindingRevision", 0)
        check(ownerId == bodyId && revision > 0 &&
            status.optString("state") == "OWNER_LOCAL_RETAINED_RECORDS_READABLE" &&
            status.optString("logicalResourceId") == "continuum-storage" &&
            status.optString("formulaExecution") == "NOT_EXECUTED") { "CONTINUUM_LOCAL_BINDING_INVALID" }
        return Bound(ownerId, bodyId, revision)
    }

    fun retrieve(query: String, currentEventId: Long): LocalContinuumReply {
        validateQuery(query)
        require(currentEventId > 0) { "CONTINUUM_CURRENT_EVENT_CUTOFF_REQUIRED" }
        val selected = bound()
        val page = json("search", linkedMapOf("query" to query, "viewId" to "all",
            "limit" to MAX_HITS.toString(), "cursor" to currentEventId.toString()))
        check(page.optString("retrievalMode") == "LEXICAL_STORED_TEXT_NO_EMBEDDINGS_NO_MODEL") {
            "CONTINUUM_RETRIEVAL_MODE_UNEXPECTED"
        }
        val records = page.optJSONArray("records") ?: error("CONTINUUM_RESPONSE_INVALID")
        check(records.length() <= MAX_HITS) { "CONTINUUM_RESPONSE_LIMIT" }
        val hasMore = page.opt("nextCursor").let { it != null && it != JSONObject.NULL }
        val seen = mutableSetOf<String>()
        val sources = JSONArray()
        val extracts = mutableListOf<String>()
        val citations = mutableListOf<String>()
        var previewUnavailable = 0

        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: error("CONTINUUM_RESPONSE_INVALID")
            val recordId = requiredString(record, "recordId")
            val universeId = requiredString(record, "universeId")
            val source = record.optJSONObject("source") ?: error("CONTINUUM_SOURCE_FIELD_INVALID")
            val rowId = requiredString(source, "rowId", 19).toLongOrNull()
                ?: error("CONTINUUM_SOURCE_ROW_INVALID")
            check(seen.add(recordId)) { "CONTINUUM_DUPLICATE_RECORD" }
            check(rowId > 0 && rowId < currentEventId &&
                recordId == "continuum-event:${selected.bodyId}:$rowId") { "CONTINUUM_CURRENT_EVENT_EXCLUDED" }
            val root = "continuum:${selected.bodyId}"
            check((universeId == root || universeId.startsWith("$root:")) &&
                requiredString(record, "bodyId") == selected.bodyId &&
                requiredString(record, "ownerId") == selected.ownerId &&
                source.optString("provider") == "OWNER_LOCAL_ANDROID_CONTINUUM" &&
                source.optString("logicalResourceId") == "continuum-storage" &&
                source.optString("table") == "continuum_events" &&
                source.optLong("bindingRevision", 0) == selected.revision &&
                requiredString(source, "universeId") == universeId) { "CONTINUUM_SOURCE_BINDING_MISMATCH" }
            check(record.optString("formulaExecution") == "NOT_EXECUTED") { "CONTINUUM_SOURCE_GOVERNANCE_UNEXPECTED" }
            if (record.opt("contentAvailable") != true) { previewUnavailable++; continue }
            if (sources.length() >= MAX_EXTRACTS) continue
            val expectedHash = requiredString(record, "contentSha256", 64)
            check(Regex("[0-9a-f]{64}").matches(expectedHash)) { "CONTINUUM_CONTENT_HASH_REQUIRED" }
            val expectedSize = record.optLong("byteLength", -1)
            check(expectedSize in 0..MAX_BYTES.toLong()) { "CONTINUUM_CONTENT_PREVIEW_LIMIT" }
            val content = api.get("content", mapOf("recordId" to recordId))
            check(content.status == 200) { "CONTINUUM_SOURCE_CONTENT_UNAVAILABLE" }
            check(content.bytes.size <= MAX_BYTES && content.bytes.size.toLong() == expectedSize) {
                "CONTINUUM_CONTENT_LENGTH_MISMATCH"
            }
            fun header(name: String) = content.headers.entries.firstOrNull { it.key.equals(name, true) }?.value
            check(header("X-Continuum-Record-Id") == recordId) { "CONTINUUM_CONTENT_ID_MISMATCH" }
            val actualHash = sha256(content.bytes)
            check(actualHash == expectedHash && header("X-Continuum-Content-SHA256") == actualHash) {
                "CONTINUUM_CONTENT_HASH_MISMATCH"
            }
            val original = utf8(content.bytes)
            val payload = runCatching { JSONObject(original) }.getOrNull()
            val retainedText = (payload?.opt("text") as? String) ?: original
            val extract = excerpt(retainedText, query)
            val sourceNumber = sources.length() + 1
            val title = requiredString(record, "title", 200)
            extracts.add("Source $sourceNumber, ${excerpt(title, query, 120)}: “$extract”")
            citations.add("[$sourceNumber] $recordId\nUniverse: $universeId\nSHA-256: $actualHash")
            sources.put(JSONObject().put("recordId", recordId).put("universeId", universeId)
                .put("sourceRowId", rowId.toString()).put("bindingRevision", selected.revision)
                .put("byteLength", content.bytes.size).put("contentSha256", actualHash)
                .put("retainedReceiptId", record.opt("receiptId") ?: JSONObject.NULL)
                .put("formulaExecution", "NOT_EXECUTED")
                .put("verification", "ORIGINAL_RETAINED_BYTES_SHA256_CHECKED"))
        }
        check(bound() == selected) { "CONTINUUM_BINDING_CHANGED" }
        val count = records.length()
        val introduction = if (count == 0) "I found no prior retained records in this device's Continuum matching “$query”."
            else "I found ${if (hasMore) "at least " else ""}$count prior retained ${if (count == 1) "record" else "records"} in this device's Continuum matching “$query”."
        val spoken = buildString {
            append(introduction)
            if (extracts.isNotEmpty()) append(" Here are ${extracts.size} excerpts from the stored content. ")
            append(extracts.joinToString(" "))
            if (previewUnavailable > 0) append(" $previewUnavailable returned records have no available bounded content preview.")
            if (count > extracts.size) append(" Open Continuum to inspect the returned records.")
        }
        check(spoken.length <= MAX_SPOKEN_CHARS) { "CONTINUUM_SPOKEN_RESPONSE_LIMIT" }
        val evidence = JSONObject().put("schema", "leeway.continuum.local-retrieval-source-evidence.v1")
            .put("kind", "ENGINEERING_SOURCE_DIGEST_NOT_GOVERNANCE_RECEIPT")
            .put("ownerId", selected.ownerId).put("bodyId", selected.bodyId)
            .put("bindingRevision", selected.revision).put("query", query)
            .put("beforeEventId", currentEventId.toString()).put("returnedMatches", count)
            .put("moreMatchesAvailable", hasMore).put("checkedSources", sources.length())
            .put("previewUnavailable", previewUnavailable)
            .put("retrievalMode", "LEXICAL_STORED_TEXT_NO_EMBEDDINGS_NO_MODEL")
            .put("formulaExecution", "NOT_EXECUTED").put("governedReceiptCreated", false)
            .put("sources", sources).toString()
        val digest = sha256(evidence.toByteArray(Charsets.UTF_8))
        val display = spoken + if (sources.length() == 0) "" else
            "\n\nSources\n" + citations.joinToString("\n\n") +
                "\n\nOriginal retained bytes checked. Formula execution: NOT_EXECUTED.\nSource evidence SHA-256: $digest (engineering digest; no governed receipt created)."
        return LocalContinuumReply(display, spoken, evidence, digest, count, sources.length())
    }
}
