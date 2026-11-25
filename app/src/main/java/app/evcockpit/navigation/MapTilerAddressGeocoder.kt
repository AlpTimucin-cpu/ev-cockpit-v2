package app.evcockpit.navigation

import app.evcockpit.planner.data.BoundedResponseReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Documented MapTiler Search API; existing user key, no embedded credentials. */
object MapTilerAddressParser {
    fun parse(raw: String): List<PhotonPlace> {
        val features = JSONObject(raw).getJSONArray("features")
        return (0 until minOf(features.length(), 8)).mapNotNull { index ->
            runCatching {
                val item = features.getJSONObject(index)
                val center = item.optJSONArray("center") ?: return@runCatching null
                val point = GeoPoint(center.getDouble(1), center.getDouble(0))
                val label = item.optString("place_name").trim().take(180)
                if (label.isBlank()) null else PhotonPlace(label, point)
            }.getOrNull()
        }
    }
}

class MapTilerAddressGeocoder {
    suspend fun search(query: String, key: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        require(AutocompletePolicy.eligible(query))
        require(key.matches(Regex("[a-zA-Z0-9_-]{6,256}")))
        val path = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val url = URL("https://api.maptiler.com/geocoding/$path.json?key=$key&autocomplete=true&language=tr&country=tr&limit=8")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        conn.instanceFollowRedirects = false
        try {
            if (conn.responseCode != 200) return@withContext emptyList()
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { BoundedResponseReader.read(it, 128_000) }
            MapTilerAddressParser.parse(raw)
        } finally { conn.disconnect() }
    }
}
