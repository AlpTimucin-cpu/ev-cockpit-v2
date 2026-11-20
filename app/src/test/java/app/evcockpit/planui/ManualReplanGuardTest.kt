package app.evcockpit.planui

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.obd.SocReading
import org.junit.Assert.*
import org.junit.Test

class ManualReplanGuardTest {
    private val p=PlannerOptions()
    private val a=GeoPoint(38.0,35.0)
    private val b=GeoPoint(38.1,35.5)

    @Test fun missingOrStaleGpsCannotIssueNewRoute() {
        assertTrue(ManualReplanGuard.reason(null,b,true,5f,
            SocReading(60.0,"MANUEL"),p)!!.contains("GPS"))
        assertTrue(ManualReplanGuard.reason(a,b,false,5f,
            SocReading(60.0,"MANUEL"),p)!!.contains("GPS"))
        assertTrue(ManualReplanGuard.reason(a,b,true,120f,
            SocReading(60.0,"MANUEL"),p)!!.contains("GPS"))
    }
    @Test fun sourceAndTargetMustBeDistinct() {
        assertTrue(ManualReplanGuard.reason(a,a,true,10f,
            SocReading(60.0,"MANUEL"),p)!!.contains("Hedef"))
        assertTrue(ManualReplanGuard.reason(a,null,true,10f,
            SocReading(60.0,"MANUEL"),p)!!.contains("Hedef"))
    }
    @Test fun currentManualSocAllowedButDemoOrMissingRejected() {
        assertNull(ManualReplanGuard.reason(a,b,true,10f,
            SocReading(60.0,"MANUEL — araç göstergesi"),p))
        assertTrue(ManualReplanGuard.reason(a,b,true,10f,
            SocReading(null,"yok"),p)!!.contains("SoC"))
        assertTrue(ManualReplanGuard.reason(a,b,true,10f,
            SocReading(60.0,"DEMO — gerçek değil"),p)!!.contains("DEMO"))
    }
}
