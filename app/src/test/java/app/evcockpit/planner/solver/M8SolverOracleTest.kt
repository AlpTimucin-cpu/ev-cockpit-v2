package app.evcockpit.planner.solver

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.energy.*
import app.evcockpit.planner.model.*
import app.evcockpit.planner.route.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.floor
import kotlin.system.measureTimeMillis

/** Synthetic graph verification; NOT a claim of actual route/vehicle performance. */
class M8SolverOracleTest {
    private val car=VehicleProfile.defaults()
    private val p=ModelParameters()
    private val solver=PlanSolver()
    private val charging=ChargingTimeCalculator(p)
    private fun route(km:Int,speed:Double=100.0):RouteProfile =
        RouteProfile((0..km/10).map{i->
            RoutePoint(0.0,0.0,i*10_000.0,0.0,speed)
        },false,"EĞİM VERİSİ YOK — sentetik",0.0,0.0,km/speed)
    private fun charger(km:Double,index:Int=0,power:Double=100.0):ChargerCandidate =
        ChargerCandidate("test-"+km+"-"+index,"Sentetik","TEST",0.0,0.0,
            SourcedValue(power,"SENTETİK",false),ChargerConfidence.OSM_TAGGED_CCS2,
            distanceAlongMeters=km*1000.0)

    // Exhaustive enumeration of the DISCRETE choice graph, independent of DP table logic.
    private fun brute(req:PlanRequest):Double? {
        val prefix=RouteEnergyPrefix(req.route,req.car,req.temperatureC,req.hvac)
        val ordered=req.candidates.sortedWith(compareBy<ChargerCandidate>{it.distanceAlongMeters}.thenBy{it.id})
        val locations=listOf(0.0)+ordered.map{it.distanceAlongMeters}+listOf(prefix.totalMeters)
        val last=locations.lastIndex
        val cap=req.car.capacityAtSohKwh
        val step=p.socGridStep.value.toInt()
        val ceiling=floor(req.maxChargeSoc/step).toInt()*step
        var best=Double.POSITIVE_INFINITY
        fun dfs(node:Int,soc:Int,stops:Int,cost:Double) {
            if(cost>=best-1e-9)return
            val origin=ordered.getOrNull(node-1)
            val departures=if(node==0)listOf(soc)
                else (((soc/step+1)*step)..ceiling step step).toList()
            for(dep in departures){
                val count=stops+if(origin==null)0 else 1
                if(count>req.maxStops)continue
                val minutes=if(origin==null)0.0 else charging.minutes(
                    req.car,origin.powerKw.value,soc.toDouble(),dep.toDouble(),req.temperatureC)
                val overhead=if(origin==null)0.0 else p.stopOverheadMinutes.value+origin.detourMinutes
                val penalty=if(origin==null)0.0 else when(origin.confidence){
                    ChargerConfidence.OSM_TAGGED_CCS2->0.0
                    ChargerConfidence.PROBABLE_CCS2->p.probableStationPenaltyMinutes.value
                    ChargerConfidence.UNKNOWN->p.unknownStationPenaltyMinutes.value
                }
                for(next in node+1..last){
                    val threshold=if(next==last)req.minArrivalSoc else req.minStopSoc
                    val peak=prefix.maximumRequiredKwh(locations[node],locations[next])/cap*100.0
                    if(dep-peak<threshold-1e-9)continue
                    val net=prefix.betweenKwh(locations[node],locations[next])/cap*100.0
                    val arrival=floor((dep-net).coerceIn(0.0,100.0)+1e-9).toInt()
                    if(arrival<threshold)continue
                    val score=cost+minutes+overhead+penalty+
                        prefix.betweenMinutes(locations[node],locations[next])
                    if(next==last)best=minOf(best,score)
                    else dfs(next,arrival,count,score)
                }
            }
        }
        dfs(0,floor(req.startSocPct!!).toInt(),0,0.0)
        return best.takeIf{it.isFinite()}
    }

    @Test fun exhaustiveGridOracleSmallCases() {
        val cases=listOf(
            PlanRequest(route(160,95.0),car,
                listOf(charger(40.0),charger(80.0),charger(120.0)),55.0,maxStops=3),
            PlanRequest(route(220,105.0),car,
                listOf(charger(40.0),charger(90.0),charger(140.0),charger(180.0)),60.0,maxStops=4),
            PlanRequest(route(100,90.0),car,listOf(charger(30.0)),95.0,maxStops=2)
        )
        cases.forEachIndexed{ i,req->
            val expected=brute(req)
            val actual=solver.solve(req)
            if(expected==null)assertTrue("oracle "+i,actual is PlanResult.Failure)
            else {
                assertTrue("oracle "+i,actual is PlanResult.Success)
                assertEquals("oracle "+i,expected,(actual as PlanResult.Success).riskAdjustedMinutes,1e-7)
            }
        }
    }

    @Test fun fiveHundredAndTenSafetyReplays() {
        val random=Random(20261120L)
        var valid=0
        repeat(510){
            val km=(6+random.nextInt(25))*10
            val soc=(20+random.nextInt(71)).toDouble()
            val chargers=(0 until (1+random.nextInt(5))).map{i->
                charger(10.0+random.nextInt(km-20),i)
            }.distinctBy{it.distanceAlongMeters}
            val req=PlanRequest(route(km,90.0+random.nextInt(40)),
                car,chargers,soc,maxStops=5)
            val result=solver.solve(req)
            if(result is PlanResult.Success){
                valid++
                val prefix=RouteEnergyPrefix(req.route,req.car,req.temperatureC,req.hvac)
                val cap=req.car.capacityAtSohKwh
                var pos=0.0
                var depart=floor(soc)
                result.stops.forEach{stop->
                    val to=stop.charger.distanceAlongMeters
                    assertTrue(depart-prefix.maximumRequiredKwh(pos,to)/cap*100.0 >=
                        req.minStopSoc-1e-8)
                    val arr=floor((depart-prefix.betweenKwh(pos,to)/cap*100.0)
                        .coerceIn(0.0,100.0)+1e-9).toInt()
                    assertEquals(arr,stop.arrivalSocPct)
                    assertTrue(stop.departureSocPct>arr)
                    pos=to;depart=stop.departureSocPct.toDouble()
                }
                assertTrue(depart-prefix.maximumRequiredKwh(pos,prefix.totalMeters)/cap*100.0 >=
                    req.minArrivalSoc-1e-8)
                val arrive=floor((depart-prefix.betweenKwh(pos,prefix.totalMeters)/cap*100.0)
                    .coerceIn(0.0,100.0)+1e-9).toInt()
                assertEquals(arrive,result.arrivalSocPct)
                assertEquals(result.physicalMinutes+result.riskPenaltyMinutes,
                    result.riskAdjustedMinutes,1e-7)
                assertFalse(result.verified)
            }
        }
        assertTrue(valid>=10)
        println("M8_SAFETY_CASES=510 FEASIBLE="+valid+" PASSED=true")
    }

    @Test fun deterministicAndBrokenInput() {
        val req=PlanRequest(route(200,100.0),car,
            listOf(charger(50.0),charger(100.0)),60.0)
        assertEquals(solver.solve(req),solver.solve(req))
        assertThrows(IllegalArgumentException::class.java) {
            solver.solve(req.copy(startSocPct=Double.NaN))
        }
        assertTrue(solver.solve(req.copy(startSocPct=null)) is PlanResult.Failure)
        assertTrue(solver.solve(req.copy(candidates=emptyList(),
            startSocPct=15.0)) is PlanResult.Failure)
    }

    @Test fun monotonicForSyntheticFixedInputs() {
        val cs=(25..275 step 25).map{charger(it.toDouble())}
        val low=solver.solve(PlanRequest(route(300,100.0),car,cs,35.0,maxStops=6))
        val high=solver.solve(PlanRequest(route(300,100.0),car,cs,90.0,maxStops=6))
        if(low is PlanResult.Success && high is PlanResult.Success){
            assertTrue(high.riskAdjustedMinutes<=low.riskAdjustedMinutes+1e-8)
            assertTrue(high.stops.size<=low.stops.size)
        }
        val small=solver.solve(PlanRequest(route(300,100.0),car,cs,60.0,
            maxStops=6,maxChargeSoc=60.0))
        val large=solver.solve(PlanRequest(route(300,100.0),car,cs,60.0,
            maxStops=6,maxChargeSoc=80.0))
        if(small is PlanResult.Success && large is PlanResult.Success)
            assertTrue(large.riskAdjustedMinutes<=small.riskAdjustedMinutes+1e-8)
    }

    @Test fun thousandKmThreeHundredCandidatesCiBudget() {
        val chargers=(1..300).map{i->charger(i*1000.0/301.0,i,90.0)}
        val req=PlanRequest(route(1000,105.0),car,chargers,90.0,maxStops=8)
        var result:PlanResult?=null
        val elapsed=measureTimeMillis {result=solver.solve(req)}
        println("M8_PERF_1000KM_300CANDIDATES_MS="+elapsed+" SYNTHETIC=true")
        assertNotNull(result)
        assertTrue("Exceeded 10 second CI threshold: "+elapsed,elapsed<10_000)
    }

    @Test fun alternativesUseUnverifiedBackupLabels() {
        val req=PlanRequest(route(250,105.0),car,listOf(
            charger(40.0),charger(45.0),charger(95.0),charger(100.0),
            charger(150.0),charger(155.0)),65.0,maxStops=6)
        val alternatives=AlternativeGenerator().calculate(req)
        assertTrue(alternatives.fastest is PlanResult.Success)
        assertTrue(alternatives.backups.all{it.note.contains("DOĞRULANMADI")})
        alternatives.safer?.let{safe->
            assertTrue(safe.stops.all{
                it.charger.confidence==ChargerConfidence.OSM_TAGGED_CCS2
            })
        }
    }

    @Test fun illustrativeIntercityFixturesNotTrueGeography() {
        val stations=(35..665 step 35).map{charger(it.toDouble())}
        val summer=PlanRequest(route(700,110.0),car,stations,90.0,
            temperatureC=25.0,hvac=HvacMode.AUTO,maxStops=8)
        val winter=summer.copy(temperatureC=-5.0,hvac=HvacMode.HEATING)
        val highSpeed=summer.copy(route=route(700,130.0))
        val lowSoc=summer.copy(startSocPct=30.0)
        val ankara=summer.copy(route=route(450,110.0),
            candidates=stations.filter{it.distanceAlongMeters<450_000.0})
        listOf("NIGDE-ISTANBUL SUMMER" to summer,
            "NIGDE-ISTANBUL WINTER" to winter,
            "NIGDE-ISTANBUL 130KPH" to highSpeed,
            "NIGDE-ISTANBUL SOC30" to lowSoc,
            "ANKARA-ISTANBUL" to ankara).forEach{(name,req)->
            val output=solver.solve(req)
            val value=if(output is PlanResult.Success)
                "STOPS="+output.stops.size+" ARRIVAL_SOC="+output.arrivalSocPct+
                    " PHYSICAL_MIN="+("%.1f".format(java.util.Locale.US,output.physicalMinutes))
            else "NO_PLAN"
            println("M8_SCENARIO "+name+" SYNTHETIC=true "+value)
        }
        val model=PhysicsConsumptionModel()
        val s=DrivingSegment(100_000.0,110.0,0.0,25.0)
        assertTrue(model.compute(s.copy(temperatureC=-5.0),car,HvacMode.HEATING).kwh >
            model.compute(s,car,HvacMode.AUTO).kwh)
        assertTrue(model.compute(s.copy(speedKmh=130.0),car,HvacMode.AUTO).kwh >
            model.compute(s,car,HvacMode.AUTO).kwh)
    }
}
