package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.planner.model.*
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.m8ProfileStore by preferencesDataStore("m8_vehicle_profile_v1")
object ProfileCodec {
    private fun v(p:SourcedValue)=JSONObject().put("v",p.value).put("source",p.source)
        .put("verified",p.verified).put("note",p.note)
    private fun read(o:JSONObject)=SourcedValue(o.getDouble("v"),o.getString("source"),
        o.optBoolean("verified",false),o.optString("note","TAHMİN"))
    private fun curve(x:SourcedCurve)=JSONObject().put("source",x.source)
        .put("verified",x.verified).put("note",x.note)
        .put("points",JSONArray().also { a -> x.points.forEach { a.put(JSONArray().put(it.x).put(it.y)) } })
    private fun readCurve(o:JSONObject):SourcedCurve {
        val a=o.getJSONArray("points")
        return SourcedCurve((0 until a.length()).map { idx ->
            a.getJSONArray(idx).let { CurvePoint(it.getDouble(0),it.getDouble(1)) }
        },o.getString("source"),o.optBoolean("verified",false),o.optString("note","TAHMİN"))
    }
    fun encode(p:VehicleProfile):String=JSONObject().also { o ->
        listOf("usableKwh" to p.usableKwh,"sohPercent" to p.sohPercent,
            "massKg" to p.massKg,"extraLoadKg" to p.extraLoadKg,
            "cdA" to p.cdA,"crr" to p.crr,
            "drivetrainEfficiency" to p.drivetrainEfficiency,
            "regenEfficiency" to p.regenEfficiency,"maxRegenKw" to p.maxRegenKw,
            "maxDcKw" to p.maxDcKw,"maxAcKw" to p.maxAcKw,
            "auxBaseKw" to p.auxBaseKw,"hvacAutoKw" to p.hvacAutoKw,
            "hvacHeatingKw" to p.hvacHeatingKw,"consumptionAdjust" to p.consumptionAdjust
        ).forEach { (k,x)->o.put(k,v(x)) }
        o.put("chargingCurve",curve(p.chargingCurve))
        o.put("coldDerateTable",curve(p.coldDerateTable))
    }.toString()
    fun decode(raw:String?):VehicleProfile=runCatching {
        if(raw.isNullOrBlank()) return@runCatching VehicleProfile.defaults()
        val o=JSONObject(raw)
        val p=VehicleProfile(read(o.getJSONObject("usableKwh")),read(o.getJSONObject("sohPercent")),
            read(o.getJSONObject("massKg")),read(o.getJSONObject("extraLoadKg")),
            read(o.getJSONObject("cdA")),read(o.getJSONObject("crr")),
            read(o.getJSONObject("drivetrainEfficiency")),read(o.getJSONObject("regenEfficiency")),
            read(o.getJSONObject("maxRegenKw")),read(o.getJSONObject("maxDcKw")),
            read(o.getJSONObject("maxAcKw")),read(o.getJSONObject("auxBaseKw")),
            read(o.getJSONObject("hvacAutoKw")),read(o.getJSONObject("hvacHeatingKw")),
            read(o.getJSONObject("consumptionAdjust")),readCurve(o.getJSONObject("chargingCurve")),
            readCurve(o.getJSONObject("coldDerateTable")))
        require(p.validate().isEmpty());p
    }.getOrElse { VehicleProfile.defaults() }
}
class VehicleProfileStore(context:Context) {
    private val store=context.applicationContext.m8ProfileStore
    private val key=stringPreferencesKey("profile_json")
    val profile=store.data.map { ProfileCodec.decode(it[key]) }
    suspend fun save(p:VehicleProfile) {
        require(p.validate().isEmpty());store.edit { it[key]=ProfileCodec.encode(p) }
    }
    suspend fun reset(){ store.edit { it.remove(key) } }
}
