package app.evcockpit.planner.solver

import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.model.PlanResult

/** Measures only solver CPU/wall time; excludes network, display and elevation. */
data class TimedPlan(val result: PlanResult, val elapsedMs: Long,
    val verifiedOnPhone: Boolean = false)
class TimedPlanSolver(
    private val solve: (PlanRequest) -> PlanResult = { PlanSolver().solve(it) },
    private val nanos: () -> Long = { System.nanoTime() },
) {
    fun execute(request: PlanRequest): TimedPlan {
        val start = nanos()
        val answer = solve(request)
        val elapsed = (nanos() - start).coerceAtLeast(0L) / 1_000_000L
        return TimedPlan(answer, elapsed)
    }
}
