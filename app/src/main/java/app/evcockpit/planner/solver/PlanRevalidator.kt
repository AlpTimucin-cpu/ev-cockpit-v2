package app.evcockpit.planner.solver

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.data.RouteProjectionEstimator
import app.evcockpit.planner.model.ModelParameters
import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.route.RouteProfile
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlin.math.max

/** Injected adapter; no device, API or network access is performed by this core. */
fun interface WaypointRouteClient {
    suspend fun routeVia(points: List<GeoPoint>): RouteProfile
}
data class RouteVerification(
    val plan: PlanResult,
    val firstDifferencePercent: Double?,
    val finalDifferencePercent: Double?,
    val replannedOnce: Boolean,
    val warning: String,
    val verifiedOnVehicle: Boolean = false,
)

/**
 * User-triggered second pass. At most two route requests and one re-solve.
 * Provider/network failures are surfaced as DOĞRULANMADI, not invented data.
 */
class PlanRevalidator(
    private val routes: WaypointRouteClient,
    private val solver: PlanSolver = PlanSolver(),
    private val params: ModelParameters = ModelParameters(),
) {
    private fun stopsToPoints(route: RouteProfile, plan: PlanResult.Success): List<GeoPoint> =
        listOf(GeoPoint(route.points.first().lat, route.points.first().lon)) +
            plan.stops.map { GeoPoint(it.charger.latitude, it.charger.longitude) } +
            listOf(GeoPoint(route.points.last().lat, route.points.last().lon))

    private fun energy(req: PlanRequest, route: RouteProfile): Double =
        RouteEnergyPrefix(route, req.car, req.temperatureC, req.hvac).totalKwh

    private fun deltaPercent(reference: Double, actual: Double): Double {
        require(reference.isFinite() && actual.isFinite())
        return abs(actual - reference) / max(abs(reference), 1e-6) * 100.0
    }

    private fun reproject(
        candidates: List<ChargerCandidate>,
        verifiedRoute: RouteProfile,
    ): List<ChargerCandidate> = candidates.map { candidate ->
        val projection = RouteProjectionEstimator.project(candidate, verifiedRoute.points, params)
        candidate.copy(distanceAlongMeters = projection.distanceAlongMeters,
            detourKm = projection.detourKm,
            detourMinutes = projection.detourMinutes)
    }

    suspend fun check(req: PlanRequest, original: PlanResult.Success): RouteVerification {
        var firstDelta: Double? = null
        var replanned = false
        var updatedPlan: PlanResult = original
        try {
            val first = routes.routeVia(stopsToPoints(req.route, original))
            firstDelta = deltaPercent(energy(req, req.route), energy(req, first))
            if (firstDelta <= params.revalidationThresholdPercent.value) {
                return RouteVerification(original, firstDelta, firstDelta, false,
                    "ARA DURAKLAR YENİDEN ROTALANDI — model/saha DOĞRULANMADI")
            }
            replanned = true
            val revisedReq = req.copy(route = first,
                candidates = reproject(req.candidates, first))
            updatedPlan = solver.solve(revisedReq)
            if (updatedPlan !is PlanResult.Success) {
                return RouteVerification(updatedPlan, firstDelta, null, true,
                    "Gerçek sapma farkı yüzde 2'yi aştı; yeniden plan menzil koşullarını sağlayamadı — DOĞRULANMADI")
            }
            val second = routes.routeVia(stopsToPoints(first, updatedPlan))
            val finalDelta = deltaPercent(energy(req, first), energy(req, second))
            val warning = if (finalDelta > params.revalidationThresholdPercent.value)
                "Yeniden plan sonrası fark yüzde 2'den fazla: " +
                    ("%.2f".format(java.util.Locale.US, finalDelta)) +
                    "% — planı doğrula; DOĞRULANMADI"
            else "İkinci rota karşılaştırması eşik içinde; saha DOĞRULANMADI"
            return RouteVerification(updatedPlan, firstDelta, finalDelta, true, warning)
        } catch (cancel:CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return RouteVerification(updatedPlan, firstDelta, null, replanned,
                "Rota sağlayıcısı/bağlantı hatası — yeniden doğrulama tamamlanamadı (DOĞRULANMADI)")
        }
    }
}
