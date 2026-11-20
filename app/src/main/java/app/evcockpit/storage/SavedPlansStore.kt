package app.evcockpit.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.planner.model.PlanResult
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val Context.m8SavedPlansStore by preferencesDataStore("m8_saved_plans_v1")

data class StoredChargeStop(
    val id:String,val name:String,val operator:String,val point:GeoPoint,
    val arrivalSoc:Int,val departureSoc:Int,val chargeMinutes:Double,
)
data class StoredPlan(
    val id:Long,val title:String,val destination:GeoPoint,
    val stops:List<StoredChargeStop>,val arrivalSoc:Int,
    val optimisticSoc:Double,val pessimisticSoc:Double,
    val physicalMinutes:Double,val distanceKm:Double,
    val verified:Boolean=false,
)
object SavedPlanBuilder {
    fun from(result:PlanResult.Success,route:NavigationRoute,id:Long):StoredPlan {
        require(route.geometry.size>=2)
        return StoredPlan(id,"EV Rota Planı — TAHMİN",route.geometry.last(),
            result.stops.map { s->
                StoredChargeStop(s.charger.id,s.charger.name.take(100),
                    s.charger.operator.take(100),
                    GeoPoint(s.charger.latitude,s.charger.longitude),
                    s.arrivalSocPct,s.departureSocPct,s.chargeMinutes)
            },
            result.arrivalSocPct,result.optimisticArrivalSocPct,
            result.pessimisticArrivalSocPct,result.physicalMinutes,result.distanceKm)
    }
    /** Coordinate-free export, deliberately excludes untrusted station and user labels. */
    fun shareText(p:StoredPlan):String=
        "EV Rota Planlayıcı — TAHMİN / DOĞRULANMADI\n"+
        "Mesafe: "+("%.1f".format(Locale.US,p.distanceKm))+" km\n"+
        "Tahmini toplam: "+("%.0f".format(Locale.US,p.physicalMinutes))+" dk\n"+
        "Durak: "+p.stops.size+"\n"+
        "Varış SoC: ~%"+p.arrivalSoc+" (iyimser ~%"+
        ("%.0f".format(Locale.US,p.optimisticSoc))+", kötümser ~%"+
        ("%.0f".format(Locale.US,p.pessimisticSoc))+")\n"+
        "Canlı trafik, doluluk ve fiyat bilinmiyor. Araç/telefon sahada DOĞRULANMADI."
}
object SavedPlanCodec {
    fun encode(plans:List<StoredPlan>):String=JSONArray().also{a->
        plans.take(10).forEach{p->
            val stops=JSONArray()
            p.stops.take(8).forEach{s->
                stops.put(JSONObject().put("id",s.id).put("name",s.name)
                    .put("operator",s.operator).put("lat",s.point.latitude)
                    .put("lon",s.point.longitude).put("arrival",s.arrivalSoc)
                    .put("departure",s.departureSoc).put("minutes",s.chargeMinutes))
            }
            a.put(JSONObject().put("id",p.id).put("title",p.title)
                .put("destinationLat",p.destination.latitude)
                .put("destinationLon",p.destination.longitude)
                .put("stops",stops).put("arrival",p.arrivalSoc)
                .put("optimistic",p.optimisticSoc).put("pessimistic",p.pessimisticSoc)
                .put("minutes",p.physicalMinutes).put("km",p.distanceKm))
        }
    }.toString()
    fun decode(raw:String?):List<StoredPlan>=runCatching {
        val a=JSONArray(raw?:"[]")
        (0 until minOf(a.length(),10)).mapNotNull{i->
            runCatching{
                val o=a.getJSONObject(i);val s=o.getJSONArray("stops")
                val stops=(0 until minOf(s.length(),8)).map{j->
                    val c=s.getJSONObject(j)
                    StoredChargeStop(c.getString("id"),c.getString("name"),
                        c.getString("operator"),
                        GeoPoint(c.getDouble("lat"),c.getDouble("lon")),
                        c.getInt("arrival"),c.getInt("departure"),c.getDouble("minutes"))
                }
                StoredPlan(o.getLong("id"),o.getString("title"),
                    GeoPoint(o.getDouble("destinationLat"),o.getDouble("destinationLon")),
                    stops,o.getInt("arrival"),o.getDouble("optimistic"),
                    o.getDouble("pessimistic"),o.getDouble("minutes"),o.getDouble("km"))
            }.getOrNull()
        }
    }.getOrElse{emptyList()}
    fun add(old:List<StoredPlan>,new:StoredPlan):List<StoredPlan>=
        (listOf(new)+old.filterNot{it.id==new.id}).take(10)
}
class SavedPlansStore(context:Context) {
    private val store=context.applicationContext.m8SavedPlansStore
    private val key=stringPreferencesKey("plans_json")
    val plans=store.data.map{SavedPlanCodec.decode(it[key])}
    suspend fun save(plan:StoredPlan) {
        require(!plan.verified)
        store.edit{it[key]=SavedPlanCodec.encode(
            SavedPlanCodec.add(SavedPlanCodec.decode(it[key]),plan))}
    }
    suspend fun delete(id:Long) {
        store.edit{it[key]=SavedPlanCodec.encode(
            SavedPlanCodec.decode(it[key]).filterNot{p->p.id==id})}
    }
}
