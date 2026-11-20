package app.evcockpit.planner.data

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.route.RouteDistance
import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.SourcedValue
import org.json.JSONObject

object OverpassParser {
    private val powerRegex=Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kW|W)\b""",RegexOption.IGNORE_CASE)
    fun kw(raw:String):Double? {
        val match=powerRegex.find(raw)?:return null
        val number=match.groupValues[1].replace(',','.').toDoubleOrNull()?:return null
        val units=match.groupValues[2]
        val kw=if(units.equals("W",true))number/1000.0 else number
        return kw.takeIf { it>0 && it<=500.0 }
    }
    fun parse(raw:String):List<ChargerCandidate> {
        val objects=JSONObject(raw).getJSONArray("elements")
        return (0 until objects.length()).mapNotNull { i ->
            val o=objects.getJSONObject(i)
            val tags=o.optJSONObject("tags")?:return@mapNotNull null
            if(tags.optString("amenity")!="charging_station")return@mapNotNull null
            val center=if(o.optString("type")=="node")o else o.optJSONObject("center")
                ?:return@mapNotNull null
            if(!center.has("lat")||!center.has("lon"))return@mapNotNull null
            val lat=center.getDouble("lat");val lon=center.getDouble("lon")
            if(!lat.isFinite()||!lon.isFinite() || lat !in -90.0..90.0 ||
                lon !in -180.0..180.0)return@mapNotNull null
            val combo=tags.optString("socket:type2_combo").trim().lowercase()
            val isCombo=combo=="yes" || combo.toIntOrNull()?.let { it>0 }==true
            val operator=tags.optString("operator").ifBlank {
                tags.optString("network").ifBlank { tags.optString("brand","bilinmiyor") }
            }
            val info=OperatorHeuristics.source(operator)
            val confidence=if(isCombo)ChargerConfidence.OSM_TAGGED_CCS2
                else if(info!=null)ChargerConfidence.PROBABLE_CCS2
                else ChargerConfidence.UNKNOWN
            val output=kw(tags.optString("socket:type2_combo:output"))
                ?:kw(tags.optString("socket:type2_combo:output:power"))
            val power=when {
                output!=null -> SourcedValue(output,"OSM socket:type2_combo:output",false,
                    "OSM etiket gücü; canlı/gerçek doğrulanmadı")
                info!=null -> SourcedValue(60.0,info,false,
                    "Operatörün bazı DC istasyonları var; bu istasyon gücü TAHMİN")
                else -> SourcedValue(11.0,"M8 konservatif varsayım",false,
                    "Soket/güç bilinmiyor — TAHMİN, AC olabilir")
            }
            val auth=tags.keys().asSequence().filter { it.startsWith("authentication:") }
                .map { it+"="+tags.optString(it) }.take(5).joinToString(", ")
            val capacity=tags.optInt("capacity",-1).takeIf { it>=0 }
            ChargerCandidate(o.optString("type")+":"+o.optLong("id"),tags.optString("name").ifBlank {
                operator+" şarj noktası"
            }.take(100),operator.take(100),lat,lon,power,confidence,
                access=tags.optString("access","bilinmiyor"),fee=tags.optString("fee","bilinmiyor"),
                openingHours=tags.optString("opening_hours","bilinmiyor"),
                authentication=auth.ifBlank { "bilinmiyor" },capacity=capacity,
                chademo=tags.optString("socket:chademo","bilinmiyor"),
                type2=tags.optString("socket:type2","bilinmiyor"))
        }
    }
    fun dedupe(candidates:List<ChargerCandidate>,radiusMeters:Double=75.0):List<ChargerCandidate> {
        require(radiusMeters>0)
        val result=mutableListOf<ChargerCandidate>()
        for(item in candidates) {
            val index=result.indexOfFirst { prev->
                RouteDistance.haversine(GeoPoint(item.latitude,item.longitude),
                    GeoPoint(prev.latitude,prev.longitude))<radiusMeters
            }
            if(index<0)result+=item
            else {
                val prev=result[index]
                if(rank(item)>rank(prev) || (rank(item)==rank(prev) &&
                    item.powerKw.value>prev.powerKw.value))result[index]=item
            }
        }
        return result
    }
    private fun rank(c:ChargerCandidate):Int=when(c.confidence) {
        ChargerConfidence.OSM_TAGGED_CCS2->3
        ChargerConfidence.PROBABLE_CCS2->2
        ChargerConfidence.UNKNOWN->1
    }
}
