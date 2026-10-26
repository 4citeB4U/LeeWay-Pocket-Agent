/*
REGION: LEEWAY.TEST.VISION.EDGE
TAG: LFW.V1.ANDROID_RIGHT_TAG_GEOMETRY_TEST
5WH: WHAT=Bound the Vision side tag on a Fold6 screen; WHY=No inaccessible controls or horizontal free-floating rail; WHO=LeeWay UI reviewer; WHERE=Android unit tests; WHEN=2026-10-10; HOW=Pure geometry tests; LICENSE=MIT
*/
package industries.leeway.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionEdgeGeometryTest {
    private val fold = OverlayPlacement.Frame(884, 2208, 0, 70, 0, 80)

    @Test fun collapsedTagIsAnchoredToRight() {
        val p=VisionEdgeGeometry.place(fold,130,360,.72f)
        assertEquals(0,p.right)
        assertEquals(130,p.width)
        assertEquals(360,p.height)
        assertTrue(p.top >= fold.insetTop)
        assertTrue(p.top+p.height <= fold.screenHeight-fold.insetBottom)
    }

    @Test fun expandedPanelClampsToFoldedWidthAndInsets() {
        val narrow=OverlayPlacement.Frame(340,840,0,42,6,44)
        val p=VisionEdgeGeometry.place(narrow,400,900,1f)
        assertEquals(6,p.right)
        assertEquals(334,p.width)
        assertEquals(754,p.height)
        assertEquals(42,p.top)
    }

    @Test fun movingTagChangesOnlyVerticalPlacement() {
        val a=VisionEdgeGeometry.place(fold,130,360,.4f)
        val shifted=VisionEdgeGeometry.shift(fold,360,.4f,180f)
        val b=VisionEdgeGeometry.place(fold,130,360,shifted)
        assertEquals(a.right,b.right)
        assertEquals(a.width,b.width)
        assertEquals(a.height,b.height)
        assertTrue(b.top>a.top)
    }

    @Test fun verticalMoveNeverEscapesScreen() {
        val shifted=VisionEdgeGeometry.shift(fold,360,.8f,99999f)
        val p=VisionEdgeGeometry.place(fold,130,360,shifted)
        assertEquals(1f,shifted,0f)
        assertEquals(fold.screenHeight-fold.insetBottom-p.height,p.top)
    }

    @Test fun popupAppearsLeftOfAnUnmovedRightEdgeTag() {
        val tab=VisionEdgeGeometry.place(fold,130,360,.72f)
        val popup=VisionEdgeGeometry.panel(fold,tab,530,900,12)
        val after=VisionEdgeGeometry.place(fold,130,360,.72f)
        assertEquals(tab,after)
        assertEquals(0,tab.right)
        assertEquals(tab.right+tab.width+12,popup.right)
        assertEquals(530,popup.width)
        assertTrue(popup.top>=fold.insetTop)
        assertTrue(popup.top+popup.height<=fold.screenHeight-fold.insetBottom)
    }

    @Test fun popupIsClampedBesideTagOnNarrowDisplay() {
        val narrow=OverlayPlacement.Frame(340,840,0,42,6,44)
        val tab=VisionEdgeGeometry.place(narrow,130,160,.67f)
        val popup=VisionEdgeGeometry.panel(narrow,tab,500,700,10)
        assertEquals(146,popup.right)
        assertEquals(194,popup.width)
        assertTrue(popup.top>=42)
        assertTrue(popup.top+popup.height<=796)
    }
    @Test fun invalidGeometryFailsClosed() {
        var blocked=false
        try{VisionEdgeGeometry.place(OverlayPlacement.Frame(0,0),100,100,.5f)}
        catch(_:IllegalArgumentException){blocked=true}
        assertTrue(blocked)
    }
}
