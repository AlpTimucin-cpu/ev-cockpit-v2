package app.evcockpit.planui

import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.PlannerOptionsCodec
import org.junit.Assert.*
import org.junit.Test

class PlannerOptionsTest {
    @Test fun defaultOptionsAreValid() {
        assertTrue(PlannerOptions().errors().isEmpty())
    }
    @Test fun settingsCodecRoundTrip() {
        val o=PlannerOptions(minArrivalSoc=12.0,temperatureC=-5.0,
            hvac=HvacMode.HEATING,onlyTaggedCcs2=true,
            excludedNetworks="networkX",destinationChargeKw=22.0)
        assertEquals(o,PlannerOptionsCodec.decode(PlannerOptionsCodec.encode(o)))
    }
    @Test fun invalidOrMalformedFailsClosed() {
        assertEquals(PlannerOptions(),PlannerOptionsCodec.decode("{"))
        assertEquals(PlannerOptions(),PlannerOptionsCodec.decode("""{"arrival":-100}"""))
        assertTrue(PlannerOptions(maxCruiseKmh=Double.NaN).errors().isNotEmpty())
        assertTrue(PlannerOptions(searchRadiusMeters=20000).errors().isNotEmpty())
    }
    @Test fun missingRouteSocAndDemoHaveActionableErrors() {
        assertTrue(PlannerGuard.reason(false,65.0,"MANUEL",PlannerOptions())!!.contains("Rota"))
        assertTrue(PlannerGuard.reason(true,null,"yok",PlannerOptions())!!.contains("SoC"))
        assertTrue(PlannerGuard.reason(true,50.0,"DEMO",PlannerOptions())!!.contains("DEMO"))
        assertNull(PlannerGuard.reason(true,50.0,"MANUEL",PlannerOptions()))
        assertTrue(PlannerGuard.label(PlannerStage.STATIONS).contains("Overpass"))
    }
    @Test fun userOverridesRemainUnverified() {
        val p=PlannerOptions(extraLoadKg=50.0,consumptionAdjust=0.12)
            .applyTo(VehicleProfile.defaults())
        assertEquals(50.0,p.extraLoadKg.value,0.0001)
        assertFalse(p.extraLoadKg.verified)
        assertEquals(0.12,p.consumptionAdjust.value,0.0001)
    }
}
