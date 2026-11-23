package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class OfflineTurkishPlacesTest {
    private val sample = OfflineTurkishPlaces.fromDelimited(
        """
        # il|ilce|lat|lon
        Niğde|Niğde|37.969849|34.676449
        Kayseri|Melikgazi|38.731000|35.498000
        Kayseri|Kocasinan|38.745000|35.474000
        Antalya|Muratpaşa|36.879000|30.700000
        Isparta|Isparta|37.764000|30.557000
        missing|broken|nope|35
        """.trimIndent(),
    )

    @Test fun twoLettersProduceInstantOfflineSuggestions() {
        val list = sample.suggest("Ka")
        assertEquals(2, list.size)
        assertTrue(list.all { it.label.contains("Kayseri") })
        assertEquals(38.745, list.first { it.label.startsWith("Kocasinan") }.point.latitude, 0.0001)
    }

    @Test fun asciiAndTurkishLetterFormsMatchTheSameCity() {
        val a = sample.suggest("Nigde")
        val b = sample.suggest("Niğ")
        assertEquals(1, a.size)
        assertEquals(a, b)
        assertEquals(1, sample.suggest("ISPA").size)
        assertEquals(1, sample.suggest("ant").size)
    }

    @Test fun noInventedStreetOrInvalidCoordinates() {
        assertTrue(sample.suggest("istanbul sokak").isEmpty())
        assertTrue(sample.suggest("k").isEmpty())
        assertTrue(sample.suggest("ka", 0).isEmpty())
        assertTrue(sample.suggest("ka", 2).size <= 2)
        assertTrue(sample.suggest("no match").isEmpty())
    }

    @Test fun emptyOrMalformedInputNeverCreatesFakePlaces() {
        assertTrue(OfflineTurkishPlaces.fromDelimited("not|four").suggest("no").isEmpty())
        assertTrue(OfflineTurkishPlaces.empty().suggest("kayseri").isEmpty())
    }
}
