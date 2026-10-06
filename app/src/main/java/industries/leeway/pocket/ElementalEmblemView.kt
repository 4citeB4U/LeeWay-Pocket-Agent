/* REGION: LEEWAY.UI; TAG: SHARED_ARTWORK_OPERATIONAL_STATE
WHO: Agent Lee owner. WHAT: Reuse approved PC artwork for Android tab and identity button.
WHEN: Idle and native voice callbacks. WHERE: Visual layer only, not telemetry or cognition.
WHY: No generic substitute glyph. HOW: Original bitmap plus reversible hue/filter animation.
LICENSE: MIT */
package industries.leeway.pocket

import android.content.Context
import android.graphics.*
import android.view.View
import android.os.SystemClock
import kotlin.math.*

class ElementalEmblemView(context:Context):View(context) {
    private val bitmap=context.assets.open("agent-lee-emblem.png").use{BitmapFactory.decodeStream(it)}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val start=SystemClock.elapsedRealtime()
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val phase=(SystemClock.elapsedRealtime()-start)/1000f
        val state=visualState
        val angle=when(state){"LISTENING"->105f;"SPEAKING"->0f;"THINKING"->210f;"BLOCKED"->280f;else->(phase*24f)%360f}
        paint.colorFilter=ColorMatrixColorFilter(hue(angle))
        val centerX=width/2f;val centerY=height/2f
        canvas.save();canvas.scale(.94f+.04f*sin(phase*1.4f),.98f,centerX,centerY)
        canvas.drawBitmap(bitmap,null,RectF(1f,1f,width-1f,height-1f),paint);canvas.restore()
        if(isAttachedToWindow&&visibility==VISIBLE)postInvalidateDelayed(80)
    }
    private fun hue(degrees:Float):ColorMatrix {
        val c=cos(degrees*PI/180).toFloat();val s=sin(degrees*PI/180).toFloat()
        return ColorMatrix(floatArrayOf(.213f+.787f*c-.213f*s,.715f-.715f*c-.715f*s,.072f-.072f*c+.928f*s,0f,0f,
            .213f-.213f*c+.143f*s,.715f+.285f*c+.140f*s,.072f-.072f*c-.283f*s,0f,0f,
            .213f-.213f*c-.787f*s,.715f-.715f*c+.715f*s,.072f+.928f*c+.072f*s,0f,0f,0f,0f,0f,1f,0f))
    }
    companion object { @Volatile var visualState="IDLE" }
}
