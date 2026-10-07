/* REGION: LEEWAY.UI.SURFACE; TAG: OWNER_FLOATING_ACTIVITY_CONTROLS
WHO: Owner of existing Pocket Agent surfaces. WHAT: Minimize, restore, move and resize their existing Activities.
WHEN: Owner opens a hamburger surface. WHERE: Android Activity window only.
WHY: Keep Brain, Skills, Models, Continuum, VT and Voice available without replacing their native bindings.
HOW: Existing Android Window and ViewGroup APIs; no new runtime, registry or database. LICENSE: MIT */
package industries.leeway.pocket

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

internal object OwnerSurfaceWindow {
    fun attach(activity: Activity, title: String) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val original = content.getChildAt(0) ?: return
        content.removeView(original)
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).roundToInt()
        val shell = FrameLayout(activity).apply {
            setBackgroundColor(Color.rgb(6, 13, 24))
        }
        val barHeight = dp(48)
        shell.addView(original, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = barHeight })
        val bar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(4), 0)
            background = GradientDrawable().apply { setColor(Color.rgb(15, 29, 47)); setStroke(dp(1), Color.rgb(76, 131, 160)) }
        }
        val heading = TextView(activity).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 1
            isSingleLine = true
        }
        bar.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fun button(symbol: String, action: () -> Unit) {
            bar.addView(Button(activity).apply {
                text = symbol
                textSize = 16f
                minWidth = dp(44)
                minimumWidth = dp(44)
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(dp(52), barHeight))
        }
        var collapsed = false
        var rememberedHeight = 0
        var rememberedWidth = 0
        val display = activity.resources.displayMetrics
        val initialWidth = (display.widthPixels * .86f).roundToInt().coerceAtLeast(dp(280))
        val initialHeight = (display.heightPixels * .76f).roundToInt().coerceAtLeast(dp(260))
        fun setWindow(width: Int, height: Int) {
            val attrs = activity.window.attributes
            attrs.width = width
            attrs.height = height
            attrs.gravity = Gravity.TOP or Gravity.START
            activity.window.attributes = attrs
        }
        button("−") {
            val attrs = activity.window.attributes
            if (!collapsed) {
                rememberedHeight = attrs.height
                rememberedWidth = attrs.width
                original.visibility = View.GONE
                collapsed = true
                setWindow(attrs.width, barHeight)
            } else {
                original.visibility = View.VISIBLE
                collapsed = false
                setWindow(rememberedWidth.coerceAtLeast(dp(280)), rememberedHeight.coerceAtLeast(dp(260)))
            }
        }
        button("□") {
            if (collapsed) {
                original.visibility = View.VISIBLE
                collapsed = false
            }
            setWindow(initialWidth, initialHeight)
        }
        button("×") { activity.finish() }
        shell.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight, Gravity.TOP))
        val grip = TextView(activity).apply {
            text = "◢"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
        }
        shell.addView(grip, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END or Gravity.BOTTOM))
        var downX = 0f; var downY = 0f; var initialX = 0; var initialY = 0
        heading.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    initialX = activity.window.attributes.x; initialY = activity.window.attributes.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val attrs = activity.window.attributes
                    attrs.x = initialX + (event.rawX - downX).roundToInt()
                    attrs.y = initialY + (event.rawY - downY).roundToInt()
                    activity.window.attributes = attrs
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        var resizeX = 0f; var resizeY = 0f; var resizeW = 0; var resizeH = 0
        grip.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    resizeX = event.rawX; resizeY = event.rawY
                    resizeW = activity.window.attributes.width; resizeH = activity.window.attributes.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!collapsed) {
                        val w = (resizeW + (event.rawX - resizeX).roundToInt()).coerceIn(dp(280), display.widthPixels)
                        val h = (resizeH + (event.rawY - resizeY).roundToInt()).coerceIn(dp(260), display.heightPixels)
                        setWindow(w, h)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        content.addView(shell, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        activity.window.setDimAmount(0f)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        activity.window.setBackgroundDrawableResource(android.R.color.transparent)
        setWindow(initialWidth, initialHeight)
    }
}
