package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DashboardMathTest {
    @Test fun socModel52KwhOrnekHesap() {
        // 80% - (10 / 52)*100 = %60,769...
        assertEquals(60.769230769, DashboardMath.estimatedSocPercent(
            80.0, 10.0, 52.0)!!, 0.00001)
    }

    @Test fun socKapasiteProfilParametresindenGelir() {
        val at52 = DashboardMath.estimatedSocPercent(80.0, 10.0, 52.0)!!
        val at45 = DashboardMath.estimatedSocPercent(80.0, 10.0, 45.0)!!
        assertTrue(at45 < at52)
        assertEquals(57.77777777, at45, 0.0001)
    }

    @Test fun socOlcumYoksaRastgeleAzalmaz() {
        assertNull(DashboardMath.estimatedSocPercent(80.0, null, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(null, 4.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, 4.0, null))
    }

    @Test fun socGecersizKapasiteVeEnerjiReddedilir() {
        assertNull(DashboardMath.estimatedSocPercent(80.0, 10.0, 0.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, 10.0, -52.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, -2.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(101.0, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(-1.0, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(Double.NaN, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(10.0, 20.0, 52.0))
    }

    @Test fun socSinirDegerleriGercekOlcumOlabilir() {
        assertEquals(0.0, DashboardMath.estimatedSocPercent(0.0, 0.0, 52.0)!!, 0.001)
        assertEquals(100.0, DashboardMath.estimatedSocPercent(
            100.0, 0.0, 52.0)!!, 0.001)
        assertEquals(50.0, DashboardMath.estimatedSocPercent(
            75.0, 13.0, 52.0)!!, 0.001)
    }

    @Test fun menzilDogruBirimdeHesaplanir() {
        // 80% x 52 kWh = 41.6 kWh; /16kWh/100km = 260 km.
        assertEquals(260.0, DashboardMath.estimatedRangeKm(
            80.0, 52.0, 16.0)!!, 0.00001)
    }

    @Test fun menzilSifirNegatifNullVeSonsuzTuketimReddedilir() {
        assertNull(DashboardMath.estimatedRangeKm(null, 52.0, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, 0.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, -4.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, Double.NaN))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, Double.POSITIVE_INFINITY))
        assertNull(DashboardMath.estimatedRangeKm(50.0, null, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, -52.0, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(200.0, 52.0, 16.0))
    }

    @Test fun menzilSinirDegerleri() {
        assertEquals(0.0, DashboardMath.estimatedRangeKm(
            0.0, 52.0, 16.0)!!, 0.001)
        assertEquals(520.0, DashboardMath.estimatedRangeKm(
            100.0, 52.0, 10.0)!!, 0.001)
    }

    @Test fun gpsHizMpsKmSaDonusumu() {
        assertEquals(90.0, DashboardMath.speedKmhFromGpsMetersPerSecond(25.0)!!, 0.001)
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(null))
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(-1.0))
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(Double.NaN))
    }

    @Test fun kismiFiyatKesinToplamDegildir() {
        assertNull(ChargeCostSummary.label(listOf(PricedStop(null), PricedStop(null))))
        assertNull(ChargeCostSummary.label(emptyList()))
        val partial = ChargeCostSummary.label(
            listOf(PricedStop(120.0), PricedStop(null), PricedStop(55.0)))!!
        assertTrue(partial.startsWith("en az "))
        assertTrue(partial.contains("1 durakta fiyat yok"))
        assertTrue(partial.contains("175"))
        val full = ChargeCostSummary.label(listOf(PricedStop(120.0), PricedStop(55.0)))!!
        assertFalse(full.startsWith("en az "))
        assertTrue(full.contains("tahmini"))
    }

    @Test fun kaynagiVeZamaniOlmayanDegerSifirOlmaz() {
        val raw = StampedValue(82.0, DashboardOrigin.GPS, timestampEpochMs = null)
        assertNull(raw.freshValue(1000L, 100L))
        val old = StampedValue(82.0, DashboardOrigin.GPS, timestampEpochMs = 1000L)
        assertNull(old.freshValue(3000L, 100L))
        assertEquals(82.0, old.freshValue(1050L, 100L)!!, 0.001)
    }
}
