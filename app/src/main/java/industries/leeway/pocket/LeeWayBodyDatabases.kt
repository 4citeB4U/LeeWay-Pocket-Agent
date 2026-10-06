package industries.leeway.pocket

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LeeWayBodyDatabases(context:Context): java.io.Closeable {
    companion object {
        const val BRAIN_NAME="leeway-digital-brain.db"
        const val CONTINUUM_NAME="leeway-continuum.db"
        const val WORKING_NAME="leeway-working-memory.db"
    }
    init {
        val expected=setOf(BRAIN_NAME,CONTINUUM_NAME,WORKING_NAME)
        val other=context.databaseList().filter { name -> name.startsWith("leeway-") && name.endsWith(".db") && name !in expected }
        check(other.isEmpty()) { "EXISTING_DATABASE_SET_REQUIRES_OWNER_MIGRATION" }
    }
    override fun close(){brain.close();continuum.close();ldwmd.close()}
    val brain=BrainDb(context)
    val continuum=ContinuumDb(context)
    val ldwmd=LdwmdDb(context)

    class BrainDb(c:Context):SQLiteOpenHelper(c,BRAIN_NAME,null,2){
        override fun onCreate(db:SQLiteDatabase){ createOwnership(db); db.execSQL("""
          CREATE TABLE nodes(id TEXT PRIMARY KEY,parent_id TEXT,type TEXT,subtype TEXT,title TEXT,status TEXT,source_path TEXT,content_hash TEXT,metadata_json TEXT,created_at INTEGER,updated_at INTEGER)
        """); db.execSQL("CREATE TABLE edges(id TEXT PRIMARY KEY,source_id TEXT,target_id TEXT,predicate TEXT,provenance_kind TEXT,metadata_json TEXT)");
          db.execSQL("CREATE TABLE provenance(id INTEGER PRIMARY KEY AUTOINCREMENT,node_id TEXT,kind TEXT,source TEXT,description TEXT,captured_at INTEGER)");
          db.execSQL("CREATE TABLE formula_passports(node_id TEXT PRIMARY KEY,formula_id TEXT,receipt_ref TEXT,state TEXT,created_at INTEGER)");
          db.execSQL("CREATE TABLE sync_events(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT,op TEXT,node_id TEXT,prev_node_id TEXT,path TEXT,prev_path TEXT,hash TEXT,prev_hash TEXT,captured_at INTEGER)");
          db.execSQL("CREATE TABLE node_tombstones(node_id TEXT PRIMARY KEY,deleted_at INTEGER,reason TEXT,metadata_json TEXT)");
          db.execSQL("CREATE TABLE experiences(experience_id TEXT PRIMARY KEY,timestamp INTEGER,scope TEXT,capability_id TEXT,agent_id TEXT,goal TEXT,formula_version TEXT,outcome TEXT,veritas_status TEXT,hardware_state_json TEXT,provenance TEXT,learning_eligibility TEXT)");
          db.execSQL("CREATE TABLE hardware_stats(id INTEGER PRIMARY KEY AUTOINCREMENT,captured_at INTEGER,cpu_json TEXT,memory_json TEXT,storage_json TEXT,battery_json TEXT,thermal_json TEXT,display_json TEXT,sensors_json TEXT,network_json TEXT)");
        }
        private fun createOwnership(db:SQLiteDatabase){
            db.execSQL("CREATE TABLE IF NOT EXISTS brain_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),device_id TEXT NOT NULL UNIQUE,key_fingerprint TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS brain_resource_bindings(logical_id TEXT PRIMARY KEY,body_id TEXT NOT NULL,resource_uri TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision>0),owner_authorized INTEGER NOT NULL CHECK(owner_authorized IN (0,1)))")
        }
        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){if(o<2)createOwnership(db)}
    }
    class ContinuumDb(c:Context):SQLiteOpenHelper(c,CONTINUUM_NAME,null,1){
        override fun onCreate(db:SQLiteDatabase){
          db.execSQL("CREATE TABLE universes(id TEXT PRIMARY KEY,parent_id TEXT,kind TEXT,title TEXT,authority TEXT,schema_version TEXT,trust_state TEXT,health_state TEXT,metadata_json TEXT,created_at INTEGER,updated_at INTEGER)");
          db.execSQL("CREATE TABLE universe_links(id TEXT PRIMARY KEY,source_id TEXT,target_id TEXT,predicate TEXT,provenance TEXT,receipt_ref TEXT)");
          db.execSQL("CREATE TABLE continuum_events(id INTEGER PRIMARY KEY AUTOINCREMENT,universe_id TEXT,event_type TEXT,payload_json TEXT,receipt_ref TEXT,captured_at INTEGER)");
          db.execSQL("CREATE TABLE federation_peers(body_id TEXT PRIMARY KEY,state TEXT,last_sync_at INTEGER,authority TEXT,metadata_json TEXT)");
        }
        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){}
    }
    class LdwmdDb(c:Context):SQLiteOpenHelper(c,WORKING_NAME,null,1){
        override fun onCreate(db:SQLiteDatabase){
          db.execSQL("CREATE TABLE working_set(key TEXT PRIMARY KEY,value_json TEXT,priority REAL,tier TEXT,expires_at INTEGER,updated_at INTEGER)");
          db.execSQL("CREATE TABLE working_events(id INTEGER PRIMARY KEY AUTOINCREMENT,key TEXT,op TEXT,from_tier TEXT,to_tier TEXT,reason TEXT,captured_at INTEGER)");
        }
        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){}
    }
}
