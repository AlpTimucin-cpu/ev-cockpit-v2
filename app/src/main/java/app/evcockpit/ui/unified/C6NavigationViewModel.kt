package app.evcockpit.ui.unified

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.CockpitApplication
import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.*
import app.evcockpit.ui.unified.map.OverlayPoint
import app.evcockpit.ui.unified.map.OverlayVehicle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class C6Progress(
    val vehicle: OverlayVehicle? = null,
    val remainingMeters: Double? = null,
    val remainingSeconds: Double? = null,
    val instruction: String? = null,
    val maneuverMeters: Double? = null,
    val maneuverIndex: Int? = null,
    val offRoute: Boolean = false,
    val arrived: Boolean = false,
)

object C6NavigationPolicy {
    const val MAX_ACCURACY_M = 50f
    const val OFF_ROUTE_M = 100.0
    const val OFF_ROUTE_FIXES = 3
    const val ARRIVAL_M = 35.0
    const val MIN_VOICE_SPEED_KMH = 3.0

    fun validFix(gps: GpsSnapshot, elapsed: Long): Boolean =
        gps.active && gps.isFresh(elapsed) && gps.provider == "gps" &&
            gps.accuracyMeters?.let { it.isFinite() && it in 0f..MAX_ACCURACY_M } == true

    fun vehicle(gps: GpsSnapshot, elapsed: Long, epoch: Long): OverlayVehicle? {
        if (!validFix(gps, elapsed)) return null
        val point = gps.point ?: return null
        return OverlayVehicle(OverlayPoint(point.latitude, point.longitude),
            gps.courseDegrees, gps.accuracyMeters?.toDouble(),
            epoch - (elapsed - gps.receivedElapsedRealtimeMs),
            gps.speedKmh?.div(3.6))
    }

    fun progress(nav: NavigationRoute, gps: GpsSnapshot, elapsed: Long, epoch: Long): C6Progress {
        val fix = vehicle(gps, elapsed, epoch) ?: return C6Progress()
        val point = gps.point ?: return C6Progress()
        val matched = RouteProgress.locate(nav, point, OFF_ROUTE_M)
            ?: return C6Progress(vehicle = fix, offRoute = true)
        val end = nav.geometry.lastOrNull()
        val arrived = end != null && matched.remainingMeters <= ARRIVAL_M &&
            app.evcockpit.planner.route.RouteDistance.haversine(point, end) <= ARRIVAL_M
        val step = matched.nextManeuverIndex?.let { nav.maneuvers.getOrNull(it) }
        val seconds = if (nav.distanceMeters > 0.0)
            nav.durationSeconds * matched.remainingMeters / nav.distanceMeters else null
        return C6Progress(fix, matched.remainingMeters, seconds,
            step?.instructionTr, matched.distanceToManeuverMeters,
            matched.nextManeuverIndex, arrived = arrived)
    }
}

/** Navigation only while this screen is visible. No hidden reroute or background TTS. */
class C6NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as CockpitApplication).locationRepository
    private val mutable = MutableStateFlow(C6Progress())
    val state = mutable.asStateFlow()
    private val voiceMutable = MutableStateFlow(false)
    val voiceEnabled = voiceMutable.asStateFlow()
    private val statusMutable = MutableStateFlow<String?>(null)
    val voiceStatus = statusMutable.asStateFlow()
    private var speaker: TurkishNavigationSpeech? = null
    private var speakerJob: Job? = null
    private var tracking: Job? = null
    private val delivered = mutableSetOf<String>()
    private var routeId: String? = null

    fun track(route: RouteModel?) {
        tracking?.cancel()
        if (routeId != route?.id) {
            delivered.clear()
            speaker?.stop()
            routeId = route?.id
        }
        mutable.value = C6Progress()
        val nav = route?.let(C5Projection::navigation)
        tracking = viewModelScope.launch {
            var lastFix = -1L
            var offCount = 0
            var cached = C6Progress()
            while (isActive) {
                val gps = repo.state.value
                val elapsed = SystemClock.elapsedRealtime()
                val epoch = System.currentTimeMillis()
                if (!C6NavigationPolicy.validFix(gps, elapsed)) {
                    offCount = 0
                    lastFix = -1L
                    cached = C6Progress()
                    speaker?.stop()
                } else if (gps.receivedElapsedRealtimeMs != lastFix) {
                    lastFix = gps.receivedElapsedRealtimeMs
                    cached = if (nav == null) C6Progress(vehicle = C6NavigationPolicy.vehicle(gps, elapsed, epoch))
                    else withContext(Dispatchers.Default) {
                        C6NavigationPolicy.progress(nav, gps, elapsed, epoch)
                    }
                    offCount = if (cached.offRoute) offCount + 1 else 0
                    if (cached.offRoute) speaker?.stop()
                    if (nav != null && !cached.offRoute && !cached.arrived && voiceMutable.value &&
                        (gps.speedKmh ?: 0.0) >= C6NavigationPolicy.MIN_VOICE_SPEED_KMH) {
                        val index = cached.maneuverIndex
                        val instruction = cached.instruction
                        if (index != null && instruction != null) {
                            val cue = VoiceAnnouncementPolicy.select(index, cached.maneuverMeters, instruction, delivered)
                            if (cue != null && speaker?.speak(cue.text) == true) delivered.add(cue.id)
                        }
                    }
                }
                mutable.value = cached.copy(offRoute = offCount >= C6NavigationPolicy.OFF_ROUTE_FIXES)
                delay(500L)
            }
        }
    }

    fun toggleVoice() {
        if (voiceMutable.value) {
            voiceMutable.value = false
            speaker?.stop()
            return
        }
        if (speaker == null) {
            val instance = TurkishNavigationSpeech(getApplication<Application>())
            speaker = instance
            speakerJob = viewModelScope.launch { instance.status.collect { statusMutable.value = it } }
        }
        voiceMutable.value = true
    }

    fun suspendTracking() {
        tracking?.cancel()
        tracking = null
        speaker?.stop()
        mutable.value = C6Progress()
    }

    fun endNavigation() {
        suspendTracking()
        voiceMutable.value = false
        delivered.clear()
        routeId = null
    }

    override fun onCleared() {
        suspendTracking()
        speakerJob?.cancel()
        speaker?.shutdown()
        super.onCleared()
    }
}
