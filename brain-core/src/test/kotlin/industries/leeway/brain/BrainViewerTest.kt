/*
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: RECURSIVE_VIEWER_READ_BOUNDARIES
WHO: LeeWay engineering; WHAT: Execute the real portable view contract against explicit fixture records.
WHEN: Before native-device acceptance; WHERE: JVM tests, not Android SQLite execution.
WHY: Pagination, containment, ownership and evidence restrictions need behavioral proof.
HOW: Store adapter fixture with faults, real BrainViewer methods, no manufactured product PASS.
LICENSE: MIT
*/
package industries.leeway.brain

import org.junit.Assert.*
import org.junit.Test

class BrainViewerTest {
    private val identity=BodyIdentity("viewer-fixture","a".repeat(64))
    private val root=DigitalBrain.rootId(identity)
    private fun row(id:String,parent:String?,count:Long=0)=BrainViewNode(id,parent,id.substringAfterLast(':'),"universe","observed","{}",count)
    private inner class Store:BrainViewStore {
        var currentOwner:BodyIdentity?=identity
        val rows=linkedMapOf(root to row(root,null,2),"$root:system" to row("$root:system",root,0),"$root:user" to row("$root:user",root,1),"$root:user:files" to row("$root:user:files","$root:user"))
        var injectedChildren:List<BrainViewNode>?=null
        var links=listOf(BrainViewLink("$root:system","$root:user","RECORDED_TEST_RELATION","TEST_FIXTURE"))
        var evidence=listOf(BrainViewEvidence("TEST_FIXTURE",null,"not device evidence",100))
        override fun owner()=currentOwner
        var mismatchedNode=false
        override fun node(id:String)=if(mismatchedNode)rows["$root:user"] else rows[id]
        override fun children(parentId:String,offset:Int,limit:Int)=injectedChildren?:rows.values.filter{it.parentId==parentId}.drop(offset).take(limit)
        override fun childIndex(parentId:String,childId:String)=rows.values.filter{it.parentId==parentId}.indexOfFirst{it.id==childId}.toLong()
        override fun search(query:String,limit:Int)=rows.values.filter{it.title.contains(query,true)}.take(limit)
        override fun relationships(id:String,limit:Int)=links.filter{it.sourceId==id||it.targetId==id}.take(limit)
        override fun provenance(id:String,limit:Int)=evidence.take(limit)
    }
    @Test fun returnedNodeMustMatchRequestedIdentity(){val s=Store();s.mismatchedNode=true;assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).root()}}
    @Test fun exactSiblingRankSelectsTheCorrectPage(){val s=Store();val parent="$root:page";s.rows[parent]=row(parent,root,257);repeat(257){i->s.rows["$parent:$i"]=row("$parent:$i",parent)};assertEquals(256,BrainViewer(identity,s).pageOffset("$parent:256"));assertEquals(0,BrainViewer(identity,s).pageOffset(root))}
    @Test fun rootComesFromTheSameBrainIdentity(){val s=Store();assertEquals(root,BrainViewer(identity,s).root().id)}
    @Test fun directChildrenDoNotIncludeGrandchildren(){val s=Store();val p=BrainViewer(identity,s).children(root);assertEquals(2,p.children.size);assertFalse(p.children.any{it.id.endsWith(":files")})}
    @Test fun relationshipNeighborsCannotLeakIntoChildren(){val s=Store();s.injectedChildren=listOf(s.rows.getValue("$root:system"));assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).children("$root:user")}}
    @Test fun paginationPreservesCountAndHasMore(){val s=Store();val v=BrainViewer(identity,s);val first=v.children(root,0,1);val second=v.children(root,1,1);assertTrue(first.hasMore);assertFalse(second.hasMore);assertNotEquals(first.children,second.children);assertEquals(2L,first.total)}
    @Test fun pageAndSearchBoundsAreEnforced(){val v=BrainViewer(identity,Store());assertThrows(IllegalArgumentException::class.java){v.children(root,0,129)};assertThrows(IllegalArgumentException::class.java){v.children(root,-1)};assertThrows(IllegalArgumentException::class.java){v.search("x")};assertThrows(IllegalArgumentException::class.java){v.search("x".repeat(161))}}
    @Test fun ownerMismatchBlocksRootAndSearch(){val s=Store();s.currentOwner=BodyIdentity("other","b".repeat(64));val v=BrainViewer(identity,s);assertThrows(IllegalStateException::class.java){v.root()};assertThrows(IllegalStateException::class.java){v.search("user")}}
    @Test fun outsideBodyAndMissingNodesAreRejected(){val v=BrainViewer(identity,Store());assertThrows(IllegalArgumentException::class.java){v.node("brain:another:node")};assertThrows(IllegalStateException::class.java){v.node("$root:missing")}}
    @Test fun tombstonedRecordsCannotBeRendered(){val s=Store();s.rows[root]=s.rows.getValue(root).copy(status="tombstoned");assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).root()}}
    @Test fun ancestryRetainsExactRecursiveParentChain(){val v=BrainViewer(identity,Store());assertEquals(listOf(root,"$root:user","$root:user:files"),v.ancestors("$root:user:files").map{it.id})}
    @Test fun ancestryCycleCannotHangViewer(){val s=Store();s.rows["$root:user"]=s.rows.getValue("$root:user").copy(parentId="$root:user:files");assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).ancestors("$root:user:files")}}
    @Test fun unrootedNodeCannotClaimBrainAncestry(){val s=Store();s.rows["$root:user"]=s.rows.getValue("$root:user").copy(parentId=null);assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).ancestors("$root:user")}}
    @Test fun duplicateChildRowsAreRejected(){val s=Store();s.injectedChildren=listOf(s.rows.getValue("$root:user"),s.rows.getValue("$root:user"));assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).children(root)}}
    @Test fun foreignRelationEndpointIsRejected(){val s=Store();s.links=listOf(BrainViewLink(root,"brain:other:node","test",null));assertThrows(IllegalArgumentException::class.java){BrainViewer(identity,s).relationships(root)}}
    @Test fun evidenceIsReadWithoutInventingFormulaPassports(){val v=BrainViewer(identity,Store());assertEquals("TEST_FIXTURE",v.provenance(root).single().kind);assertEquals("not device evidence",v.provenance(root).single().description)}
    @Test fun oversizedMetadataIsRejected(){val s=Store();s.rows[root]=s.rows.getValue(root).copy(metadataJson="x".repeat(65537));assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).root()}}
    @Test fun inconsistentChildCountIsRejected(){val s=Store();s.rows[root]=s.rows.getValue(root).copy(childCount=0);assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).children(root)}}
    @Test fun titleInjectionRemainsRawTextDataNotExecutedCode(){val s=Store();val text="<img src=x onerror=alert(1)>";s.rows[root]=s.rows.getValue(root).copy(title=text);assertEquals(text,BrainViewer(identity,s).root().title)}
}
