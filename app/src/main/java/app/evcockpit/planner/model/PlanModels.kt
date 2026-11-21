package app.evcockpit.planner.model

import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.route.RouteProfile

data class PlanRequest(
    val route: RouteProfile,
    val car: VehicleProfile,
    val candidates: List<ChargerCandidate>,
    val startSocPct: Double?,
    val socSource: String = "MANUEL — DOĞRULANMADI",
    val temperatureC: Double = 20.0,
    val hvac: HvacMode = HvacMode.AUTO,
    val minArrivalSoc: Double = 10.0,
    val minStopSoc: Double = 10.0,
    val maxChargeSoc: Double = 80.0,
    val maxStops: Int = 8,
    val allowSlowCharging: Boolean = false,
    val onlyOsmTaggedCcs2: Boolean = false,
    val stationDataNote: String = "OSM veri kaynağı — güncellik DOĞRULANMADI",
    val preferredNetworks: Set<String> = emptySet(),
    val preferenceBonusMinutes: Double = 2.0,
)
data class PlannedStop(
    val charger: ChargerCandidate,
    val arrivalSocPct: Int,
    val departureSocPct: Int,
    val chargeMinutes: Double,
    val extraMinutes: Double,
    val riskPenaltyMinutes: Double,
    val source: String = "TAHMİN — doluluk bilinmiyor",
)
sealed interface PlanResult {
    data class Success(
        val stops: List<PlannedStop>,
        val arrivalSocPct: Int,
        val optimisticArrivalSocPct: Double,
        val pessimisticArrivalSocPct: Double,
        val distanceKm: Double,
        val drivingMinutes: Double,
        val chargingMinutes: Double,
        val stopAndDetourMinutes: Double,
        val riskPenaltyMinutes: Double,
        val physicalMinutes: Double,
        val riskAdjustedMinutes: Double,
        val warnings: List<String>,
        val verified: Boolean = false,
    ): PlanResult
    data class Failure(val reason: String, val nearestReachableKm: Double): PlanResult
}
