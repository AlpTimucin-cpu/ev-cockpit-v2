package app.evcockpit.navigation

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Enlem geçersiz" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Boylam geçersiz" }
    }
}

data class RouteManeuver(
    val instructionTr: String,
    val geometryIndex: Int,
    val type: Int,
)

data class NavigationRoute(
    val geometry: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuvers: List<RouteManeuver>,
    val provider: String = "openrouteservice",
)

/** API key is user-supplied at runtime; never a build-time constant. */
interface RouteProvider {
    suspend fun calculate(start: GeoPoint, destination: GeoPoint, userApiKey: String): NavigationRoute
}
