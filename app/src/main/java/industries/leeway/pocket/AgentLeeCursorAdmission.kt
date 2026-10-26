package industries.leeway.pocket

import kotlin.math.abs

/** Android presentation gate mirroring Device Bridge agent-cursor-admission.mjs.
 * A UI projection is evidence-linked; it is NOT authority to dispatch a gesture.
 */
internal object AgentLeeCursorAdmission {
    data class Event(
        val agentId:String, val authority:String, val requestId:String, val actionId:String,
        val action:String, val device:String, val ownerApproved:Boolean, val cancelled:Boolean,
        val x:Double?, val y:Double?, val timestamp:Long
    )
    data class Execution(
        val requestId:String, val actionId:String, val device:String,
        val state:String, val provider:String, val receiptHash:String
    )
    data class Observation(
        val requestId:String, val actionId:String, val postStateVerified:Boolean, val receiptHash:String
    )
    private val id=Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{11,127}$")
    private val receipt=Regex("^[a-fA-F0-9]{64}$")
    private val actions=setOf("move","click","double-click","scroll","type","tap","swipe")
    private val positioned=setOf("move","click","double-click","tap","swipe")
    fun admit(e:Event?,r:Execution?,o:Observation?,nowMs:Long=System.currentTimeMillis()):Boolean {
        if(e==null||r==null||o==null)return false
        if(e.agentId!="agent-lee"||e.authority!="4citeB4U/LEEWAY-DEVICE-BRIDGE")return false
        if(!id.matches(e.requestId)||!id.matches(e.actionId))return false
        if(e.device!="android"||e.action !in actions||!e.ownerApproved||e.cancelled)return false
        if(r.requestId!=e.requestId||r.actionId!=e.actionId||r.device!=e.device)return false
        if(r.state!="EXECUTED"||r.provider!="LEEWAY_DEVICE_BRIDGE"||!receipt.matches(r.receiptHash))return false
        if(o.requestId!=e.requestId||o.actionId!=e.actionId||!o.postStateVerified||o.receiptHash!=r.receiptHash)return false
        if(e.action in positioned) {
            val x=e.x?:return false
            val y=e.y?:return false
            if(!x.isFinite()||!y.isFinite()||x<0||y<0||x>32768||y>32768)return false
        }
        if(abs(nowMs.toDouble()-e.timestamp.toDouble())>5000.0)return false
        return true
    }
}
