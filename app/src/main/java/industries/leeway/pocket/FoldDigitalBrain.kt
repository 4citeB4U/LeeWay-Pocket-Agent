package industries.leeway.pocket

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object FoldDigitalBrain {
    private const val BODY = "phone-fold6"
    private fun stableId(value:String):String {
        val bytes=MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
    fun bootstrap(context:Context){
        val dbs=LeeWayBodyDatabases(context)
        val brain=dbs.brain.writableDatabase
        val now=System.currentTimeMillis()
        fun node(key:String,parent:String?,type:String,title:String,meta:JSONObject){
            val v=ContentValues()
            v.put("id",key);v.put("parent_id",parent);v.put("type",type);v.put("title",title);v.put("status","active")
            v.put("metadata_json",meta.toString());v.put("created_at",now);v.put("updated_at",now)
            brain.insertWithOnConflict("nodes",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
        val root="brain:"+BODY
        val system=root+":system";val user=root+":user";val hardware=system+":hardware";val runtime=system+":runtime"
        val apps=system+":applications";val files=user+":files"
        node(root,null,"universe","Fold6 Digital Brain",JSONObject().put("bodyId",BODY).put("architecture","recursive-universe"))
        node(system,root,"universe","SYSTEM UNIVERSE",JSONObject())
        node(user,root,"universe","USER UNIVERSE",JSONObject())
        node(hardware,system,"universe","HARDWARE UNIVERSE",deviceSnapshot())
        node(runtime,system,"universe","RUNTIME UNIVERSE",runtimeSnapshot())
        node(apps,system,"universe","APPLICATION UNIVERSE",JSONObject())
        node(files,user,"universe","FILESYSTEM UNIVERSE",JSONObject().put("authorizedRoot",context.filesDir.absolutePath))
        context.packageManager.getInstalledApplications(0).take(500).forEach { app ->
            node(apps+":"+stableId(app.packageName),apps,"application",app.packageName,JSONObject().put("package",app.packageName))
        }
        val hw=ContentValues()
        hw.put("captured_at",now);hw.put("cpu_json","{}");hw.put("memory_json","{}");hw.put("storage_json",deviceSnapshot().toString())
        hw.put("battery_json","{}");hw.put("thermal_json","{}");hw.put("display_json","{}");hw.put("sensors_json","{}");hw.put("network_json","{}")
        brain.insert("hardware_stats",null,hw)
        val continuum=dbs.continuum.writableDatabase
        fun universe(uid:String,parent:String?,kind:String,title:String){
            val v=ContentValues();v.put("id",uid);v.put("parent_id",parent);v.put("kind",kind);v.put("title",title);v.put("authority",BODY)
            v.put("schema_version","leeway.continuum.v1");v.put("trust_state","LOCAL");v.put("health_state","ACTIVE");v.put("metadata_json","{}");v.put("created_at",now);v.put("updated_at",now)
            continuum.insertWithOnConflict("universes",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
        val croot="continuum:"+BODY
        universe(croot,null,"body","Fold6 Continuum")
        universe(croot+":brain",croot,"digital-brain","Digital Brain Universe")
        universe(croot+":workstation",croot,"workstation","Workstation Universe")
        universe(croot+":runtime",croot,"runtime","Runtime Universe")
        universe(croot+":evidence",croot,"evidence","Evidence Universe")
        dbs.ldwmd.writableDatabase
    }
    private fun deviceSnapshot():JSONObject {
        val stat=StatFs(Environment.getDataDirectory().path)
        return JSONObject().put("manufacturer",Build.MANUFACTURER).put("model",Build.MODEL).put("device",Build.DEVICE)
            .put("android",Build.VERSION.RELEASE).put("sdk",Build.VERSION.SDK_INT).put("storageBytes",stat.totalBytes).put("storageAvailableBytes",stat.availableBytes)
    }
    private fun runtimeSnapshot()=JSONObject().put("agentId","agent-lee").put("bodyId",BODY).put("consciousness","SHARED_AGENT_LEE")
        .put("digitalBrain","LOCAL").put("continuum","LOCAL_FEDERATED").put("ldwmd","LOCAL_ISOLATED")
        .put("voiceAuthority","4citeB4U/LeeWay-Voice-Fabric").put("skillsAuthority","4citeB4U/LeeWay-Agent-Skills")
        .put("formulaAuthority","4citeB4U/Leeway-formula-live").put("deviceAuthority","INTERNAL_LEEWAY_DEVICE_FABRIC")
    fun snapshot(context:Context):String {
        val db=LeeWayBodyDatabases(context).brain.readableDatabase
        val counts=JSONObject()
        listOf("nodes","edges","provenance","formula_passports","sync_events","node_tombstones","experiences","hardware_stats").forEach { table ->
            db.rawQuery("SELECT COUNT(*) FROM "+table,null).use { c -> if(c.moveToFirst()) counts.put(table,c.getLong(0)) }
        }
        val roots=JSONArray()
        db.rawQuery("SELECT id,title,type,metadata_json FROM nodes WHERE parent_id IS NULL OR type='universe' ORDER BY title LIMIT 100",null).use { c ->
            while(c.moveToNext()) roots.put(JSONObject().put("id",c.getString(0)).put("title",c.getString(1)).put("type",c.getString(2)).put("metadata",JSONObject(c.getString(3))))
        }
        return JSONObject().put("schemaVersion","leeway.digital-brain.context.v1").put("bodyId",BODY).put("counts",counts).put("universes",roots).put("runtimeBrain",runtimeSnapshot()).toString()
    }
}
