package app.evcockpit.ui.unified.map

/**
 * Android ve harita motorundan bağımsız gerçek veriler.
 * Varsayılan boş: konum/rota/hava/istasyon asla kendiliğinden yaratılmaz.
 */
data class OverlayPoint(val latitude: Double, val longitude: Double) {
    fun valid(): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class OverlayStation(
    val id: String,
    val point: OverlayPoint,
    val powerKw: Double?,
    val dc: Boolean,
    val selected: Boolean = false,
)

data class OverlayWeatherAlert(
    val point: OverlayPoint,
    val rainExpected: Boolean?,
    val text: String? = null,
)

data class OverlayVehicle(
    val point: OverlayPoint,
    val bearingDegrees: Double? = null,
    val accuracyMeters: Double? = null,
    val timestampEpochMs: Long,
    val speedMetersPerSecond: Double? = null,
)

data class OverlayModel(
    val selectedRoute: List<OverlayPoint> = emptyList(),
    val alternativeRoutes: List<List<OverlayPoint>> = emptyList(),
    val weatherAlerts: List<OverlayWeatherAlert> = emptyList(),
    val stations: List<OverlayStation> = emptyList(),
    val origin: OverlayPoint? = null,
    val destination: OverlayPoint? = null,
    val vehicle: OverlayVehicle? = null,
    val zoomLevel: Double? = null,
) {
    val hasAnyGeometry: Boolean get() =
        selectedRoute.isNotEmpty() || alternativeRoutes.isNotEmpty() ||
            weatherAlerts.isNotEmpty() || stations.isNotEmpty() ||
            origin != null || destination != null || vehicle != null
}
