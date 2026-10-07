/*
LEEWAY HEADER - DO NOT REMOVE
REGION: LEEWAY.CONTINUUM.ANDROID
TAG: EXISTING_OWNER_STORAGE_READ_ADAPTER
5WH:
WHAT = Inspect retained Continuum events through the existing Agent VT activity.
WHY = The owner's Continuum view must read actual retained records and original universe IDs.
WHO = LeeWay Industries / the existing installation owner, verified against AndroidKeyStore.
WHERE = Pocket's Android storage boundary; the portable viewer receives bounded JSON or content.
WHEN = A hash-admitted Continuum asset or explicit native retrieval requests its local, read-only API.
HOW = Existing identity and Brain resource binding, OPEN_READONLY, parameterized SQL and hashes.
AUTHORIZED ROLES: OWNER_READ / INSPECT / RETRIEVE. No ingestion, schema creation or execution grant.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.webkit.WebResourceResponse
import industries.leeway.brain.BodyIdentity
import industries.leeway.brain.DigitalBrain
import industries.leeway.pocket.devices.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.time.Instant

/** Called by source-locked AgentTabletActivity or native UnifiedAgentLeeRuntime retrieval; each call verifies the existing owner/storage binding. */
object AndroidContinuumReadAdapter {
    private const val MAX_PAGE = 50
    private const val MAX_CONTENT_BYTES = 1_048_576
    private const val MAX_JSON_BYTES = 1_048_576
    private const val MAX_UNIVERSE_GROUPS = 256
    private const val GOVERNANCE = "CONTINUUM_STORAGE_ADMISSION_UNBOUND"
    private const val WRITE_REASON = "Existing retained events are available. File import requires the qualified native owner storage operation."

    private data class Binding(val bodyId: String, val uri: String, val revision: Long)
    private data class Scope(val sql: String, val args: List<String>)
    private data class Event(
        val id: Long, val universeId: String, val eventType: String?, val payload: String?,
        val receipt: String?, val capturedAt: Long?, val storedBytes: Long?, val payloadNull: Boolean
    )
    private class Bound(
        val db: SQLiteDatabase, val brain: SQLiteDatabase, val body: BodyIdentity, val binding: Binding,
        val root: String = DigitalBrain.continuumRootId(body)
    )

    fun response(context: Context, uri: Uri, method: String): WebResourceResponse {
        if (method != "GET") return failure(409, GOVERNANCE)
        if (uri.scheme != "https" || uri.host != "appassets.androidplatform.net" ||
            uri.port != -1 || uri.userInfo != null || uri.fragment != null ||
            !uri.path.orEmpty().startsWith("/api/continuum/")) return failure(403, "CONTINUUM_LOCAL_VIEW_REQUIRED")
        return try {
            withExistingStorage(context) { bound ->
                when (uri.path) {
                    "/api/continuum/status" -> json(200, status(bound))
                    "/api/continuum/counts" -> json(200, counts(bound))
                    "/api/continuum/records" -> json(200, records(bound, uri, search = false))
                    "/api/continuum/search" -> json(200, records(bound, uri, search = true))
                    "/api/continuum/record" -> {
                        val id=parameter(uri,"recordId",required=true)!!
                        if(id.startsWith("brain:"))json(200,brainDescriptor(bound,id).first)
                        else json(200,descriptor(bound,event(bound,id)))
                    }
                    "/api/continuum/content" -> {
                        val id=parameter(uri,"recordId",required=true)!!
                        if(id.startsWith("brain:"))brainContent(bound,id)
                        else content(bound,event(bound,id))
                    }
                    else -> failure(404, "CONTINUUM_ROUTE_NOT_FOUND")
                }
            }
        } catch (error: Exception) {
            val code = error.message?.takeIf { Regex("[A-Z_]{3,100}").matches(it) }
                ?: "CONTINUUM_READ_BLOCKED"
            val status = when (code) {
                "CONTINUUM_RECORD_NOT_FOUND" -> 404
                "CONTINUUM_VIEW_PROVIDER_UNBOUND" -> 503
                else -> 409
            }
            failure(status, code)
        }
    }

    private fun <T> withExistingStorage(context: Context, operation: (Bound) -> T): T {
        // readExisting never creates an identity. No SQLiteOpenHelper/bootstrap/migration is used here.
        val identity = DeviceIdentity.readExisting(context.applicationContext)
        val brainFile = existingFile(context.getDatabasePath(LeeWayBodyDatabases.BRAIN_NAME))
        SQLiteDatabase.openDatabase(brainFile.path, null, SQLiteDatabase.OPEN_READONLY).use { brain ->
            check(brain.isReadOnly) { "CONTINUUM_READONLY_REQUIRED" }
            val owner = brain.rawQuery("SELECT device_id,key_fingerprint FROM brain_owner WHERE singleton=1", null).use { c ->
                check(c.moveToFirst()) { "CONTINUUM_BRAIN_OWNER_MISSING" }
                val value = BodyIdentity(c.getString(0), c.getString(1))
                check(!c.moveToNext()) { "CONTINUUM_BRAIN_OWNER_AMBIGUOUS" }
                value
            }
            check(owner == identity) { "CONTINUUM_BRAIN_OWNER_MISMATCH" }
            val brainBinding = binding(brain, "brain-storage", identity)
            boundFile(brainBinding, brainFile)
            val selected = binding(brain, "continuum-storage", identity)
            val continuumFile = boundFile(selected, context.getDatabasePath(LeeWayBodyDatabases.CONTINUUM_NAME))
            SQLiteDatabase.openDatabase(continuumFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                check(db.isReadOnly) { "CONTINUUM_READONLY_REQUIRED" }
                val tables = mutableSetOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    while (c.moveToNext()) tables.add(c.getString(0))
                }
                check(setOf("universes", "continuum_events", "universe_links", "federation_peers").all { it in tables }) {
                    "CONTINUUM_EXISTING_SCHEMA_REQUIRED"
                }
                val bound = Bound(db, brain, identity, selected)
                val rootAuthority = db.rawQuery("SELECT authority FROM universes WHERE id=?", arrayOf(bound.root)).use { c ->
                    check(c.moveToFirst()) { "CONTINUUM_ROOT_UNBOUND" }
                    val value = c.getString(0)
                    check(!c.moveToNext()) { "CONTINUUM_ROOT_AMBIGUOUS" }
                    value
                }
                check(rootAuthority == identity.deviceId) { "CONTINUUM_ROOT_OWNER_MISMATCH" }
                val result = operation(bound)
                check(binding(brain, "continuum-storage", identity) == selected) { "CONTINUUM_BINDING_CHANGED" }
                return result
            }
        }
    }

    private fun existingFile(file: File): File {
        check(file.isFile && file.length() > 0) { "CONTINUUM_EXISTING_DATABASE_REQUIRED" }
        return file.canonicalFile
    }

    private fun binding(brain: SQLiteDatabase, logicalId: String, identity: BodyIdentity): Binding =
        brain.rawQuery("SELECT body_id,resource_uri,revision,owner_authorized FROM brain_resource_bindings WHERE logical_id=?", arrayOf(logicalId)).use { c ->
            check(c.moveToFirst()) { "CONTINUUM_RESOURCE_BINDING_MISSING" }
            val result = Binding(c.getString(0), c.getString(1), c.getLong(2))
            check(result.bodyId == identity.deviceId && result.revision > 0 && c.getInt(3) == 1) { "CONTINUUM_RESOURCE_NOT_AUTHORIZED" }
            check(!c.moveToNext()) { "CONTINUUM_RESOURCE_BINDING_AMBIGUOUS" }
            result
        }

    private fun boundFile(binding: Binding, expected: File): File {
        val uri = URI(binding.uri)
        check(uri.scheme == "file" && uri.rawAuthority.isNullOrEmpty() && uri.rawQuery == null && uri.rawFragment == null) {
            "CONTINUUM_RESOURCE_URI_INVALID"
        }
        val file = existingFile(File(uri))
        check(file == existingFile(expected)) { "CONTINUUM_RESOURCE_TARGET_MISMATCH" }
        return file
    }

    private fun parameter(uri: Uri, name: String, required: Boolean = false): String? {
        val values = uri.getQueryParameters(name)
        check(values.size <= 1) { "CONTINUUM_PARAMETER_AMBIGUOUS" }
        val value = values.firstOrNull()
        check(value == null || value.length <= 1024 && value.none { it.code < 32 }) { "CONTINUUM_PARAMETER_INVALID" }
        if (required) check(!value.isNullOrBlank()) { "CONTINUUM_PARAMETER_REQUIRED" }
        return value?.takeIf { it.isNotEmpty() }
    }

    private fun scope(bound: Bound, universeId: String? = null, viewId: String? = null, query: String? = null): Scope {
        val prefix = bound.root + ":"
        val terms = mutableListOf("(e.universe_id=? OR substr(e.universe_id,1,?)=?)")
        val args = mutableListOf(bound.root, prefix.length.toString(), prefix)
        if (universeId != null) {
            check(universeId.length <= 512 && (universeId == bound.root || universeId.startsWith(prefix))) { "CONTINUUM_UNIVERSE_OUTSIDE_BODY" }
            terms.add("e.universe_id=?"); args.add(universeId)
        }
        if (viewId != null && viewId != "all") {
            check(viewId == "experience") { "CONTINUUM_VIEW_PROVIDER_UNBOUND" }
            // Presentation mapping only: original universe_id and event_type are never rewritten.
            terms.add("e.event_type=?"); args.add("conversation.turn")
        }
        if (query != null) {
            check(query.length in 2..160) { "CONTINUUM_SEARCH_QUERY_INVALID" }
            terms.add("(instr(lower(COALESCE(e.payload_json,'')),lower(?))>0 OR instr(lower(COALESCE(e.event_type,'')),lower(?))>0)")
            args.add(query); args.add(query)
        }
        return Scope(terms.joinToString(" AND "), args)
    }

    private fun number(bound: Bound, expression: String, scope: Scope): Long =
        bound.db.rawQuery("SELECT $expression FROM continuum_events e WHERE ${scope.sql}", scope.args.toTypedArray()).use { c ->
            check(c.moveToFirst()) { "CONTINUUM_COUNT_UNAVAILABLE" }; c.getLong(0)
        }

    private fun status(bound: Bound): JSONObject {
        val count = number(bound,"count(*)",scope(bound)) + brainCount(bound,"knowledge") + brainCount(bound,"devices")
        return JSONObject().put("ownerId", bound.body.deviceId).put("bodyId", bound.body.deviceId)
            .put("state", "OWNER_LOCAL_RETAINED_RECORDS_READABLE").put("recordCount", count).put("records", count)
            .put("writeAllowed", false).put("downloadAllowed", false).put("governanceState", GOVERNANCE).put("writeBlockReason", WRITE_REASON)
            .put("retrievalMode", "LEXICAL_STORED_TEXT_NO_EMBEDDINGS_NO_MODEL")
            .put("source", "EXISTING_OWNER_BOUND_CONTINUUM_SQLITE").put("logicalResourceId", "continuum-storage")
            .put("bindingRevision", bound.binding.revision).put("formulaExecution", "NOT_EXECUTED")
            .put("maxPageSize", MAX_PAGE).put("maxContentPreviewBytes", MAX_CONTENT_BYTES)
    }

    private fun counts(bound: Bound): JSONObject {
        val all = scope(bound)
        val total = number(bound, "count(*)", all)
        val groups = JSONArray()
        val ids = mutableSetOf<String>()
        val sql = "SELECT e.universe_id,count(*),COALESCE(sum(length(CAST(e.payload_json AS BLOB))),0) FROM continuum_events e WHERE ${all.sql} GROUP BY e.universe_id ORDER BY e.universe_id LIMIT ?"
        bound.db.rawQuery(sql, (all.args + (MAX_UNIVERSE_GROUPS + 1).toString()).toTypedArray()).use { c ->
            while (c.moveToNext()) {
                ids.add(c.getString(0))
                if (groups.length() < MAX_UNIVERSE_GROUPS) groups.put(JSONObject().put("universeId", c.getString(0))
                    .put("id", c.getString(0)).put("count", c.getLong(1)).put("recordCount", c.getLong(1))
                    .put("byteLength", c.getLong(2)).put("source", "OBSERVED_EVENT_UNIVERSE_GROUP"))
            }
        }
        val metadata = JSONArray()
        val prefix = bound.root + ":"
        bound.db.rawQuery("SELECT id,parent_id,kind,title,authority,schema_version,trust_state,health_state FROM universes WHERE authority=? AND (id=? OR substr(id,1,?)=?) ORDER BY id LIMIT ?",
            arrayOf(bound.body.deviceId, bound.root, prefix.length.toString(), prefix, MAX_UNIVERSE_GROUPS.toString())).use { c ->
            while (c.moveToNext()) metadata.put(JSONObject().put("universeId", c.getString(0)).put("parentId", nullable(c, 1))
                .put("kind", nullable(c, 2)).put("title", nullable(c, 3)).put("authority", nullable(c, 4))
                .put("schemaVersion", nullable(c, 5)).put("trustState", nullable(c, 6)).put("healthState", nullable(c, 7))
                .put("source", "PERSISTED_UNIVERSE_ROW"))
        }
        val experience = scope(bound, viewId = "experience")
        val sourceUniverseIds = JSONArray()
        bound.db.rawQuery("SELECT DISTINCT e.universe_id FROM continuum_events e WHERE ${experience.sql} ORDER BY e.universe_id LIMIT ?",
            (experience.args + MAX_UNIVERSE_GROUPS.toString()).toTypedArray()).use { c -> while (c.moveToNext()) sourceUniverseIds.put(c.getString(0)) }
        val view = JSONObject().put("id", "experience").put("count", number(bound, "count(*)", experience))
            .put("description", "Retained conversation events; original Continuum universe IDs preserved in each record")
            .put("sourceUniverseIds", sourceUniverseIds).put("mappingKind", "PRESENTATION_ONLY")
        val knowledgeCount=brainCount(bound,"knowledge")
        val devicesCount=brainCount(bound,"devices")
        val expandedViews=JSONArray().put(view)
            .put(JSONObject().put("id","knowledge").put("count",knowledgeCount)
                .put("description","Owner-local Brain index records, original file bytes not implied")
                .put("source","EXISTING_PHONE_BRAIN_SQLITE"))
            .put(JSONObject().put("id","devices").put("count",devicesCount)
                .put("description","Owner-local recorded hardware and device observations")
                .put("source","EXISTING_PHONE_BRAIN_SQLITE"))
        return JSONObject().put("total",total+knowledgeCount+devicesCount)
            .put("totalRecords",total+knowledgeCount+devicesCount)
            .put("records",total+knowledgeCount+devicesCount)
            .put("byteLength", number(bound, "COALESCE(sum(length(CAST(e.payload_json AS BLOB))),0)", all))
            .put("universes", groups).put("universeMetadata", metadata).put("views", expandedViews)
            .put("universeGroupsTruncated", ids.size > MAX_UNIVERSE_GROUPS).put("ownerId", bound.body.deviceId)
            .put("bodyId", bound.body.deviceId).put("writeAllowed", false).put("governanceState", GOVERNANCE)
    }

    // Owner-local node projection; data remains in the existing Brain.
    private fun brainPredicate(view:String):String {
        val active="COALESCE(n.status,'') != 'tombstoned'"
        val devices="(n.type='hardware-component' OR n.id LIKE 'device-current:%')"
        return when(view){
            "knowledge"->"$active AND NOT $devices"
            "devices"->"$active AND $devices"
            else->error("CONTINUUM_BRAIN_VIEW_NOT_ADMITTED")
        }
    }
    private fun brainCount(bound:Bound,view:String):Long =
        bound.brain.rawQuery("SELECT count(*) FROM nodes n WHERE "+brainPredicate(view),null).use{c->
            check(c.moveToFirst()){"CONTINUUM_BRAIN_COUNT_MISSING"};c.getLong(0)
        }
    private fun encodeBrainId(id:String):String=android.util.Base64.encodeToString(id.toByteArray(Charsets.UTF_8),
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
    private fun decodeBrainId(token:String):String {
        check(token.matches(Regex("[A-Za-z0-9_-]{1,5500}"))){"CONTINUUM_BRAIN_ID_INVALID"}
        val value=runCatching{android.util.Base64.decode(token,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)}
            .getOrElse{error("CONTINUUM_BRAIN_ID_INVALID")}.toString(Charsets.UTF_8)
        check(value.length in 1..4096 && encodeBrainId(value)==token){"CONTINUUM_BRAIN_ID_INVALID"}
        return value
    }
    private fun brainRecordId(bound:Bound,view:String,id:String):String =
        "brain:"+bound.body.deviceId+":"+view+":"+encodeBrainId(id)
    private fun resolveBrainId(bound:Bound,recordId:String):Pair<String,String>{
        val prefix="brain:"+bound.body.deviceId+":"
        check(recordId.startsWith(prefix)){"CONTINUUM_RECORD_OUTSIDE_BODY"}
        val rest=recordId.removePrefix(prefix)
        val pivot=rest.indexOf(':')
        check(pivot>0){"CONTINUUM_BRAIN_ID_INVALID"}
        val view=rest.substring(0,pivot)
        brainPredicate(view)
        return Pair(view,decodeBrainId(rest.substring(pivot+1)))
    }
    private fun brainDescriptor(bound:Bound,recordId:String):Pair<JSONObject,ByteArray>{
        val (view,id)=resolveBrainId(bound,recordId)
        val sql="SELECT n.id,n.parent_id,n.title,n.type,n.status,n.source_path,n.metadata_json,n.created_at,n.updated_at FROM nodes n WHERE n.id=? AND "+brainPredicate(view)+" LIMIT 2"
        val row=bound.brain.rawQuery(sql,arrayOf(id)).use{c->
            check(c.moveToFirst()){"CONTINUUM_RECORD_NOT_FOUND"}
            val obj=JSONObject()
            val keys=listOf("id","parent_id","title","type","status","source_path","metadata_json","created_at","updated_at")
            for(i in keys.indices)obj.put(keys[i],if(c.isNull(i))JSONObject.NULL else if(i>=7)c.getLong(i) else c.getString(i))
            check(!c.moveToNext()){"CONTINUUM_BRAIN_ID_AMBIGUOUS"}
            obj
        }
        val bytes=row.toString(2).toByteArray(Charsets.UTF_8)
        check(bytes.size<=MAX_CONTENT_BYTES){"CONTINUUM_CONTENT_PREVIEW_LIMIT"}
        val parent=row.optString("parent_id").takeUnless{it.isBlank()||it=="null"}
        val label=parent?.let{key->
            bound.brain.rawQuery("SELECT title FROM nodes WHERE id=? LIMIT 1",arrayOf(key)).use{c->
                if(c.moveToFirst())c.getString(0) else null
            }
        }
        val src=JSONObject().put("kind","EXISTING_PHONE_BRAIN_SQLITE").put("table","nodes")
            .put("sourceRecordId",id).put("bodyId",bound.body.deviceId).put("presentationOnly",true)
            .put("representation","RETAINED_RECORD_JSON")
            .put("recordedRelativePath",row.optString("source_path").takeUnless{it.isBlank()||it=="null"}?:JSONObject.NULL)
            .put("parentSourceRecordId",parent?:JSONObject.NULL).put("parentLabel",label?:JSONObject.NULL)
        val desc=JSONObject().put("recordId",recordId).put("viewId",view)
            .put("collection",if(view=="devices")"Recorded device observations" else "Existing phone Brain index")
            .put("title",row.optString("title").takeIf{it.isNotBlank()&&it!="null"}?:id)
            .put("kind","index").put("mimeType","application/json").put("byteLength",bytes.size)
            .put("contentSha256",sha256(bytes)).put("bodyId",bound.body.deviceId).put("ownerId",bound.body.deviceId)
            .put("source",src).put("storageKind","INDEX_REFERENCE")
            .put("verification",JSONObject().put("state","SOURCE_RECORD_READ").put("originalFileVerified",false)
                .put("formulaExecution","NOT_EXECUTED"))
        return Pair(desc,bytes)
    }
    private fun brainContent(bound:Bound,id:String):WebResourceResponse {
        val (record,bytes)=brainDescriptor(bound,id)
        return WebResourceResponse("application/json","UTF-8",200,"OK",
            headers()+mapOf("X-Continuum-Record-Id" to id,
               "X-Continuum-Content-SHA256" to record.getString("contentSha256")),
            ByteArrayInputStream(bytes))
    }
    private fun brainRecords(bound:Bound,uri:Uri,view:String,search:Boolean):JSONObject {
        val where=brainPredicate(view)
        val limit=parameter(uri,"limit")?.toIntOrNull()?:MAX_PAGE
        check(limit in 1..MAX_PAGE){"CONTINUUM_PAGE_INVALID"}
        val q=if(search)parameter(uri,"query",required=true)?.trim() else null
        if(q!=null)check(q.length in 2..160){"CONTINUUM_SEARCH_QUERY_INVALID"}
        val cursor=parameter(uri,"cursor")
        val prefix="node:"+view+":"
        val after=if(cursor==null)null else {
            check(cursor.startsWith(prefix)){"CONTINUUM_CURSOR_SCOPE_INVALID"}
            decodeBrainId(cursor.removePrefix(prefix))
        }
        val sql=StringBuilder(where)
        val args=mutableListOf<String>()
        if(after!=null){sql.append(" AND n.id COLLATE BINARY > ?");args.add(after)}
        if(q!=null){sql.append(" AND instr(lower(COALESCE(n.title,'')),lower(?))>0");args.add(q)}
        args.add((limit+1).toString())
        val ids=mutableListOf<String>()
        bound.brain.rawQuery("SELECT n.id FROM nodes n WHERE "+sql+" ORDER BY n.id COLLATE BINARY LIMIT ?",args.toTypedArray()).use{c->
            while(c.moveToNext())ids.add(c.getString(0))
        }
        val page=ids.take(limit)
        val records=JSONArray()
        for(id in page)records.put(brainDescriptor(bound,brainRecordId(bound,view,id)).first)
        return JSONObject().put("records",records).put("total",brainCount(bound,view))
            .put("nextCursor",if(ids.size>limit&&page.isNotEmpty())prefix+encodeBrainId(page.last()) else JSONObject.NULL)
            .put("retrievalMode","OWNER_LOCAL_SQLITE_SOURCE_ID_KEYSET").put("llmUsed",false)
    }

    private const val COLUMNS = "e.id,e.universe_id,e.event_type,CASE WHEN length(CAST(e.payload_json AS BLOB)) <= 1048576 THEN e.payload_json ELSE NULL END,e.receipt_ref,e.captured_at,length(CAST(e.payload_json AS BLOB)),e.payload_json IS NULL"
    private fun read(c: Cursor) = Event(c.getLong(0), c.getString(1), if (c.isNull(2)) null else c.getString(2), if (c.isNull(3)) null else c.getString(3),
        if (c.isNull(4)) null else c.getString(4), if (c.isNull(5)) null else c.getLong(5), if (c.isNull(6)) null else c.getLong(6), c.getInt(7) == 1)

    private fun records(bound: Bound, uri: Uri, search: Boolean): JSONObject {
        val sourceView=parameter(uri,"viewId")
        if(sourceView=="knowledge"||sourceView=="devices")return brainRecords(bound,uri,sourceView,search)
        val query = if (search) parameter(uri, "query", required = true)!!.trim() else null
        val selected = scope(bound, parameter(uri, "universeId"), parameter(uri, "viewId"), query)
        val rawLimit = parameter(uri, "limit")
        val limit = if (rawLimit == null) MAX_PAGE else rawLimit.toIntOrNull() ?: error("CONTINUUM_PAGE_INVALID")
        check(limit in 1..MAX_PAGE) { "CONTINUUM_PAGE_INVALID" }
        val cursor = parameter(uri, "cursor")?.let {
            check(Regex("[1-9][0-9]{0,18}").matches(it)) { "CONTINUUM_CURSOR_INVALID" }
            it.toLongOrNull() ?: error("CONTINUUM_CURSOR_INVALID")
        }
        val terms = selected.sql + if (cursor == null) "" else " AND e.id<?"
        val args = selected.args + (cursor?.let { listOf(it.toString()) } ?: emptyList()) + (limit + 1).toString()
        val rows = mutableListOf<Event>()
        bound.db.rawQuery("SELECT $COLUMNS FROM continuum_events e WHERE $terms ORDER BY e.id DESC LIMIT ?", args.toTypedArray()).use { c ->
            while (c.moveToNext()) rows.add(read(c))
        }
        val page = rows.take(limit)
        return JSONObject().put("records", JSONArray(page.map { descriptor(bound, it) }))
            .put("nextCursor", if (rows.size > limit) page.last().id.toString() else JSONObject.NULL)
            .put("total", number(bound, "count(*)", selected)).put("retrievalMode", "LEXICAL_STORED_TEXT_NO_EMBEDDINGS_NO_MODEL")
    }

    private fun recordId(bound: Bound, event: Event) = "continuum-event:${bound.body.deviceId}:${event.id}"
    private fun event(bound: Bound, recordId: String): Event {
        val prefix = "continuum-event:${bound.body.deviceId}:"
        check(recordId.startsWith(prefix)) { "CONTINUUM_RECORD_OUTSIDE_BODY" }
        val raw = recordId.removePrefix(prefix)
        check(Regex("[1-9][0-9]{0,18}").matches(raw)) { "CONTINUUM_RECORD_ID_INVALID" }
        val id = raw.toLongOrNull() ?: error("CONTINUUM_RECORD_ID_INVALID")
        val selected = scope(bound)
        return bound.db.rawQuery("SELECT $COLUMNS FROM continuum_events e WHERE ${selected.sql} AND e.id=?", (selected.args + id.toString()).toTypedArray()).use { c ->
            check(c.moveToFirst()) { "CONTINUUM_RECORD_NOT_FOUND" }; read(c)
        }
    }

    private fun descriptor(bound: Bound, event: Event): JSONObject {
        val bytes = event.payload?.toByteArray(Charsets.UTF_8)
        val contentState = if (event.payloadNull) "NULL_PAYLOAD_COLUMN" else if (bytes == null) "PREVIEW_SIZE_LIMIT" else "RETAINED_JSON_TEXT"
        return JSONObject().put("recordId", recordId(bound, event)).put("universeId", event.universeId)
            .put("collection", "continuum_events").put("title", "${(event.eventType ?: "Retained event").take(160)} · ${event.id}")
            .put("mimeType", "application/json").put("byteLength", bytes?.size?.toLong() ?: event.storedBytes ?: 0L)
            .put("contentSha256", bytes?.let { sha256(it) } ?: JSONObject.NULL).put("contentAvailable", bytes != null)
            .put("ownerId", bound.body.deviceId).put("bodyId", bound.body.deviceId)
            .put("source", JSONObject().put("kind", "RETAINED_CONTENT").put("provider", "OWNER_LOCAL_ANDROID_CONTINUUM")
                .put("logicalResourceId", "continuum-storage").put("table", "continuum_events").put("rowId", event.id.toString())
                .put("eventType", event.eventType ?: JSONObject.NULL).put("universeId", event.universeId).put("bindingRevision", bound.binding.revision)
                .put("contentState", contentState))
            .put("createdAt", event.capturedAt?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL)
            .put("storageKind", "SQLITE_RETAINED_PAYLOAD").put("verification", "OBSERVED_RETAINED_BYTES_NOT_GOVERNED_ADMISSION")
            .put("receiptId", event.receipt ?: JSONObject.NULL).put("formulaExecution", "NOT_EXECUTED")
    }

    private fun content(bound: Bound, event: Event): WebResourceResponse {
        check(!event.payloadNull) { "CONTINUUM_NO_RETAINED_PAYLOAD" }
        val text = event.payload ?: error("CONTINUUM_CONTENT_PREVIEW_LIMIT")
        val bytes = text.toByteArray(Charsets.UTF_8)
        check(bytes.size <= MAX_CONTENT_BYTES) { "CONTINUUM_CONTENT_PREVIEW_LIMIT" }
        return WebResourceResponse("application/json", "UTF-8", 200, "OK", headers() + mapOf(
            "X-Continuum-Record-Id" to recordId(bound, event), "X-Continuum-Content-SHA256" to sha256(bytes)
        ), ByteArrayInputStream(bytes))
    }

    private fun nullable(c: Cursor, column: Int): Any = if (c.isNull(column)) JSONObject.NULL else c.getString(column)
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun headers() = mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff", "Referrer-Policy" to "no-referrer")
    private fun json(status: Int, result: JSONObject): WebResourceResponse {
        val data = JSONObject().put("ok", true).put("result", result).toString().toByteArray(Charsets.UTF_8)
        check(data.size <= MAX_JSON_BYTES) { "CONTINUUM_RESPONSE_LIMIT" }
        return WebResourceResponse("application/json", "UTF-8", status, "OK", headers(), ByteArrayInputStream(data))
    }
    private fun failure(status: Int, code: String): WebResourceResponse {
        val bytes = JSONObject().put("ok", false).put("error", code).put("writeAllowed", false)
            .put("governanceState", GOVERNANCE).put("formulaExecution", "NOT_EXECUTED").toString().toByteArray(Charsets.UTF_8)
        return WebResourceResponse("application/json", "UTF-8", status, if (status == 404) "Not Found" else "Blocked", headers(), ByteArrayInputStream(bytes))
    }
}