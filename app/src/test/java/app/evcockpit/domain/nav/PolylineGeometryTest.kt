package app.evcockpit.domain.nav

import org.junit.Assert.*
import org.junit.Test

class PolylineGeometryTest {
    @Test fun duzYolAraNoktaSilinirBasVeSonKorunur() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.0005, 34.0005),
            WgsPoint(39.0010, 34.0010),
        )
        val simplified = PolylineGeometry.simplify(path, 10.0)
        assertEquals(2, simplified.size)
        assertEquals(path.first(), simplified.first())
        assertEquals(path.last(), simplified.last())
    }

    @Test fun sertDonusToleransIcindeKaybolmaz() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.01, 34.0),
            WgsPoint(39.01, 34.01),
        )
        val reduced = PolylineGeometry.simplify(path, 20.0)
        assertEquals(3, reduced.size)
        assertEquals(path[1], reduced[1])
    }

    @Test fun ufakVeyaGecersizToleranstaOrijinalKorunur() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.001, 34.002),
            WgsPoint(39.004, 34.005),
        )
        assertEquals(path, PolylineGeometry.simplify(path, 0.0))
        assertEquals(path, PolylineGeometry.simplify(path, -1.0))
        assertEquals(path, PolylineGeometry.simplify(path, Double.NaN))
    }

    @Test fun kuzeyAcisi359Ve1Arasinda0aYakinKalir() {
        val heading = PolylineGeometry.circularHeadingDegrees(359.0, 1.0, 0.5)!!
        assertTrue("Dairesel ortalama kuzeye yakın olmalı: $heading",
            heading < 2.0 || heading > 358.0)
    }

    @Test fun gecersizHeadingUygulanmaz() {
        assertNull(PolylineGeometry.circularHeadingDegrees(80.0, Double.NaN, 0.5))
        assertNull(PolylineGeometry.circularHeadingDegrees(80.0, 20.0, -0.5))
        assertEquals(270.0,
            PolylineGeometry.circularHeadingDegrees(null, -90.0, 0.5)!!, 0.0001)
    }
}
