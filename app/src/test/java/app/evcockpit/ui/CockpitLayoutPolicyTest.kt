package app.evcockpit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CockpitLayoutPolicyTest {
    @Test fun handsetLandscapeUsesTwoColumns() {
        assertFalse(CockpitLayoutPolicy.useTwoColumns(360))
        assertFalse(CockpitLayoutPolicy.useTwoColumns(599))
        assertTrue(CockpitLayoutPolicy.useTwoColumns(600))
        assertTrue(CockpitLayoutPolicy.useTwoColumns(800))
    }

    @Test fun lowHeightEnablesCompactReadouts() {
        assertTrue(CockpitLayoutPolicy.compactDashboard(310))
        assertFalse(CockpitLayoutPolicy.compactDashboard(340))
        assertFalse(CockpitLayoutPolicy.compactDashboard(500))
    }

    @Test fun speedNeverInventedForMissingOrInvalidReadings() {
        assertEquals("--", CockpitLayoutPolicy.speedLabel(null))
        assertEquals("--", CockpitLayoutPolicy.speedLabel(Double.NaN))
        assertEquals("--", CockpitLayoutPolicy.speedLabel(Double.POSITIVE_INFINITY))
        assertEquals("--", CockpitLayoutPolicy.speedLabel(-4.0))
        assertEquals("90", CockpitLayoutPolicy.speedLabel(89.7))
    }

    @Test fun existingTabIndicesStayStable() {
        assertEquals(7, CockpitLayoutPolicy.tabLabels.size)
        assertEquals("Gösterge", CockpitLayoutPolicy.tabLabels[0])
        assertEquals("OBD Test", CockpitLayoutPolicy.tabLabels[1])
        assertEquals("Rota", CockpitLayoutPolicy.tabLabels[2])
        assertEquals("Plan", CockpitLayoutPolicy.tabLabels[3])
        assertEquals("Yolcu video", CockpitLayoutPolicy.tabLabels[4])
        assertEquals("Ayarlar", CockpitLayoutPolicy.tabLabels[5])
        assertEquals("Radyo / IPTV", CockpitLayoutPolicy.tabLabels[6])
    }
}
