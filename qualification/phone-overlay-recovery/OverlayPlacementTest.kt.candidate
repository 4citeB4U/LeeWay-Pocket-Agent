/*
REGION: LEEWAY.UI.QUALIFICATION
TAG: EXISTING_OVERLAY_RECOVERY_REGRESSIONS
WHO: LeeWay engineering. WHAT: Execute actual geometry and owner-restore decisions.
WHEN: Before native overlay acceptance. WHERE: JVM unit tests, not phone screenshots.
WHY: Off-screen coordinates and permission assumptions must not be silent failures.
HOW: Real helper calls against explicit display fixtures; no fixture is device evidence.
LICENSE: MIT
*/
package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class OverlayPlacementTest {
    private val screen=OverlayPlacement.Frame(1000,1800,0,40,20,60)
    @Test fun physicalRightInsetIsPreserved(){val p=OverlayPlacement.place(screen,68,.5f);assertEquals(20,p.right);assertEquals(68,p.size)}
    @Test fun restoredOffscreenAboveIsClamped(){assertEquals(40,OverlayPlacement.place(screen,68,-1000f).top)}
    @Test fun restoredOffscreenBelowIsClamped(){assertEquals(1800-60-68,OverlayPlacement.place(screen,68,1000f).top)}
    @Test fun nonFinitePositionReturnsToVisibleMiddle(){assertEquals(OverlayPlacement.place(screen,68,.5f),OverlayPlacement.place(screen,68,Float.NaN));assertEquals(OverlayPlacement.place(screen,68,.5f),OverlayPlacement.place(screen,68,Float.POSITIVE_INFINITY))}
    @Test fun dragBelowScreenCannotLoseButton(){assertEquals(1f,OverlayPlacement.fraction(screen,68,100000f),0f)}
    @Test fun dragAboveScreenCannotLoseButton(){assertEquals(0f,OverlayPlacement.fraction(screen,68,-100000f),0f)}
    @Test fun displayChangeRestoresRelativePositionNotOldPixels(){val fraction=OverlayPlacement.fraction(screen,68,900f);val changed=OverlayPlacement.Frame(1800,1000,0,20,0,40);val p=OverlayPlacement.place(changed,68,fraction);assertTrue(p.top>=20&&p.top+p.size<=960);assertNotEquals(900,p.top)}
    @Test fun narrowDisplayCapsBadgeToSafeArea(){val p=OverlayPlacement.place(OverlayPlacement.Frame(30,40),68,.5f);assertEquals(30,p.size);assertTrue(p.top+p.size<=40)}
    @Test fun invalidScreenFailsExplicitly(){assertThrows(IllegalArgumentException::class.java){OverlayPlacement.place(OverlayPlacement.Frame(0,100),68,.5f)};assertThrows(IllegalArgumentException::class.java){OverlayPlacement.place(screen,0,.5f)}}
    @Test fun invalidInsetsFailExplicitly(){assertThrows(IllegalArgumentException::class.java){OverlayPlacement.place(screen.copy(insetTop=-1),68,.5f)};assertThrows(IllegalArgumentException::class.java){OverlayPlacement.place(screen.copy(insetRight=1001),68,.5f)}}
    @Test fun ownerDisableCannotBeOverruledByPermission(){assertFalse(OverlayPlacement.shouldRestore(false,true))}
    @Test fun requestedEnableCannotBypassPermission(){assertFalse(OverlayPlacement.shouldRestore(true,false));assertFalse(OverlayPlacement.shouldRestore(false,false));assertTrue(OverlayPlacement.shouldRestore(true,true))}
    @Test fun onlyShortUnmovedUncancelledGestureIsATap(){assertTrue(OverlayPlacement.tapAllowed(false,false,100));assertFalse(OverlayPlacement.tapAllowed(true,false,100));assertFalse(OverlayPlacement.tapAllowed(false,true,100));assertFalse(OverlayPlacement.tapAllowed(false,false,600));assertFalse(OverlayPlacement.tapAllowed(false,false,-1))}
}
