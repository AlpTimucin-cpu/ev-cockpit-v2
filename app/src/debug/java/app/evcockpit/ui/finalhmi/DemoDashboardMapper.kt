package app.evcockpit.ui.finalhmi

import app.evcockpit.planner.model.VehicleProfile

/** Debug kaynak kümesinde. Release APK içinde bu örnek veriler derlenmez. */
/**
 * YALNIZCA ayrı önizleme moduna ait örnekler.
 * RealDashboardMapper bunu çağırmaz, ortak bir depo/OBD durumunu değiştirmez.
 */
object DemoDashboardMapper {
    fun map(nowEpochMs: Long): DashboardUiState {
        fun <T> demo(value: T): StampedValue<T> =
            StampedValue(value, DashboardOrigin.DEMO, nowEpochMs)
        // Demo bile mevcut tek araç profilinin varsayılanını kullanır, kapasiteyi sabitlemez.
        val capacity = VehicleProfile.defaults().capacityAtSohKwh
        val soc = 74.0
        val consumption = 17.2
        return DashboardUiState(
            demo = true,
            clock = StampedValue(DashboardFormat.time(nowEpochMs),
                DashboardOrigin.SYSTEM, nowEpochMs),
            outdoorTemperatureC = demo(18.0),
            speedKmh = demo(83.0),
            socPercent = demo(soc),
            estimatedRangeKm = DashboardMath.estimatedRangeKm(soc, capacity, consumption)
                ?.let(::demo) ?: StampedValue(),
            consumptionKwhPer100Km = demo(consumption),
            powerKw = demo(21.0),
            arrivalMinutes = demo(88.0),
            remainingDistanceKm = demo(110.0),
            arrivalSocPercent = demo(31.0),
            chargeTotalLabel = demo("en az 124,0 TL (1 durakta fiyat yok)"),
            weatherSamples = listOf(
                RouteWeatherSample(25.0, demo(17.0), demo(false)),
                RouteWeatherSample(50.0, demo(15.0), demo(true)),
                RouteWeatherSample(75.0, demo(13.0), demo(false)),
            ),
            nextCharge = NextChargeUiState(
                stationName = demo("Örnek DC istasyonu"),
                distanceKm = demo(48.0),
                chargingMinutes = demo(24.0),
                targetSocPercent = demo(75.0),
                costLabel = demo("124,0 TL (örnek, kısmi)"),
            ),
        )
    }
}
