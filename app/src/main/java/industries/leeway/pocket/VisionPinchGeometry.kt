/*
REGION: LEEWAY.UI.VISION.PINCH_GEOMETRY
TAG: LEEWAY.LFW.V1.ANDROID_PROPORTIONAL_PINCH
5WH: WHAT=Pure bounded whole-Vision-window geometry. WHY=Restore real two-finger shrink/enlarge of the approved Eye without clipping. WHO=LeeWay Creator/Agent Lee; WHERE=Existing Pocket VisionActivity adapter. WHEN=2026-10-10. HOW=Aspect-preserving scale and inset-safe bounds. LICENSE=MIT.
*/
package industries.leeway.pocket

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object VisionPinchGeometry {
    data class Rect(val x:Int, val y:Int, val width:Int, val height:Int)
    fun scaled(start:Rect, gestureRatio:Float, minimumWidth:Int, minimumHeight:Int,
               availableWidth:Int, availableHeight:Int):Rect {
        require(start.width>0 && start.height>0 && minimumWidth>0 && minimumHeight>0) {
            "VISION_PINCH_START_INVALID"
        }
        require(availableWidth>0 && availableHeight>0 && gestureRatio.isFinite() && gestureRatio>0f) {
            "VISION_PINCH_SCREEN_OR_GESTURE_INVALID"
        }
        val minScale=max(minimumWidth.toFloat()/start.width,minimumHeight.toFloat()/start.height)
        val maxScale=min(availableWidth.toFloat()/start.width,availableHeight.toFloat()/start.height)
        require(maxScale>0f){"VISION_PINCH_SCREEN_UNAVAILABLE"}
        val factor=gestureRatio.coerceIn(min(minScale,maxScale),maxScale)
        val width=(start.width*factor).roundToInt().coerceIn(1,availableWidth)
        val height=(start.height*factor).roundToInt().coerceIn(1,availableHeight)
        val x=(start.x+(start.width-width)/2f).roundToInt().coerceIn(0,(availableWidth-width).coerceAtLeast(0))
        val y=(start.y+(start.height-height)/2f).roundToInt().coerceIn(0,(availableHeight-height).coerceAtLeast(0))
        return Rect(x,y,width,height)
    }
}
