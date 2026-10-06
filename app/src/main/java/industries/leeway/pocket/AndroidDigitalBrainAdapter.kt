/*
REGION: LEEWAY.BRAIN.ADAPTER.ANDROID
TAG: NATIVE_STORAGE_AND_OBSERVATION_FOR_PORTABLE_BRAIN
WHO: Creator-authorized LeeWay; WHAT: Connect the existing local Brain tables to the shared core.
WHEN: Bootstrap, snapshot and binding resolution; WHERE: Android boundary only.
WHY: Reusable Brain behavior must not import Android APIs or a fixed device identity.
HOW: Canonical Device Bridge identity, SQLite transactions and platform-provided storage URIs.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Environment
import android.os.StatFs
import industries.leeway.brain.*
import industries.leeway.pocket.devices.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object AndroidDigitalBrainAdapter {
    fun identity(context: Context): BodyIdentity = DeviceIdentity.ensure(context.applicationContext)
    private fun stableId(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { b -> "%02x".format(b) }

    private fun deviceObservation(): Map<String,String> {
        val stat=StatFs(Environment.getDataDirectory().path)
        return mapOf("manufacturer" to Build.MANUFACTURER,"model" to Build.MODEL,"device" to Build.DEVICE,
            "android" to Build.VERSION.RELEASE,"sdk" to Build.VERSION.SDK_INT.toString(),
            "storageBytes" to stat.totalBytes.toString(),"storageAvailableBytes" to stat.availableBytes.toString())
    }

    private fun runtimeObservation() = mapOf(
        "agentId" to "agent-lee", "brainBootstrap" to "LOCAL_IMPLEMENTATION_REQUIRES_DEVICE_QUALIFICATION",
        "continuum" to "LOCAL_SCHEMA_ONLY_CANONICAL_RUNTIME_UNBOUND",
        "ldwmd" to "LOCAL_SCHEMA_ONLY_CANONICAL_RUNTIME_UNBOUND",
        "conversation" to "CANONICAL_EXECUTOR_UNBOUND", "voice" to "SELECTED_RENDERER_REQUIRES_QUALIFICATION",
        "skills" to "AUTHORITY_REFERENCE_NOT_EXECUTION", "formula" to "NOT_EXECUTED"
    )

    @Synchronized fun bootstrap(context: Context) {
        val body=identity(context)
        LeeWayBodyDatabases(context).use { dbs ->
            val store=SqliteBrainStore(dbs.brain.writableDatabase)
            val apps=context.packageManager.getInstalledApplications(0).map { app ->
                ApplicationObservation(stableId(app.packageName),app.packageName,mapOf("package" to app.packageName))
            }
            DigitalBrain.bootstrap(body,store,BrainObservation(deviceObservation(),apps,runtimeObservation()),System.currentTimeMillis())
            // This adapter observes platform storage. Only the Brain persists these physical mappings.
            val resources=mapOf(
                "app-private-files" to context.filesDir.toURI().toString(),
                "brain-storage" to context.getDatabasePath(LeeWayBodyDatabases.BRAIN_NAME).toURI().toString(),
                "continuum-storage" to context.getDatabasePath(LeeWayBodyDatabases.CONTINUUM_NAME).toURI().toString(),
                "working-memory-storage" to context.getDatabasePath(LeeWayBodyDatabases.WORKING_NAME).toURI().toString()
            )
            resources.forEach { (logicalId,uri) ->
                val old=store.binding(logicalId)
                val revision=if(old==null)1L else if(old.resourceUri==uri && old.ownerAuthorized)old.revision else Math.addExact(old.revision,1)
                DigitalBrain.bindResource(store,body,ResourceBinding(logicalId,body.deviceId,uri,revision,true))
            }
            // These existing scaffolds are not promoted to canonical Continuum or LDWMD execution.
            val continuum=dbs.continuum.writableDatabase
            val root=DigitalBrain.continuumRootId(body)
            val names=mapOf("" to "Device Continuum",":brain" to "Digital Brain Universe",":workstation" to "Workstation Universe",":runtime" to "Runtime Universe",":evidence" to "Evidence Universe")
            continuum.beginTransaction()
            try {
                names.forEach { (suffix,title) ->
                    val values=ContentValues().apply {
                        put("id",root+suffix);if(suffix.isEmpty())putNull("parent_id")else put("parent_id",root)
                        put("kind",if(suffix.isEmpty())"body" else suffix.removePrefix(":"));put("title",title)
                        put("authority",body.deviceId);put("schema_version","leeway.continuum.v1")
                        put("trust_state","LOCAL");put("health_state","UNQUALIFIED");put("metadata_json","{}")
                        put("created_at",System.currentTimeMillis());put("updated_at",System.currentTimeMillis())
                    }
                    continuum.insertWithOnConflict("universes",null,values,SQLiteDatabase.CONFLICT_IGNORE)
                }
                continuum.setTransactionSuccessful()
            } finally {continuum.endTransaction()}
            dbs.ldwmd.writableDatabase
        }
    }

    @Synchronized fun snapshot(context: Context): String {
        val body=identity(context)
        return LeeWayBodyDatabases(context).use { dbs ->
            val db=dbs.brain.readableDatabase
            check(SqliteBrainStore(db).owner()==body) { "BRAIN_OWNER_IDENTITY_MISMATCH" }
            val counts=JSONObject()
            listOf("nodes","edges","provenance","formula_passports","sync_events","node_tombstones","experiences","hardware_stats").forEach { table ->
                db.rawQuery("SELECT COUNT(*) FROM "+table,null).use { c -> if(c.moveToFirst()) counts.put(table,c.getLong(0)) }
            }
            val nodes=JSONArray()
            db.rawQuery("SELECT id,parent_id,title,type,metadata_json FROM nodes WHERE type='universe' ORDER BY title",null).use { c ->
                while(c.moveToNext())nodes.put(JSONObject().put("id",c.getString(0)).put("parentId",if(c.isNull(1))JSONObject.NULL else c.getString(1))
                    .put("title",c.getString(2)).put("type",c.getString(3)).put("metadata",JSONObject(c.getString(4))))
            }
            JSONObject().put("schemaVersion","leeway.digital-brain.context.v1").put("bodyId",body.deviceId)
                .put("state","LOCAL_BOOTSTRAP_ONLY_NOT_FULL_BRAIN_ACCEPTANCE").put("counts",counts).put("universes",nodes)
                .put("informationBrain",JSONObject().put("counts",counts).put("universes",nodes))
                .put("deviceBrain",JSONObject(deviceObservation())).put("runtimeBrain",JSONObject(runtimeObservation())).toString()
        }
    }

    private class SqliteBrainStore(private val db: SQLiteDatabase): BrainStore {
        override fun <T> atomic(operation: () -> T): T {
            db.beginTransaction()
            try {val value=operation();db.setTransactionSuccessful();return value} finally {db.endTransaction()}
        }
        override fun owner(): BodyIdentity? = db.rawQuery("SELECT device_id,key_fingerprint FROM brain_owner WHERE singleton=1",null).use { c ->
            if(c.moveToFirst())BodyIdentity(c.getString(0),c.getString(1))else null
        }
        override fun hasRecords(): Boolean = db.rawQuery("SELECT COUNT(*) FROM nodes",null).use {c->c.moveToFirst();c.getLong(0)>0}
        override fun claimOwner(identity: BodyIdentity) {
            check(owner()==null && !hasRecords()) { "BRAIN_OWNERSHIP_NOT_EMPTY" }
            db.insertOrThrow("brain_owner",null,ContentValues().apply{put("singleton",1);put("device_id",identity.deviceId);put("key_fingerprint",identity.publicKeyFingerprintSha256)})
        }
        override fun upsertNode(node: BrainNode, observedAtMs: Long) {
            val values=ContentValues().apply {
                put("parent_id",node.parentId);put("type",node.type);put("title",node.title);put("status","observed")
                put("metadata_json",JSONObject(node.metadata).toString());put("updated_at",observedAtMs)
            }
            if(db.update("nodes",values,"id=?",arrayOf(node.id))==0){values.put("id",node.id);values.put("created_at",observedAtMs);db.insertOrThrow("nodes",null,values)}
        }
        override fun binding(logicalId: String): ResourceBinding? = db.rawQuery("SELECT body_id,resource_uri,revision,owner_authorized FROM brain_resource_bindings WHERE logical_id=?",arrayOf(logicalId)).use {c->
            if(c.moveToFirst())ResourceBinding(logicalId,c.getString(0),c.getString(1),c.getLong(2),c.getInt(3)==1)else null
        }
        override fun putBinding(binding: ResourceBinding) {
            val v=ContentValues().apply{put("body_id",binding.bodyId);put("resource_uri",binding.resourceUri);put("revision",binding.revision);put("owner_authorized",if(binding.ownerAuthorized)1 else 0)}
            if(db.update("brain_resource_bindings",v,"logical_id=?",arrayOf(binding.logicalId))==0){v.put("logical_id",binding.logicalId);db.insertOrThrow("brain_resource_bindings",null,v)}
        }
    }
}