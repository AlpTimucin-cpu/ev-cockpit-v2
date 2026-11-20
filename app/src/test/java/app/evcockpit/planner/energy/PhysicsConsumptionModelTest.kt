package app.evcockpit.planner.energy

import app.evcockpit.planner.model.VehicleProfile
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhysicsConsumptionModelTest {
    private val m=PhysicsConsumptionModel()
    private val p=VehicleProfile.defaults()
    private fun sample(speed:Double,temp:Double=20.0,slope:Double=0.0)=
        DrivingSegment(100_000.0,speed,slope,temp)
    @Test fun speedMonotonicAndReasonableSanityBand() {
        val a=m.compute(sample(90.0),p,HvacMode.AUTO).kwh
        val b=m.compute(sample(110.0),p,HvacMode.AUTO).kwh
        val c=m.compute(sample(130.0),p,HvacMode.AUTO).kwh
        assertTrue(a < b && b < c)
        assertTrue("90 speed kWh/100km $a",a in 13.0..30.0)
        assertTrue("130 speed kWh/100km $c",c in 13.0..30.0)
    }
    @Test fun heatAndColdRaiseConsumption() {
        val warm=m.compute(sample(90.0,25.0),p,HvacMode.HEATING).kwh
        val cold=m.compute(sample(90.0,-5.0),p,HvacMode.HEATING).kwh
        assertTrue(cold>warm)
    }
    @Test fun climbingRaisesConsumptionAndDescentRegenIsCapped() {
        val flat=m.compute(sample(90.0),p,HvacMode.OFF)
        val up=m.compute(sample(90.0,20.0,0.07),p,HvacMode.OFF)
        val down=m.compute(sample(90.0,20.0,-0.15),p,HvacMode.OFF)
        assertTrue(up.kwh>flat.kwh)
        assertTrue(down.kwh<flat.kwh)
        assertTrue(down.tractionKw>=-p.maxRegenKw.value)
    }
    @Test fun stopEnergyAndNoDivisionByZero() {
        val stop=m.compute(DrivingSegment(0.0,0.0,0.0,20.0,3600.0),p,HvacMode.AUTO)
        assertEquals(p.auxBaseKw.value+p.hvacAutoKw.value,stop.kwh,0.0001)
        assertThrows(IllegalArgumentException::class.java) {
            m.compute(DrivingSegment(100.0,0.0,0.0,20.0),p,HvacMode.OFF)
        }
    }
    @Test fun uncertaintyIsSymmetricAssumptionNotConfidenceGuarantee() {
        val b=m.route(listOf(sample(90.0)),p,HvacMode.AUTO)
        assertTrue(b.optimisticKwh<b.centralKwh && b.centralKwh<b.pessimisticKwh)
        assertFalse(b.verified)
    }
    @Test fun unitsAndDeterminism() {
        val kmh=90.0
        assertEquals(25.0,kmh/3.6,0.001)
        val a=m.compute(sample(90.0),p,HvacMode.AUTO)
        assertEquals(a,m.compute(sample(90.0),p,HvacMode.AUTO))
        assertTrue(a.kwh.isFinite())
        assertTrue(abs(a.elapsedHours-(100.0/90.0))<0.0001)
    }
}
