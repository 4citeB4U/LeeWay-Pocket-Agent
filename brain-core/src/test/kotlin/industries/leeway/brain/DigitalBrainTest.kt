/*
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: PORTABLE_BRAIN_BEHAVIOR_TESTS
WHO: LeeWay engineering; WHAT: Execute shared bootstrap and binding behavior with explicit test stores.
WHEN: Before native qualification; WHERE: Kotlin/JVM test harness, not a physical-device claim.
WHY: Fixed identities, stale handles and cross-body writes must fail; reuse must retain state.
HOW: Real shared functions with deterministic fixture inputs and no model dependency.
LICENSE: MIT
*/
package industries.leeway.brain

import org.junit.Assert.*
import org.junit.Test

class DigitalBrainTest {
    private val a=BodyIdentity("device-a","a".repeat(64))
    private val b=BodyIdentity("device-b","b".repeat(64))
    private val observation=BrainObservation(mapOf("kind" to "test-sensor"),listOf(ApplicationObservation("app-1","Test App",emptyMap())))
    private class Store: BrainStore {
        var identity: BodyIdentity?=null
        val nodes=linkedMapOf<String,BrainNode>()
        val created=linkedMapOf<String,Long>()
        val bindings=linkedMapOf<String,ResourceBinding>()
        override fun <T> atomic(operation: () -> T): T {
            val i=identity;val n=nodes.toMap();val c=created.toMap();val r=bindings.toMap()
            try{return operation()}catch(e:Exception){identity=i;nodes.clear();nodes.putAll(n);created.clear();created.putAll(c);bindings.clear();bindings.putAll(r);throw e}
        }
        override fun owner()=identity
        override fun hasRecords()=nodes.isNotEmpty()
        override fun claimOwner(identity:BodyIdentity){this.identity=identity}
        override fun upsertNode(node:BrainNode,observedAtMs:Long){nodes[node.id]=node;created.putIfAbsent(node.id,observedAtMs)}
        override fun binding(logicalId:String)=bindings[logicalId]
        override fun putBinding(binding:ResourceBinding){bindings[binding.logicalId]=binding}
    }
    private fun ready(identity:BodyIdentity=a)=Store().also{DigitalBrain.bootstrap(identity,it,observation,100)}
    private fun binding(body:BodyIdentity=a,uri:String="test-storage://owner/location",revision:Long=1)=ResourceBinding("workspace",body.deviceId,uri,revision,true)

    @Test fun freshBrainUsesSuppliedIdentity(){val s=ready();assertEquals(a,s.owner());assertTrue(s.nodes.containsKey("brain:"+a.deviceId));assertEquals(8,s.nodes.size)}
    @Test fun twoInstallationsHaveIndependentRoots(){val x=ready(a);val y=ready(b);assertTrue(x.nodes.keys.intersect(y.nodes.keys).isEmpty());assertNotEquals(x.owner(),y.owner())}
    @Test fun recursiveHierarchyIsRetained(){val s=ready();val root=DigitalBrain.rootId(a);assertEquals(root+":system",s.nodes[root+":system:applications"]?.parentId);assertEquals(root+":user",s.nodes[root+":user:files"]?.parentId)}
    @Test fun repeatedBootstrapDoesNotDuplicateOrResetCreatedTime(){val s=ready();val before=s.created.toMap();DigitalBrain.bootstrap(a,s,observation,500);assertEquals(8,s.nodes.size);assertEquals(before,s.created)}
    @Test fun existingOtherOwnerBlocksBeforeMutation(){val s=ready();val before=s.nodes.toMap();assertThrows(IllegalStateException::class.java){DigitalBrain.bootstrap(b,s,observation,200)};assertEquals(before,s.nodes);assertEquals(a,s.owner())}
    @Test fun unownedNonemptyBrainRequiresExplicitMigration(){val s=Store();s.nodes["existing"]=BrainNode("existing",null,"universe","Owner data");assertThrows(IllegalStateException::class.java){DigitalBrain.bootstrap(a,s,observation,200)};assertNull(s.owner());assertEquals(1,s.nodes.size)}
    @Test fun invalidBodyIdentityRejected(){assertThrows(IllegalArgumentException::class.java){BodyIdentity("../escape","a".repeat(64))};assertThrows(IllegalArgumentException::class.java){BodyIdentity("device","invalid")}}
    @Test fun duplicateApplicationIdentityRejected(){val s=Store();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bootstrap(a,s,observation.copy(applications=observation.applications+observation.applications),1)};assertNull(s.owner());assertTrue(s.nodes.isEmpty())}
    @Test fun invalidObservationTimeRejected(){assertThrows(IllegalArgumentException::class.java){DigitalBrain.bootstrap(a,Store(),observation,-1)}}
    @Test fun authorizedResourceResolves(){val s=ready();val next=binding();val h=DigitalBrain.bindResource(s,a,next);assertEquals(next,DigitalBrain.resolveResource(s,a,h))}
    @Test fun relocationKeepsIdentityAndInvalidatesOldHandle(){val s=ready();val h=DigitalBrain.bindResource(s,a,binding());val next=binding(uri="test-storage://new/location",revision=2);val h2=DigitalBrain.bindResource(s,a,next);assertEquals(a,s.owner());assertEquals(next,DigitalBrain.resolveResource(s,a,h2));assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,h)}}
    @Test fun resourceLocationCanContainSpacesAndUnicode(){val s=ready();val next=binding(uri="test-storage://owner/Folder with spaces/資料");val h=DigitalBrain.bindResource(s,a,next);assertEquals(next.resourceUri,DigitalBrain.resolveResource(s,a,h).resourceUri)}
    @Test fun unapprovedResourceNeverBecomesBound(){val s=ready();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding().copy(ownerAuthorized=false))};assertTrue(s.bindings.isEmpty())}
    @Test fun bindingCannotCrossBodies(){val s=ready();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding(b))};assertTrue(s.bindings.isEmpty())}
    @Test fun handleCannotCrossBodies(){val x=ready(a);val y=ready(b);val h=DigitalBrain.bindResource(x,a,binding(a));assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(y,b,h)}}
    @Test fun changedLocationRequiresNewRevision(){val s=ready();DigitalBrain.bindResource(s,a,binding());assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding(uri="test-storage://other/location"))};assertEquals("test-storage://owner/location",s.bindings["workspace"]?.resourceUri)}
    @Test fun sameBindingIsIdempotent(){val s=ready();val first=DigitalBrain.bindResource(s,a,binding());assertEquals(first,DigitalBrain.bindResource(s,a,binding()));assertEquals(1,s.bindings.size)}
    @Test fun missingOrWithdrawnResourceFailsClosed(){val s=ready();assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,ResourceHandle("missing",a.deviceId,1))};val h=DigitalBrain.bindResource(s,a,binding());s.bindings["workspace"]=binding().copy(ownerAuthorized=false);assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,h)}}
}