package app.evcockpit.planner.solver

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.*
import kotlin.math.*

/** Ordered DAG dynamic programming over (node, integer arrival SoC, stop count). */
class PlanSolver(
    private val parameters:ModelParameters=ModelParameters(),
    private val charge:ChargingTimeCalculator=ChargingTimeCalculator(parameters),
    private val pruner:CandidatePruner=CandidatePruner(parameters),
) {
    private data class Node(val s:Double,val charger:ChargerCandidate?)
    private data class Cell(val cost:Double,val prevNode:Int,
        val prevSoc:Int,val prevStops:Int,val departSoc:Int,
        val chargeMinutes:Double,val extraMinutes:Double,val riskMinutes:Double)

    fun solve(req:PlanRequest):PlanResult {
        if(req.startSocPct==null)return PlanResult.Failure(
            "SoC kaynağı yok — manuel SoC gir.",0.0)
        require(req.car.validate().isEmpty()){"Araç profili geçersiz"}
        require(req.startSocPct.isFinite() && req.startSocPct in 0.0..100.0)
        require(req.temperatureC.isFinite())
        require(req.minArrivalSoc in 0.0..100.0 && req.minStopSoc in 0.0..100.0)
        require(req.maxChargeSoc in 0.0..100.0 && req.maxStops in 0..8)
        val cap=req.car.capacityAtSohKwh
        require(cap>0 && cap.isFinite())
        val prefix=RouteEnergyPrefix(req.route,req.car,req.temperatureC,req.hvac)
        val start=floor(req.startSocPct).toInt()
        val limit=prefix.totalMeters
        val baseWarnings=buildList {
            add("TAHMİN — model saha kalibrasyonu DOĞRULANMADI")
            if(!req.route.slopeKnown)add("EĞİM VERİSİ YOK — tahmin zayıf")
            add(req.stationDataNote)
            add("Canlı şarj doluluğu/fiyatı bilinmiyor")
        }
        fun band(arrival:Int):Pair<Double,Double> {
            val net=prefix.totalKwh/cap*100.0
            return (arrival+(1-parameters.optimisticFactor.value)*net).coerceIn(0.0,100.0) to
                (arrival-(parameters.pessimisticFactor.value-1)*net).coerceIn(0.0,100.0)
        }
        val directPeak=prefix.maximumRequiredKwh(0.0,limit)/cap*100.0
        val directSoc=floor((start-prefix.totalKwh/cap*100.0).coerceAtMost(100.0)+1e-9).toInt()
        if(start-directPeak>=req.minArrivalSoc && directSoc>=req.minArrivalSoc) {
            val uncertainty=band(directSoc)
            return PlanResult.Success(emptyList(),directSoc,uncertainty.first,uncertainty.second,
                limit/1000.0,prefix.totalMinutes,0.0,0.0,0.0,
                prefix.totalMinutes,prefix.totalMinutes,baseWarnings)
        }

        val rawCandidates=req.candidates.filter {
            it.distanceAlongMeters>0 && it.distanceAlongMeters<limit &&
                (!req.onlyOsmTaggedCcs2 || it.confidence==ChargerConfidence.OSM_TAGGED_CCS2) &&
                (req.allowSlowCharging || it.powerKw.value>=parameters.minPreferredStationKw.value)
        }
        val candidates=pruner.prune(rawCandidates,limit)
        val nodes=listOf(Node(0.0,null))+candidates.map {Node(it.distanceAlongMeters,it)}+
            listOf(Node(limit,null))
        val end=nodes.lastIndex
        val cells=Array(nodes.size){Array(101){arrayOfNulls<Cell>(req.maxStops+1)}}
        cells[0][start][0]=Cell(0.0,-1,0,0,start,0.0,0.0,0.0)
        val step=parameters.socGridStep.value.toInt().coerceAtLeast(1)
        val maxDeparture=floor(req.maxChargeSoc/step).toInt()*step
        for(i in 0 until end) {
            for(soc in 0..100) for(stops in 0..req.maxStops) {
                val current=cells[i][soc][stops]?:continue
                val origin=nodes[i].charger
                val departures=if(i==0)listOf(soc) else
                    ((soc/step+1)*step..maxDeparture step step).toList()
                for(dep in departures) {
                    if(i>0 && dep<=soc)continue
                    val nextCount=stops+(if(i==0)0 else 1)
                    if(nextCount>req.maxStops)continue
                    val chargeMinutes=if(origin==null)0.0 else charge.minutes(
                        req.car,origin.powerKw.value,soc.toDouble(),dep.toDouble(),req.temperatureC)
                    val extraMinutes=if(origin==null)0.0 else
                        parameters.stopOverheadMinutes.value+origin.detourMinutes
                    val riskMinutes=if(origin==null)0.0 else when(origin.confidence) {
                        ChargerConfidence.OSM_TAGGED_CCS2->0.0
                        ChargerConfidence.PROBABLE_CCS2->parameters.probableStationPenaltyMinutes.value
                        ChargerConfidence.UNKNOWN->parameters.unknownStationPenaltyMinutes.value
                    }
                    val costAtDeparture=current.cost+chargeMinutes+extraMinutes+riskMinutes
                    for(j in i+1..end) {
                        val threshold=if(j==end)req.minArrivalSoc else req.minStopSoc
                        val requiredPeak=prefix.maximumRequiredKwh(nodes[i].s,nodes[j].s)/cap*100.0
                        if(dep-requiredPeak<threshold-1e-9)continue
                        val net=prefix.betweenKwh(nodes[i].s,nodes[j].s)/cap*100.0
                        val at=floor((dep-net).coerceIn(0.0,100.0)+1e-9).toInt()
                        if(at<threshold)continue
                        val candidateCost=costAtDeparture+
                            prefix.betweenMinutes(nodes[i].s,nodes[j].s)
                        val currentBest=cells[j][at][nextCount]
                        if(currentBest==null || candidateCost<currentBest.cost-1e-9) {
                            cells[j][at][nextCount]=Cell(candidateCost,i,soc,stops,dep,
                                chargeMinutes,extraMinutes,riskMinutes)
                        }
                    }
                }
            }
        }
        var bestSoc=-1;var bestCount=0;var best:Cell?=null
        for(soc in 0..100) for(stops in 0..req.maxStops) {
            val cell=cells[end][soc][stops]?:continue
            if(best==null || cell.cost<best.cost-1e-9 ||
                (abs(cell.cost-best.cost)<1e-9 && stops<bestCount)) {
                best=cell;bestSoc=soc;bestCount=stops
            }
        }
        if(best==null) {
            val initialReserve=min(req.minArrivalSoc,req.minStopSoc)
            val available=cap*(start-initialReserve)/100.0
            val nearest=req.route.points.takeWhile {
                prefix.maximumRequiredKwh(0.0,it.distanceMeters)<=available+1e-9
            }.lastOrNull()?.distanceMeters?.div(1000.0)?:0.0
            return PlanResult.Failure(
                if(candidates.isEmpty())"Menzil yetersiz ve uygun şarj adayı yok; en yakın ulaşılabilir nokta tahminidir."
                else "SoC alt sınırlarını koruyan plan bulunamadı; filtre/istasyonları gözden geçir.",
                nearest)
        }
        val stops=mutableListOf<PlannedStop>()
        var node=end;var soc=bestSoc;var stopCount=bestCount
        while(node>0) {
            val cell=cells[node][soc][stopCount]!!
            val origin=nodes[cell.prevNode].charger
            if(origin!=null)stops+=PlannedStop(origin,cell.prevSoc,cell.departSoc,
                cell.chargeMinutes,cell.extraMinutes,cell.riskMinutes)
            node=cell.prevNode;soc=cell.prevSoc;stopCount=cell.prevStops
        }
        stops.reverse()
        val charged=stops.sumOf {it.chargeMinutes}
        val extra=stops.sumOf {it.extraMinutes}
        val risk=stops.sumOf {it.riskPenaltyMinutes}
        val physical=prefix.totalMinutes+charged+extra
        val uncertainty=band(bestSoc)
        return PlanResult.Success(stops,bestSoc,uncertainty.first,uncertainty.second,
            limit/1000.0,prefix.totalMinutes,charged,extra,risk,
            physical,physical+risk,baseWarnings)
    }
}
