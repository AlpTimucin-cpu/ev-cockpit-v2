package app.evcockpit.ui.finalhmi

import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RouteAlternativePreview
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.ui.tesla.DriveModeSnapshot
import app.evcockpit.ui.unified.C6NavigationPolicy

/** C8: bind only available real data; community PID scales remain unverified. */
object C8DashboardProjection {
    fun map(
        base: DashboardUiState, gps: GpsSnapshot, vehicle: VehicleState,
        drive: DriveModeSnapshot, manualSoc: Double?, route: NavigationRoute?,
        profile: VehicleProfile, elapsed: Long,
    ): DashboardUiState {
        if (base.demo) return base
        val now = System.currentTimeMillis()
        fun <T> stamp(value: T?, source: DashboardOrigin): StampedValue<T> =
            if (value == null) StampedValue() else StampedValue(value, source, now)
        val speed = gps.speedKmh?.takeIf {
            C6NavigationPolicy.validFix(gps, elapsed) && it.isFinite() && it in 0.0..300.0
        }
        val manual = manualSoc?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val obdSoc = vehicle.socPct?.takeIf { vehicle.connected && !vehicle.demo &&
            drive.obdSocFresh && it.isFinite() && it in 0.0..100.0 }
        val obdSpeed = vehicle.speedKmh?.takeIf { vehicle.connected && !vehicle.demo &&
            drive.obdSpeedFresh && it.isFinite() && it in 0.0..300.0 }
        val obdPower = vehicle.powerKw?.takeIf { vehicle.connected && !vehicle.demo &&
            drive.obdPowerFresh && it.isFinite() }
        val soc = obdSoc ?: manual
        val preview = route?.let { RouteAlternativePreview.calculate(it, profile, soc) }
        return base.copy(
            speedKmh = stamp(speed ?: obdSpeed, if (speed != null) DashboardOrigin.GPS else DashboardOrigin.OBD_UNVERIFIED),
            socPercent = stamp(soc, if (obdSoc != null) DashboardOrigin.OBD_UNVERIFIED else DashboardOrigin.MANUAL),
            estimatedRangeKm = stamp(DashboardMath.estimatedRangeKm(soc,
                profile.capacityAtSohKwh, preview?.averageKwhPer100Km), DashboardOrigin.MODEL),
            consumptionKwhPer100Km = stamp(preview?.averageKwhPer100Km, DashboardOrigin.MODEL),
            powerKw = stamp(obdPower, DashboardOrigin.OBD_UNVERIFIED),
            arrivalSocPercent = StampedValue(),
        )
    }
}
