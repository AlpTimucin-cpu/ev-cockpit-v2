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
    val elevationsMeters: List<Double>? = null,
)

/** API key is user-supplied at runtime; never a build-time constant. */
interface RouteProvider {
    suspend fun calculate(start: GeoPoint, destination: GeoPoint, userApiKey: String): NavigationRoute

    /** User-triggered via-route; ORS implementation supports up to 50 waypoints. */
    suspend fun calculateVia(waypoints: List<GeoPoint>, userApiKey: String): NavigationRoute {
        require(waypoints.size in 2..50) { "Rota en az 2, en fazla 50 nokta içerir." }
        if (waypoints.size == 2) return calculate(waypoints[0], waypoints[1], userApiKey)
        throw UnsupportedOperationException("Bu sağlayıcı ara noktalı rota desteklemiyor.")
    }
}
