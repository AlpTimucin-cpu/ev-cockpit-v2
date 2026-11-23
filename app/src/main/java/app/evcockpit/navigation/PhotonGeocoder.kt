package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
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
            "&limit=8&lang=tr&lat=$lat&lon=$lon&zoom=6&location_bias_scale=0.4"
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
    private var lastRequestAtMs = -PhotonPolicy.MIN_INTERVAL_MS

    suspend fun search(rawQuery: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this@PhotonGeocoder) {
            if (!PhotonPolicy.canRequest(query, now - lastRequestAtMs)) {
                throw IllegalArgumentException("Photon hız sınırı: ağdan en erken 10 saniyede bir sorgu.")
            }
            lastRequestAtMs = now
        }
        val connection = URL(PhotonPolicy.requestUrl(query, focusPoint())).openConnection()
            as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.instanceFollowRedirects = false
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
