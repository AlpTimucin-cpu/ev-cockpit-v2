package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.SourcedValue
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.m8CustomChargeStore by preferencesDataStore("m8_user_stations")
object CustomChargerCodec {
    fun encode(stations:List<ChargerCandidate>)=JSONArray().also { arr ->
        stations.take(30).forEach {
            arr.put(JSONObject().put("id",it.id).put("name",it.name)
                .put("lat",it.latitude).put("lon",it.longitude)
                .put("kw",it.powerKw.value))
        }
    }.toString()
    fun decode(raw:String?):List<ChargerCandidate>=runCatching {
        val a=JSONArray(raw ?: "[]")
        (0 until minOf(30,a.length())).map { index ->
            val o=a.getJSONObject(index)
            ChargerCandidate(o.getString("id"),o.getString("name"),"KULLANICI",
                o.getDouble("lat"),o.getDouble("lon"),
                SourcedValue(o.getDouble("kw"),"KULLANICI",false,"Özel istasyon — DOĞRULANMADI"),
                ChargerConfidence.UNKNOWN)
        }
    }.getOrElse{emptyList()}
}
class CustomChargerStore(context:Context) {
    private val data=context.applicationContext.m8CustomChargeStore
    private val key=stringPreferencesKey("stations")
    val stations=data.data.map { CustomChargerCodec.decode(it[key]) }
    suspend fun save(s:ChargerCandidate) {
        require(s.powerKw.value>0 && s.latitude in -90.0..90.0 && s.longitude in -180.0..180.0)
        data.edit { it[key]=CustomChargerCodec.encode(
            CustomChargerCodec.decode(it[key]).filterNot { old->old.id==s.id }+s) }
    }
    suspend fun clear() { data.edit { it.remove(key) } }
}
