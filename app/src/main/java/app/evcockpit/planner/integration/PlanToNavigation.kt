package app.evcockpit.planner.integration

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.model.PlanResult

/** Ordered intermediate navigation points only; no automatic route request. */
object PlanToNavigation {
    fun intermediateStops(plan:PlanResult.Success):List<GeoPoint> {
        require(plan.stops.size<=48)
        return plan.stops.map { GeoPoint(it.charger.latitude,it.charger.longitude) }
    }
}
