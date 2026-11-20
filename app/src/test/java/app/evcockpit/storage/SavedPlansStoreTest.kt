package app.evcockpit.storage

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.planner.model.*
import app.evcockpit.planner.charging.*
import app.evcockpit.planner.integration.PlanToNavigation
import org.junit.Assert.*
import org.junit.Test

class SavedPlansStoreTest {
    private val station=ChargerCandidate("c","s","provider",38.123456,35.654321,
        SourcedValue(100.0,"fixture",false),ChargerConfidence.UNKNOWN)
    private fun sample(id:Long):StoredPlan {
        val r=PlanResult.Success(listOf(PlannedStop(station,20,80,20.0,4.0,25.0)),
            13,20.0,7.0,300.0,150.0,20.0,4.0,25.0,174.0,199.0,emptyList())
        val route=NavigationRoute(listOf(GeoPoint(37.0,34.0),GeoPoint(39.0,30.0)),
            300_000.0,10_800.0,emptyList())
        return SavedPlanBuilder.from(r,route,id)
    }
    @Test fun roundTripPersistsOrderedStopsOnlyLocally() {
        val input=sample(100)
        assertEquals(input,SavedPlanCodec.decode(SavedPlanCodec.encode(listOf(input))).single())
    }
    @Test fun retainsOnlyTenNewestAndDeduplicates() {
        val plans=(1L..12L).fold(emptyList<StoredPlan>()){all,i->
            SavedPlanCodec.add(all,sample(i))
        }
        assertEquals(10,plans.size)
        assertEquals(12L,plans.first().id)
        assertEquals(3L,plans.last().id)
    }
    @Test fun shareNeverIncludesGpsVinOrApiKey() {
        val text=SavedPlanBuilder.shareText(sample(1))
        assertTrue(text.contains("TAHMİN"))
        assertFalse(text.contains("38.123456"))
        assertFalse(text.contains("35.654321"))
        assertFalse(text.contains("39.0,30.0"))
        assertFalse(text.contains("VIN"))
        assertFalse(text.contains("Authorization"))
    }
    @Test fun navigationStopsKeepOrderAndExcludeDestination() {
        val stop=sample(9).stops.single().point
        assertEquals(GeoPoint(38.123456,35.654321),stop)
    }
}
