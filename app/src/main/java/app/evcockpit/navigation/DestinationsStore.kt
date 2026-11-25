package app.evcockpit.navigation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.destinationsStore by preferencesDataStore("destinations_v1")

data class SavedDestination(
    val type: String,
    val label: String,
    val point: GeoPoint,
)

object DestinationCodec {
    const val MAX_RECENT = 10
    const val MAX_FAVORITES = 20
    const val MAX_ENTRIES = MAX_RECENT + MAX_FAVORITES + 2
    val types = setOf("HOME", "WORK", "RECENT", "FAVORITE")

    fun decode(raw: String?): List<SavedDestination> = runCatching {
        require((raw?.length ?: 0) <= 64_000)
        val arr = JSONArray(raw ?: "[]")
        bounded((0 until minOf(arr.length(), MAX_ENTRIES)).mapNotNull { i ->
            runCatching {
                val x = arr.getJSONObject(i)
                val kind = x.getString("type")
                if (kind !in types) return@runCatching null
                val label = x.getString("label").trim().take(100)
                if (label.isBlank()) return@runCatching null
                SavedDestination(kind, label,
                    GeoPoint(x.getDouble("lat"), x.getDouble("lon")))
            }.getOrNull()
        })
    }.getOrElse { emptyList() }

    fun encode(list: List<SavedDestination>): String = JSONArray().also { arr ->
        bounded(list).forEach { p ->
            arr.put(JSONObject().put("type", p.type).put("label", p.label.take(100))
                .put("lat", p.point.latitude).put("lon", p.point.longitude))
        }
    }.toString()

    private fun same(a: SavedDestination, b: SavedDestination): Boolean =
        a.type == b.type && (a.type in setOf("HOME", "WORK") || a.point == b.point)

    private fun bounded(items: List<SavedDestination>): List<SavedDestination> = buildList {
        for (item in items) {
            if (item.type !in types || item.label.isBlank() || any { same(it, item) }) continue
            val limit = when (item.type) {
                "RECENT" -> MAX_RECENT
                "FAVORITE" -> MAX_FAVORITES
                else -> 1
            }
            if (count { it.type == item.type } < limit) add(item.copy(label = item.label.take(100)))
        }
    }

    fun add(existing: List<SavedDestination>, item: SavedDestination): List<SavedDestination> =
        bounded(listOf(item) + existing)
}

class DestinationsStore(context: Context) {
    private val store = context.applicationContext.destinationsStore
    private val key = stringPreferencesKey("entries")
    val entries: Flow<List<SavedDestination>> =
        store.data.map { DestinationCodec.decode(it[key]) }

    suspend fun save(item: SavedDestination) {
        require(item.type in DestinationCodec.types && item.label.isNotBlank())
        store.edit { prefs ->
            prefs[key] = DestinationCodec.encode(
                DestinationCodec.add(DestinationCodec.decode(prefs[key]), item))
        }
    }
    suspend fun remove(item: SavedDestination) {
        store.edit { prefs ->
            prefs[key] = DestinationCodec.encode(
                DestinationCodec.decode(prefs[key]).filterNot { it.type == item.type && it.point == item.point })
        }
    }

    suspend fun clear(type: String) {
        store.edit { prefs ->
            prefs[key] = DestinationCodec.encode(
                DestinationCodec.decode(prefs[key]).filterNot { it.type == type })
        }
    }
}
