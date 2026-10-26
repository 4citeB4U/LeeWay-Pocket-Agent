/*
REGION: LEEWAY.TEST.VISION.PINCH_GEOMETRY
TAG: LFW.V1.ANDROID_PINCH_ACCEPTANCE
5WH: WHAT=Validate whole-window pinch geometry; WHY=avoid clipped image and box-only resizing; WHO=LeeWay reviewer; WHERE=Pocket JUnit; WHEN=2026-10-10; HOW=pure deterministic tests. LICENSE=MIT.
*/
package industries.leeway.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionPinchGeometryTest {
    private val source=VisionPinchGeometry.Rect(30,120,800,1400)
    @Test fun shrinkDownKeepsAspectAndCenter() {
        val p=VisionPinchGeometry.scaled(source,.5f,260,280,920,2200)
        assertEquals(400,p.width);assertEquals(700,p.height)
        assertEquals(230,p.x);assertEquals(470,p.y)
    }
    @Test fun enlargeReachesSafeScreenBounds() {
        val p=VisionPinchGeometry.scaled(source,4f,260,280,900,2100)
        assertEquals(900,p.width)
        assertTrue(p.height<=2100)
        assertEquals(0,p.x)
    }
    @Test fun minimumSizeStopsShrinking() {
        val p=VisionPinchGeometry.scaled(source,.01f,260,280,900,2100)
        assertEquals(260,p.width)
        assertEquals(455,p.height)
    }
    @Test fun resizeWorksOnUnfoldedDisplay() {
        val p=VisionPinchGeometry.scaled(source,1.25f,260,280,1900,2200)
        assertEquals(1000,p.width);assertEquals(1750,p.height)
    }
    @Test fun offscreenPositionsAreClamped() {
        val s=VisionPinchGeometry.Rect(-450,500,800,1400)
        val p=VisionPinchGeometry.scaled(s,.75f,200,200,920,1800)
        assertEquals(0,p.x);assertTrue(p.y>=0)
    }
    @Test fun invalidGestureIsRejected() {
        var blocked=false
        try{VisionPinchGeometry.scaled(source,Float.NaN,200,200,900,1900)}
        catch(_:IllegalArgumentException){blocked=true}
        assertTrue(blocked)
    }
    @Test fun tinyDisplayDoesNotOverflow() {
        val p=VisionPinchGeometry.scaled(source,2f,260,280,260,360)
        assertTrue(p.width<=260&&p.height<=360)
        assertTrue(p.x>=0&&p.y>=0)
    }
}
