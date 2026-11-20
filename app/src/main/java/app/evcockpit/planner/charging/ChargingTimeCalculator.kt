package app.evcockpit.planner.charging

import app.evcockpit.planner.model.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

/** Closed-form piecewise linear integral. Never silently use a constant-average charge rate. */
class ChargingTimeCalculator(private val parameters:ModelParameters=ModelParameters()) {
    fun minutes(car:VehicleProfile,stationMaxKw:Double,
        fromSoc:Double,toSoc:Double,batteryTempC:Double):Double {
        require(car.validate().isEmpty())
        require(stationMaxKw.isFinite() && stationMaxKw>0)
        require(fromSoc.isFinite() && toSoc.isFinite() &&
            fromSoc in 0.0..100.0 && toSoc in fromSoc..100.0)
        require(batteryTempC.isFinite())
        if(fromSoc==toSoc)return 0.0
        val cap=min(stationMaxKw,car.maxDcKw.value)
        val curve=car.chargingCurve
        val segments=mutableListOf(fromSoc,toSoc)
        segments+=curve.points.map { it.x }.filter { it>fromSoc && it<toSoc }
        // Add the intersection of each linear portion with station's power cap.
        for((a,b) in curve.points.zipWithNext()) {
            if(a.y!=b.y) {
                val crossing=a.x+(cap-a.y)*(b.x-a.x)/(b.y-a.y)
                if(crossing>maxOf(fromSoc,a.x) && crossing<minOf(toSoc,b.x))
                    segments+=crossing
            }
        }
        val breaks=segments.distinct().sorted()
        val derate=car.coldDerateTable.at(batteryTempC)
        require(derate.isFinite() && derate>0)
        val kwPercentage=car.capacityAtSohKwh/parameters.socDenominator.value
        var hours=0.0
        for((a,b) in breaks.zipWithNext()) {
            val pa=min(cap,curve.at(a))*derate
            val pb=min(cap,curve.at(b))*derate
            require(pa>0 && pb>0)
            val slope=(pb-pa)/(b-a)
            val integral=if(abs(slope)<1e-9) (b-a)/pa
                else ln(pb/pa)/slope
            hours+=kwPercentage*integral
        }
        val result=hours*parameters.minutesPerHour.value
        require(result.isFinite() && result>=0)
        return result
    }
}
