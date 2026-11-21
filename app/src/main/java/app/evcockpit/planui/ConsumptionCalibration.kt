package app.evcockpit.planui

import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.solver.RouteEnergyPrefix
import kotlin.math.abs

/**
 * Model comparison, not a BMS calibration or measurement certification.
 * No coordinates, VIN or charger identifiers are retained in the evidence.
 */
data class CalibrationSuggestion(
    val modelKwhPer100Km: Double,
    val observedKwhPer100Km: Double,
    val plannedArrivalSoc: Double,
    val observedArrivalSoc: Double,
    val suggestedConsumptionAdjust: Double,
    val relativeConsumptionDifferencePercent: Double,
    val arrivalSocDifferencePoints: Double,
    val confidenceNote: String,
    val verified: Boolean = false,
)

object ConsumptionCalibration {
    fun modelKwhPer100Km(req: PlanRequest): Double? = runCatching {
        val prefix = RouteEnergyPrefix(req.route, req.car, req.temperatureC, req.hvac)
        val routeKm = prefix.totalMeters / 1000.0
        if (!routeKm.isFinite() || routeKm <= 0.0) return@runCatching null
        val wh = prefix.totalKwh / routeKm * 100.0
        wh.takeIf { it.isFinite() && it > 0.0 }
    }.getOrNull()

    fun suggest(
        modelKwhPer100Km: Double,
        observedKwhPer100Km: Double,
        plannedArrivalSoc: Double,
        observedArrivalSoc: Double,
        existingAdjustment: Double,
    ): CalibrationSuggestion {
        require(modelKwhPer100Km.isFinite() && modelKwhPer100Km in 1.0..100.0)
        require(observedKwhPer100Km.isFinite() && observedKwhPer100Km in 1.0..100.0)
        require(plannedArrivalSoc.isFinite() && plannedArrivalSoc in 0.0..100.0)
        require(observedArrivalSoc.isFinite() && observedArrivalSoc in 0.0..100.0)
        require(existingAdjustment.isFinite() && existingAdjustment in -0.30..0.30)
        val diff = (observedKwhPer100Km / modelKwhPer100Km - 1.0) * 100.0
        // Approximate incremental correction; auxiliary/HVAC model may not scale linearly.
        val suggested = (existingAdjustment + diff / 100.0).coerceIn(-0.30, 0.30)
        val socDiff = observedArrivalSoc - plannedArrivalSoc
        val note = if (abs(socDiff) >= 5.0) {
            "SoC farkı 5 puan veya üstünde; rota/hava/şarj/OBD kaynaklarını incele. " +
                "Önerilen düzeltme yalnız yaklaşık model tavsiyesidir."
        } else "SoC farkı 5 puanın altında; bu tek başına model doğrulaması değildir."
        return CalibrationSuggestion(
            modelKwhPer100Km, observedKwhPer100Km, plannedArrivalSoc,
            observedArrivalSoc, suggested, diff, socDiff,
            "$note TAHMİN / DOĞRULANMADI", false,
        )
    }
}
