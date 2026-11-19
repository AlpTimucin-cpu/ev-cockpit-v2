package app.evcockpit.obd

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.manualSocDataStore by preferencesDataStore(name = "manual_soc_v1")

/** A user reading copied from the vehicle display, not a decoded ECU value. */
class ManualSocStore(private val context: Context) {
    private val key = doublePreferencesKey("manual_soc_percent")
    val socPct: Flow<Double?> = context.manualSocDataStore.data.map { it[key] }

    suspend fun save(value: Double) {
        require(value.isFinite() && value in 0.0..100.0)
        context.manualSocDataStore.edit { it[key] = value }
    }

    suspend fun clear() {
        context.manualSocDataStore.edit { it.remove(key) }
    }
}

/** Source selection does not imply that community OBD PID scales are verified. */
data class SocReading(val pct: Double?, val source: String)

object SocSelector {
    fun select(
        obdConnected: Boolean,
        demo: Boolean,
        obdPct: Double?,
        manualPct: Double?,
    ): SocReading {
        val obd = obdPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val manual = manualPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        return when {
            demo -> SocReading(obd, "DEMO — gerçek değil")
            obdConnected && obd != null -> SocReading(obd, "OBD — PID doğrulanmadı")
            manual != null -> SocReading(manual, "MANUEL — araç göstergesinden")
            else -> SocReading(null, "SoC verisi yok")
        }
    }
}

object ManualSocInput {
    fun parse(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }
}
