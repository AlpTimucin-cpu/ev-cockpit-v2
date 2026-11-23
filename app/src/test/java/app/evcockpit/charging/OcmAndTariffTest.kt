package app.evcockpit.charging

import app.evcockpit.charging.ocm.*
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.GeoPoint
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class OcmAndTariffTest {
    private val sample = """
        [{
            "ID": 18945,
            "AddressInfo": { "Title": "Örnek Şarj", "Latitude": 37.969,
                "Longitude": 34.671 },
            "OperatorInfo": { "Title": "ZES" },
            "StatusType": { "Title": "Operational" },
            "Connections": [
                { "ConnectionType": {"ID":33,"Title":"CCS (Type 2)"},
                  "CurrentType":{"Title":"DC"},
                  "PowerKW":180.0 },
                { "ConnectionType":{"ID":25,"Title":"Type 2"},
                  "CurrentType":{"Title":"AC"},"PowerKW":22.0 }
            ]
        },{
            "ID": 18946,
            "AddressInfo":{"Title":"Gücü Bilinmeyen","Latitude":38.0,"Longitude":34.6},
            "OperatorInfo":{"Title":"Bilinmiyor"},
            "Connections":[{"ConnectionType":{"Title":"Type 2"}}]
        }]
    """.trimIndent()

    @Test fun ocmKonektorGucuVeTurleriUydurulmaz() {
        val list = OcmParser.parse(sample)
        assertEquals(2, list.size)
        assertTrue(list[0].hasCcs2)
        assertTrue(list[0].hasDc)
        assertTrue(list[0].hasAc)
        assertEquals(180.0, list[0].powerKw!!, 0.001)
        assertEquals("ULTRA DC", list[0].powerGroup)
        assertNull(list[1].powerKw)
        assertEquals("GÜÇ BİLİNMİYOR", list[1].powerGroup)
    }

    @Test fun bolgeDisiKayitVeOlmayanRotaSahteSonucaDonusmez() {
        assertTrue(OcmParser.parse("""[{"ID":99,"AddressInfo":
            {"Title":"Test","Latitude":52.0,"Longitude":13.0}}]""").isEmpty())
        val route = listOf(GeoPoint(37.96,34.67),GeoPoint(37.9,34.8))
        val sampleStation = OcmParser.parse(sample)[0]
        assertNotNull(OcmCorridor.project(sampleStation, route))
        assertNull(OcmCorridor.project(sampleStation.copy(
            point = GeoPoint(40.0, 30.0)), route))
    }

    @Test fun uzunRotaOrnekleriSinirlanirVeEksikBayragiVerilir() {
        val geometry = (0..50).map { GeoPoint(36.5 + it * 0.06, 29.5 + it * 0.2) }
        val plan = OcmCorridor.sample(geometry)
        assertTrue(plan.points.size <= OcmCorridor.MAX_CALLS)
        assertTrue(plan.incomplete)
        assertEquals(geometry.first(), plan.points.first())
        assertEquals(geometry.last(), plan.points.last())
    }

    private val json = """
        {"schema_version":1,"operators":[
          {"id":"zes","operator":"ZES","aliases":["zes"],
           "ac_tl_kwh":null,"dc_tl_kwh":null,"updated_at":null,"source_url":null},
          {"id":"trugo","operator":"Trugo","aliases":["trugo"],
           "ac_tl_kwh":9.25,"dc_tl_kwh":11.50,"updated_at":"2026-11-21",
           "source_url":"https://example.org/price"}
        ]}
    """.trimIndent()

    @Test fun katalogFiyatBosIseAslaSifirFiyatDemez() {
        val catalog = TariffCatalogCodec.parse(json)
        val unknown = TariffResolver.resolve(catalog, emptyMap(), "ZES",
            ChargingKind.DC, LocalDate.parse("2026-11-23"))
        assertNull(unknown.priceTlPerKwh)
        assertEquals("Fiyat bilinmiyor", unknown.source)
        assertNull(TariffResolver.resolve(catalog, emptyMap(), "Trugo",
            ChargingKind.UNKNOWN).priceTlPerKwh)
    }

    @Test fun manuelAcDcAyrilirVeEskiFiyatUyariVerir() {
        val catalog = TariffCatalogCodec.parse(json)
        val manual = mapOf("zes" to ManualPrice(ac = 7.25, dc = 12.0,
            updatedAt = "2026-11-01"))
        val ac = TariffResolver.resolve(catalog, manual, "ZES", ChargingKind.AC,
            LocalDate.parse("2026-11-23"))
        val dc = TariffResolver.resolve(catalog, manual, "ZES", ChargingKind.DC,
            LocalDate.parse("2026-11-23"))
        assertEquals(7.25, ac.priceTlPerKwh!!, 0.01)
        assertEquals(12.0, dc.priceTlPerKwh!!, 0.01)
        assertTrue(dc.stale)
    }

    @Test fun fiyatIcinKaynakVeTarihSarttir() {
        val invalid = json.replace("\"updated_at\":\"2026-11-21\"",
            "\"updated_at\":null")
        assertThrows(IllegalArgumentException::class.java) {
            TariffCatalogCodec.parse(invalid)
        }
    }
}
