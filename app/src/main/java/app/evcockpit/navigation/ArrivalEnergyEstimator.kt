package app.evcockpit.navigation

object ArrivalEnergyEstimator {
    /**
     * Tahmin kWh = usableKWh * SoC/100 - remainingKm * averageConsumptionKWhPer100Km/100.
     * Returns null rather than fabricating missing/stale inputs.
     */
    fun estimate(
        usableKwh: Double,
        socPct: Double?,
        remainingKm: Double?,
        averageConsumptionKwhPer100Km: Double?,
    ): Double? {
        if (!usableKwh.isFinite() || usableKwh <= 0.0) return null
        val soc = socPct?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: return null
        val km = remainingKm?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
        val average = averageConsumptionKwhPer100Km
            ?.takeIf { it.isFinite() && it > 0.0 && it <= 80.0 } ?: return null
        return usableKwh * soc / 100.0 - km * average / 100.0
    }
}
