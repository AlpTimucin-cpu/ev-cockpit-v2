package app.evcockpit.planui

import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.PlannedStop
import app.evcockpit.planner.model.SourcedValue
import app.evcockpit.planner.charging.*
import org.junit.Assert.*
import org.junit.Test

class PlannerDisplayTest {
    private fun sample()=PlanResult.Success(
        stops=emptyList(),arrivalSocPct=21,optimisticArrivalSocPct=29.0,
        pessimisticArrivalSocPct=13.0,distanceKm=140.0,drivingMinutes=90.0,
        chargingMinutes=0.0,stopAndDetourMinutes=0.0,riskPenaltyMinutes=0.0,
        physicalMinutes=90.0,riskAdjustedMinutes=90.0,warnings=emptyList())
    @Test fun uncertaintyAndEstimateAlwaysExplicit() {
        assertTrue(PlannerDisplay.uncertainty(sample()).contains("TAHMİN"))
        assertTrue(PlannerDisplay.uncertainty(sample()).contains("kötümser"))
        assertTrue(PlannerDisplay.overview(sample()).contains("TAHMİN"))
    }
    @Test fun headlineUsesUnverifiedModelCapacityNotDeviceData() {
        val plan=sample()
        assertEquals(10.92,PlannerDisplay.arrivalKwh(plan,52.0),1e-9)
        assertEquals("90 dk",PlannerDisplay.totalTimeLabel(plan))
        assertEquals("0 dk",PlannerDisplay.chargingTimeLabel(plan))
        assertThrows(IllegalArgumentException::class.java) {
            PlannerDisplay.arrivalKwh(plan,Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlannerDisplay.arrivalKwh(plan,0.0)
        }
    }
    @Test fun stopLabelStatesOccupancyUnknownAndNoCoordinates() {
        val c=ChargerCandidate("abc","TEST","TEST",38.76543,35.12345,
            SourcedValue(100.0,"TEST",false),ChargerConfidence.UNKNOWN)
        val s=PlannedStop(c,15,80,30.0,4.0,25.0)
        val message=PlannerDisplay.stop(s)
        assertTrue(message.contains("doluluk bilinmiyor"))
        assertFalse(message.contains("38.76543"))
        assertFalse(message.contains("35.12345"))
    }
}
