/*
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: INGESTION_TRANSACTION_BEHAVIOR
WHO: LeeWay qualification; WHAT: Execute actual shared reconciliation with an explicit test store.
WHEN: Before native acceptance; WHERE: JVM unit tests, not physical-device or SQLite proof.
WHY: Partial scans, revoked handles and stale observations must not erase owner records.
HOW: Fixture store rollback plus actual core calls; no fixture enters product evidence.
LICENSE: MIT
*/
package industries.leeway.brain

import org.junit.Assert.*
import org.junit.Test

class BrainIngestionTest {
    private val identity=BodyIdentity("test-device","a".repeat(64))
    private val fileKey="b".repeat(64)
    private val dirKey="c".repeat(64)
    private class Store: BrainIngestionStore {
        var identity: BodyIdentity?=null
        var failEvent=false
        val nodes=linkedMapOf<String,BrainNode>()
        val bindings=linkedMapOf<String,ResourceBinding>()
        val files=linkedMapOf<String,StoredFileObservation>()
        val tombstones=mutableSetOf<String>()
        val events=mutableListOf<BrainFileChange>()
        override fun <T> atomic(operation:()->T):T {
            val i=identity;val n=nodes.toMap();val b=bindings.toMap();val f=files.toMap();val t=tombstones.toSet();val e=events.toList()
            try{return operation()}catch(error:Exception){identity=i;nodes.clear();nodes.putAll(n);bindings.clear();bindings.putAll(b);files.clear();files.putAll(f);tombstones.clear();tombstones.addAll(t);events.clear();events.addAll(e);throw error}
        }
        override fun owner()=identity
        override fun hasRecords()=nodes.isNotEmpty()
        override fun claimOwner(identity:BodyIdentity){this.identity=identity}
        override fun upsertNode(node:BrainNode,observedAtMs:Long){nodes[node.id]=node}
        override fun binding(logicalId:String)=bindings[logicalId]
        override fun putBinding(binding:ResourceBinding){bindings[binding.logicalId]=binding}
        override fun resourceFiles(resourceId:String)=files.values.filter{it.resourceId==resourceId}
        override fun putFile(record:StoredFileObservation,parentNodeId:String){files[record.nodeId]=record;nodes[record.nodeId]=BrainNode(record.nodeId,parentNodeId,if(record.observation.directory)"directory" else "file",record.observation.title)}
        override fun retainTombstone(record:StoredFileObservation,reason:String,capturedAtMs:Long){files[record.nodeId]=record;tombstones.add(record.nodeId)}
        override fun clearTombstone(nodeId:String){tombstones.remove(nodeId)}
        override fun appendFileChange(change:BrainFileChange){check(!failEvent){"TEST_TRANSACTION_FAULT"};events.add(change)}
    }
    private fun ready():Store=Store().also{
        DigitalBrain.bootstrap(identity,it,BrainObservation(emptyMap(),emptyList()),1)
        DigitalBrain.bindResource(it,identity,ResourceBinding("workspace",identity.deviceId,"test-storage://owner/root",1,true))
    }
    private fun entry(key:String=fileKey,parent:String?=null)=FileObservation(key,parent,"note.txt",false,"test-storage://owner/root/note.txt","metadata-v1",20,10)
    private fun census(entries:List<FileObservation> = listOf(entry()),time:Long=100,complete:Boolean=true)=FileCensus("scan-"+time,ResourceHandle("workspace",identity.deviceId,1),time,entries,complete,if(complete)emptyList()else listOf("TEST_PARTIAL"))
    private fun apply(s:Store,c:FileCensus=census())=BrainIngestion.reconcile(identity,s,c)

    @Test fun createdFileRetainsHistoryAndHonestContentState(){val s=ready();val r=apply(s);assertEquals(1,r.created);assertEquals(1,s.events.size);assertEquals("create",s.events[0].operation);assertEquals("METADATA_ONLY_CONTENT_NOT_READ",r.contentState);assertEquals("SOURCE_CHANGE_TO_COMMIT_NOT_MEASURED",r.freshnessState);assertEquals("NOT_EXECUTED",r.formulaState)}
    @Test fun unchangedCensusDoesNotDuplicateNodesOrEvents(){val s=ready();apply(s);val count=s.nodes.size;val r=apply(s,census(time=200));assertEquals(1,r.unchanged);assertEquals(count,s.nodes.size);assertEquals(1,s.events.size)}
    @Test fun metadataChangeRecordsPreviousVersion(){val s=ready();apply(s);val r=apply(s,census(listOf(entry().copy(metadataVersion="v2",sizeBytes=22)),200));assertEquals(1,r.modified);assertEquals("metadata-v1",s.events.last().previous?.observation?.metadataVersion);assertEquals(1,s.files.size)}
    @Test fun suppliedStableIdentitySupportsRenameWithoutGuessing(){val s=ready();apply(s);val r=apply(s,census(listOf(entry().copy(title="renamed.txt",sourceUri="test-storage://owner/root/renamed.txt")),200));assertEquals(1,r.renamed);assertEquals(1,s.files.size);assertEquals(s.events.first().current.nodeId,s.events.last().current.nodeId)}
    @Test fun completeAbsenceRetainsTombstoneAndProvenanceEvent(){val s=ready();apply(s);val r=apply(s,census(emptyList(),200));assertEquals(1,r.deleted);assertEquals(1,s.files.size);assertFalse(s.files.values.single().active);assertEquals(1,s.tombstones.size);assertEquals("delete",s.events.last().operation)}
    @Test fun partialAbsenceCannotDelete(){val s=ready();apply(s);val r=apply(s,census(emptyList(),200,false));assertEquals(0,r.deleted);assertTrue(s.files.values.single().active);assertTrue(s.tombstones.isEmpty())}
    @Test fun repeatedDeletionIsIdempotent(){val s=ready();apply(s);apply(s,census(emptyList(),200));val count=s.events.size;assertEquals(0,apply(s,census(emptyList(),300)).deleted);assertEquals(count,s.events.size)}
    @Test fun returningSourceRestoresWithoutLosingDeletionHistory(){val s=ready();apply(s);apply(s,census(emptyList(),200));assertEquals(1,apply(s,census(time=300)).restored);assertEquals(listOf("create","delete","restore"),s.events.map{it.operation});assertTrue(s.tombstones.isEmpty())}
    @Test fun directoryHierarchyUsesExistingBrainNodes(){val s=ready();val dir=entry(dirKey).copy(directory=true,title="folder",sizeBytes=null);val file=entry(parent=dirKey);apply(s,census(listOf(dir,file)));val root=BrainIngestion.resourceNodeId(identity,"workspace");assertEquals(root+":"+dirKey,s.nodes[root+":"+fileKey]?.parentId)}
    @Test fun revokedPermissionBlocksBeforeAnyDeletion(){val s=ready();apply(s);s.bindings["workspace"]=s.bindings.getValue("workspace").copy(ownerAuthorized=false);assertThrows(IllegalStateException::class.java){apply(s,census(emptyList(),200))};assertTrue(s.files.values.single().active);assertEquals(1,s.events.size)}
    @Test fun staleResourceHandleBlocksCommit(){val s=ready();apply(s);s.bindings["workspace"]=s.bindings.getValue("workspace").copy(revision=2);assertThrows(IllegalStateException::class.java){apply(s,census(emptyList(),200))};assertTrue(s.files.values.single().active)}
    @Test fun anotherBodyCannotUseTheSameHandle(){val s=ready();val other=BodyIdentity("other-device","d".repeat(64));assertThrows(IllegalStateException::class.java){BrainIngestion.reconcile(other,s,census())};assertTrue(s.files.isEmpty())}
    @Test fun duplicateKeysBlockEntireCensus(){val s=ready();assertThrows(IllegalArgumentException::class.java){apply(s,census(listOf(entry(),entry())))};assertTrue(s.files.isEmpty())}
    @Test fun sourceParentCycleBlocksBeforeMutation(){val s=ready();val a=entry().copy(directory=true,parentKey=dirKey);val b=entry(dirKey,fileKey).copy(directory=true);assertThrows(IllegalArgumentException::class.java){apply(s,census(listOf(a,b)))};assertTrue(s.files.isEmpty())}
    @Test fun missingOrNonDirectoryParentIsRejected(){val s=ready();assertThrows(IllegalArgumentException::class.java){apply(s,census(listOf(entry(parent=dirKey))))};assertThrows(IllegalArgumentException::class.java){apply(s,census(listOf(entry(dirKey),entry(parent=dirKey))))};assertTrue(s.files.isEmpty())}
    @Test fun staleCensusCannotOverwriteNewerState(){val s=ready();apply(s,census(time=200));assertThrows(IllegalStateException::class.java){apply(s,census(emptyList(),100))};assertTrue(s.files.values.single().active)}
    @Test fun writeFailureRollsBackAllMetadataAndEvents(){val s=ready();val before=s.nodes.toMap();s.failEvent=true;assertThrows(IllegalStateException::class.java){apply(s)};assertEquals(before,s.nodes);assertTrue(s.files.isEmpty());assertTrue(s.events.isEmpty())}
    @Test fun omissionsCannotBeLabelledComplete(){val s=ready();assertThrows(IllegalArgumentException::class.java){apply(s,census().copy(omissions=listOf("permission failure")))};assertTrue(s.files.isEmpty())}
    @Test fun resourceScopeCorruptionIsRejected(){val s=ready();s.files["outside"]=StoredFileObservation("brain:other:node","workspace",entry(),1,true);assertThrows(IllegalStateException::class.java){apply(s)};assertEquals(1,s.files.size)}
}
