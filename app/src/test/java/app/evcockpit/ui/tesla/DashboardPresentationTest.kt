package app.evcockpit.ui.tesla

import app.evcockpit.location.SpeedReading
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RoutePosition
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.PlannedStop
import app.evcockpit.planner.model.SourcedValue
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class DashboardPresentationTest {
    @Test fun threePaneOnlyForUsableLandscape() {
        assertTrue(TeslaLayoutPolicy.useThreePanes(900,400))
        assertFalse(TeslaLayoutPolicy.useThreePanes(600,400))
        assertFalse(TeslaLayoutPolicy.useThreePanes(900,1100))
    }
    @Test fun sourceLabelsIncludeManualDemoAndMissing() {
        assertEquals(SourceKind.MANUEL,DashboardPresentation.source("MANUEL — araç ekranından"))
        assertEquals(SourceKind.OBD,DashboardPresentation.source("OBD — PID doğrulanmadı"))
        assertEquals(SourceKind.ESKI,DashboardPresentation.source("ESKİ — veri"))
        assertEquals(SourceKind.DEMO,DashboardPresentation.source("DEMO — gerçek değil"))
        assertEquals(SourceKind.YOK,DashboardPresentation.source("hata"))
    }
    @Test fun batteryEnergyUsesStoredUsableCapacityAtSoh() {
        val v=DashboardPresentation.vehicle(VehicleState(),SocReading(50.0,"MANUEL"),
            SpeedReading(null,"Hız verisi yok"),46.8)
        assertEquals(23.4,v.estimatedKwh!!,0.00001)
        assertNull(v.estimatedRangeKm)
        assertEquals(SourceKind.MANUEL,v.source)
        assertEquals("--",TeslaDisplayPolicy.speed(v.speedKmh))
    }
    @Test fun gpsSpeedAndManualBatteryHaveDifferentTruthfulSources() {
        val panel = DashboardPresentation.vehicle(
            VehicleState(),
            SocReading(50.0,"MANUEL — araç göstergesinden"),
            SpeedReading(18.0,"GPS — yedek"),
            52.0,
        )
        assertEquals(SourceKind.MANUEL,panel.source)
        assertEquals(SourceKind.GPS,panel.speedSource)
        assertEquals(18.0,panel.speedKmh!!,0.0)
    }

    @Test fun missingSocAndInvalidCapacityAreNotInvented() {
        val v=DashboardPresentation.vehicle(VehicleState(),SocReading(null,"YOK"),
            SpeedReading(null,"YOK"),0.0)
        assertNull(v.estimatedKwh)
        assertNull(v.socPercent)
        assertEquals(PanelStatus.EMPTY,v.status)
    }
    @Test fun demoNeverPrintsPowerAsReal() {
        val s=VehicleState(demo=true,hvVolt=400.0,hvAmp=60.0)
        val v=DashboardPresentation.vehicle(s,SocReading(55.0,"DEMO"),
            SpeedReading(90.0,"DEMO"),52.0)
        assertNull(v.powerKw)
        assertEquals(SourceKind.DEMO,v.source)
        assertTrue(v.message!!.contains("DEMO"))
    }
    private fun route()=NavigationRoute(
        geometry=listOf(GeoPoint(38.0,35.0),GeoPoint(39.0,36.0)),
        distanceMeters=100_000.0,durationSeconds=3600.0,maneuvers=emptyList())
    private fun stop(km:Int)=PlannedStop(
        ChargerCandidate("c$km","Sentetik","Örnek",38.0,35.0,
            SourcedValue(100.0,"SENTETİK",false),ChargerConfidence.UNKNOWN,
            distanceAlongMeters=km*1000.0),
        15,80,25.0,5.0,0.0)
    private fun plan()=PlanResult.Success(
        listOf(stop(30),stop(70)),20,25.0,10.0,
        100.0,60.0,50.0,10.0,10.0,120.0,130.0,
        warnings=listOf("TAHMİN"))
    @Test fun mapHeaderIsExplicitlyAnEstimate() {
        val sum=MapHeadlinePolicy.create(route(),true,
            RoutePosition(50_000.0,null,null),plan(),0L,TimeZone.getTimeZone("UTC"))
        assertTrue(sum.distance.contains("TAHMİN"))
        assertTrue(sum.eta.contains("00:30"))
        assertTrue(sum.arrivalSoc.contains("TAHMİN"))
    }
    @Test fun missingRouteHasNoFabricatedEtaOrSoc() {
        val sum=MapHeadlinePolicy.create(null,false,null,null,0L)
        assertEquals("ETA yok",sum.eta)
        assertEquals("SoC yok",sum.arrivalSoc)
    }
    @Test fun nextStopDoesNotShowPassedStationAsNext() {
        val next=MapHeadlinePolicy.nextStop(
            plan(),route(),RoutePosition(50_000.0,null,null))
        assertEquals("c70",next!!.charger.id)
        assertEquals("c30",MapHeadlinePolicy.nextStop(plan(),route(),null)!!.charger.id)
        assertNull(MapHeadlinePolicy.nextStop(null,route(),null))
    }
}
