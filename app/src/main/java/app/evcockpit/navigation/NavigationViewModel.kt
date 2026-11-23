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
    val message: String = "Nereden – nereye noktalarını seç ve rota hesapla.",
    val origin: GeoPoint? = null,  // null means current fresh GPS
    val originLabel: String = "Konumum (GPS)",
    val destinationLabel: String = "",
    val alternatives: List<NavigationRoute> = emptyList(),
    val activeAlternativeIndex: Int = 0,
    val alternativesNote: String = "Rota seçenekleri henüz hesaplanmadı.",
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

    /**
     * Rota için kullanıcıdan enlem/boylam istenmez.
     * Hedef, arama önerisi / kayıtlı favori / haritadan seçimle belirlenir.
     */
    fun calculateSelected() {
        if (mutable.value.busy) return
        val previous = mutable.value
        val destination = previous.destination ?: run {
            mutable.value = previous.copy(message = "Önce Nereye alanından bir hedef seç.")
            return
        }
        val gps = app.locationRepository.state.value
        val gpsValid = gps.point != null &&
            gps.isFresh(SystemClock.elapsedRealtime()) &&
            (gps.accuracyMeters == null || gps.accuracyMeters <= 100f)
        val start = previous.origin ?: gps.point.takeIf { gpsValid }
        if (start == null) {
            mutable.value = previous.copy(
                route = null, alternatives = emptyList(),
                message = "Başlangıç için bir yer seç veya kesin GPS konumunu aç.")
            return
        }
        if (start == destination) {
            mutable.value = previous.copy(message = "Başlangıç ve hedef aynı; başka yer seç.")
            return
        }

        deliveredCues.clear()
        speaker?.stop()
        mutable.value = previous.copy(
            route = null, alternatives = emptyList(),
            busy = true, message = "OSRM yol ağı ve alternatifler sorgulanıyor…")
        viewModelScope.launch {
            try {
                var usingFallback = false
                val options = try {
                    OsrmRouteProvider().calculateAlternatives(start, destination, "")
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    throw cancel
                } catch (osrmError: Exception) {
                    // Kullanıcı kendi ORS anahtarını kaydetmişse eski servise geçilir.
                    // ORS'nin uzun yol alternatif sınırı olduğundan uydurma seçenek eklenmez.
                    val key = runCatching { keys.read() }.getOrNull()
                    if (key.isNullOrBlank()) throw RouteServiceException(
                        "Ücretsiz OSRM rotası alınamadı. Bağlantıyı kontrol edip tekrar dene. " +
                        "İstersen Ayarlar'a kendi ORS anahtarını ekle.")
                    val fallback = provider.calculateAlternatives(start, destination, key)
                    usingFallback = true
                    fallback
                }
                require(options.isNotEmpty()) { "Sağlayıcı rota döndürmedi." }
                val note = if (usingFallback)
                    "OSRM kullanılamadı: ORS yedek rota kullanıldı. " +
                    "Uzun yolda tek rota dönebilir; canlı trafik yok."
                else if (options.size == 1)
                    "OSRM tek gerçek güzergâh döndürdü; 2-3 alternatif garanti değil."
                else "OSRM'den ${options.size} gerçek alternatif rota alındı • trafik yok."
                mutable.value = mutable.value.copy(
                    route = options.first(), alternatives = options,
                    activeAlternativeIndex = 0, alternativesNote = note,
                    busy = false, message = note)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                mutable.value = mutable.value.copy(busy = false,
                    message = "Rota isteği iptal edildi.")
                throw cancel
            } catch (error: RouteServiceException) {
                mutable.value = mutable.value.copy(
                    route = null, alternatives = emptyList(), busy = false,
                    message = error.message ?: "Rota servisi kullanılamıyor.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    route = null, alternatives = emptyList(), busy = false,
                    message = "Rota alınamadı. İnternet veya harita sağlayıcısı kullanılamıyor.")
            }
        }
    }

    fun selectOrigin(point: GeoPoint, name: String) {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            origin = point, originLabel = name.take(140),
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Başlangıç seçildi: ${name.take(80)}. Hedef seçip hesapla.")
    }

    fun useGpsOrigin() {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            origin = null, originLabel = "Konumum (GPS)",
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Başlangıç GPS'e ayarlandı; GPS kapalıysa rota hesaplanamaz.")
    }

    fun clearDestinationSelection() {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(
            destination = null, destinationLabel = "",
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Hedef için adres önerilerinden bir yer seç.",
        )
    }

    fun selectNamedDestination(point: GeoPoint, name: String) {
        selectDestination(point)
        mutable.value = mutable.value.copy(destinationLabel = name.take(140))
    }

    fun selectAlternative(index: Int) {
        val options = mutable.value.alternatives
        if (mutable.value.busy || index !in options.indices) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            route = options[index], activeAlternativeIndex = index,
            message = "Güzergâh ${index + 1} seçildi • enerji tahmini hesaplanabilir, canlı trafik yok.")
    }

    /** Explicit user action: fetch actual waypoint route; no auto-OBD recalculation. */
    fun navigateVia(stops:List<GeoPoint>,destination:GeoPoint) {
        if(mutable.value.busy)return
        if(stops.size>48) {
            mutable.value=RouteUiState(destination=destination,
                message="En çok 48 şarj ara noktası kullanılabilir.")
            return
        }
        val gps=app.locationRepository.state.value
        if(gps.point==null || !gps.isFresh(SystemClock.elapsedRealtime()) ||
            (gps.accuracyMeters?:999f)>100f) {
            mutable.value=RouteUiState(destination=destination,
                message="Önce Ayarlar'dan GPS'i başlat; taze konum olmadan navigasyon yok.")
            return
        }
        deliveredCues.clear()
        speaker?.stop()
        mutable.value=RouteUiState(destination=destination,busy=true,
            message="Duraklı rota açık kullanıcı isteğiyle hesaplanıyor...")
        viewModelScope.launch {
            try {
                val key=keys.read()?:throw RouteServiceException(
                    "HeiGIT anahtarı yok. Ayarlar > API anahtarı bölümüne git.")
                val origin=app.locationRepository.state.value.point
                    ?:throw RouteServiceException("GPS konumu kayboldu; yeniden dene.")
                val via=listOf(origin)+stops+listOf(destination)
                val calculated=provider.calculateVia(via,key)
                mutable.value=RouteUiState(route=calculated,destination=destination,
                    message="Şarj ara duraklı ORS rota yüklendi — TAHMİN, sahada DOĞRULANMADI.")
            } catch(error:RouteServiceException) {
                mutable.value=RouteUiState(destination=destination,
                    message=error.message?:"Rota sağlayıcı hatası.")
            } catch(_:Exception) {
                mutable.value=RouteUiState(destination=destination,
                    message="Duraklı rota alınamadı; internet, GPS, kota ve anahtarı kontrol et.")
            }
        }
    }

    fun selectDestination(point: GeoPoint) {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            route = null, destination = point, destinationLabel = "Haritadan seçilen nokta",
            alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Haritada hedef seçildi. Rota sekmesinde Rota hesapla düğmesine bas.",
        )
    }

    fun clear() {
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = RouteUiState(message = "Rota temizlendi.")
    }
}
