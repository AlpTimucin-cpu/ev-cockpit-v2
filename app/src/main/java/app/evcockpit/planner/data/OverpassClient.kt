package app.evcockpit.planner.data

import app.evcockpit.planner.route.RoutePoint
import app.evcockpit.planner.charging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class OverpassResponse(val status:Int,val body:String,val retryAfterSeconds:Int?=null)
interface OverpassTransport { suspend fun post(query:String):OverpassResponse }
class PublicOverpassTransport:OverpassTransport {
    override suspend fun post(query:String):OverpassResponse=withContext(Dispatchers.IO) {
        val connection=URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
        try {
            connection.requestMethod="POST"
            connection.doOutput=true;connection.instanceFollowRedirects=false
            connection.connectTimeout=10_000;connection.readTimeout=30_000
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent",
                "EV-Rota-Planlayici/0.3 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
            val form="data="+URLEncoder.encode(query,"UTF-8")
            connection.outputStream.use {it.write(form.toByteArray(Charsets.UTF_8))}
            val status=connection.responseCode
            val retry=connection.getHeaderField("Retry-After")?.toIntOrNull()
            val body=if(status==200)connection.inputStream.bufferedReader().use {
                it.readText().take(2_000_000)
            } else ""
            OverpassResponse(status,body,retry)
        }finally{connection.disconnect()}
    }
}
class ChargerCache(
    private val directory:File,
    private val nowMillis:()->Long={System.currentTimeMillis()},
    private val ttlMillis:Long=7L*24*60*60*1000,
) {
    data class Entry(val body:String,val stale:Boolean)
    private fun fileFor(query:String):File {
        val digest=MessageDigest.getInstance("SHA-256")
            .digest(query.toByteArray()).joinToString(""){"%02x".format(it)}
        return File(directory,"overpass_"+digest+".json")
    }
    fun read(query:String):Entry?=runCatching {
        val f=fileFor(query);if(!f.isFile || f.length()>2_100_000)return@runCatching null
        val text=f.readText()
        val pos=text.indexOf('\n');if(pos<1)return@runCatching null
        val time=text.substring(0,pos).toLongOrNull()?:return@runCatching null
        Entry(text.substring(pos+1),nowMillis()-time>ttlMillis)
    }.getOrNull()
    fun write(query:String,body:String) {
        require(body.length<=2_000_000)
        directory.mkdirs()
        fileFor(query).writeText(nowMillis().toString()+"\n"+body,Charsets.UTF_8)
    }
}
data class ChargerQueryResult(
    val chargers:List<ChargerCandidate>,val fromStaleCache:Boolean,val incomplete:Boolean,
    val status:String,
)
class OverpassRepository(
    private val transport:OverpassTransport,
    private val cache:ChargerCache,
    private val chunker:CorridorChunker=CorridorChunker(),
    private val sleep:suspend(Long)->Unit={delay(it)},
) {
    suspend fun search(route:List<RoutePoint>,radiusMeters:Int=3000,
        filter:ChargerFilter=ChargerFilter(),extra:List<ChargerCandidate> = emptyList()):ChargerQueryResult {
        val chunks=chunker.chunks(route)
        require(chunks.size<=16){"Çok uzun rota/koridor; alanı daraltıp yeniden dene"}
        val candidates=mutableListOf<ChargerCandidate>()
        var stale=false;var missing=false
        for((index,chunk) in chunks.withIndex()) {
            val q=chunker.query(chunk,radiusMeters)
            val cached=cache.read(q)
            var raw=cached?.takeIf { !it.stale }?.body
            if(raw==null) {
                if(index>0) sleep(1500)
                var obtained=false
                for(attempt in 0..2) {
                    val response=runCatching { transport.post(q) }.getOrNull()
                    if(response!=null && response.status==200) {
                        raw=response.body
                        cache.write(q,raw)
                        obtained=true
                        break
                    }
                    if(response?.status in listOf(429,504) && attempt<2) {
                        val secs=response?.retryAfterSeconds?.coerceIn(1,60)
                            ?: (30 * (1 shl attempt)).coerceAtMost(60)
                        sleep(secs*1000L)
                    } else break
                }
                if(!obtained) {
                    val staleEntry=cache.read(q)
                    raw=staleEntry?.body
                    stale=stale || staleEntry!=null
                    missing=missing || raw==null
                }
            }
            if(raw!=null) {
                try { candidates+=OverpassParser.parse(raw) }
                catch (_:Exception) { missing=true }
            }
        }
        val merged=OverpassParser.dedupe(candidates+extra).map { item ->
            val projection=RouteProjectionEstimator.project(item,route)
            item.copy(distanceAlongMeters=projection.distanceAlongMeters,
                detourKm=projection.detourKm,detourMinutes=projection.detourMinutes)
        }
        return ChargerQueryResult(ChargerFiltering.apply(merged,filter),
            stale,missing,when {
                missing->"ŞARJ VERİSİ EKSİK — plan sonucu DOĞRULANMADI"
                stale->"ÖNBELLEK — güncel olmayabilir"
                else->"OSM verisi — canlı doluluk/fiyat yok; DOĞRULANMADI"
            })
    }
}
