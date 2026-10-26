/*
REGION: LEEWAY.UI.OVERLAY
TAG: BOUNDED_DEVICE_EDGE_PLACEMENT
WHO: Owner-enabled Agent Lee. WHAT: Keep the existing emblem within actual usable screen bounds.
WHEN: Attach, drag, rotate or resize. WHERE: Pure UI geometry; no device identity or Voice authority.
WHY: Saved coordinates must not lose the tab after a display changes.
HOW: Clamped physical-right placement and normalized vertical position; invalid geometry is rejected.
LICENSE: MIT
*/
package industries.leeway.pocket

import kotlin.math.roundToInt

object OverlayPlacement {
    data class Frame(val screenWidth:Int,val screenHeight:Int,val insetLeft:Int=0,val insetTop:Int=0,val insetRight:Int=0,val insetBottom:Int=0)
    data class Placement(val right:Int,val top:Int,val size:Int)
    fun place(frame:Frame,requestedSize:Int,verticalFraction:Float):Placement {
        require(frame.screenWidth>0 && frame.screenHeight>0 && requestedSize>0){"OVERLAY_SCREEN_METRICS_INVALID"}
        require(listOf(frame.insetLeft,frame.insetTop,frame.insetRight,frame.insetBottom).all{it>=0}){"OVERLAY_INSETS_INVALID"}
        val width=frame.screenWidth-frame.insetLeft-frame.insetRight
        val height=frame.screenHeight-frame.insetTop-frame.insetBottom
        require(width>0 && height>0){"OVERLAY_SAFE_AREA_UNAVAILABLE"}
        val size=minOf(requestedSize,width,height)
        val fraction=if(verticalFraction.isFinite())verticalFraction.coerceIn(0f,1f)else .5f
        return Placement(frame.insetRight,frame.insetTop+((height-size)*fraction).roundToInt(),size)
    }
    fun fraction(frame:Frame,requestedSize:Int,top:Float):Float {
        val placement=place(frame,requestedSize,.5f)
        val travel=frame.screenHeight-frame.insetTop-frame.insetBottom-placement.size
        if(travel<=0 || !top.isFinite())return .5f
        return ((top-frame.insetTop)/travel).coerceIn(0f,1f)
    }
    fun shouldRestore(ownerEnabled:Boolean,permissionGranted:Boolean)=ownerEnabled&&permissionGranted
    fun tapAllowed(cancelled:Boolean,moved:Boolean,durationMs:Long)=!cancelled&&!moved&&durationMs in 0..599
}
