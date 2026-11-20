package app.evcockpit.planner.solver

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.OrsViaRequest
import app.evcockpit.planner.model.*
import app.evcockpit.planner.route.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RouteRevalidationTest {
    private val car=VehicleProfile.defaults()
    private fun route(km:Int): RouteProfile = RouteProfile(
        (0..km/10).map { i -> RoutePoint(0.0,i*0.001,i*10_000.0,0.0,90.0) },
        false,"Sentetik",0.0,0.0,km/90.0)
    private fun input(): Pair<PlanRequest, PlanResult.Success> {
        val req=PlanRequest(route(100),car,emptyList(),95.0)
        return req to (PlanSolver().solve(req) as PlanResult.Success)
    }
    @Test fun minorDifferenceOneQueryAndNoReplan() = runBlocking {
        val (req,original)=input()
        var count=0
        val verifier=PlanRevalidator(WaypointRouteClient { _ ->
            count++;route(101).copy(points=(0..100).map { i ->
                RoutePoint(0.0,i*0.00101,i*1010.0,0.0,90.0)
            })
        })
        val checked=verifier.check(req,original)
        assertEquals(1,count)
        assertFalse(checked.replannedOnce)
        assertTrue((checked.firstDifferencePercent ?: 100.0)<2.0)
        assertFalse(checked.verifiedOnVehicle)
    }
    @Test fun largeDifferenceOneReplanMaxTwoViaRequests() = runBlocking {
        val (req,original)=input()
        var count=0
        val verifier=PlanRevalidator(WaypointRouteClient { _ ->
            count++
            if(count==1)route(110) else route(111).copy(
                points=(0..111).map { i ->
                    RoutePoint(0.0,i*0.001,i*1000.0,0.0,90.0)
                })
        })
        val checked=verifier.check(req,original)
        assertEquals(2,count)
        assertTrue(checked.replannedOnce)
        assertNotNull(checked.firstDifferencePercent)
        assertNotNull(checked.finalDifferencePercent)
        assertFalse(checked.verifiedOnVehicle)
    }
    @Test fun secondDiscrepancyRemainsVisibleNotSilentlyAccepted() = runBlocking {
        val (req,original)=input()
        var count=0
        val verifier=PlanRevalidator(WaypointRouteClient { _ ->
            count++;if(count==1)route(110) else route(140)
        })
        val checked=verifier.check(req,original)
        assertEquals(2,count)
        assertTrue((checked.finalDifferencePercent ?: 0.0)>2.0)
        assertTrue(checked.warning.contains("DOĞRULANMADI"))
    }
    @Test fun networkFailureCannotBeReportedAsSuccess() = runBlocking {
        val (req,original)=input()
        val checked=PlanRevalidator(WaypointRouteClient { _ ->
            throw IllegalStateException("secret=NEVER_LEAK")
        }).check(req,original)
        assertNull(checked.finalDifferencePercent)
        assertFalse(checked.warning.contains("NEVER_LEAK"))
        assertTrue(checked.warning.contains("DOĞRULANMADI"))
    }
    @Test fun orsViaJsonHasOnlyCoordinatesAndNoApiKey() {
        val points=listOf(GeoPoint(38.7,35.5),GeoPoint(39.0,36.0),GeoPoint(40.0,29.0))
        val raw=OrsViaRequest.build(points)
        val json=JSONObject(raw)
        assertEquals(3,json.getJSONArray("coordinates").length())
        assertEquals(35.5,json.getJSONArray("coordinates").getJSONArray(0).getDouble(0),1e-6)
        assertEquals("tr",json.getString("language"))
        assertFalse(raw.contains("Authorization",ignoreCase=true))
        assertThrows(IllegalArgumentException::class.java) {
            OrsViaRequest.build(List(51){GeoPoint(0.0,it.toDouble())})
        }
    }

    @Test fun cancelledRouteRequestPropagatesToViewModel() = runBlocking {
        val (req,original)=input()
        var caught=false
        try {
            PlanRevalidator(WaypointRouteClient { _ ->
                throw kotlinx.coroutines.CancellationException("cancel selected check")
            }).check(req,original)
        } catch(_:kotlinx.coroutines.CancellationException) {
            caught=true
        }
        assertTrue("Cancellation must not be converted into a completed verification",caught)
    }

    @Test fun failureResponseRemainsUnverifiedAndDoesNotEchoSecret() = runBlocking {
        val (req,original)=input()
        val outcome=PlanRevalidator(WaypointRouteClient { _ ->
            throw IllegalStateException("MY_API_KEY_DO_NOT_ECHO")
        }).check(req,original)
        assertFalse(outcome.verifiedOnVehicle)
        assertNull(outcome.finalDifferencePercent)
        assertTrue(outcome.warning.contains("DOĞRULANMADI"))
        assertFalse(outcome.warning.contains("MY_API_KEY_DO_NOT_ECHO"))
    }

}
