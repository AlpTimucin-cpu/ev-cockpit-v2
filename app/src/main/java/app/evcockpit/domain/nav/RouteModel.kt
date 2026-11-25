package app.evcockpit.domain.nav

/** Harita motorundan bağımsız gerçek rotanın kaynağı için veri modeli. */
data class RouteModel(
    val id: String,
    val geometry: List<WgsPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuvers: List<RouteManeuver> = emptyList(),
    val segmentDistancesMeters: List<Double> = emptyList(),
    val segmentDurationsSeconds: List<Double> = emptyList(),
    val source: String,
)

data class RouteManeuver(
    val point: WgsPoint,
    val type: String,
    val modifier: String?,
    val roadName: String?,
    val distanceMeters: Double,
    val instructionTr: String? = null,
    val geometryIndex: Int? = null,
    val providerType: Int? = null,
)
