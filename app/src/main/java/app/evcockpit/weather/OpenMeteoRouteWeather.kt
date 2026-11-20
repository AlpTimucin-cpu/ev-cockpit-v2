package app.evcockpit.weather

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherPoint(val temperatureC: Double, val code: Int?)

object OpenMeteoWeatherLogic {
    /** At most 3 route samples, never polls GPS or shares raw coordinates in logs. */
    fun sample(route: NavigationRoute): List<GeoPoint> {
        val geo = route.geometry
        return when (geo.size) {
            0 -> emptyList()
            1 -> listOf(geo[0])
            2 -> geo
            else -> listOf(geo.first(), geo[geo.size / 2], geo.last())
        }
    }

    fun parse(json: String): List<WeatherPoint> = runCatching {
        val trimmed = json.trim()
        val objects = if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            (0 until minOf(arr.length(), 3)).map { arr.getJSONObject(it) }
        } else listOf(JSONObject(trimmed))
        objects.mapNotNull { root ->
            val current = root.optJSONObject("current") ?: return@mapNotNull null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            if (!temp.isFinite()) return@mapNotNull null
            WeatherPoint(temp, if (current.has("weather_code")) current.optInt("weather_code")
                else null)
        }
    }.getOrElse { emptyList() }

    private fun describe(code: Int?): String = when (code) {
        0 -> "Açık"
        1, 2 -> "Parçalı bulutlu"
        3 -> "Bulutlu"
        45, 48 -> "Sis"
        in 51..67, in 80..82 -> "Yağış"
        in 71..77, in 85..86 -> "Kar"
        in 95..99 -> "Fırtına"
        else -> "Durum bilinmiyor"
    }

    fun summary(items: List<WeatherPoint>): String {
        if (items.isEmpty()) return "Hava: veri yok — DOĞRULANMADI"
        val labels = if (items.size == 1) listOf("Hedef")
            else if (items.size == 2) listOf("Başlangıç", "Hedef")
            else listOf("Başlangıç", "Orta", "Hedef")
        val formatted = items.take(3).mapIndexed { i, v ->
            labels[i] + " " + String.format(Locale.US, "%.0f°C", v.temperatureC) +
                " " + describe(v.code)
        }
        return "Open-Meteo (TAHMİN): " + formatted.joinToString(" • ")
    }
}

/** An explicit route triggers one forecast fetch; no background polling or service secrets. */
class OpenMeteoRouteWeather {
    suspend fun forecast(route: NavigationRoute): List<WeatherPoint> =
        withContext(Dispatchers.IO) {
            val points = OpenMeteoWeatherLogic.sample(route)
            if (points.isEmpty()) return@withContext emptyList()
            fun values(pick: (GeoPoint) -> Double): String =
                points.joinToString(",") { String.format(Locale.US, "%.4f", pick(it)) }
            val query = "latitude=" + values { it.latitude } +
                "&longitude=" + values { it.longitude } +
                "&current=temperature_2m,weather_code&timezone=auto"
            val conn = URL("https://api.open-meteo.com/v1/forecast?" + query)
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "EV-Cockpit-v2-PrivateTest")
            try {
                if (conn.responseCode != 200) return@withContext emptyList()
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                OpenMeteoWeatherLogic.parse(raw)
            } finally {
                conn.disconnect()
            }
        }
}
