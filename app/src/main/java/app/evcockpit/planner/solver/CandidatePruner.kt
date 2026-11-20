package app.evcockpit.planner.solver

import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence
import app.evcockpit.planner.model.ModelParameters
import kotlin.math.floor

/** Exact solver domain is untouched when there are eight or fewer choices. */
class CandidatePruner(private val parameters:ModelParameters=ModelParameters()) {
    fun prune(candidates:List<ChargerCandidate>,routeLengthMeters:Double):List<ChargerCandidate> {
        require(routeLengthMeters.isFinite() && routeLengthMeters>0)
        val ordered=candidates.filter {
            it.distanceAlongMeters.isFinite() && it.distanceAlongMeters in 0.0..routeLengthMeters
        }.sortedWith(compareBy<ChargerCandidate> {it.distanceAlongMeters}.thenBy {it.id})
        if(ordered.size<=8)return ordered
        val maxPer=parameters.pruneMaxPerWindow.value.toInt().coerceAtLeast(1)
        val bin=parameters.pruneWindowKm.value*parameters.metersPerKm.value
        return ordered.groupBy {floor(it.distanceAlongMeters/bin).toInt()}
            .toSortedMap().values.flatMap { group ->
                val preferred=group.filter {it.powerKw.value>=parameters.minPreferredStationKw.value}
                    .ifEmpty { group }
                preferred.sortedWith(compareByDescending<ChargerCandidate> {
                    when(it.confidence) {
                        ChargerConfidence.OSM_TAGGED_CCS2->3
                        ChargerConfidence.PROBABLE_CCS2->2
                        ChargerConfidence.UNKNOWN->1
                    }
                }.thenByDescending{it.powerKw.value}.thenBy{it.detourMinutes}.thenBy{it.id})
                    .take(maxPer)
            }.sortedWith(compareBy<ChargerCandidate> {it.distanceAlongMeters}.thenBy {it.id})
    }
}
