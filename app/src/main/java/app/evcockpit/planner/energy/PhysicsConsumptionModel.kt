package app.evcockpit.planner.energy

import app.evcockpit.planner.model.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

enum class HvacMode { OFF, AUTO, HEATING }
data class DrivingSegment(
    val lengthMeters: Double, val speedKmh: Double,
    val slopeRadians: Double, val temperatureC: Double,
    val stoppedSeconds: Double = 0.0,
)
data class EnergySegment(
    val kwh: Double, val tractionKw: Double,
    val wheelKw: Double, val elapsedHours: Double,
    val source: String = "FİZİK MODELİ — TAHMİN / DOĞRULANMADI",
)
data class UncertaintyBand(val optimisticKwh: Double, val centralKwh: Double,
    val pessimisticKwh: Double, val verified: Boolean = false)

interface ConsumptionModel {
    fun compute(part: DrivingSegment, car: VehicleProfile, mode: HvacMode): EnergySegment
}
class PhysicsConsumptionModel(private val params: ModelParameters = ModelParameters()) : ConsumptionModel {
    override fun compute(part: DrivingSegment, car: VehicleProfile, mode: HvacMode): EnergySegment {
        require(car.validate().isEmpty())
        require(part.lengthMeters.isFinite() && part.lengthMeters >= 0)
        require(part.speedKmh.isFinite() && part.speedKmh >= 0)
        require(part.slopeRadians.isFinite() && part.slopeRadians in -1.5..1.5)
        require(part.temperatureC.isFinite() && part.temperatureC > -params.kelvinOffset.value)
        require(part.stoppedSeconds.isFinite() && part.stoppedSeconds >= 0)
        val hvac = when (mode) {
            HvacMode.OFF -> 0.0
            HvacMode.AUTO -> car.hvacAutoKw.value
            HvacMode.HEATING -> car.hvacHeatingKw.value
        }
        val auxiliaryKw = car.auxBaseKw.value + hvac
        // Speeds below minimum are treated as stationary; stop duration must be explicit.
        if (part.speedKmh < params.minSpeedKmh.value) {
            require(part.lengthMeters == 0.0) {
                "5 km/h altı hareket için yürüyen parça hızı model dışı; süreyi ayrı gir"
            }
            val h = part.stoppedSeconds / params.secondsPerHour.value
            return EnergySegment(auxiliaryKw * h, 0.0, 0.0, h)
        }
        val v = part.speedKmh * params.metersPerKm.value / params.secondsPerHour.value
        val rho = params.seaLevelDensity.value *
            (params.referenceTempC.value + params.kelvinOffset.value) /
            (part.temperatureC + params.kelvinOffset.value)
        val mass = car.massKg.value + car.extraLoadKg.value
        val aeroW = 0.5 * rho * car.cdA.value * v.pow(3)
        val rollW = car.crr.value * mass * params.gravity.value * v * cos(part.slopeRadians)
        val gradeW = mass * params.gravity.value * v * sin(part.slopeRadians)
        val wheelKw = (aeroW + rollW + gradeW) / params.wattsPerKw.value
        val tractionKw = if (wheelKw >= 0) wheelKw / car.drivetrainEfficiency.value
            else max(-car.maxRegenKw.value, wheelKw * car.regenEfficiency.value)
        val hour = (part.lengthMeters / params.metersPerKm.value) / part.speedKmh +
            part.stoppedSeconds / params.secondsPerHour.value
        val energy = (tractionKw + auxiliaryKw) * hour *
            (1.0 + car.consumptionAdjust.value)
        // Cold multiplier is deliberately not applied to recovered/negative energy.
        val coldMultiplier = if (energy > 0) params.coldDrivingFactor.at(part.temperatureC) else 1.0
        val result = energy * coldMultiplier
        require(result.isFinite())
        return EnergySegment(result, tractionKw, wheelKw, hour)
    }

    fun route(parts: List<DrivingSegment>, car: VehicleProfile, mode: HvacMode): UncertaintyBand {
        val central = parts.sumOf { compute(it, car, mode).kwh }
        return UncertaintyBand(central * params.optimisticFactor.value,central,
            central * params.pessimisticFactor.value)
    }
}
