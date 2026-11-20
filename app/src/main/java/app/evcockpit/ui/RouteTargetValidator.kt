package app.evcockpit.ui

import app.evcockpit.navigation.GeoPoint

object RouteTargetValidator {
    fun parse(latitude: String, longitude: String): GeoPoint? {
        val lat = latitude.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val lon = longitude.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return runCatching { GeoPoint(lat, lon) }.getOrNull()
    }
}
