package app.evcockpit.planner.model

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.solver.*
import app.evcockpit.planner.route.*
import org.junit.Assert.*
import org.junit.Test

class M8eChargingProfileTest {
    @Test fun conservativeDefaultIsUnverifiedAndDocumented() {
        val p=VehicleProfile.defaults()
        assertEquals(100.0,p.maxDcKw.value,0.0001)
        assertFalse(p.maxDcKw.verified)
        assertTrue(p.maxDcKw.source.startsWith("https://"))
    }
    @Test fun observedScreenValueScalesCurveAndChangesChargingTime() {
        val normal=VehicleProfile.defaults()
        val measured=normal.copy(observedDcKw=SourcedValue(65.0,"user display",false))
        assertEquals(65.0,measured.effectiveChargingCurve.points.maxOf{it.y},0.00001)
        assertFalse(measured.effectiveChargingCurve.verified)
        assertTrue(ChargingTimeCalculator().minutes(measured,150.0,10.0,80.0,25.0) >
            ChargingTimeCalculator().minutes(normal,150.0,10.0,80.0,25.0))
        assertTrue(measured.validate().isEmpty())
    }
    @Test fun selfReportNeverClaimsVerifiedAndRejectsUnsafePower() {
        val p=VehicleProfile.defaults()
        assertFalse(p.copy(observedDcKw=SourcedValue(200.0,"self",true)).validate().isEmpty())
        assertFalse(p.copy(observedDcKw=SourcedValue(0.0,"self",false)).validate().isEmpty())
    }
    @Test fun sixCandidatesPerBinAndConfigurableCount() {
        val list=(0 until 40).map { i ->
            ChargerCandidate("test"+i,"Station","TEST",0.0,0.0,
                SourcedValue(80.0,"fixture"),ChargerConfidence.OSM_TAGGED_CCS2,
                distanceAlongMeters=i*1000.0+100.0)
        }
        assertEquals(6,CandidatePruner().prune(list,100_000.0).size)
        val three=ModelParameters(pruneMaxPerWindow=SourcedValue(3.0,"test",false))
        assertEquals(3,CandidatePruner(three).prune(list,100_000.0).size)
    }
    @Test fun elapsedMeasurementUsesInjectableClockAndDoesNotLeakData() {
        val route=RouteProfile((0..10).map { i ->
            RoutePoint(0.0,0.0,i*1000.0,0.0,100.0)
        },false,"synthetic",0.0,0.0,0.1)
        val req=app.evcockpit.planner.model.PlanRequest(route,VehicleProfile.defaults(),
            emptyList(),80.0)
        var now=0L
        val measured=TimedPlanSolver(solve={ PlanSolver().solve(it) },
            nanos={ now+=5_000_000L;now }).execute(req)
        assertEquals(5L,measured.elapsedMs)
        assertFalse(measured.verifiedOnPhone)
    }
}
