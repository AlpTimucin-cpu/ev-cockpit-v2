package app.evcockpit.navigation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.CockpitApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceUiState(
    val enabled: Boolean = false,
    val status: String = "Sesli tarif kapalı",
)

data class RouteUiState(
    val route: NavigationRoute? = null,
    val destination: GeoPoint? = null,
    val busy: Boolean = false,
    val message: String = "Rota hesaplamak için GPS aç, hedefi seç ve kendi HeiGIT anahtarını gir.",
)

/** No hidden or automatic network requests. The user explicitly requests routing. */
class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CockpitApplication
    private val provider: RouteProvider = OrsRouteProvider()
    private val keys = EncryptedRouteKeyStore(app)
    private val mutable = MutableStateFlow(RouteUiState())
    val state = mutable.asStateFlow()
    private val voiceMutable = MutableStateFlow(VoiceUiState())
    val voice = voiceMutable.asStateFlow()
    private val deliveredCues = mutableSetOf<String>()
    private var speaker: TurkishNavigationSpeech? = null

    init {
        viewModelScope.launch {
            app.locationRepository.state.collect { gps ->
                if (!voiceMutable.value.enabled) return@collect
                val route = mutable.value.route ?: return@collect
                if (!gps.isFresh(SystemClock.elapsedRealtime()) ||
                    gps.point == null || (gps.accuracyMeters ?: 999f) > 100f
                ) return@collect
                val position = RouteProgress.locate(route, gps.point) ?: return@collect
                val index = position.nextManeuverIndex ?: return@collect
                val step = route.maneuvers.getOrNull(index) ?: return@collect
                val cue = VoiceAnnouncementPolicy.select(
                    index, position.distanceToManeuverMeters, step.instructionTr, deliveredCues,
                ) ?: return@collect
                if (speaker?.speak(cue.text) == true) deliveredCues.add(cue.id)
            }
        }
    }

    fun toggleVoice() {
        if (voiceMutable.value.enabled) {
            voiceMutable.value = VoiceUiState()
            speaker?.stop()
            return
        }
        if (speaker == null) {
            val newSpeaker = TurkishNavigationSpeech(app)
            speaker = newSpeaker
            viewModelScope.launch {
                newSpeaker.status.collect { message ->
                    if (voiceMutable.value.enabled) {
                        voiceMutable.update { it.copy(status = message) }
                    }
                }
            }
        }
        voiceMutable.value = VoiceUiState(
            enabled = true,
            status = speaker?.status?.value ?: "Türkçe TTS bekleniyor",
        )
    }

    fun testNextManeuver() {
        val gps = app.locationRepository.state.value
        val route = mutable.value.route
        if (route == null || gps.point == null ||
            !gps.isFresh(SystemClock.elapsedRealtime()) ||
            (gps.accuracyMeters ?: 999f) > 100f
        ) {
            voiceMutable.update { it.copy(status = "Gerçek GPS/rota yok; sesli manevra okunamıyor") }
            return
        }
        val pos = RouteProgress.locate(route, gps.point)
        val step = pos?.nextManeuverIndex?.let { route.maneuvers.getOrNull(it) }
        if (step == null) {
            voiceMutable.update { it.copy(status = "Sıradaki manevra belirlenemedi") }
            return
        }
        if (speaker == null) {
            voiceMutable.update { it.copy(status = "Önce sesli tarifi aç") }
            return
        }
        val metres = pos.distanceToManeuverMeters?.toInt()?.coerceAtLeast(0)
        speaker?.speak(
            if (metres == null) step.instructionTr else
                "$metres metre sonra ${step.instructionTr}",
        )
    }

    override fun onCleared() {
        speaker?.shutdown()
        super.onCleared()
    }

    fun calculate(destinationLatitude: String, destinationLongitude: String) {
        if (mutable.value.busy) return
        val lat = destinationLatitude.trim().replace(',', '.').toDoubleOrNull()
        val lon = destinationLongitude.trim().replace(',', '.').toDoubleOrNull()
        val destination = try {
            GeoPoint(lat ?: Double.NaN, lon ?: Double.NaN)
        } catch (_: IllegalArgumentException) {
            mutable.value = RouteUiState(message = "Hedef enlem ve boylam geçersiz.")
            return
        }
        val gps = app.locationRepository.state.value
        if (!gps.isFresh(SystemClock.elapsedRealtime()) || gps.point == null ||
            (gps.accuracyMeters != null && gps.accuracyMeters > 100f)
        ) {
            mutable.value = RouteUiState(
                destination = destination,
                message = "Gerçek ve yeterince hassas konum yok; GPS takibini açıp konum bekle.",
            )
            return
        }

        deliveredCues.clear()
        speaker?.stop()
        mutable.value = RouteUiState(destination = destination, busy = true, message = "Rota isteniyor...")
        viewModelScope.launch {
            try {
                val userKey = keys.read()
                    ?: throw RouteServiceException("HeiGIT API anahtarı yok. Ayarlar'dan gir.")
                val route = provider.calculate(gps.point, destination, userKey)
                mutable.value = RouteUiState(
                    route = route,
                    destination = destination,
                    message = "Son hesaplanan rota: openrouteservice (trafik dahil değil, sahada doğrulanmadı)",
                )
            } catch (error: RouteServiceException) {
                mutable.value = RouteUiState(
                    destination = destination,
                    message = error.message ?: "Rota hizmeti kullanılamıyor.",
                )
            } catch (_: Exception) {
                mutable.value = RouteUiState(
                    destination = destination,
                    message = "Rota alınamadı. İnternet, konum, anahtar ve servis erişimini kontrol et.",
                )
            }
        }
    }

    fun selectDestination(point: GeoPoint) {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = RouteUiState(
            destination = point,
            message = "Haritada hedef seçildi. Rota sekmesinde Rota hesapla düğmesine bas.",
        )
    }

    fun clear() {
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = RouteUiState(message = "Rota temizlendi.")
    }
}
