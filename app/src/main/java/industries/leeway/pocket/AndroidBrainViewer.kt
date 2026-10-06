/*
REGION: LEEWAY.BRAIN.ADAPTER.ANDROID
TAG: OWNER_SCOPED_RECURSIVE_VIEWER_SQLITE
WHO: Existing Agent Lee local WebView; WHAT: Project the same persisted Brain read-only.
WHEN: Root, children, search and inspector navigation; WHERE: Android adapter only.
WHY: The original recursive renderer must read this installation, not embedded owner records.
HOW: Existing Brain database/identity, parameterized SQL, bounded portable view contract.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import industries.leeway.brain.*
import org.json.JSONArray
import org.json.JSONObject

object AndroidBrainViewer {
    fun request(context:Context,operation:String,arguments:String):String {
        if(arguments.length>8192)return failure("VIEWER_REQUEST_LIMIT")
        return try {
            val args=JSONObject(arguments)
            val identity=AndroidDigitalBrainAdapter.identity(context)
            LeeWayBodyDatabases(context).use { databases ->
                val db=databases.brain.readableDatabase
                db.beginTransaction()
                try {
                    val viewer=BrainViewer(identity,SqliteViewStore(db,identity))
                    val value=when(operation){
                        "root"->JSONObject().put("node",encode(viewer.root())).put("bodyId",identity.deviceId)
                        "children"->{val page=viewer.children(args.getString("id"),args.optInt("offset",0),args.optInt("limit",BrainViewer.PAGE_LIMIT));JSONObject()
                            .put("node",encode(page.node)).put("nodes",JSONArray(page.children.map{encode(it)}))
                            .put("offset",page.offset).put("total",page.total).put("hasMore",page.hasMore)}
                        "node"->{val id=args.getString("id");JSONObject().put("node",encode(viewer.node(id)))
                            .put("pageOffset",viewer.pageOffset(id)).put("ancestors",JSONArray(viewer.ancestors(id).map{encode(it)}))
                            .put("links",JSONArray(viewer.relationships(id).map{JSONObject().put("source",it.sourceId).put("target",it.targetId).put("predicate",it.predicate).put("provenance_kind",it.provenanceKind)}))
                            .put("provenance",JSONArray(viewer.provenance(id).map{JSONObject().put("kind",it.kind).put("source",it.source).put("description",it.description).put("capturedAtMs",it.capturedAtMs)}))}
                        "search"->JSONObject().put("nodes",JSONArray(viewer.search(args.getString("query")).map{encode(it)}))
                        else->throw IllegalArgumentException("VIEWER_OPERATION_NOT_ALLOWED")
                    }
                    val text=JSONObject().put("ok",true).put("result",value).put("bodyId",identity.deviceId)
                        .put("source","OWNER_LOCAL_BRAIN_SQLITE").put("formulaExecution","NOT_EXECUTED").toString()
                    check(text.length<=1048576){"VIEWER_RESPONSE_LIMIT"}
                    db.setTransactionSuccessful();text
                } finally {db.endTransaction()}
            }
        } catch(error:Exception) {
            val code=error.message?.takeIf{Regex("[A-Z_]{3,100}").matches(it)}?:"VIEWER_NATIVE_QUERY_BLOCKED"
            failure(code)
        }
    }
    private fun failure(reason:String)=JSONObject().put("ok",false).put("error",reason).put("source","OWNER_LOCAL_BRAIN_SQLITE").toString()
    private fun encode(node:BrainViewNode)=JSONObject().put("id",node.id).put("parent_id",node.parentId ?: JSONObject.NULL)
        .put("label",node.title).put("title",node.title).put("type",node.type).put("status",node.status)
        .put("child_count",node.childCount).put("expandable",node.childCount>0 || node.type in setOf("directory","universe"))
        .put("source_path",node.sourceUri ?: "").put("metadata",JSONObject(node.metadataJson))
        .put("domain",if(node.type in setOf("file","directory"))"fs" else "system")
        .put("origin","OWNER_LOCAL_BRAIN_SQLITE")

    private class SqliteViewStore(private val db:SQLiteDatabase,private val identity:BodyIdentity):BrainViewStore {
        private val root=DigitalBrain.rootId(identity)
        private val prefix=root+":"
        private val columns="n.id,n.parent_id,n.title,n.type,n.status,n.metadata_json,n.source_path,(SELECT COUNT(*) FROM nodes c WHERE c.parent_id=n.id AND COALESCE(c.status,'')!='tombstoned')"
        private val active="COALESCE(n.status,'')!='tombstoned'"
        private fun read(c:Cursor)=BrainViewNode(c.getString(0),if(c.isNull(1))null else c.getString(1),c.getString(2)?:c.getString(0),
            c.getString(3)?:"object",c.getString(4)?:"observed",c.getString(5)?:"{}",c.getLong(7),if(c.isNull(6))null else c.getString(6))
        private fun rows(sql:String,args:Array<String>)=db.rawQuery(sql,args).use{c->buildList{while(c.moveToNext())add(read(c))}}
        override fun owner():BodyIdentity?=db.rawQuery("SELECT device_id,key_fingerprint FROM brain_owner WHERE singleton=1",null).use{c->if(c.moveToFirst())BodyIdentity(c.getString(0),c.getString(1))else null}
        override fun node(id:String)=rows("SELECT $columns FROM nodes n WHERE n.id=? AND $active",arrayOf(id)).singleOrNull()
        override fun children(parentId:String,offset:Int,limit:Int)=rows("SELECT $columns FROM nodes n WHERE n.parent_id=? AND $active ORDER BY COALESCE(n.title,'') COLLATE NOCASE,n.id LIMIT ? OFFSET ?",arrayOf(parentId,limit.toString(),offset.toString()))
        override fun childIndex(parentId:String,childId:String):Long = db.rawQuery("SELECT COUNT(*) FROM nodes c JOIN nodes n ON n.id=? AND n.parent_id=? WHERE c.parent_id=n.parent_id AND COALESCE(c.status,'')!='tombstoned' AND (COALESCE(c.title,'') COLLATE NOCASE < COALESCE(n.title,'') COLLATE NOCASE OR (COALESCE(c.title,'') COLLATE NOCASE = COALESCE(n.title,'') COLLATE NOCASE AND c.id<n.id))",arrayOf(childId,parentId)).use{c->check(c.moveToFirst());c.getLong(0)}
        override fun search(query:String,limit:Int)=rows("SELECT $columns FROM nodes n WHERE (n.id=? OR substr(n.id,1,?)=?) AND $active AND instr(lower(COALESCE(n.title,'')),lower(?))>0 ORDER BY n.title COLLATE NOCASE,n.id LIMIT ?",arrayOf(root,prefix.length.toString(),prefix,query,limit.toString()))
        override fun relationships(id:String,limit:Int):List<BrainViewLink> =db.rawQuery("SELECT e.source_id,e.target_id,e.predicate,e.provenance_kind FROM edges e JOIN nodes s ON s.id=e.source_id JOIN nodes t ON t.id=e.target_id WHERE (e.source_id=? OR e.target_id=?) AND COALESCE(s.status,'')!='tombstoned' AND COALESCE(t.status,'')!='tombstoned' ORDER BY e.id LIMIT ?",arrayOf(id,id,limit.toString())).use{c->buildList{while(c.moveToNext())add(BrainViewLink(c.getString(0),c.getString(1),c.getString(2)?:"CONNECTED",c.getString(3)))}}
        override fun provenance(id:String,limit:Int):List<BrainViewEvidence> =db.rawQuery("SELECT kind,source,description,captured_at FROM provenance WHERE node_id=? ORDER BY id DESC LIMIT ?",arrayOf(id,limit.toString())).use{c->buildList{while(c.moveToNext())add(BrainViewEvidence(c.getString(0)?:"OBSERVATION",c.getString(1),c.getString(2),if(c.isNull(3))null else c.getLong(3)))}}
    }
}
