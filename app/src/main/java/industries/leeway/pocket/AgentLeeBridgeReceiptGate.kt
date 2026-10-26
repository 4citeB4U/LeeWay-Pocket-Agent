package industries.leeway.pocket

/** Validates receipt fields against the existing Device Bridge receipt schema. */
internal object AgentLeeBridgeReceiptGate {
 data class Receipt(val receiptId:String,val deviceId:String,val capability:String,
                    val result:String,val authorizationBasis:String,val preGate:String,
                    val postGate:String,val evidenceHashes:List<String>)
 private val hash=Regex("^[a-fA-F0-9]{64}$")
 fun accept(receipt:Receipt?,expectedDeviceId:String):Boolean {
  if(receipt==null || expectedDeviceId.isBlank() || receipt.deviceId!=expectedDeviceId)return false
  if(receipt.receiptId.isBlank() || receipt.capability!="device.ui.tap")return false
  if(receipt.result!="PASS" || receipt.authorizationBasis.isBlank())return false
  if(receipt.preGate!="PASS" || receipt.postGate!="PASS")return false
  return receipt.evidenceHashes.isNotEmpty() && receipt.evidenceHashes.all { hash.matches(it) }
 }
}
