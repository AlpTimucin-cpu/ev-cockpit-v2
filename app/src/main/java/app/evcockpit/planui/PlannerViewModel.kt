package app.evcockpit.planui

import android.app.Application
import android.os.SystemClock
import app.evcockpit.CockpitApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.OrsRouteProvider
import app.evcockpit.navigation.EncryptedRouteKeyStore
import app.evcockpit.planner.integration.OrsViaRouteAdapter
import app.evcockpit.obd.SocReading
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerFilter
import app.evcockpit.planner.data.*
import app.evcockpit.planner.model.*
import app.evcockpit.planner.route.*
import app.evcockpit.planner.solver.*
import app.evcockpit.storage.CustomChargerStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlannerUiState(
    val stage:PlannerStage=PlannerStage.IDLE,
    val message:String=PlannerGuard.label(PlannerStage.IDLE),
    val plan:PlanResult?=null,
    val alternatives:PlanAlternatives?=null,
    val route:NavigationRoute?=null,
    val request:PlanRequest?=null,
    val candidates:List<ChargerCandidate> = emptyList(),
    val solverElapsedMs:Long?=null,
    val verification:RouteVerification?=null,
    val stationStatus:String="Şarj verisi henüz aranmadı",
    val warning:String="TAHMİN — araç, telefon ve istasyon DOĞRULANMADI",
)
class PlannerViewModel(application:Application):AndroidViewModel(application) {
    private val assembler=RouteProfileAssembler(OpenMeteoElevationClient())
    private val overpass=OverpassRepository(PublicOverpassTransport(),
        ChargerCache(File(application.cacheDir,"m8-overpass")))
    private val custom=CustomChargerStore(application)
    private val keys=EncryptedRouteKeyStore(application)
    private val mutable=MutableStateFlow(PlannerUiState())
    val state=mutable.asStateFlow()
    private var job:Job?=null


    /**
     * User-triggered ORS waypoint revalidation. Max two requests and one solver
     * rerun. No hidden requests, no API key or coordinates in diagnostic text.
     */
    fun verifySelectedStops() {
        if(job?.isActive==true)return
        val original=mutable.value.plan as? PlanResult.Success
        val request=mutable.value.request
        if(original==null || request==null) {
            mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                message="Önce şarj planı oluştur, sonra ara durakları doğrula.")
            return
        }
        job=viewModelScope.launch {
            try {
                val key=keys.read()
                if(key.isNullOrBlank()) {
                    mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                        message="HeiGIT anahtarı yok. Ayarlar > API anahtarı bölümünü aç.")
                    return@launch
                }
                mutable.value=mutable.value.copy(stage=PlannerStage.VERIFYING,
                    message=PlannerGuard.label(PlannerStage.VERIFYING),
                    verification=null)
                val source=OrsViaRouteAdapter(OrsRouteProvider(),assembler){ key }
                val check=withContext(Dispatchers.IO) {
                    PlanRevalidator(source).check(request,original)
                }
                val serviceFailed=check.warning.contains("tamamlanamadı",ignoreCase=true)
                val stage=when {
                    serviceFailed || check.plan is PlanResult.Failure -> PlannerStage.FAILURE
                    else -> PlannerStage.READY
                }
                val info=when {
                    serviceFailed ->
                        "ORS ikinci rota isteği tamamlanamadı; ağ/kota/anahtarı kontrol et. DOĞRULANMADI"
                    check.plan is PlanResult.Failure ->
                        "Yeniden hesaplanan rota menzil koşullarını sağlamadı. DOĞRULANMADI"
                    else -> "ORS ara durak karşılaştırması tamamlandı — TAHMİN / DOĞRULANMADI"
                }
                mutable.value=mutable.value.copy(stage=stage,plan=check.plan,
                    alternatives=null,verification=check,message=info,
                    warning=check.warning+" • gerçek rota/araç/istasyon saha testi DOĞRULANMADI")
            } catch(cancel:CancellationException) {
                mutable.value=mutable.value.copy(stage=PlannerStage.CANCELLED,
                    message=PlannerGuard.label(PlannerStage.CANCELLED))
                throw cancel
            } catch(_:Exception) {
                mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                    message="ORS ikinci geçişi başarısız. İnternet, anahtar veya kotayı kontrol et. DOĞRULANMADI")
            }
        }
    }

    /**
     * Explicit tap-only: obtains a new driving route from FRESH GPS to the saved
     * destination, then recomputes the plan with the current SoC. No telemetry
     * polling or automatic reroute; no coords/key in user-facing error text.
     */
    fun replanFromCurrentGps(soc:SocReading,options:PlannerOptions,car:VehicleProfile) {
        if(job?.isActive==true)return
        val gps=(getApplication<Application>() as CockpitApplication).locationRepository.state.value
        val destination=mutable.value.route?.geometry?.lastOrNull()
        val reason=ManualReplanGuard.reason(gps.point,destination,
            gps.isFresh(SystemClock.elapsedRealtime()),gps.accuracyMeters,soc,options)
        if(reason!=null) {
            mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,message=reason)
            return
        }
        val from=gps.point ?: return
        val to=destination ?: return
        job=viewModelScope.launch {
            try {
                val key=keys.read()
                if(key.isNullOrBlank()) {
                    mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                        message="HeiGIT API anahtarı yok. Ayarlar > Anahtar bölümünü aç.")
                    return@launch
                }
                mutable.value=mutable.value.copy(stage=PlannerStage.ELEVATION,
                    message="Güncel GPS konumundan rota yeniden isteniyor — TAHMİN / DOĞRULANMADI")
                val current=OrsRouteProvider().calculate(from,to,key)
                // Previous request has completed; launch the normal planner
                // against the NEW origin and latest manually supplied SoC.
                job=null
                plan(current,soc,options,car)
            } catch(cancel:CancellationException) {
                mutable.value=mutable.value.copy(stage=PlannerStage.CANCELLED,
                    message=PlannerGuard.label(PlannerStage.CANCELLED))
                throw cancel
            } catch(_:Exception) {
                mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                    message="Güncel GPS rotası alınamadı. Ağ/kota/anahtarı kontrol et. DOĞRULANMADI")
            }
        }
    }

    fun cancel() {
        job?.cancel();job=null
        mutable.value=mutable.value.copy(stage=PlannerStage.CANCELLED,
            message=PlannerGuard.label(PlannerStage.CANCELLED))
    }

    /** Explicit user action only; never a hidden polling task. */
    fun plan(navigation:NavigationRoute?,soc:SocReading,options:PlannerOptions,car:VehicleProfile) {
        if(job?.isActive==true)return
        val error=PlannerGuard.reason(navigation!=null,soc.pct,soc.source,options)
        if(error!=null) {
            mutable.value=PlannerUiState(stage=PlannerStage.FAILURE,message=error)
            return
        }
        val selected=navigation ?:return
        val socPct=soc.pct ?:return
        val vehicle=options.applyTo(car)
        if(vehicle.validate().isNotEmpty()) {
            mutable.value=PlannerUiState(stage=PlannerStage.FAILURE,
                message="Araç profili hatalı. Ayarlar > Araç profili alanını kontrol et.")
            return
        }
        job=viewModelScope.launch {
            try {
                mutable.value=PlannerUiState(stage=PlannerStage.ELEVATION,
                    message=PlannerGuard.label(PlannerStage.ELEVATION),route=selected)
                val sampled=withContext(Dispatchers.IO) {
                    assembler.prepare(selected,options.maxCruiseKmh)
                }
                mutable.value=mutable.value.copy(stage=PlannerStage.STATIONS,
                    message=PlannerGuard.label(PlannerStage.STATIONS))
                val saved=custom.stations.first()
                val found=overpass.search(sampled.points,
                    radiusMeters=options.searchRadiusMeters,
                    filter=ChargerFilter(onlyOsmTaggedCcs2=options.onlyTaggedCcs2,
                        minKw=options.minStationKw,
                        excludedNetworks=options.excludedNetworks.split(',')
                            .map{it.trim().lowercase()}.filter{it.isNotEmpty()}.toSet()),
                    extra=saved)
                mutable.value=mutable.value.copy(stage=PlannerStage.OPTIMIZING,
                    message=PlannerGuard.label(PlannerStage.OPTIMIZING),
                    stationStatus=found.status,candidates=found.chargers)
                val request=PlanRequest(sampled,vehicle,found.chargers,socPct,
                    socSource=soc.source,temperatureC=options.temperatureC,
                    hvac=options.hvac,minArrivalSoc=options.minArrivalSoc,
                    minStopSoc=options.minStopSoc,maxChargeSoc=options.maxChargeSoc,
                    maxStops=options.maxStops,allowSlowCharging=options.allowSlowCharging,
                    onlyOsmTaggedCcs2=options.onlyTaggedCcs2,stationDataNote=found.status)
                val timed=withContext(Dispatchers.Default) {
                    TimedPlanSolver().execute(request)
                }
                val alternatives=if(timed.result is PlanResult.Success)
                    withContext(Dispatchers.Default){AlternativeGenerator().calculate(request)}
                    else null
                val stage=if(timed.result is PlanResult.Success)
                    PlannerStage.READY else PlannerStage.FAILURE
                val message=if(timed.result is PlanResult.Failure)
                    timed.result.reason else PlannerGuard.label(stage)
                mutable.value=mutable.value.copy(stage=stage,message=message,plan=timed.result,
                    alternatives=alternatives,request=request,solverElapsedMs=timed.elapsedMs,
                    warning="TAHMİN — "+sampled.slopeWarning+"; "+found.status)
            } catch(cancel:CancellationException) {
                mutable.value=mutable.value.copy(stage=PlannerStage.CANCELLED,
                    message=PlannerGuard.label(PlannerStage.CANCELLED))
                throw cancel
            } catch(_:Exception) {
                mutable.value=mutable.value.copy(stage=PlannerStage.FAILURE,
                    message="Plan başarısız. İnternet, kota, GPS, rota ve anahtarı kontrol et. DOĞRULANMADI")
            }
        }
    }
}
