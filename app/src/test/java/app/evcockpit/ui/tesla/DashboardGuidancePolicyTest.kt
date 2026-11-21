package app.evcockpit.ui.tesla

import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.model.PlanResult
import org.junit.Assert.*
import org.junit.Test

class DashboardGuidancePolicyTest {
    private val gps = GpsSnapshot(point=GeoPoint(38.0,35.0),
        receivedElapsedRealtimeMs=1000L,active=true)
    private val route = NavigationRoute(
        geometry=listOf(GeoPoint(38.0,35.0),GeoPoint(39.0,36.0)),
        distanceMeters=100_000.0,durationSeconds=3600.0,
        maneuvers=emptyList())
    private val vehicle = VehicleState(connected=true,socPct=60.0)
    private val soc = SocReading(60.0,"OBD — PID DOĞRULANMADI")

    @Test fun noChargerPlanShowsHonestNearestReachableAndAction() {
        val issue=DashboardGuidancePolicy.evaluate(
            vehicle,soc,gps,true,RouteUiState(route=route),
            PlanResult.Failure("Menzil yetersiz ve uygun şarj adayı yok",45.7))
        assertEquals(GuidanceKind.NO_CHARGER,issue.kind)
        assertTrue(issue.title.contains("durak bulunamadı"))
        assertTrue(issue.detail.contains("~45.7 km"))
        assertTrue(issue.detail.contains("TAHMİN"))
        assertEquals(GuidanceAction.PLAN,issue.action)
    }

    @Test fun routeKeyFailureOpensSettingsWithoutLeakingSecret() {
        val issue=DashboardGuidancePolicy.evaluate(
            vehicle,soc,gps,true,
            RouteUiState(message="HeiGIT API anahtarı yok. Ayarlar'dan gir."),null)
        assertEquals(GuidanceKind.ROUTE_KEY,issue.kind)
        assertEquals(GuidanceAction.SETTINGS,issue.action)
        assertFalse(issue.detail.contains("sk-test-secret"))
    }

    @Test fun gpsMissingOffersActualPermissionAction() {
        val issue=DashboardGuidancePolicy.evaluate(
            vehicle,soc,GpsSnapshot(),false,RouteUiState(),null)
        assertEquals(GuidanceKind.GPS_MISSING,issue.kind)
        assertEquals(GuidanceAction.LOCATION_PERMISSION,issue.action)
        assertTrue(issue.title.contains("Konum alınamıyor"))
    }

    @Test fun staleObdOffersRefreshWithoutFakingBatteryValue() {
        val issue=DashboardGuidancePolicy.evaluate(
            vehicle,SocReading(null,"ESKİ — OBD SoC 3 saniyeyi aştı"),
            gps,true,RouteUiState(),null)
        assertEquals(GuidanceKind.STALE,issue.kind)
        assertEquals(GuidanceAction.OBD_TEST,issue.action)
        assertTrue(issue.title.contains("ESKİ"))
    }

    @Test fun noObdManualFallbackUsesExistingSettingsTab() {
        val issue=DashboardGuidancePolicy.evaluate(
            VehicleState(),SocReading(null,"SoC verisi yok"),gps,true,
            RouteUiState(),null)
        assertEquals(GuidanceKind.NO_OBD,issue.kind)
        assertEquals(GuidanceAction.SETTINGS,issue.action)
        assertTrue(issue.actionText.contains("Manuel SoC"))
    }

    @Test fun loadingAndMissingRouteAreSeparate() {
        val loading=DashboardGuidancePolicy.evaluate(
            vehicle,soc,gps,true,RouteUiState(busy=true),null)
        assertEquals(GuidanceKind.ROUTE_LOADING,loading.kind)
        assertEquals(GuidanceAction.NONE,loading.action)
        val empty=DashboardGuidancePolicy.evaluate(
            vehicle,soc,gps,true,RouteUiState(),null)
        assertEquals(GuidanceKind.NO_ROUTE,empty.kind)
        assertEquals(GuidanceAction.ROUTE,empty.action)
    }

    @Test fun completeInputsRequireNoErrorCard() {
        val normal=DashboardGuidancePolicy.evaluate(
            vehicle,soc,gps,true,RouteUiState(route=route),null)
        assertEquals(GuidanceKind.NONE,normal.kind)
    }
}
