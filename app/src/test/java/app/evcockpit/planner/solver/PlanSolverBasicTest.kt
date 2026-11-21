package app.evcockpit.planner.solver

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.*
import app.evcockpit.planner.route.*
import org.junit.Assert.*
import org.junit.Test

class PlanSolverBasicTest {
    private val p=VehicleProfile.defaults()
    private fun route(distanceKm:Int,speed:Double=100.0):RouteProfile {
        val pts=(0..distanceKm/10).map {
            RoutePoint(38.0,35.0,it*10_000.0,0.0,speed)
        }
        return RouteProfile(pts,false,"EĞİM VERİSİ YOK",0.0,0.0,distanceKm/speed)
    }
    private fun c(atKm:Double)=ChargerCandidate("c"+atKm,"Test","OSM",
        38.0,35.0,SourcedValue(150.0,"SENTETİK",false),
        ChargerConfidence.OSM_TAGGED_CCS2,distanceAlongMeters=atKm*1000)
    @Test fun noStopIfRangeIsEnough() {
        val r=PlanSolver().solve(PlanRequest(route(60),p,emptyList(),70.0))
        assertTrue(r is PlanResult.Success)
        assertEquals(0,(r as PlanResult.Success).stops.size)
    }
    @Test fun clearFailureAndNearestReachableWithoutStations() {
        val r=PlanSolver().solve(PlanRequest(route(400),p,emptyList(),30.0))
        assertTrue(r is PlanResult.Failure)
        val fail=r as PlanResult.Failure
        assertTrue(fail.reason.contains("Menzil yetersiz"))
        assertTrue(fail.nearestReachableKm>=0.0)
    }
    @Test fun chargingStopMaintainsMinimumSoc() {
        val r=PlanSolver().solve(PlanRequest(route(300),p,
            listOf(c(90.0),c(180.0),c(250.0)),50.0))
        assertTrue("Expected plan with charge",r is PlanResult.Success)
        val ok=r as PlanResult.Success
        assertTrue(ok.stops.isNotEmpty())
        assertTrue(ok.arrivalSocPct>=10)
        assertTrue(ok.stops.all {it.arrivalSocPct>=10 && it.departureSocPct>it.arrivalSocPct})
        assertFalse(ok.verified)
    }
    @Test fun sameInputsGiveIdenticalOutput() {
        val req=PlanRequest(route(300),p,listOf(c(90.0),c(180.0)),50.0)
        assertEquals(PlanSolver().solve(req),PlanSolver().solve(req))
    }
    @Test fun preferredNetworkBonusChangesSolverRankingNotPhysicalEstimate() {
        val alternate=c(90.0).copy(id="nonpref",operator="Diğer")
        val favored=c(90.0).copy(id="preferred",operator="ZES")
        val next=c(180.0).copy(id="next",operator="Diğer")
        val req=PlanRequest(route(300),p,listOf(alternate,favored,next),50.0,
            preferredNetworks=setOf("zes"),preferenceBonusMinutes=4.0)
        val result=PlanSolver().solve(req)
        assertTrue(result is PlanResult.Success)
        val chosen=result as PlanResult.Success
        assertTrue(chosen.stops.any { it.charger.id=="preferred" })
        assertFalse(chosen.verified)
        assertTrue(chosen.physicalMinutes>0.0)
    }
    @Test fun invalidPreferenceBonusCannotChangeSafetyConstraints() {
        val req=PlanRequest(route(300),p,listOf(c(90.0)),50.0,
            preferenceBonusMinutes=500.0)
        assertThrows(IllegalArgumentException::class.java) { PlanSolver().solve(req) }
    }
    @Test fun requiresRealOrManualSoc() {
        val r=PlanSolver().solve(PlanRequest(route(200),p,emptyList(),null))
        assertTrue(r is PlanResult.Failure)
        assertTrue((r as PlanResult.Failure).reason.contains("manuel SoC"))
    }
}
