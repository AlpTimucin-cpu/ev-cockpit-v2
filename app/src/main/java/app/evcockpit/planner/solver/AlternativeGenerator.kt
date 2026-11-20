package app.evcockpit.planner.solver

import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.*
import kotlin.math.abs

data class BackupCandidate(
    val originalStopId: String,
    val candidate: ChargerCandidate,
    val note: String = "TAHMİN — alternatifin erişimi/doluluğu/sapması DOĞRULANMADI",
)
data class PlanAlternatives(
    val fastest: PlanResult,
    val fewerStops: PlanResult.Success?,
    val safer: PlanResult.Success?,
    val backups: List<BackupCandidate>,
)

/** Suggestions cannot be mistaken for confirmed, operational charge points. */
class AlternativeGenerator(
    private val solver: PlanSolver = PlanSolver(),
    private val nearMeters: Double = 12_000.0,
) {
    init { require(nearMeters.isFinite() && nearMeters > 0.0) }
    fun calculate(request: PlanRequest): PlanAlternatives {
        val fastest = solver.solve(request)
        if (fastest !is PlanResult.Success) {
            return PlanAlternatives(fastest, null, null, emptyList())
        }
        val less = if (fastest.stops.isEmpty()) null else
            (solver.solve(request.copy(maxStops=fastest.stops.size-1))
                as? PlanResult.Success)?.takeIf {it.stops.size < fastest.stops.size}
        val safe = solver.solve(request.copy(
            minArrivalSoc=(request.minArrivalSoc+5.0).coerceAtMost(100.0),
            minStopSoc=(request.minStopSoc+5.0).coerceAtMost(100.0),
            onlyOsmTaggedCcs2=true
        )) as? PlanResult.Success
        val backups = fastest.stops.flatMap {stop ->
            request.candidates.asSequence()
                .filter { it.id != stop.charger.id &&
                    abs(it.distanceAlongMeters - stop.charger.distanceAlongMeters) <= nearMeters }
                .sortedWith(compareByDescending<ChargerCandidate> {
                    when(it.confidence) {
                        ChargerConfidence.OSM_TAGGED_CCS2 -> 3
                        ChargerConfidence.PROBABLE_CCS2 -> 2
                        ChargerConfidence.UNKNOWN -> 1
                    }
                }.thenByDescending {it.powerKw.value}.thenBy {it.detourMinutes}.thenBy {it.id})
                .take(2).map {BackupCandidate(stop.charger.id,it)}.toList()
        }
        return PlanAlternatives(fastest,less,safe,backups)
    }
}
