package app.evcockpit.planner.charging

import app.evcockpit.planner.model.*
import app.evcockpit.planner.solver.CandidatePruner
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ChargingTimeAndPrunerTest {
    private val car=VehicleProfile.defaults()
    private val calc=ChargingTimeCalculator()
    @Test fun constantCurveHasExactAnalyticalSolution() {
        val flat=car.copy(chargingCurve=SourcedCurve(
            listOf(CurvePoint(0.0,100.0),CurvePoint(100.0,100.0)),"test",false),
            coldDerateTable=SourcedCurve(listOf(CurvePoint(-20.0,1.0),
                CurvePoint(40.0,1.0)),"test",false))
        val answer=calc.minutes(flat,150.0,20.0,80.0,20.0)
        assertEquals(52.0*0.60/100.0*60.0,answer,1e-8)
    }
    @Test fun segmentedAnalyticAgreesWithMidpointNumericalIntegration() {
        val actual=calc.minutes(car,150.0,10.0,80.0,25.0)
        val step=0.01
        var approx=0.0;var soc=10.0
        while(soc<80.0-1e-8) {
            approx+=car.capacityAtSohKwh/100.0*step /
                minOf(150.0,car.maxDcKw.value,car.chargingCurve.at(soc+step/2))*60.0
            soc+=step
        }
        assertEquals(approx,actual,0.02)
    }
    @Test fun slowerStationAndColdBatteryTakeLonger() {
        val fast=calc.minutes(car,150.0,10.0,80.0,25.0)
        assertTrue(calc.minutes(car,50.0,10.0,80.0,25.0)>fast)
        assertTrue(calc.minutes(car,150.0,10.0,80.0,-5.0)>fast)
    }
    @Test fun malformedSocNeverProducesFakeTime() {
        assertThrows(IllegalArgumentException::class.java) {
            calc.minutes(car,100.0,80.0,10.0,20.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            calc.minutes(car,0.0,10.0,80.0,20.0)
        }
    }
    @Test fun smallDomainIsNotPruned() {
        val list=(1..8).map {
            ChargerCandidate("id$it","Test","Test",38.0,35.0,
                SourcedValue(20.0,"test"),ChargerConfidence.UNKNOWN,
                distanceAlongMeters=it*20000.0)
        }
        assertEquals(8,CandidatePruner().prune(list,300_000.0).size)
    }
    @Test fun largeDomainDeterministicallyPruned() {
        val list=(1..300).map {
            ChargerCandidate("id$it","Test","Test",38.0,35.0,
                SourcedValue(20.0+it%10,"test"),ChargerConfidence.UNKNOWN,
                distanceAlongMeters=it*3_000.0)
        }
        val pruner=CandidatePruner()
        val a=pruner.prune(list,1_000_000.0)
        // Updated M8.5e requirement: at most SIX candidates per 40 km window.
        assertTrue("Expected meaningful pruning", a.size < list.size)
        assertEquals(a,pruner.prune(list,1_000_000.0))
        val bins = a.groupBy { kotlin.math.floor(it.distanceAlongMeters / 40_000.0).toInt() }
        assertTrue("More than 6 candidates in a 40-km window",
            bins.values.all { it.size <= 6 })
        assertTrue(a.size <= 6 * bins.size)
    }
}
