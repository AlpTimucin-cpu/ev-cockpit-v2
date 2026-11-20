package app.evcockpit.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Public Radio Browser API. No API key or third-party APK code.
 * Fail closed on HTTP streams and cap the remote response to 1 MiB.
 */
class RadioBrowserClient {
    suspend fun getTurkishStations(): List<MediaStream> = withContext(Dispatchers.IO) {
        val endpoint = "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/TR" +
            "?order=clickcount&reverse=true&hidebroken=true&limit=80"
        parseStations(request(endpoint))
    }

    suspend fun registerClick(uuid: String) = withContext(Dispatchers.IO) {
        if (!Regex("^[a-fA-F0-9-]{36}$").matches(uuid)) return@withContext
        runCatching {
            request("https://de1.api.radio-browser.info/json/url/$uuid")
        }
        Unit
    }

    internal fun parseStations(json: String): List<MediaStream> {
        val entries = JSONArray(json)
        val stations = ArrayList<MediaStream>()
        val seen = HashSet<String>()
        for (index in 0 until minOf(entries.length(), 160)) {
            val node = entries.optJSONObject(index) ?: continue
            val id = node.optString("stationuuid", "")
            val title = node.optString("name", "").trim().take(100)
            val stream = MediaUrlPolicy.validHttps(node.optString("url_resolved", ""))
                ?: MediaUrlPolicy.validHttps(node.optString("url", ""))
                ?: continue
            if (title.isBlank() || id.isBlank() || !seen.add(id)) continue
            stations.add(MediaStream(id, title, stream, StreamKind.RADIO))
        }
        return stations
    }

    private fun request(target: String): String {
        val connection = URL(target).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 6_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "EVCockpit/0.3 (radio-browser compatible)")
        connection.setRequestProperty("Accept", "application/json")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Radio Browser HTTP ${connection.responseCode}")
            }
            val result = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val bytes = ByteArray(8192)
                while (true) {
                    val count = input.read(bytes)
                    if (count < 0) break
                    if (result.size() + count > 1024 * 1024) {
                        throw IOException("Radio Browser response too large")
                    }
                    result.write(bytes, 0, count)
                }
            }
            return result.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}
