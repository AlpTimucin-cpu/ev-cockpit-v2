package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RouteServiceException(message: String) : Exception(message)

/**
 * HeiGIT hosted openrouteservice Directions v2. Fixed HTTPS host, no redirects.
 * The user's API key is sent only in Authorization; never URL/logs/exception text.
 */
class OrsRouteProvider : RouteProvider {
    override suspend fun calculate(
        start: GeoPoint,
        destination: GeoPoint,
        userApiKey: String,
    ): NavigationRoute = withContext(Dispatchers.IO) {
        val key = userApiKey.trim()
        if (key.isEmpty()) throw RouteServiceException("Önce HeiGIT API anahtarını ayarlardan girin.")
        if (start == destination) throw RouteServiceException("Başlangıç ve hedef aynı olamaz.")
        val body = JSONObject().apply {
            put("coordinates", JSONArray()
                .put(JSONArray().put(start.longitude).put(start.latitude))
                .put(JSONArray().put(destination.longitude).put(destination.latitude)))
            put("language", "tr")
            put("instructions", true)
            put("elevation", true) // ORS supports optional third Z coordinate; parser falls back if absent.
        }.toString()

        val connection = URL(ENDPOINT).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 25_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", key)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/geo+json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            when (val status = connection.responseCode) {
                in 200..299 -> {
                    val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    OrsGeoJsonParser.parse(json)
                }
                400 -> throw RouteServiceException("Rota isteği geçersiz veya bu profil için uygun yol yok (400).")
                401, 403 -> throw RouteServiceException("HeiGIT anahtarı, yetkisi veya kotası kontrol edilmeli ($status).")
                429 -> throw RouteServiceException("Rota kotası veya dakika limiti aşıldı (429).")
                in 300..399 -> throw RouteServiceException("Rota servisi yönlendirme döndürdü ($status); güvenlik nedeniyle izlenmedi.")
                else -> throw RouteServiceException("Rota servisi yanıtı: HTTP $status.")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ENDPOINT =
            "https://api.heigit.org/openrouteservice/v2/directions/driving-car/geojson"
    }
}

/** Parses only real ORS FeatureCollection route geometry + Turkish instruction fields. */
object OrsGeoJsonParser {
    fun parse(response: String): NavigationRoute {
        val root = JSONObject(response)
        require(root.optString("type") == "FeatureCollection") { "Rota yanıtı GeoJSON değil." }
        val features = root.getJSONArray("features")
        require(features.length() >= 1) { "Rota bulunamadı." }
        val feature = features.getJSONObject(0)
        val geometry = feature.getJSONObject("geometry")
        require(geometry.getString("type") == "LineString") { "Rota çizgisi desteklenmiyor." }
        val points = geometry.getJSONArray("coordinates")
        val path = (0 until points.length()).map { index ->
            val item = points.getJSONArray(index)
            GeoPoint(latitude = item.getDouble(1), longitude = item.getDouble(0))
        }
        require(path.size >= 2) { "Rota geometrisi eksik." }

        val properties = feature.getJSONObject("properties")
        val summary = properties.getJSONObject("summary")
        val distance = summary.getDouble("distance")
        val duration = summary.getDouble("duration")
        require(distance.isFinite() && distance > 0.0 && duration.isFinite() && duration > 0.0) {
            "Rota mesafesi veya süresi geçersiz."
        }

        val segments = properties.optJSONArray("segments") ?: JSONArray()
        val steps = buildList {
            for (s in 0 until segments.length()) {
                val segmentSteps = segments.getJSONObject(s).optJSONArray("steps") ?: continue
                for (i in 0 until segmentSteps.length()) {
                    val step = segmentSteps.getJSONObject(i)
                    val text = step.optString("instruction", "").trim()
                    if (text.isEmpty()) continue
                    val waypoint = step.optJSONArray("way_points") ?: continue
                    if (waypoint.length() < 1) continue
                    val index = waypoint.getInt(0)
                    require(index in path.indices) { "Manevra konumu rota dışı." }
                    add(RouteManeuver(text, index, step.optInt("type", -1)))
                }
            }
        }
        val altitudes = (0 until points.length()).map { index ->
            val arr = points.getJSONArray(index)
            if (arr.length() >= 3) arr.optDouble(2, Double.NaN) else Double.NaN
        }.takeIf { it.size == path.size && it.all { value -> value.isFinite() } }
        return NavigationRoute(path, distance, duration, steps, elevationsMeters = altitudes)
    }
}
