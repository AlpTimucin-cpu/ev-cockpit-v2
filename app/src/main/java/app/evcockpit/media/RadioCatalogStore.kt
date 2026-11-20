package app.evcockpit.media

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persist only public Radio Browser station metadata, never service tokens or user credentials.
 * The driver-safe audio media library uses this bounded snapshot after phone-side refresh.
 */
object RadioCatalogStore {
    private const val PREFS = "radio_catalog_v1"
    private const val KEY = "stations"

    fun save(context: Context, stations: List<MediaStream>) {
        val array = JSONArray()
        stations.asSequence()
            .filter { it.kind == StreamKind.RADIO && MediaUrlPolicy.validHttps(it.url) != null }
            .take(80)
            .forEach {
                array.put(JSONObject()
                    .put("id", it.id.take(64))
                    .put("title", it.title.take(100))
                    .put("url", it.url))
            }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    fun load(context: Context): List<MediaStream> {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        if (raw.length > 256 * 1024) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                val seen = HashSet<String>()
                for (index in 0 until minOf(array.length(), 80)) {
                    val node = array.optJSONObject(index) ?: continue
                    val id = node.optString("id")
                    val title = node.optString("title")
                    val url = MediaUrlPolicy.validHttps(node.optString("url")) ?: continue
                    if (id.isBlank() || title.isBlank() || !seen.add(id)) continue
                    add(MediaStream(id, title.take(100), url, StreamKind.RADIO))
                }
            }
        }.getOrDefault(emptyList())
    }
}
