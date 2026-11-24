package app.evcockpit.ui.unified.map

/** MapLibre stil URL'si değişse de overlay kimlikleri sabittir. */
internal object OverlayIds {
    const val SELECTED_ROUTE_SOURCE = "src-route-sel"
    const val SELECTED_ROUTE_CASING = "lyr-route-casing"
    const val SELECTED_ROUTE_LAYER = "lyr-route-sel"
    const val WEATHER_SOURCE = "src-weather"
    const val WEATHER_LAYER = "lyr-weather"
    const val STATIONS_SOURCE = "src-stations"
    const val STATIONS_LAYER = "lyr-stations"
    const val ENDPOINTS_SOURCE = "src-endpoints"
    const val ENDPOINTS_LAYER = "lyr-endpoints"
    const val VEHICLE_ACCURACY_SOURCE = "src-vehicle-accuracy"
    const val VEHICLE_ACCURACY_LAYER = "lyr-vehicle-accuracy"
    const val VEHICLE_SOURCE = "src-vehicle"
    const val VEHICLE_LAYER = "lyr-vehicle"
    const val START_ICON = "img-origin"
    const val FINISH_ICON = "img-destination"
    const val WEATHER_ICON = "img-weather"
    const val VEHICLE_ICON = "img-vehicle"
    const val STATION_AC_ICON = "img-station-ac"
    const val STATION_50_ICON = "img-station-dc50"
    const val STATION_100_ICON = "img-station-dc100"
    const val STATION_150_ICON = "img-station-dc150"
    const val STATION_UNKNOWN_ICON = "img-station-unknown"

    fun alternativeSource(index: Int) = "src-route-alt-$index"
    fun alternativeLayer(index: Int) = "lyr-route-alt-$index"
}
