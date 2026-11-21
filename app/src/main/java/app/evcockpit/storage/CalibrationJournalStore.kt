package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import app.evcockpit.planui.CalibrationSuggestion

private val Context.calibrationJournalPrefs by preferencesDataStore("m8_calibration_journal_v1")

data class CalibrationRecord(
    val modelKwhPer100Km: Double,
    val actualKwhPer100Km: Double,
    val plannedArrivalSoc: Double,
    val actualArrivalSoc: Double,
    val recommendedAdjustmentPercent: Double,
    val verified: Boolean = false,
)

/** Stores numbers only: no route geometry, GPS position, VIN, API key or device ID. */
object CalibrationJournalCodec {
    fun fromSuggestion(s: CalibrationSuggestion): CalibrationRecord =
        CalibrationRecord(s.modelKwhPer100Km, s.observedKwhPer100Km,
            s.plannedArrivalSoc, s.observedArrivalSoc,
            s.suggestedConsumptionAdjust * 100.0, false)

    fun valid(x: CalibrationRecord): Boolean =
        x.modelKwhPer100Km.isFinite() && x.modelKwhPer100Km in 1.0..100.0 &&
        x.actualKwhPer100Km.isFinite() && x.actualKwhPer100Km in 1.0..100.0 &&
        x.plannedArrivalSoc.isFinite() && x.plannedArrivalSoc in 0.0..100.0 &&
        x.actualArrivalSoc.isFinite() && x.actualArrivalSoc in 0.0..100.0 &&
        x.recommendedAdjustmentPercent.isFinite() &&
        x.recommendedAdjustmentPercent in -30.0..30.0 && !x.verified

    fun encode(items: List<CalibrationRecord>): String {
        val json = JSONArray()
        items.take(10).filter(::valid).forEach { item ->
            json.put(JSONObject().put("modelKwh", item.modelKwhPer100Km)
                .put("actualKwh", item.actualKwhPer100Km)
                .put("planSoc", item.plannedArrivalSoc)
                .put("actualSoc", item.actualArrivalSoc)
                .put("adjustPct", item.recommendedAdjustmentPercent))
        }
        return json.toString()
    }

    fun decode(raw: String?): List<CalibrationRecord> = runCatching {
        if (raw == null || raw.length > 4096) return@runCatching emptyList()
        val json = JSONArray(raw)
        buildList {
            for (i in 0 until minOf(json.length(), 10)) {
                val node = json.optJSONObject(i) ?: continue
                val item = CalibrationRecord(
                    node.optDouble("modelKwh", Double.NaN),
                    node.optDouble("actualKwh", Double.NaN),
                    node.optDouble("planSoc", Double.NaN),
                    node.optDouble("actualSoc", Double.NaN),
                    node.optDouble("adjustPct", Double.NaN),
                )
                if (valid(item)) add(item)
            }
        }
    }.getOrDefault(emptyList())
}

class CalibrationJournalStore(context: Context) {
    private val store = context.applicationContext.calibrationJournalPrefs
    private val key = stringPreferencesKey("records")
    val records = store.data.map { CalibrationJournalCodec.decode(it[key]) }

    suspend fun add(record: CalibrationRecord) {
        require(CalibrationJournalCodec.valid(record))
        store.edit {
            val old = CalibrationJournalCodec.decode(it[key])
            it[key] = CalibrationJournalCodec.encode((listOf(record) + old).take(10))
        }
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }
}
