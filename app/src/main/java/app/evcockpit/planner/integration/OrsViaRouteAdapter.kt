package app.evcockpit.planner.integration

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.RouteProvider
import app.evcockpit.planner.route.RouteProfile
import app.evcockpit.planner.route.RouteProfileAssembler
import app.evcockpit.planner.solver.WaypointRouteClient

/**
 * Explicit call only; caller supplies a Keystore-backed key read callback.
 * No API key, waypoints or VIN are recorded in logs or exceptions.
 */
class OrsViaRouteAdapter(
    private val routeProvider: RouteProvider,
    private val assembler: RouteProfileAssembler,
    private val getUserKey: suspend () -> String,
): WaypointRouteClient {
    override suspend fun routeVia(points: List<GeoPoint>): RouteProfile =
        assembler.prepare(routeProvider.calculateVia(points, getUserKey()))
}
