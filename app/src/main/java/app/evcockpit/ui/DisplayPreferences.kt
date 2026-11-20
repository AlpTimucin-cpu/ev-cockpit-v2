package app.evcockpit.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.displayPreferences by preferencesDataStore("display_preferences_v1")

object DisplayDefaults {
    const val DARK_THEME = true
    const val KEEP_SCREEN_ON = false
}

class DisplayPreferences(context: Context) {
    private val store = context.applicationContext.displayPreferences
    private val dark = booleanPreferencesKey("dark_theme")
    private val screenOn = booleanPreferencesKey("keep_screen_on")
    val darkTheme: Flow<Boolean> =
        store.data.map { it[dark] ?: DisplayDefaults.DARK_THEME }
    val keepScreenOn: Flow<Boolean> =
        store.data.map { it[screenOn] ?: DisplayDefaults.KEEP_SCREEN_ON }
    suspend fun setDarkTheme(enabled: Boolean) {
        store.edit { it[dark] = enabled }
    }
    suspend fun setKeepScreenOn(enabled: Boolean) {
        store.edit { it[screenOn] = enabled }
    }
}
