package app.evcockpit.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.evMapLayerPrefs by preferencesDataStore("ev_map_layer_v1")

/**
 * Only whitelisted, documented MapLibre/OpenFreeMap styles.
 * Styles use OpenStreetMap data, NOT satellite or a live-traffic feed.
 */
enum class MapLayer(val id: String, val titleTr: String, val styleUrl: String) {
    DARK("dark", "Koyu", "https://tiles.openfreemap.org/styles/dark"),
    LIBERTY("liberty", "Yol / sokak", "https://tiles.openfreemap.org/styles/liberty"),
    BRIGHT("bright", "Açık", "https://tiles.openfreemap.org/styles/bright"),
    POSITRON("positron", "Sade", "https://tiles.openfreemap.org/styles/positron"),
    SATELLITE("satellite", "Uydu (MapTiler)", ""),
    TOPOGRAPHIC("topographic", "Arazi / topografya (MapTiler)", "");

    companion object {
        fun fromId(raw: String?): MapLayer =
            entries.firstOrNull { it.id == raw } ?: DARK
    }
}

class MapLayerPreferences(context: Context) {
    private val prefs = context.applicationContext.evMapLayerPrefs
    private val key = stringPreferencesKey("layer")
    val selected = prefs.data.map { MapLayer.fromId(it[key]) }
    suspend fun choose(layer: MapLayer) {
        prefs.edit { it[key] = layer.id }
    }
}

/**
 * Özel harita sağlayıcısının anahtarı olmadan uydu/topografya URL'si oluşturulmaz.
 * URL uygulama loguna yazılmaz. Harita karo/stil istekleri sağlayıcıya gider.
 */
object MapStyleResolver {
    fun url(layer: MapLayer, mapTilerKey: String?): String? = when (layer) {
        MapLayer.SATELLITE, MapLayer.TOPOGRAPHIC -> {
            val key = mapTilerKey?.trim()
            if (key.isNullOrEmpty() || !key.matches(Regex("[a-zA-Z0-9_-]{6,256}")))
                null
            else {
                val styleId = if (layer == MapLayer.SATELLITE) "satellite" else "topo"
                "https://api.maptiler.com/maps/$styleId/style.json?key=$key"
            }
        }
        else -> layer.styleUrl
    }
}
