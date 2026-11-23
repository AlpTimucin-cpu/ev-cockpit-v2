package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.planner.charging.ManualChargeTariff
import app.evcockpit.planner.charging.TripChargeCost
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.manualChargeTariffs by preferencesDataStore("manual_charge_tariffs_v1")

/** No route, GPS, VIN or device identifiers. User-entered operator + observed price only. */
object ManualTariffCodec {
    const val MAX_OPERATORS = 30
    const val MAX_JSON = 8_192

    fun encode(prices: Map<String, ManualChargeTariff>): String =
        JSONObject().apply {
            prices.entries.take(MAX_OPERATORS).forEach { (operator, entry) ->
                if (entry.isValid() && operator.length in 1..100) {
                    put(TripChargeCost.normalizedOperator(operator),
                        JSONObject().put("tlPerKwh", entry.priceTlPerKwh)
                            .put("observedAt", entry.observedAtEpochMs))
                }
            }
        }.toString()

    fun decode(raw: String?): Map<String, ManualChargeTariff> = runCatching {
        if (raw.isNullOrBlank() || raw.length > MAX_JSON)
            return@runCatching emptyMap()
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            var count = 0
            while (keys.hasNext() && count < MAX_OPERATORS) {
                count++
                val name = keys.next()
                val item = json.optJSONObject(name) ?: continue
                val entry = ManualChargeTariff(
                    priceTlPerKwh = item.optDouble("tlPerKwh", Double.NaN),
                    observedAtEpochMs = item.optLong("observedAt", 0L),
                )
                if (entry.isValid() && name.length in 1..100) {
                    put(TripChargeCost.normalizedOperator(name), entry)
                }
            }
        }
    }.getOrDefault(emptyMap())
}

class ManualChargeTariffStore(context: Context) {
    private val store = context.applicationContext.manualChargeTariffs
    private val key = stringPreferencesKey("operator_prices")
    val tariffs = store.data.map { ManualTariffCodec.decode(it[key]) }

    suspend fun save(operator: String, priceTlPerKwh: Double) {
        val normalized = TripChargeCost.normalizedOperator(operator)
        require(normalized.length in 1..100 &&
            normalized != "operator bilinmiyor")
        val entry = ManualChargeTariff(priceTlPerKwh, System.currentTimeMillis())
        require(entry.isValid())
        store.edit { prefs ->
            val old = ManualTariffCodec.decode(prefs[key])
            val fresh = (old + (normalized to entry)).toList().takeLast(
                ManualTariffCodec.MAX_OPERATORS
            ).toMap()
            prefs[key] = ManualTariffCodec.encode(fresh)
        }
    }

    suspend fun remove(operator: String) {
        val keyName = TripChargeCost.normalizedOperator(operator)
        store.edit { prefs ->
            prefs[key] = ManualTariffCodec.encode(
                ManualTariffCodec.decode(prefs[key]) - keyName
            )
        }
    }
}
