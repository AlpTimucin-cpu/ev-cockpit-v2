package app.evcockpit.ui.tesla

import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.obd.VehicleState
import org.junit.Assert.*
import org.junit.Test

class DriveModePolicyTest {
    @Test fun thresholdIsStrictlyGreaterThanFiveForGps() {
        val atFive=GpsSnapshot(point=GeoPoint(38.0,35.0),speedKmh=5.0,
            receivedElapsedRealtimeMs=1000L)
        val aboveFive=atFive.copy(speedKmh=5.01)
        assertEquals(DriveMode.PARKED,
            DriveModePolicy.evaluate(VehicleState(),atFive,1500L).mode)
        assertEquals(DriveMode.DRIVING,
            DriveModePolicy.evaluate(VehicleState(),aboveFive,1500L).mode)
    }
    @Test fun anyFreshPositiveObdSpeedMeansDriving() {
        val vehicle=VehicleState(connected=true,speedKmh=0.1,
            speedUpdatedElapsedMs=1000L)
        assertEquals(DriveMode.DRIVING,
            DriveModePolicy.evaluate(vehicle,GpsSnapshot(),2000L).mode)
        assertEquals(DriveMode.UNKNOWN,
            DriveModePolicy.evaluate(vehicle,GpsSnapshot(),4001L).mode)
    }
    @Test fun threeSecondFreshnessBoundaryIsExact() {
        assertTrue(DriveModePolicy.fresh(1000,4000))
        assertFalse(DriveModePolicy.fresh(1000,4001))
        assertFalse(DriveModePolicy.fresh(0,1000))
        assertFalse(DriveModePolicy.fresh(1000,999))
    }
    @Test fun demoSpeedIsNeverTreatedAsRealObd() {
        val demo=VehicleState(connected=true,demo=true,speedKmh=110.0,
            speedUpdatedElapsedMs=1900L)
        assertEquals(DriveMode.UNKNOWN,
            DriveModePolicy.evaluate(demo,GpsSnapshot(),2000L).mode)
    }
    @Test fun powerRequiresIndependentFreshVoltageAndCurrent() {
        val oneFresh=VehicleState(connected=true,hvVolt=400.0,hvAmp=100.0,
            voltageUpdatedElapsedMs=1000L,currentUpdatedElapsedMs=4000L)
        assertFalse(DriveModePolicy.evaluate(oneFresh,GpsSnapshot(),4001L).obdPowerFresh)
        val both=oneFresh.copy(voltageUpdatedElapsedMs=4000L)
        assertTrue(DriveModePolicy.evaluate(both,GpsSnapshot(),4001L).obdPowerFresh)
    }
    @Test fun manualSocWinsOverStaleObdAndDemo() {
        val stale=VehicleState(connected=true,socPct=80.0,
            socUpdatedElapsedMs=1000L)
        val snapshot=DriveModePolicy.evaluate(stale,GpsSnapshot(),4001L)
        val result=SafeSocSelection.select(stale,44.0,snapshot)
        assertEquals(44.0,result.pct!!,0.0)
        assertTrue(result.source.startsWith("MANUEL"))
        val demo=VehicleState(connected=true,demo=true,socPct=75.0)
        assertTrue(SafeSocSelection.select(demo,44.0,DriveModeSnapshot())
            .source.startsWith("MANUEL"))
    }
    @Test fun freshObdHasFirstPriorityButStaleIsMarked() {
        val live=VehicleState(connected=true,socPct=55.0,socUpdatedElapsedMs=2000L)
        val fresh=DriveModePolicy.evaluate(live,GpsSnapshot(),2500L)
        assertTrue(SafeSocSelection.select(live,45.0,fresh).source.startsWith("OBD"))
        val stale=DriveModePolicy.evaluate(live,GpsSnapshot(),6000L)
        val value=SafeSocSelection.select(live,null,stale)
        assertNull(value.pct)
        assertTrue(value.source.startsWith("ESKİ"))
    }
    @Test fun unknownModeFailsClosedForPhoneVideo() {
        assertFalse(DriveModePolicy.allowedTab(4,DriveMode.UNKNOWN))
        assertFalse(DriveModePolicy.allowedTab(4,DriveMode.DRIVING))
        assertTrue(DriveModePolicy.allowedTab(4,DriveMode.PARKED))
        assertFalse(DriveModePolicy.allowedTab(3,DriveMode.DRIVING))
        assertFalse(DriveModePolicy.allowedTab(5,DriveMode.DRIVING))
        assertTrue(DriveModePolicy.allowedTab(2,DriveMode.DRIVING))
        assertFalse(DriveModePolicy.allowedAction(
            TeslaAction.VIDEO,DriveMode.DRIVING,false))
        assertTrue(DriveModePolicy.allowedAction(
            TeslaAction.PLAN,DriveMode.PARKED,false))
        assertFalse(DriveModePolicy.allowedAction(
            TeslaAction.MOLA,DriveMode.PARKED,false))
    }
}
