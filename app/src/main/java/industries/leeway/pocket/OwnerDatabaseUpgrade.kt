/*
REGION: LEEWAY.BRAIN.OWNER_UPGRADE
TAG: SAME_APPLICATION_SCHEMA_MIGRATION
WHO: Existing app owner; WHAT: Upgrade the same app-private databases without deleting records.
WHEN: Signed in-place update before Brain bootstrap; WHERE: Android storage adapter only.
WHY: Fixed installation names must not cause duplicate Brains or block an owner out.
HOW: Schema identification, exact one-root check, resumable rename plan, original backups, transactions.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import industries.leeway.brain.BodyIdentity
import industries.leeway.pocket.devices.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OwnerDatabaseUpgrade {
    @Synchronized fun apply(context:Context){
        val targets=setOf(LeeWayBodyDatabases.BRAIN_NAME,LeeWayBodyDatabases.CONTINUUM_NAME,LeeWayBodyDatabases.WORKING_NAME)
        val pending=File(context.noBackupFilesDir,"brain-owner-upgrade.pending.json")
        val legacy=context.databaseList().filter{it.startsWith("leeway-")&&it.endsWith(".db")&&it !in targets}
        if(!pending.exists()&&legacy.isEmpty())return
        val identity=DeviceIdentity.ensure(context)
        val plan=if(pending.exists())JSONObject(pending.readText())else {
            check(context.databaseList().none{it in targets}){"OWNER_UPGRADE_AMBIGUOUS_MIXED_DATABASES"}
            val matches=linkedMapOf<String,String>();var previousRoot:String?=null
            for(name in legacy){
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,SQLiteDatabase.OPEN_READONLY).use{db->
                    val tables=mutableSetOf<String>();db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'",null).use{c->while(c.moveToNext())tables.add(c.getString(0))}
                    val target=when{
                        setOf("nodes","edges","provenance","sync_events","hardware_stats").all{it in tables}->LeeWayBodyDatabases.BRAIN_NAME
                        setOf("universes","universe_links","continuum_events","federation_peers").all{it in tables}->LeeWayBodyDatabases.CONTINUUM_NAME
                        setOf("working_set","working_events").all{it in tables}->LeeWayBodyDatabases.WORKING_NAME
                        else->error("OWNER_UPGRADE_UNKNOWN_SCHEMA")
                    }
                    check(target !in matches){"OWNER_UPGRADE_MULTIPLE_BRAINS_REQUIRE_REVIEW"};matches[target]=name
                    if(target==LeeWayBodyDatabases.BRAIN_NAME){
                        val roots=mutableListOf<String>();db.rawQuery("SELECT id FROM nodes WHERE parent_id IS NULL",null).use{c->while(c.moveToNext())roots.add(c.getString(0))}
                        check(roots.size==1&&roots[0].startsWith("brain:")){"OWNER_UPGRADE_ROOT_AMBIGUOUS"};previousRoot=roots[0]
                        db.rawQuery("SELECT count(*) FROM nodes WHERE id!=? AND substr(id,1,?)!=?",arrayOf(roots[0],(roots[0].length+1).toString(),roots[0]+":")).use{c->c.moveToFirst();check(c.getLong(0)==0L){"OWNER_UPGRADE_MIXED_GRAPH_IDENTITIES"}}
                    }
                }
            }
            check(matches.keys==targets&&previousRoot!=null){"OWNER_UPGRADE_COMPONENT_SET_INCOMPLETE"}
            JSONObject().put("deviceId",identity.deviceId).put("fingerprint",identity.publicKeyFingerprintSha256).put("previousRoot",previousRoot).put("files",JSONObject(matches)).also{writeAtomic(pending,it.toString())}
        }
        check(plan.getString("deviceId")==identity.deviceId&&plan.getString("fingerprint")==identity.publicKeyFingerprintSha256){"OWNER_UPGRADE_IDENTITY_CHANGED"}
        val backup=File(context.noBackupFilesDir,"brain-owner-upgrade-originals").apply{mkdirs()}
        val files=plan.getJSONObject("files")
        for(target in targets){
            val original=files.getString(target)
            check(File(original).name==original){"OWNER_UPGRADE_PATH_INVALID"}
            val old=context.getDatabasePath(original);val current=context.getDatabasePath(target)
            check(!(old.exists()&&current.exists())){"OWNER_UPGRADE_DUPLICATE_DATABASE_REQUIRES_REVIEW"}
            if(old.exists()){
                // Recover any SQLite journal before copying/renaming; no application writers have started.
                SQLiteDatabase.openDatabase(old.path,null,SQLiteDatabase.OPEN_READWRITE).use{db->db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null).use{it.moveToFirst()}}
                if(!File(backup,original).exists())old.copyTo(File(backup,original),overwrite=false)
                check(old.renameTo(current)){"OWNER_UPGRADE_RENAME_FAILED"}
            }
            check(current.exists()){"OWNER_UPGRADE_DATABASE_MISSING"}
        }
        val oldRoot=plan.getString("previousRoot");val newRoot="brain:"+identity.deviceId
        migrate(context.getDatabasePath(LeeWayBodyDatabases.BRAIN_NAME)){db->
            val hasOwner=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='brain_owner'",null).use{it.moveToFirst();it.getInt(0)>0}
            if(hasOwner){val owner=db.rawQuery("SELECT device_id,key_fingerprint FROM brain_owner",null).use{c->if(c.moveToFirst())BodyIdentity(c.getString(0),c.getString(1))else null};check(owner==null||owner==identity){"OWNER_UPGRADE_OWNER_CONFLICT"}}
            listOf("nodes" to listOf("id","parent_id"),"edges" to listOf("source_id","target_id"),"provenance" to listOf("node_id"),"formula_passports" to listOf("node_id"),"sync_events" to listOf("node_id","prev_node_id"),"node_tombstones" to listOf("node_id")).forEach{(table,columns)->columns.forEach{remap(db,table,it,oldRoot,newRoot)}}
            db.execSQL("UPDATE nodes SET title=? WHERE id=?",arrayOf("Digital Brain",newRoot))
            db.execSQL("CREATE TABLE IF NOT EXISTS brain_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),device_id TEXT NOT NULL UNIQUE,key_fingerprint TEXT NOT NULL)")
            db.execSQL("INSERT OR IGNORE INTO brain_owner VALUES(1,?,?)",arrayOf(identity.deviceId,identity.publicKeyFingerprintSha256))
        }
        migrate(context.getDatabasePath(LeeWayBodyDatabases.CONTINUUM_NAME)){db->
            val old="continuum:"+oldRoot.removePrefix("brain:");val fresh="continuum:"+identity.deviceId
            listOf("universes" to listOf("id","parent_id"),"universe_links" to listOf("source_id","target_id"),"continuum_events" to listOf("universe_id")).forEach{(table,columns)->columns.forEach{remap(db,table,it,old,fresh)}}
            db.execSQL("UPDATE universes SET authority=? WHERE authority=?",arrayOf(identity.deviceId,oldRoot.removePrefix("brain:")))
        }
        plan.put("state","OWNER_LOCAL_UPGRADE_COMPLETED").put("recordsDeleted",false).put("newRoot",newRoot)
        writeAtomic(File(context.noBackupFilesDir,"brain-owner-upgrade.receipt.json"),plan.toString(2))
        check(pending.delete()){"OWNER_UPGRADE_JOURNAL_FINALIZE_FAILED"}
    }
    private fun remap(db:SQLiteDatabase,table:String,column:String,old:String,fresh:String){
        db.execSQL("UPDATE $table SET $column=? || substr($column,?) WHERE $column=? OR substr($column,1,?)=?",arrayOf(fresh,old.length+1,old,old.length+1,old+":"))
    }
    private fun migrate(file:File,fn:(SQLiteDatabase)->Unit){SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE).use{db->db.beginTransaction();try{fn(db);db.setTransactionSuccessful()}finally{db.endTransaction()}}}
    private fun writeAtomic(file:File,text:String){val tmp=File(file.parentFile,file.name+".tmp");tmp.outputStream().use{it.write(text.toByteArray());it.fd.sync()};check(tmp.renameTo(file)){"OWNER_UPGRADE_JOURNAL_WRITE_FAILED"}}
}
