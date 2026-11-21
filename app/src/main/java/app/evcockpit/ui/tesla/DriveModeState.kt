package app.evcockpit.ui.tesla

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.CockpitApplication
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

enum class DriveMode { DRIVING, PARKED, UNKNOWN }

data class DriveModeSnapshot(
    val mode: DriveMode = DriveMode.UNKNOWN,
    val obdSpeedFresh: Boolean = false,
    val obdSocFresh: Boolean = false,
    val obdPowerFresh: Boolean = false,
    val gpsFresh: Boolean = false,
) {
    val allowComplexActions: Boolean get() = mode == DriveMode.PARKED
    val label: String get() = when (mode) {
        DriveMode.DRIVING -> "SÜRÜŞ • dikkat dağıtan işlemler kapalı"
        DriveMode.PARKED -> "PARK / ŞARJ • telefon modu"
        DriveMode.UNKNOWN -> "HIZ BİLİNMİYOR • video kapalı"
    }
}

object DriveModePolicy {
    fun allowedTab(tab: Int, mode: DriveMode): Boolean =
        !(mode == DriveMode.DRIVING && tab in setOf(3,4,5)) &&
            !(tab == 4 && mode != DriveMode.PARKED)

    fun allowedAction(action: TeslaAction, mode: DriveMode, molaInstalled: Boolean): Boolean =
        when (action) {
            TeslaAction.ROTA -> true
            TeslaAction.MOLA -> mode == DriveMode.PARKED // card offers install guide if absent
            else -> mode == DriveMode.PARKED
        }

    fun fresh(last: Long, now: Long, maxAge: Long = TeslaTokens.staleObdAfterMs): Boolean =
        last > 0L && now >= last && now - last <= maxAge

    fun evaluate(
        vehicle: VehicleState,
        gps: GpsSnapshot,
        nowElapsedMs: Long,
    ): DriveModeSnapshot {
        val obdEnabled = vehicle.connected && !vehicle.demo
        val speedFresh = obdEnabled &&
            fresh(vehicle.speedUpdatedElapsedMs, nowElapsedMs)
        val socFresh = obdEnabled &&
            fresh(vehicle.socUpdatedElapsedMs, nowElapsedMs)
        val powerFresh = obdEnabled &&
            fresh(vehicle.voltageUpdatedElapsedMs, nowElapsedMs) &&
            fresh(vehicle.currentUpdatedElapsedMs, nowElapsedMs)
        val gpsFresh = gps.isFresh(nowElapsedMs)
        val obdSpeed = if (speedFresh) vehicle.speedKmh
            ?.takeIf { it.isFinite() && it in 0.0..300.0 } else null
        val gpsSpeed = if (gpsFresh) gps.speedKmh
            ?.takeIf { it.isFinite() && it in 0.0..300.0 } else null
        val mode = when {
            (obdSpeed != null && obdSpeed > 0.0) ||
                (gpsSpeed != null && gpsSpeed > TeslaTokens.drivingThresholdKmh) ->
                DriveMode.DRIVING
            obdSpeed != null || gpsSpeed != null -> DriveMode.PARKED
            else -> DriveMode.UNKNOWN
        }
        return DriveModeSnapshot(mode,speedFresh,socFresh,powerFresh,gpsFresh)
    }
}

/** Explicit source hierarchy; stale OBD never wins over MANUEL or DEMO. */
object SafeSocSelection {
    fun select(vehicle:VehicleState, manual:Double?, snapshot:DriveModeSnapshot): SocReading {
        val obdValue = vehicle.socPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val manualValue = manual?.takeIf { it.isFinite() && it in 0.0..100.0 }
        return when {
            snapshot.obdSocFresh && obdValue != null ->
                SocReading(obdValue,"OBD — PID DOĞRULANMADI")
            manualValue != null ->
                SocReading(manualValue,"MANUEL — araç göstergesinden")
            vehicle.demo && obdValue != null ->
                SocReading(obdValue,"DEMO — gerçek değil")
            vehicle.connected && !vehicle.demo && obdValue != null ->
                SocReading(null,"ESKİ — OBD SoC 3 saniyeyi aştı")
            else -> SocReading(null,"SoC verisi yok")
        }
    }
}

/**
 * Combines process-scoped OBD/GPS StateFlows with a clock tick.
 * Clock tick is necessary so values become stale even when no new packets arrive.
 */
class DriveModeViewModel(application: Application): AndroidViewModel(application) {
    private val app = application as CockpitApplication
    val state: StateFlow<DriveModeSnapshot> = combine(
        app.obdRepository.state,
        app.locationRepository.state,
        flow {
            while (currentCoroutineContext().isActive) {
                emit(SystemClock.elapsedRealtime())
                delay(250L)
            }
        },
    ) { vehicle, gps, elapsed ->
        DriveModePolicy.evaluate(vehicle,gps,elapsed)
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000L),DriveModeSnapshot())
}
