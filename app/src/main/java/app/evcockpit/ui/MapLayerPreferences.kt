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
    POSITRON("positron", "Sade", "https://tiles.openfreemap.org/styles/positron");

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
