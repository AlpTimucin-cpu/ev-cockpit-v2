package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.planui.PlannerOptions
import app.evcockpit.planner.energy.HvacMode
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.m8OptionsStore by preferencesDataStore("m8_planner_options")
object PlannerOptionsCodec {
    fun encode(s:PlannerOptions):String=JSONObject().also { o->
        o.put("arrival",s.minArrivalSoc);o.put("stop",s.minStopSoc)
        o.put("charge",s.maxChargeSoc);o.put("speed",s.maxCruiseKmh)
        o.put("temp",s.temperatureC);o.put("hvac",s.hvac.name)
        o.put("load",s.extraLoadKg);o.put("adjust",s.consumptionAdjust)
        o.put("power",s.minStationKw);o.put("radius",s.searchRadiusMeters)
        o.put("maxStops",s.maxStops);o.put("ccs2",s.onlyTaggedCcs2)
        o.put("slow",s.allowSlowCharging);o.put("excluded",s.excludedNetworks)
        s.destinationChargeKw?.let {o.put("destination",it)}
    }.toString()
    fun decode(raw:String?):PlannerOptions=runCatching {
        if(raw.isNullOrBlank())return@runCatching PlannerOptions()
        val o=JSONObject(raw);val d=PlannerOptions()
        PlannerOptions(
            minArrivalSoc=o.optDouble("arrival",d.minArrivalSoc),
            minStopSoc=o.optDouble("stop",d.minStopSoc),
            maxChargeSoc=o.optDouble("charge",d.maxChargeSoc),
            maxCruiseKmh=o.optDouble("speed",d.maxCruiseKmh),
            temperatureC=o.optDouble("temp",d.temperatureC),
            hvac=HvacMode.valueOf(o.optString("hvac",d.hvac.name)),
            extraLoadKg=o.optDouble("load",d.extraLoadKg),
            consumptionAdjust=o.optDouble("adjust",d.consumptionAdjust),
            minStationKw=o.optDouble("power",d.minStationKw),
            searchRadiusMeters=o.optInt("radius",d.searchRadiusMeters),
            maxStops=o.optInt("maxStops",d.maxStops),
            onlyTaggedCcs2=o.optBoolean("ccs2",d.onlyTaggedCcs2),
            allowSlowCharging=o.optBoolean("slow",d.allowSlowCharging),
            excludedNetworks=o.optString("excluded",""),
            destinationChargeKw=o.optDouble("destination",Double.NaN).takeIf{it.isFinite()}
        ).also{require(it.errors().isEmpty())}
    }.getOrElse{PlannerOptions()}
}
class PlannerOptionsStore(context:Context) {
    private val store=context.applicationContext.m8OptionsStore
    private val key=stringPreferencesKey("settings")
    val options=store.data.map{PlannerOptionsCodec.decode(it[key])}
    suspend fun save(settings:PlannerOptions) {
        require(settings.errors().isEmpty())
        store.edit {it[key]=PlannerOptionsCodec.encode(settings)}
    }
    suspend fun reset() {store.edit{it.remove(key)}}
}
