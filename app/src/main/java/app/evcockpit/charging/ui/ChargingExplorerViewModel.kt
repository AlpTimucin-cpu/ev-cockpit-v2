package app.evcockpit.charging.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.charging.ocm.OcmKeyStore
import app.evcockpit.charging.ocm.OcmRepository
import app.evcockpit.charging.ocm.OcmStation
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.NavigationRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChargingExplorerState(
    val stations: List<OcmStation> = emptyList(),
    val selected: OcmStation? = null,
    val loading: Boolean = false,
    val hasOcmKey: Boolean = false,
    val stationMessage: String = "Rota üzerinde Open Charge Map taraması yapılmadı.",
    val tariffMessage: String = "Tarifeler okunuyor…",
    val catalog: TariffCatalog = TariffCatalog(emptyList()),
    val manual: Map<String, ManualPrice> = emptyMap(),
    val incomplete: Boolean = true,
    val addedStops: List<OcmStation> = emptyList(),
)

class ChargingExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val keys = OcmKeyStore(application)
    private val repo = OcmRepository()
    private val tariffs = TariffRepository(application)
    private val mutable = MutableStateFlow(ChargingExplorerState())
    val state = mutable.asStateFlow()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            keys.hasKey.collect { available ->
                mutable.value = mutable.value.copy(hasOcmKey = available)
            }
        }
        refreshTariffs(false)
    }

    fun refreshTariffs(force: Boolean = true) {
        viewModelScope.launch {
            val loaded = runCatching { tariffs.load(force) }.getOrNull()
            val manual = runCatching { tariffs.manualOverrides() }.getOrDefault(emptyMap())
            mutable.value = mutable.value.copy(
                catalog = loaded?.catalog ?: TariffCatalog(emptyList()),
                manual = manual,
                tariffMessage = loaded?.let { "Tarifeler: ${it.origin} • fiyatlar doğrulanmalı" }
                    ?: "Tarife kataloğu açılamadı; tüm fiyatlar bilinmiyor.",
            )
        }
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            val result = runCatching { keys.save(key) }
            mutable.value = mutable.value.copy(stationMessage =
                if (result.isSuccess) "OCM anahtarı şifreli kaydedildi."
                else "OCM anahtarı kaydedilemedi; hesabındaki değeri kontrol et.")
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            keys.clear()
            mutable.value = mutable.value.copy(stations = emptyList(),
                selected = null, stationMessage = "OCM anahtarı silindi; canlı sorgu kapalı.")
        }
    }

    fun search(route: NavigationRoute?) {
        if (fetchJob?.isActive == true) return
        if (route == null || route.geometry.size < 2) {
            mutable.value = mutable.value.copy(stationMessage = "Önce gerçek rota seç.")
            return
        }
        fetchJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(
                loading = true, stations = emptyList(), selected = null,
                incomplete = true, stationMessage = "Rota çevresindeki OCM kayıtları aranıyor…")
            try {
                val key = keys.read() ?: run {
                    mutable.value = mutable.value.copy(loading = false,
                        stationMessage = "Open Charge Map anahtarını bu panelde kaydet.")
                    return@launch
                }
                val result = repo.search(route.geometry, key)
                mutable.value = mutable.value.copy(
                    loading = false, stations = result.stations,
                    incomplete = !result.complete, stationMessage = result.message,
                )
            } catch (cancel: CancellationException) {
                mutable.value = mutable.value.copy(loading = false,
                    stationMessage = "İstasyon taraması iptal edildi.")
                throw cancel
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false,
                    stationMessage = "OCM isteği tamamlanamadı; internet, anahtar veya kota sorunu.")
            }
        }
    }

    fun cancel() { fetchJob?.cancel() }
    fun select(station: OcmStation?) {
        mutable.value = mutable.value.copy(selected = station)
    }
    fun addStop(station: OcmStation) {
        val current = mutable.value.addedStops
        if (current.any { it.id == station.id } || current.size >= 10) return
        mutable.value = mutable.value.copy(
            addedStops = (current + station).sortedBy { it.alongKm })
    }
    fun removeStop(id: Long) {
        mutable.value = mutable.value.copy(
            addedStops = mutable.value.addedStops.filterNot { it.id == id })
    }
    fun savePrice(id: String, kind: ChargingKind, tl: Double) {
        viewModelScope.launch {
            val success = runCatching { tariffs.saveManual(id, kind, tl) }.isSuccess
            mutable.value = mutable.value.copy(
                manual = tariffs.manualOverrides(),
                tariffMessage = if (success) "Kullanıcı tarifesi kaydedildi; resmî fiyat değil."
                else "Fiyat kaydedilemedi; 0,01–150 TL/kWh aralığı gerekli.",
            )
        }
    }
    fun clearPrice(id: String) {
        viewModelScope.launch {
            tariffs.clearManual(id)
            mutable.value = mutable.value.copy(manual = tariffs.manualOverrides())
        }
    }
}
