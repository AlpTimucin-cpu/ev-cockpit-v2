package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class PhotonPlace(val label: String, val point: GeoPoint)

/**
 * Photon'un genel örnek servisi sınırsız değildir; her harfte ağ isteği gönderilmez.
 * Başlangıçtaki öneriler çevrim dışı il/ilçe kaydından anında gelir.
 */
object PhotonPolicy {
    const val MIN_INTERVAL_MS = 10_000L
    fun canRequest(query: String, elapsedSinceLastMs: Long): Boolean =
        query.trim().length >= 3 && query.length <= 120 &&
            elapsedSinceLastMs >= MIN_INTERVAL_MS

    /** Koordinat yanlılığı sadece sıralamayı etkiler, Türkiye dışını engellemez. */
    fun requestUrl(query: String, currentPosition: GeoPoint?): String {
        require(query.trim().length in 3..120)
        val bias = currentPosition?.takeIf {
            it.latitude in 35.0..43.0 && it.longitude in 25.0..46.0
        } ?: GeoPoint(39.0, 35.0)
        val lat = String.format(Locale.US, "%.5f", bias.latitude)
        val lon = String.format(Locale.US, "%.5f", bias.longitude)
        return "https://photon.komoot.io/api/?q=" +
            URLEncoder.encode(query.trim(), "UTF-8") +
            "&limit=8&lat=$lat&lon=$lon&zoom=6&location_bias_scale=0.4"
    }
}

object PhotonParser {
    fun parse(raw: String): List<PhotonPlace> {
        val features = JSONObject(raw).getJSONArray("features")
        return (0 until minOf(features.length(), 8)).mapNotNull { i ->
            runCatching {
                val feature = features.getJSONObject(i)
                val props = feature.getJSONObject("properties")
                val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val point = GeoPoint(coords.getDouble(1), coords.getDouble(0))
                val streetNumber = listOf(props.optString("street"), props.optString("housenumber"))
                    .filter { it.isNotBlank() && it != "null" }.joinToString(" ")
                val label = listOf(
                    props.optString("name"), streetNumber,
                    props.optString("district"), props.optString("city"),
                    props.optString("state"), props.optString("country"),
                ).filter { it.isNotBlank() && it != "null" }.distinct()
                    .joinToString(", ").take(180)
                if (label.isBlank()) null else PhotonPlace(label, point)
            }.getOrNull()
        }
    }
}

class PhotonGeocoder(
    private val focusPoint: () -> GeoPoint? = { null },
) {
    companion object {
        private val requestMutex = Mutex()
        private var lastRequestAtMs = -PhotonPolicy.MIN_INTERVAL_MS
    }

    suspend fun search(rawQuery: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        require(query.length in 3..120)
        requestMutex.withLock {
            val now = android.os.SystemClock.elapsedRealtime()
            val wait = (PhotonPolicy.MIN_INTERVAL_MS - (now - lastRequestAtMs)).coerceAtLeast(0L)
            if (wait > 0) delay(wait)
            currentCoroutineContext().ensureActive()
            lastRequestAtMs = android.os.SystemClock.elapsedRealtime()
        }
        val connection = URL(PhotonPolicy.requestUrl(query, focusPoint())).openConnection()
            as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept-Language", "tr,en;q=0.7")
        connection.setRequestProperty("User-Agent",
            "EV-Cockpit-v2-Personal/0.4 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
        try {
            if (connection.responseCode != 200) {
                throw IllegalStateException("Photon adres servisine ulaşılamadı veya kota doldu.")
            }
            val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                app.evcockpit.planner.data.BoundedResponseReader.read(reader, 96_000)
            }
            PhotonParser.parse(raw)
        } finally {
            connection.disconnect()
        }
    }
}
