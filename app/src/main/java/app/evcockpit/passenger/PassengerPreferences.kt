package app.evcockpit.passenger

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.passengerPrefs by preferencesDataStore("passenger_prefs_v1")

object PassengerUiDefaults {
    val LAYOUT = PassengerLayout.FULL
    const val SPEED_LOCK = false
    fun parseLayout(raw: String?): PassengerLayout =
        PassengerLayout.entries.firstOrNull { it.name == raw } ?: LAYOUT
}

class PassengerPreferences(context: Context) {
    private val store = context.applicationContext.passengerPrefs
    private val layoutKey = stringPreferencesKey("layout")
    private val lockKey = booleanPreferencesKey("lock_by_speed")
    val layout: Flow<PassengerLayout> =
        store.data.map { PassengerUiDefaults.parseLayout(it[layoutKey]) }
    val lock: Flow<Boolean> =
        store.data.map { it[lockKey] ?: PassengerUiDefaults.SPEED_LOCK }

    suspend fun setLayout(value: PassengerLayout) {
        store.edit { it[layoutKey] = value.name }
    }
    suspend fun setSpeedLock(enabled: Boolean) {
        store.edit { it[lockKey] = enabled }
    }
}
