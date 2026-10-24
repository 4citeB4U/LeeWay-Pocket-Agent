package industries.leeway.pocket

import kotlin.math.abs

/** Input validation only. Trusted Device Bridge must issue and authenticate requests. */
internal object AgentLeeTouchRequestGate {
 data class Request(
  val agentId:String,val authority:String,val device:String,val deviceId:String,val requestId:String,
  val actionId:String,val action:String,val ownerApproved:Boolean,val cancelled:Boolean,
  val x:Float,val y:Float,val issuedAtMs:Long
 )
 private val id=Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{11,127}$")
 fun accept(q:Request?,expectedDeviceId:String,nowMs:Long=System.currentTimeMillis()):Boolean {
  if(q==null)return false
  if(q.agentId!="agent-lee" || q.authority!="4citeB4U/LEEWAY-DEVICE-BRIDGE" || q.device!="android")return false
  if(expectedDeviceId.isBlank() || q.deviceId!=expectedDeviceId)return false
  if(q.action!="tap" || !q.ownerApproved || q.cancelled)return false
  if(!id.matches(q.requestId)||!id.matches(q.actionId))return false
  if(!q.x.isFinite()||!q.y.isFinite()||q.x<0||q.y<0||q.x>32768||q.y>32768)return false
  if(abs(nowMs.toDouble()-q.issuedAtMs.toDouble())>5000.0)return false
  return true
 }
}
