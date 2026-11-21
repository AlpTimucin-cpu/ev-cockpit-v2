package app.evcockpit.planner.charging

import app.evcockpit.planner.model.*
import app.evcockpit.planner.solver.CandidatePruner
import org.junit.Assert.*
import org.junit.Test

class PreferredNetworksTest {
    @Test fun normalizeTurkishInputAndIgnoreBlankChoices() {
        val prefs = PreferredNetworks.parse(" EŞARJ , ZES, , VOLTRUN ")
        assertTrue(PreferredNetworks.matches("eşarj", prefs))
        assertTrue(PreferredNetworks.matches("Esarj", prefs))
        assertFalse(PreferredNetworks.matches("Aksa Şarj", prefs))
        assertEquals(3, prefs.size)
    }

    @Test fun preferredChoiceSurvivesBoundedSixPerWindowPruning() {
        val base = (1..8).map {
            ChargerCandidate("regular-$it","Test","Diğer",38.0,35.0,
                SourcedValue(150.0,"SENTETİK",false),
                ChargerConfidence.OSM_TAGGED_CCS2,
                distanceAlongMeters=10_000.0 + it)
        }
        val preferred = ChargerCandidate("fav","Test","Eşarj",38.0,35.0,
            SourcedValue(120.0,"SENTETİK",false),
            ChargerConfidence.OSM_TAGGED_CCS2,
            distanceAlongMeters=10_050.0)
        val result = CandidatePruner().prune(base + preferred,120_000.0,
            PreferredNetworks.parse("Esarj"))
        assertEquals(6, result.size)
        assertTrue(result.any { it.id == "fav" })
    }

    @Test fun privateOrUnknownChargerDoesNotBecomeVerifiedByPreference() {
        val source = OperatorHeuristics.source("voltrun")
        assertNotNull(source)
        val preference = PreferredNetworks.parse("Voltrun")
        assertTrue(PreferredNetworks.matches("voltrun", preference))
        val unknown = ChargerCandidate("unknown","Test","Voltrun",38.0,35.0,
            SourcedValue(70.0,"OSM / DOĞRULANMADI",false),
            ChargerConfidence.UNKNOWN)
        assertFalse(unknown.powerKw.verified)
        assertEquals(ChargerConfidence.UNKNOWN, unknown.confidence)
    }
}
