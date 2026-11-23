package app.evcockpit.navigation

import app.evcockpit.planner.data.BoundedResponseReader
import app.evcockpit.planner.route.RouteDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * FOSSGIS'in OSRM örnek servisi: gerçek yol ağı ve gerçek alternatifler.
 * Ücretsiz örnek servis için saniyede en fazla bir istek, kullanıcı tetiklemesi,
 * açık kaynak atfı ve otomatik yeniden denemesiz çalışma zorunludur.
 * Bu servis üretim SLA'sı veya garantili 3 alternatif sağlamaz.
 */
class OsrmRouteProvider : RouteProvider {
    override suspend fun calculate(
        start: GeoPoint,
        destination: GeoPoint,
        userApiKey: String,
    ): NavigationRoute = calculateAlternatives(start, destination, userApiKey).first()

    override suspend fun calculateAlternatives(
        start: GeoPoint,
        destination: GeoPoint,
        userApiKey: String,
    ): List<NavigationRoute> {
        require(start != destination) { "Başlangıç ve hedef aynı." }
        val url = OsrmRequest.url(start, destination)
        return OsrmRequest.oneAtATime {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent",
                    "EV-Cockpit-v2-Private/0.4 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
                try {
                    when (val http = connection.responseCode) {
                        200 -> {
                            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                                BoundedResponseReader.read(it, 3_000_000)
                            }
                            OsrmResponseParser.parse(body)
                        }
                        429 -> throw RouteServiceException(
                            "OSRM sunucusu hız sınırı uyguluyor. Bir süre sonra yeniden dene.")
                        400, 404 -> throw RouteServiceException(
                            "Bu noktalar arasında OSRM yol güzergâhı bulunamadı.")
                        in 300..399 -> throw RouteServiceException(
                            "OSRM sunucusu beklenmeyen yönlendirme yaptı.")
                        else -> throw RouteServiceException(
                            "OSRM geçici olarak kullanılamıyor (HTTP $http).")
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}

object OsrmRequest {
    private const val BASE = "https://routing.openstreetmap.de/routed-car/route/v1/driving/"
    private const val MIN_GAP_MS = 1_100L
    private val guard = Mutex()
    private var lastCallMs = -MIN_GAP_MS

    fun url(start: GeoPoint, end: GeoPoint): String {
        fun point(p: GeoPoint) = String.format(
            Locale.US, "%.6f,%.6f", p.longitude, p.latitude)
        return BASE + point(start) + ";" + point(end) +
            "?alternatives=3&steps=true&geometries=geojson&overview=full"
    }

    suspend fun <T> oneAtATime(request: suspend () -> T): T = guard.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        val wait = (MIN_GAP_MS - (now - lastCallMs)).coerceAtLeast(0)
        if (wait > 0) delay(wait)
        lastCallMs = android.os.SystemClock.elapsedRealtime()
        request()
    }
}

/** Yalnız API'den dönen yol geometrilerini kabul eder; sahte alternatif çizmez. */
object OsrmResponseParser {
    fun parse(json: String): List<NavigationRoute> {
        val root = JSONObject(json)
        require(root.optString("code") == "Ok") {
            "OSRM rota yanıtı geçersiz veya güzergâh bulunamadı."
        }
        val routes = root.optJSONArray("routes")
            ?: throw IllegalArgumentException("OSRM rota listesi eksik.")
        val parsed = (0 until minOf(routes.length(), 3)).mapNotNull { i ->
            runCatching { parseSingle(routes.getJSONObject(i)) }.getOrNull()
        }.distinctBy { it.geometry }
        require(parsed.isNotEmpty()) { "OSRM kullanılabilir yol geometrisi döndürmedi." }
        return parsed
    }

    private fun parseSingle(route: JSONObject): NavigationRoute {
        val distance = route.getDouble("distance")
        val duration = route.getDouble("duration")
        require(distance.isFinite() && distance > 0.0 &&
            duration.isFinite() && duration > 0.0) { "OSRM süre ve mesafe geçersiz." }
        val geometry = route.getJSONObject("geometry")
        require(geometry.getString("type") == "LineString")
        val coordinates = geometry.getJSONArray("coordinates")
        require(coordinates.length() in 2..40_000) { "Rota geometri boyutu geçersiz." }
        val positions = (0 until coordinates.length()).map { index ->
            val item = coordinates.getJSONArray(index)
            GeoPoint(item.getDouble(1), item.getDouble(0))
        }
        // Manevra koordinatlarının çoğu rota geometrisinin birebir noktasıdır.
        // Hash indeks sayesinde uzun şehirler arası rotada her manevrada tüm yolu taramayız.
        val pointIndexes = positions.indices.groupBy { positions[it] }
        var lastManeuverIndex = 0
        val instructions = buildList {
            val legs = route.optJSONArray("legs")
            if (legs != null) {
                for (legIndex in 0 until legs.length()) {
                    val steps = legs.getJSONObject(legIndex).optJSONArray("steps") ?: continue
                    for (stepIndex in 0 until minOf(steps.length(), 400)) {
                        val step = steps.getJSONObject(stepIndex)
                        val maneuver = step.optJSONObject("maneuver") ?: continue
                        val location = maneuver.optJSONArray("location") ?: continue
                        if (location.length() < 2) continue
                        val point = runCatching {
                            GeoPoint(location.getDouble(1), location.getDouble(0))
                        }.getOrNull() ?: continue
                        val nearest = pointIndexes[point]
                            ?.firstOrNull { it >= lastManeuverIndex }
                            ?: positions.indices.minByOrNull {
                                RouteDistance.haversine(positions[it], point)
                            } ?: continue
                        lastManeuverIndex = nearest
                        val type = maneuver.optString("type")
                        val modifier = maneuver.optString("modifier")
                        val roadName = step.optString("name", "").trim()
                        val text = instruction(type, modifier, roadName)
                        if (text.isNotBlank()) {
                            add(RouteManeuver(text, nearest, type.hashCode()))
                        }
                    }
                }
            }
        }
        return NavigationRoute(
            geometry = positions,
            distanceMeters = distance,
            durationSeconds = duration,
            maneuvers = instructions,
            provider = "OSRM / FOSSGIS (trafik yok)",
        )
    }

    /** OSRM'nin manevra türlerini sürücüye okunabilir Türkçe biçime dönüştürür. */
    fun instruction(type: String, modifier: String, road: String): String {
        val direction = when (modifier) {
            "left" -> "sola"
            "slight left" -> "hafif sola"
            "sharp left" -> "keskin sola"
            "right" -> "sağa"
            "slight right" -> "hafif sağa"
            "sharp right" -> "keskin sağa"
            "uturn" -> "geriye"
            "straight" -> "düz"
            else -> ""
        }
        val action = when (type) {
            "depart" -> "Yola çıkın"
            "arrive" -> "Hedefe ulaştınız"
            "turn", "end of road", "fork", "continue", "new name" ->
                if (direction.isEmpty()) "Yola devam edin"
                else "$direction yönüne devam edin"
            "roundabout", "rotary", "roundabout turn" -> "Dönel kavşağa girin"
            "exit roundabout", "exit rotary" -> "Dönel kavşaktan çıkın"
            "merge" -> "Yola katılın"
            "on ramp" -> "Bağlantı yoluna girin"
            "off ramp" -> "Bağlantı yolundan çıkın"
            "notification" -> "Yol uyarısı"
            else -> "Yola devam edin"
        }
        return if (road.isBlank() || type == "arrive") action else "$action: ${road.take(85)}"
    }
}
