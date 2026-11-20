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
    fun decode(raw: String?): List<SavedDestination> = runCatching {
        val arr = JSONArray(raw ?: "[]")
        (0 until minOf(arr.length(), 12)).mapNotNull { i ->
            runCatching {
                val x = arr.getJSONObject(i)
                val kind = x.getString("type")
                if (kind !in setOf("HOME", "WORK", "RECENT")) return@runCatching null
                SavedDestination(kind, x.getString("label").take(100),
                    GeoPoint(x.getDouble("lat"), x.getDouble("lon")))
            }.getOrNull()
        }
    }.getOrElse { emptyList() }

    fun encode(list: List<SavedDestination>): String = JSONArray().also { arr ->
        list.take(12).forEach { p ->
            arr.put(JSONObject().put("type", p.type).put("label", p.label.take(100))
                .put("lat", p.point.latitude).put("lon", p.point.longitude))
        }
    }.toString()

    fun add(existing: List<SavedDestination>, item: SavedDestination): List<SavedDestination> {
        val without = existing.filterNot {
            it.type == item.type && (it.type != "RECENT" || it.point == item.point)
        }
        return (listOf(item) + without).filterIndexed { i, v ->
            v.type != "RECENT" || (listOf(item) + without).take(i + 1).count {
                it.type == "RECENT"
            } <= 10
        }.take(12)
    }
}

class DestinationsStore(context: Context) {
    private val store = context.applicationContext.destinationsStore
    private val key = stringPreferencesKey("entries")
    val entries: Flow<List<SavedDestination>> =
        store.data.map { DestinationCodec.decode(it[key]) }

    suspend fun save(item: SavedDestination) {
        require(item.type in setOf("HOME", "WORK", "RECENT"))
        store.edit { prefs ->
            prefs[key] = DestinationCodec.encode(
                DestinationCodec.add(DestinationCodec.decode(prefs[key]), item))
        }
    }
    suspend fun clear(type: String) {
        store.edit { prefs ->
            prefs[key] = DestinationCodec.encode(
                DestinationCodec.decode(prefs[key]).filterNot { it.type == type })
        }
    }
}
