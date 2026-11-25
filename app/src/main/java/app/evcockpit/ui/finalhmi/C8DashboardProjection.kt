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
        // Real dashboard must not promote unverified OBD values to verified telemetry.
        val manual = manualSoc?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val preview = route?.let { RouteAlternativePreview.calculate(it, profile, manual) }
        return base.copy(
            speedKmh = stamp(speed, DashboardOrigin.GPS),
            socPercent = stamp(manual, DashboardOrigin.MANUAL),
            estimatedRangeKm = stamp(DashboardMath.estimatedRangeKm(manual,
                profile.capacityAtSohKwh, preview?.averageKwhPer100Km), DashboardOrigin.MODEL),
            consumptionKwhPer100Km = stamp(preview?.averageKwhPer100Km, DashboardOrigin.MODEL),
            // Unverified PID power is deliberately not shown as calibrated data.
            powerKw = StampedValue(),
            arrivalSocPercent = StampedValue(),
        )
    }
}
