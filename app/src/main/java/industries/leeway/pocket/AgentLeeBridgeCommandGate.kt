package industries.leeway.pocket

/** Adapter validation against existing LeeWay Device Bridge command.schema.json. */
internal object AgentLeeBridgeCommandGate {
 data class Command(val commandId:String,val capability:String,val authorityTier:String,val sessionId:String,val requiresHumanConfirmation:Boolean)
 fun accept(command:Command?,actionId:String,ownerApproved:Boolean):Boolean {
  if(command==null)return false
  if(command.commandId!=actionId || command.commandId.isBlank())return false
  if(command.sessionId.isBlank() || command.sessionId.length>128)return false
  if(command.authorityTier!="OPERATE")return false
  if(command.capability!="device.ui.tap")return false
  if(!command.requiresHumanConfirmation || !ownerApproved)return false
  return true
 }
}
