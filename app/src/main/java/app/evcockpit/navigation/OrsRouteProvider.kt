package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.evcockpit.planner.route.RouteDistance
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
    ): NavigationRoute = calculateVia(listOf(start, destination), userApiKey)

    override suspend fun calculateVia(
        waypoints: List<GeoPoint>,
        userApiKey: String,
    ): NavigationRoute = request(OrsViaRequest.build(waypoints), userApiKey).first()

    override suspend fun calculateAlternatives(
        start: GeoPoint, destination: GeoPoint, userApiKey: String,
    ): List<NavigationRoute> {
        // Hosted ORS limits alternative routes to 100 km actual road distance.
        // A conservative straight-line bound avoids many predictable 400s.
        if (!OrsAlternativePolicy.eligible(start, destination))
            return listOf(calculate(start, destination, userApiKey))
        return try {
            request(OrsViaRequest.build(listOf(start, destination), alternatives = true),
                userApiKey)
        } catch (error: RouteServiceException) {
            // An over-100-km road path can still be rejected despite a short
            // straight-line distance. Retry once without alternatives.
            if (error.message?.contains("(400)") == true)
                listOf(calculate(start, destination, userApiKey)) else throw error
        }
    }

    private suspend fun request(
        body: String,
        userApiKey: String,
    ): List<NavigationRoute> = withContext(Dispatchers.IO) {
        val key = userApiKey.trim()
        if (key.isEmpty())
            throw RouteServiceException("Önce HeiGIT API anahtarını ayarlardan girin.")
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
                    val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }
                    OrsGeoJsonParser.parseAll(json)
                }
                400 -> throw RouteServiceException(
                    "Rota isteği geçersiz veya rota alternatif sınırı aşıldı (400).")
                401, 403 -> throw RouteServiceException(
                    "HeiGIT anahtarı, yetkisi veya kotası kontrol edilmeli ($status).")
                429 -> throw RouteServiceException("Rota kotası veya dakika limiti aşıldı (429).")
                in 300..399 -> throw RouteServiceException(
                    "Rota servisi yönlendirme döndürdü ($status); güvenlik nedeniyle izlenmedi.")
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
    fun parse(response: String): NavigationRoute = parseAll(response).first()

    fun parseAll(response: String): List<NavigationRoute> {
        val root = JSONObject(response)
        require(root.optString("type") == "FeatureCollection") {
            "Rota yanıtı GeoJSON değil."
        }
        val features = root.getJSONArray("features")
        require(features.length() >= 1) { "Rota bulunamadı." }
        return (0 until minOf(features.length(), 3)).map { index ->
            parseFeature(features.getJSONObject(index))
        }
    }

    private fun parseFeature(feature: JSONObject): NavigationRoute {
        val geometry = feature.getJSONObject("geometry")
        require(geometry.getString("type") == "LineString") {
            "Rota çizgisi desteklenmiyor."
        }
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
        require(distance.isFinite() && distance > 0.0 &&
            duration.isFinite() && duration > 0.0) {
            "Rota mesafesi veya süresi geçersiz."
        }
        val segments = properties.optJSONArray("segments") ?: JSONArray()
        val steps = buildList {
            for (segment in 0 until segments.length()) {
                val items = segments.getJSONObject(segment).optJSONArray("steps") ?: continue
                for (i in 0 until items.length()) {
                    val step = items.getJSONObject(i)
                    val instruction = step.optString("instruction", "").trim()
                    if (instruction.isEmpty()) continue
                    val waypoints = step.optJSONArray("way_points") ?: continue
                    if (waypoints.length() < 1) continue
                    val index = waypoints.getInt(0)
                    require(index in path.indices) { "Manevra konumu rota dışı." }
                    add(RouteManeuver(instruction, index, step.optInt("type", -1)))
                }
            }
        }
        val elevations = (0 until points.length()).map { i ->
            val arr = points.getJSONArray(i)
            if (arr.length() >= 3) arr.optDouble(2, Double.NaN)
            else Double.NaN
        }.takeIf { it.size == path.size && it.all(Double::isFinite) }
        return NavigationRoute(path, distance, duration, steps, elevationsMeters = elevations)
    }
}

/** Provider quota: 100 km road distance and at most three alternatives. */
object OrsAlternativePolicy {
    const val MAX_ALTERNATIVES = 3
    const val SAFE_AIR_DISTANCE_M = 80_000.0

    fun eligible(from: GeoPoint, to: GeoPoint): Boolean =
        from != to && RouteDistance.haversine(from, to) <= SAFE_AIR_DISTANCE_M

    fun label(count: Int, attempted: Boolean): String = when {
        !attempted -> "Uzun mesafede ücretsiz ORS alternatif rota algoritması 100 km ile sınırlı; tek gerçek rota."
        count <= 1 -> "Rota sağlayıcısı başka doğrulanmış seçenek döndürmedi."
        else -> "$count gerçek güzergâh seçeneği • trafik verisi içermez."
    }
}

/** Pure request construction: API key is NEVER included in JSON or URL. */
object OrsViaRequest {
    // HeiGIT hosted API limit: https://giscience.github.io/openrouteservice/hosted-api-restrictions
    const val MAX_WAYPOINTS = 50
    fun build(points: List<GeoPoint>, alternatives: Boolean = false): String {
        require(points.size in 2..MAX_WAYPOINTS) {
            "Rota ara nokta limiti 50; en az başlangıç ve hedef gerekli."
        }
        require(points.zipWithNext().all { it.first != it.second }) {
            "Ardışık rota noktaları aynı olamaz."
        }
        val geometry = JSONArray()
        points.forEach { geometry.put(JSONArray().put(it.longitude).put(it.latitude)) }
        val json = JSONObject().put("coordinates", geometry)
            .put("language", "tr").put("instructions", true)
            .put("elevation", true)
        if (alternatives) {
            require(points.size == 2) { "Alternatif yalnız iki noktada kullanılabilir." }
            json.put("alternative_routes", JSONObject()
                .put("target_count", OrsAlternativePolicy.MAX_ALTERNATIVES)
                .put("weight_factor", 1.5)
                .put("share_factor", 0.6))
        }
        return json.toString()
    }
}
