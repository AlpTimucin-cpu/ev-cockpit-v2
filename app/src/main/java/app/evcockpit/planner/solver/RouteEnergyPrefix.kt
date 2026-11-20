package app.evcockpit.planner.solver

import app.evcockpit.planner.energy.*
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.planner.route.RouteProfile
import kotlin.math.*

/** Prefix energy/time with an O(1) maximum-prefix range query to protect intermediate SoC. */
class RouteEnergyPrefix(route:RouteProfile,car:VehicleProfile,
    tempC:Double,hvac:HvacMode) {
    val distances=route.points.map {it.distanceMeters}.toDoubleArray()
    val energy=DoubleArray(distances.size)
    val timeMinutes=DoubleArray(distances.size)
    val totalMeters get() = distances.last()
    val totalKwh get() = energy.last()
    val totalMinutes get() = timeMinutes.last()
    private val lg=IntArray(distances.size+1)
    private val mx:Array<DoubleArray>
    init {
        require(route.points.size>=2 && distances.first()==0.0)
        require(distances.asIterable().zipWithNext().all {it.first<it.second})
        val physics=PhysicsConsumptionModel()
        for(i in 1 until distances.size) {
            val a=route.points[i-1];val b=route.points[i]
            val delta=b.distanceMeters-a.distanceMeters
            val grade=if(route.slopeKnown && a.elevationMeters!=null &&
                b.elevationMeters!=null) atan2(b.elevationMeters-a.elevationMeters,delta)
                else 0.0
            val speed=a.speedKmh ?: throw IllegalArgumentException(
                "Sağlayıcı hız profili yok — plan DOĞRULANMADI")
            val item=physics.compute(DrivingSegment(delta,speed,grade,tempC),car,hvac)
            energy[i]=energy[i-1]+item.kwh
            timeMinutes[i]=timeMinutes[i-1]+item.elapsedHours*60.0
        }
        for(i in 2 until lg.size)lg[i]=lg[i/2]+1
        mx=Array(lg[distances.size]+1){DoubleArray(distances.size){Double.NEGATIVE_INFINITY}}
        energy.indices.forEach {mx[0][it]=energy[it]}
        for(k in 1 until mx.size) {
            val span=1 shl k
            for(i in 0..distances.size-span) {
                mx[k][i]=max(mx[k-1][i],mx[k-1][i+(span/2)])
            }
        }
    }
    private fun firstAtLeast(s:Double):Int {
        var a=0;var b=distances.size-1
        while(a<b) {
            val m=(a+b)/2
            if(distances[m]<s)a=m+1 else b=m
        }
        return a
    }
    private fun at(s:Double,values:DoubleArray):Double {
        require(s in 0.0..totalMeters)
        val hi=firstAtLeast(s)
        if(hi==0)return values[0]
        val low=hi-1
        val portion=(s-distances[low])/(distances[hi]-distances[low])
        return values[low]+(values[hi]-values[low])*portion
    }
    fun energyAt(s:Double):Double=at(s,energy)
    fun minutesAt(s:Double):Double=at(s,timeMinutes)
    fun betweenKwh(from:Double,to:Double):Double=energyAt(to)-energyAt(from)
    fun betweenMinutes(from:Double,to:Double):Double=minutesAt(to)-minutesAt(from)
    private fun maximum(a:Int,b:Int):Double {
        if(a>b)return Double.NEGATIVE_INFINITY
        val span=b-a+1
        val k=lg[span]
        return max(mx[k][a],mx[k][b-(1 shl k)+1])
    }
    fun maximumRequiredKwh(from:Double,to:Double):Double {
        require(from in 0.0..to && to<=totalMeters)
        val first=firstAtLeast(from)
        val last=firstAtLeast(to)
        val peak=max(max(energyAt(from),energyAt(to)),maximum(first,last))
        return (peak-energyAt(from)).coerceAtLeast(0.0)
    }
}
