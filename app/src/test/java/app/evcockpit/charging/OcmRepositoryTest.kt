package app.evcockpit.charging

import app.evcockpit.charging.ocm.*
import app.evcockpit.navigation.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OcmRepositoryTest {
    private val route = listOf(
        GeoPoint(37.96, 34.65),
        GeoPoint(37.90, 34.92),
        GeoPoint(37.84, 35.16),
    )

    @Test fun reddedilenAnahtarSonrasiIstekYiginiOlusmaz() = runBlocking {
        var calls = 0
        val transport = object : OcmTransport {
            override suspend fun nearby(
                point: GeoPoint, apiKey: String, radiusKm: Double,
            ): List<OcmStation> {
                calls++
                throw OcmAccessException("Kötü anahtar")
            }
        }
        val repo = OcmRepository(transport, delayCall = {})
        val result = repo.search(route, "YANLIS_ANAHTAR")
        assertEquals(1, calls)
        assertTrue(result.stations.isEmpty())
        assertFalse(result.complete)
        assertTrue(result.message.contains("anahtarı"))
    }

    @Test fun kotaDoluncaDigerBolgelereIstekGonderilmez() = runBlocking {
        var calls = 0
        val transport = object : OcmTransport {
            override suspend fun nearby(
                point: GeoPoint, apiKey: String, radiusKm: Double,
            ): List<OcmStation> {
                calls++
                throw OcmRateLimitException("Kota doldu")
            }
        }
        val result = OcmRepository(transport, delayCall = {}).search(route, "KEY")
        assertEquals(1, calls)
        assertFalse(result.complete)
        assertTrue(result.message.contains("kota"))
    }

    @Test fun kismiSonuclardaEksikBilgiAcikcaRaporlanir() = runBlocking {
        var calls = 0
        val station = OcmStation(
            id = 112, name = "Örnek", operator = "ZES",
            point = GeoPoint(37.95, 34.66), powerKw = 100.0,
            hasAc = false, hasDc = true, hasCcs2 = true,
            connectors = listOf("CCS2"), access = "bilinmiyor", reportedStatus = null,
        )
        val transport = object : OcmTransport {
            override suspend fun nearby(
                point: GeoPoint, apiKey: String, radiusKm: Double,
            ): List<OcmStation> {
                calls++
                if (calls == 2) throw IllegalStateException("Ağ kesildi")
                return listOf(station)
            }
        }
        val result = OcmRepository(transport, delayCall = {}).search(route, "KEY")
        assertTrue(calls >= 2)
        assertEquals(1, result.stations.size)
        assertFalse(result.complete)
        assertTrue(result.message.contains("KISMİ"))
    }
}
