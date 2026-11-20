package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PhotonPlace(val label: String, val point: GeoPoint)

/**
 * Explicit, tap-to-search only. No autocomplete, background bulk search, or raw query logging.
 * Photon public instance is a fair-use demo without a guaranteed numerical quota or SLA.
 */
object PhotonPolicy {
    const val MIN_INTERVAL_MS = 10_000L
    fun canRequest(query: String, elapsedSinceLastMs: Long): Boolean =
        query.trim().length >= 3 && query.length <= 120 &&
            elapsedSinceLastMs >= MIN_INTERVAL_MS
}

object PhotonParser {
    fun parse(raw: String): List<PhotonPlace> {
        val features = JSONObject(raw).getJSONArray("features")
        return (0 until minOf(features.length(), 5)).mapNotNull { i ->
            runCatching {
                val feature = features.getJSONObject(i)
                val props = feature.getJSONObject("properties")
                val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val point = GeoPoint(coords.getDouble(1), coords.getDouble(0))
                val label = listOf(props.optString("name"), props.optString("city"),
                    props.optString("country"))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ").take(100)
                if (label.isBlank()) null else PhotonPlace(label, point)
            }.getOrNull()
        }
    }
}

class PhotonGeocoder {
    private var lastRequestAtMs = -PhotonPolicy.MIN_INTERVAL_MS

    suspend fun search(rawQuery: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this@PhotonGeocoder) {
            if (!PhotonPolicy.canRequest(query, now - lastRequestAtMs)) {
                throw IllegalArgumentException("Arama en az 3 karakter olmalı; 10 sn arayla tekrar dene.")
            }
            lastRequestAtMs = now
        }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://photon.komoot.io/api/?q=" + encoded + "&limit=5&lang=tr")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent",
            "EV-Cockpit-v2-PrivateTest/0.2 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
        try {
            if (connection.responseCode != 200) {
                throw IllegalStateException("Geocoding API hata; sağlayıcı limiti/erişim engeli olabilir.")
            }
            val raw = connection.inputStream.bufferedReader().use { reader ->
                val chars = CharArray(65_536)
                val size = reader.read(chars)
                if (size <= 0) "" else String(chars, 0, size)
            }
            PhotonParser.parse(raw)
        } finally {
            connection.disconnect()
        }
    }
}
