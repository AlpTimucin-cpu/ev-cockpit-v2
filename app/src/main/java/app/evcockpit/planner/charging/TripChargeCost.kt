package app.evcockpit.planner.charging

import app.evcockpit.planner.model.PlanResult
import java.util.Locale

/** A human-entered public tariff observation, NOT a live or verified feed. */
data class ManualChargeTariff(
    val priceTlPerKwh: Double,
    val observedAtEpochMs: Long,
    val source: String = "MANUEL — işletmeci/Şarj@TR üzerinden kontrol et",
    val verified: Boolean = false,
) {
    fun isValid(): Boolean =
        priceTlPerKwh.isFinite() && priceTlPerKwh > 0.0 &&
        priceTlPerKwh <= 150.0 && observedAtEpochMs > 0L &&
        !verified
}

data class PlannedChargeExpense(
    val chargerName: String,
    val operatorName: String,
    val batteryAddedKwh: Double,
    val billedKwhEstimate: Double,
    val tariff: ManualChargeTariff?,
    val costTl: Double?,
)

data class TripExpenseEstimate(
    val routeEnergyKwh: Double?,
    val totalBatteryAddedKwh: Double,
    val totalKnownCostTl: Double,
    val totalCostTl: Double?,
    val unknownPriceStops: Int,
    val stops: List<PlannedChargeExpense>,
) {
    val fullyPriced: Boolean get() = unknownPriceStops == 0
}

object TripChargeCost {
    const val BILLING_OVERHEAD = 0.0 // operator kWh is not assumed to be grid-wall energy
    const val MAX_TARIFF_AGE_DAYS = 14L

    fun normalizedOperator(name: String): String =
        PreferredNetworks.normalize(name).ifBlank { "operator bilinmiyor" }

    /**
     * The model's SoC delta is a battery-side approximation, not a meter reading.
     * No actual price can exist until a user supplies one for every operator.
     */
    fun calculate(
        plan: PlanResult.Success,
        usableCapacityAtSohKwh: Double,
        tariffs: Map<String, ManualChargeTariff>,
        routeEnergyKwh: Double? = null,
    ): TripExpenseEstimate {
        require(usableCapacityAtSohKwh.isFinite() && usableCapacityAtSohKwh > 0.0)
        val routeEnergy = routeEnergyKwh?.takeIf { it.isFinite() && it >= 0.0 }
        val stops = plan.stops.map { stop ->
            val deltaSoc = (stop.departureSocPct - stop.arrivalSocPct).coerceAtLeast(0)
            val chargedKwh = deltaSoc / 100.0 * usableCapacityAtSohKwh
            val billedKwh = chargedKwh * (1 + BILLING_OVERHEAD)
            val operator = normalizedOperator(stop.charger.operator)
            val tariff = tariffs[operator]?.takeIf { it.isValid() }
            PlannedChargeExpense(
                stop.charger.name, stop.charger.operator,
                chargedKwh, billedKwh, tariff,
                tariff?.let { billedKwh * it.priceTlPerKwh },
            )
        }
        val unknown = stops.count { it.costTl == null }
        val known = stops.sumOf { it.costTl ?: 0.0 }
        return TripExpenseEstimate(
            routeEnergy,
            stops.sumOf { it.batteryAddedKwh },
            known,
            known.takeIf { unknown == 0 },
            unknown,
            stops,
        )
    }

    fun isStale(tariff: ManualChargeTariff, nowMs: Long): Boolean =
        !tariff.isValid() || nowMs < tariff.observedAtEpochMs ||
            nowMs - tariff.observedAtEpochMs > MAX_TARIFF_AGE_DAYS * 86_400_000L

    fun formatTl(value: Double?): String =
        value?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            String.format(Locale.forLanguageTag("tr-TR"), "≈ %.2f TL • TAHMİN", it)
        } ?: "-- TL • FİYAT YOK"
}
