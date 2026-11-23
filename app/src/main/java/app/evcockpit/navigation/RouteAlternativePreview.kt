package app.evcockpit.navigation

import app.evcockpit.planner.energy.DrivingSegment
import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.energy.PhysicsConsumptionModel
import app.evcockpit.planner.model.VehicleProfile

/**
 * B aşamasının şarj istasyonu bağımsız ön hesabı.
 * Gerçek güzergâh mesafesi/süresi kullanılır; hava ve yükselti D aşamasında eklenecek.
 * Tek bir düz-yol/20°C/otomatik klima yaklaşımıdır, araçtan ölçülmüş değildir.
 */
data class AlternativeEnergyPreview(
    val routeEnergyKwh: Double,
    val averageKwhPer100Km: Double,
    val startSocPct: Double?,
    val arrivalSocPct: Double?,
    val energyDeficitKwh: Double?,
    val extraKwhForMinimum: Double?,
    val requiresCharging: Boolean?,
    val chargingMinutes: Double?,
    val chargingCostTl: Double?,
    val note: String = "FİZİK ÖN TAHMİNİ • 20°C, düz yol; trafik/eğim/rüzgâr yok",
)

object RouteAlternativePreview {
    fun calculate(
        route: NavigationRoute,
        vehicle: VehicleProfile,
        startSocPct: Double?,
        minArrivalSocPct: Double = 15.0,
    ): AlternativeEnergyPreview? {
        if (vehicle.validate().isNotEmpty() ||
            !route.distanceMeters.isFinite() || route.distanceMeters <= 0.0 ||
            !route.durationSeconds.isFinite() || route.durationSeconds <= 0.0 ||
            !minArrivalSocPct.isFinite() || minArrivalSocPct !in 0.0..100.0)
            return null
        val averageSpeed = route.distanceMeters / route.durationSeconds * 3.6
        if (averageSpeed !in 5.0..160.0) return null
        val power = runCatching {
            PhysicsConsumptionModel().compute(
                DrivingSegment(
                    lengthMeters = route.distanceMeters,
                    speedKmh = averageSpeed,
                    slopeRadians = 0.0,
                    temperatureC = 20.0,
                ), vehicle, HvacMode.AUTO,
            ).kwh
        }.getOrNull() ?: return null
        if (!power.isFinite() || power < 0.0) return null
        val capacity = vehicle.capacityAtSohKwh
        if (!capacity.isFinite() || capacity <= 0.0) return null
        val soc = startSocPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val rawArrival = soc?.minus(power / capacity * 100.0)
        val needsCharge = rawArrival?.let { it < minArrivalSocPct }
        val extra = rawArrival?.let {
            (minArrivalSocPct - it).coerceAtLeast(0.0) / 100.0 * capacity
        }
        return AlternativeEnergyPreview(
            routeEnergyKwh = power,
            averageKwhPer100Km = power * 100_000.0 / route.distanceMeters,
            startSocPct = soc,
            arrivalSocPct = rawArrival?.takeIf { it >= 0.0 },
            energyDeficitKwh = rawArrival?.takeIf { it < 0.0 }?.let {
                -it / 100.0 * capacity
            },
            extraKwhForMinimum = extra,
            requiresCharging = needsCharge,
            chargingMinutes = if (needsCharge == false) 0.0 else null,
            chargingCostTl = if (needsCharge == false) 0.0 else null,
        )
    }
}
