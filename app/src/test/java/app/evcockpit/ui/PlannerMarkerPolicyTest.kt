package app.evcockpit.ui

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.SourcedValue
import org.junit.Assert.*
import org.junit.Test

class PlannerMarkerPolicyTest {
    private fun item(confidence:ChargerConfidence)=ChargerCandidate(
        "test","Sentetik istasyon","TEST",0.0,0.0,SourcedValue(50.0,"TEST",false),confidence)
    @Test fun allMarkersRejectRealWorldOperationalCertainty() {
        ChargerConfidence.values().forEach {
            assertTrue(PlannerMarkerPolicy.label(item(it)).contains("DOĞRULANMADI"))
        }
    }
    @Test fun confidenceColorsAreDistinctAndDoNotIndicateOccupancy() {
        val colors=ChargerConfidence.values().map{PlannerMarkerPolicy.color(item(it))}
        assertEquals(3,colors.distinct().size)
        assertFalse(PlannerMarkerPolicy.label(item(ChargerConfidence.UNKNOWN))
            .contains("doluluk açık"))
    }
}
