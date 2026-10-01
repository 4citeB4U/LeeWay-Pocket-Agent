/*
LEEWAY
REGION: POCKET.UI
TAG: POCKET.LEEWAY.FLOATING_MIC.BOOT
WHAT: Restore the owner-enabled Pocket Agent side tab after device/app restart
WHY: Keep the secondary workstation voice ingress persistently reachable
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android Pocket Agent
WHEN: 2026-09-29
HOW: Boot/package receiver restarts the bounded overlay foreground service only when enabled
*/
package industries.leeway.pocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PocketBootReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context,intent: Intent?){
        if(PocketOverlayService.isEnabled(context))PocketOverlayService.start(context)
    }
}
