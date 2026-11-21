package app.evcockpit.planui

import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.PlannedStop
import java.util.Locale

/** Coordinate-free text for planner UI. Never add GPS or API response bodies here. */
object PlannerDisplay {
    /** Model-based arrival energy. Not a vehicle BMS reading. */
    fun arrivalKwh(plan:PlanResult.Success,capacityAtSohKwh:Double):Double {
        require(capacityAtSohKwh.isFinite() && capacityAtSohKwh>0.0)
        return capacityAtSohKwh * plan.arrivalSocPct.coerceIn(0,100) / 100.0
    }
    fun totalTimeLabel(plan:PlanResult.Success):String =
        "%.0f dk".format(Locale.US,plan.physicalMinutes)
    fun chargingTimeLabel(plan:PlanResult.Success):String =
        "%.0f dk".format(Locale.US,plan.chargingMinutes)

    fun uncertainty(p:PlanResult.Success):String =
        "Varış ~%"+p.arrivalSocPct+" (iyimser ~%"+
            ("%.0f".format(Locale.US,p.optimisticArrivalSocPct))+
            " · kötümser ~%"+
            ("%.0f".format(Locale.US,p.pessimisticArrivalSocPct))+") — TAHMİN"
    fun overview(p:PlanResult.Success):String =
        "Toplam ~"+("%.0f dk".format(Locale.US,p.physicalMinutes))+
            " • "+("%.1f km".format(Locale.US,p.distanceKm))+
            " • "+p.stops.size+" durak • şarj ~"+
            ("%.0f dk".format(Locale.US,p.chargingMinutes))+" — TAHMİN"
    fun stop(s:PlannedStop):String =
        s.arrivalSocPct.toString()+"% → "+s.departureSocPct+
            "% • "+("%.1f kW".format(Locale.US,s.charger.powerKw.value))+
            " (TAHMİN) • şarj "+("%.0f dk".format(Locale.US,s.chargeMinutes))+
            " • doluluk bilinmiyor"
}
