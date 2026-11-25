package app.evcockpit.ui.unified

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.unifiedRoutes by preferencesDataStore("unified_routes_v1")

data class C7Endpoints(val origin: Place? = null, val destination: Place? = null)
data class C7SavedRoute(
    val id: String,
    val title: String,
    val endpoints: C7Endpoints,
    val savedAtMs: Long,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val source: String,
)

object C7RouteCodec {
    const val MAX_ROUTES = 20
    private const val MAX_JSON = 64_000
    private const val MAX_LABEL = 180

    // A historical GPS point must never be presented as a current fix.
    fun historical(place: Place): Place = place.copy(
        id = if (place.id == "gps") "saved-gps" else place.id, source = "KAYIT")
    fun session(origin: Place?, destination: Place?): C7Endpoints =
        C7Endpoints(origin?.takeIf { it.valid && it.source != "GPS" && it.id != "gps" }?.let(::historical),
            destination?.takeIf { it.valid }?.let(::historical))

    private fun placeJson(p: Place): JSONObject {
        require(p.valid && p.name.isNotBlank())
        return JSONObject().put("id", p.id.take(MAX_LABEL)).put("name", p.name.take(MAX_LABEL))
            .put("lat", p.latitude).put("lon", p.longitude)
    }
    private fun place(o: JSONObject?): Place? = o?.let {
        runCatching {
            Place(it.getString("id").take(MAX_LABEL), it.getString("name").take(MAX_LABEL),
                it.getDouble("lat"), it.getDouble("lon"), "KAYIT")
                .takeIf { p -> p.valid && p.name.isNotBlank() }
        }.getOrNull()
    }
    private fun endpointsJson(e: C7Endpoints): JSONObject = JSONObject().apply {
        e.origin?.let { put("origin", placeJson(it)) }
        e.destination?.let { put("destination", placeJson(it)) }
    }
    private fun endpoints(o: JSONObject): C7Endpoints =
        C7Endpoints(place(o.optJSONObject("origin")), place(o.optJSONObject("destination")))

    fun encodeLast(e: C7Endpoints): String = endpointsJson(e).toString()
    fun decodeLast(raw: String?): C7Endpoints = runCatching {
        require((raw?.length ?: 0) <= MAX_JSON)
        endpoints(JSONObject(raw ?: "{}"))
    }.getOrDefault(C7Endpoints())

    fun valid(r: C7SavedRoute): Boolean =
        r.id.isNotBlank() && r.title.isNotBlank() &&
            r.endpoints.origin?.valid == true && r.endpoints.destination?.valid == true &&
            r.endpoints.origin.let { a -> r.endpoints.destination.let { b ->
                a?.latitude != b?.latitude || a?.longitude != b?.longitude
            } } && r.savedAtMs > 0 &&
            r.distanceMeters.isFinite() && r.distanceMeters > 0 &&
            r.durationSeconds.isFinite() && r.durationSeconds > 0 && r.source.isNotBlank()

    fun from(s: UnifiedUiState, title: String, now: Long): C7SavedRoute? {
        val route = s.currentRoute ?: return null
        val a = s.origin ?: return null
        val b = s.destination ?: return null
        return C7SavedRoute(UUID.randomUUID().toString(), title.trim().take(MAX_LABEL),
            C7Endpoints(historical(a), historical(b)), now, route.distanceMeters,
            route.durationSeconds, route.source).takeIf(::valid)
    }

    fun encode(routes: List<C7SavedRoute>): String = JSONArray().apply {
        routes.filter(::valid).distinctBy { it.id }.take(MAX_ROUTES).forEach { r ->
            put(JSONObject().put("id", r.id.take(MAX_LABEL)).put("title", r.title.take(MAX_LABEL))
                .put("endpoints", endpointsJson(r.endpoints)).put("savedAt", r.savedAtMs)
                .put("distance", r.distanceMeters).put("duration", r.durationSeconds)
                .put("source", r.source.take(MAX_LABEL)))
        }
    }.toString()

    fun decode(raw: String?): List<C7SavedRoute> = runCatching {
        require((raw?.length ?: 0) <= MAX_JSON)
        val arr = JSONArray(raw ?: "[]")
        (0 until minOf(arr.length(), MAX_ROUTES)).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                C7SavedRoute(o.getString("id").take(MAX_LABEL), o.getString("title").take(MAX_LABEL),
                    endpoints(o.getJSONObject("endpoints")), o.getLong("savedAt"),
                    o.getDouble("distance"), o.getDouble("duration"),
                    o.getString("source").take(MAX_LABEL)).takeIf(::valid)
            }.getOrNull()
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    fun add(old: List<C7SavedRoute>, item: C7SavedRoute): List<C7SavedRoute> =
        (listOf(item) + old.filterNot { it.id == item.id }).filter(::valid).take(MAX_ROUTES)
}

class C7RouteStore(context: Context) {
    private val store = context.applicationContext.unifiedRoutes
    private val lastKey = stringPreferencesKey("last_endpoints")
    private val listKey = stringPreferencesKey("saved_routes")
    val last = store.data.map { C7RouteCodec.decodeLast(it[lastKey]) }
    val routes = store.data.map { C7RouteCodec.decode(it[listKey]) }

    suspend fun saveLast(endpoints: C7Endpoints) {
        store.edit { it[lastKey] = C7RouteCodec.encodeLast(endpoints) }
    }
    suspend fun save(route: C7SavedRoute) {
        require(C7RouteCodec.valid(route))
        store.edit { it[listKey] = C7RouteCodec.encode(C7RouteCodec.add(C7RouteCodec.decode(it[listKey]), route)) }
    }
    suspend fun delete(id: String) {
        store.edit { it[listKey] = C7RouteCodec.encode(C7RouteCodec.decode(it[listKey]).filterNot { r -> r.id == id }) }
    }
}
