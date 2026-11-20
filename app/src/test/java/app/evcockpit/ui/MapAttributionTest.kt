package app.evcockpit.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class MapAttributionTest {
    @Test fun alwaysVisibleLabelCreditsBothProviders() {
        assertTrue(MapAttribution.LABEL.contains("© OpenStreetMap katkıcıları"))
        assertTrue(MapAttribution.LABEL.contains("OpenFreeMap"))
        assertTrue(MapAttribution.COPYRIGHT_URL.startsWith("https://"))
    }
}
