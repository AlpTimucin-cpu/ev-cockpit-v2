package app.evcockpit.planner.data

import app.evcockpit.planner.route.ElevationClient
import app.evcockpit.planner.route.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Called only for user-requested planning; no background/polling calls. */
class OpenMeteoElevationClient:ElevationClient {
    override suspend fun elevations(points:List<RoutePoint>):List<Double> = withContext(Dispatchers.IO) {
        require(points.isNotEmpty() && points.size<=4000)
        // Conservative cap: 3 public elevation requests per user plan.
        val sparse=if(points.size<=300)points else (0 until 300).map { index ->
            points[(index*(points.size-1)/299)]
        }
        val result=mutableListOf<Double>()
        for(batch in sparse.chunked(100)) {
            val lat=batch.joinToString(","){it.lat.toString()}
            val lon=batch.joinToString(","){it.lon.toString()}
            val url=URL("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lon")
            val c=url.openConnection() as HttpURLConnection
            c.connectTimeout=8000;c.readTimeout=8000;c.instanceFollowRedirects=false
            try {
                if(c.responseCode!=200)throw IllegalStateException("Yükseklik servisi erişilemedi; kota/ağ olabilir")
                val o=JSONObject(c.inputStream.bufferedReader().use { it.readText().take(65536) })
                val a=o.getJSONArray("elevation")
                require(a.length()==batch.size)
                for(i in 0 until a.length())result+=a.getDouble(i)
            } finally {c.disconnect()}
        }
        if(sparse.size==points.size)return@withContext result
        (points.indices).map { idx ->
            val position=idx.toDouble()*(sparse.size-1)/(points.size-1)
            val low=position.toInt().coerceAtMost(result.lastIndex)
            val high=(low+1).coerceAtMost(result.lastIndex)
            val f=position-low
            result[low]+(result[high]-result[low])*f
        }
    }
}
