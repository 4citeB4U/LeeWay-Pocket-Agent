/*
REGION: LEEWAY.BRAIN.PROJECTION
TAG: ORIGINAL_RECURSIVE_VIEWER_READ_CONTRACT
WHO: Owner-authorized LeeWay adapter; WHAT: Read bounded views of the existing Brain.
WHEN: Viewer root, descent, search and inspector requests; WHERE: portable core.
WHY: Renderer caches and visual relationships must never become another Brain or authority.
HOW: Reuse root/node/children/relationships semantics; require same owner and scoped records.
LICENSE: MIT
*/
package industries.leeway.brain

data class BrainViewNode(val id:String,val parentId:String?,val title:String,val type:String,
    val status:String,val metadataJson:String,val childCount:Long,val sourceUri:String?=null)
data class BrainViewLink(val sourceId:String,val targetId:String,val predicate:String,val provenanceKind:String?)
data class BrainViewEvidence(val kind:String,val source:String?,val description:String?,val capturedAtMs:Long?)
data class BrainViewPage(val node:BrainViewNode,val children:List<BrainViewNode>,val offset:Int,val total:Long,val hasMore:Boolean)

interface BrainViewStore {
    fun owner():BodyIdentity?
    fun node(id:String):BrainViewNode?
    fun children(parentId:String,offset:Int,limit:Int):List<BrainViewNode>
    fun childIndex(parentId:String,childId:String):Long
    fun search(query:String,limit:Int):List<BrainViewNode>
    fun relationships(id:String,limit:Int):List<BrainViewLink>
    fun provenance(id:String,limit:Int):List<BrainViewEvidence>
}

/** Presentation queries only. No filesystem access, mutation, Formula calculation or network. */
class BrainViewer(private val identity:BodyIdentity,private val store:BrainViewStore) {
    companion object { const val PAGE_LIMIT=128;const val SEARCH_LIMIT=32;const val EVIDENCE_LIMIT=24 }
    private val root=DigitalBrain.rootId(identity)
    private fun authority(){check(store.owner()==identity){"VIEWER_BRAIN_OWNER_MISMATCH"}}
    private fun scoped(id:String){require(id.length<=1024 && (id==root||id.startsWith(root+":")) && id.none{it<' '}){"VIEWER_NODE_OUTSIDE_BODY"}}
    private fun validate(node:BrainViewNode):BrainViewNode {
        scoped(node.id);node.parentId?.let{scoped(it)}
        check(node.childCount>=0 && node.status!="tombstoned"){"VIEWER_NODE_UNAVAILABLE"}
        check(node.metadataJson.length<=65536 && node.title.length<=4096){"VIEWER_NODE_PAYLOAD_LIMIT"}
        return node
    }
    fun node(id:String):BrainViewNode {authority();scoped(id);val result=validate(store.node(id)?:error("VIEWER_NODE_NOT_FOUND"));check(result.id==id){"VIEWER_RETURNED_NODE_MISMATCH"};return result}
    fun root():BrainViewNode=node(root)
    fun children(id:String,offset:Int=0,limit:Int=PAGE_LIMIT):BrainViewPage {
        require(offset>=0 && offset<=1000000 && limit in 1..PAGE_LIMIT){"VIEWER_PAGE_INVALID"}
        val parent=node(id)
        val rows=store.children(id,offset,limit).map{validate(it)}
        check(rows.size<=limit && rows.map{it.id}.distinct().size==rows.size){"VIEWER_CHILD_PAGE_INVALID"}
        check(rows.all{it.parentId==id}){"VIEWER_RELATION_IS_NOT_CHILD"}
        check(rows.size.toLong()<=parent.childCount && (rows.isEmpty()||offset.toLong()+rows.size<=parent.childCount)){"VIEWER_PAGE_COUNT_MISMATCH"}
        return BrainViewPage(parent,rows,offset,parent.childCount,offset.toLong()+rows.size<parent.childCount)
    }
    fun pageOffset(id:String):Int {
        val child=node(id);val parent=child.parentId?:return 0
        val position=store.childIndex(parent,id)
        check(position>=0 && position<node(parent).childCount && position<=1000000){"VIEWER_CHILD_POSITION_INVALID"}
        return (position/PAGE_LIMIT*PAGE_LIMIT).toInt()
    }
    fun ancestors(id:String):List<BrainViewNode> {
        val path=mutableListOf<BrainViewNode>();val seen=mutableSetOf<String>();var current:BrainViewNode?=node(id)
        while(current!=null){
            check(seen.add(current.id) && path.size<128){"VIEWER_ANCESTRY_CYCLE_OR_LIMIT"};path.add(current)
            current=current.parentId?.let{node(it)}
        }
        check(path.last().id==root){"VIEWER_ANCESTRY_NOT_ROOTED"}
        return path.reversed()
    }
    fun search(query:String):List<BrainViewNode> {
        authority();require(query.trim().length in 2..160){"VIEWER_SEARCH_QUERY_INVALID"}
        val rows=store.search(query.trim(),SEARCH_LIMIT).map{validate(it)}
        check(rows.size<=SEARCH_LIMIT && rows.map{it.id}.distinct().size==rows.size){"VIEWER_SEARCH_PAYLOAD_LIMIT"}
        return rows
    }
    fun relationships(id:String):List<BrainViewLink> {
        node(id);val rows=store.relationships(id,EVIDENCE_LIMIT)
        check(rows.size<=EVIDENCE_LIMIT){"VIEWER_RELATION_LIMIT"}
        rows.forEach{scoped(it.sourceId);scoped(it.targetId);check(it.sourceId==id||it.targetId==id){"VIEWER_UNRELATED_EDGE"}}
        return rows
    }
    fun provenance(id:String):List<BrainViewEvidence> {
        node(id);val rows=store.provenance(id,EVIDENCE_LIMIT)
        check(rows.size<=EVIDENCE_LIMIT && rows.all{(it.description?.length?:0)<=8192 && (it.source?.length?:0)<=8192}){"VIEWER_EVIDENCE_LIMIT"}
        return rows
    }
}
