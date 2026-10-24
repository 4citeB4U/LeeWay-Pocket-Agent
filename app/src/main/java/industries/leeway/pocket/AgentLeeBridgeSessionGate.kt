package industries.leeway.pocket

/** Checks existing Device Bridge session.schema.json fields; not authentication. */
internal object AgentLeeBridgeSessionGate {
 data class Session(val sessionId:String,val mode:String,val screenAuthority:String,val controlAuthority:String,val status:String)
 fun accept(s:Session?, expectedSessionId:String):Boolean {
  if(s==null || expectedSessionId.isBlank() || s.sessionId!=expectedSessionId)return false
  if(s.status!="ACTIVE" || s.mode!="ASSIST_SESSION")return false
  if(s.controlAuthority!="INTERACTIVE")return false
  if(s.screenAuthority!="SESSION" && s.screenAuthority!="CONTINUOUS")return false
  return true
 }
}
