package app.evcockpit.ui.unified

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * C3 durum katmanı: ağ/OBD/GPS çağrısı yapmaz. Gerçek repo bağları C4–C7'de.
 * SavedStateHandle'da yalnız küçük koordinat ve ekran tercihleri saklanır.
 * Uygulama öldükten sonra rota geometrisi saklanmaz; yenisi hesaplanmalıdır.
 */
@HiltViewModel
class UnifiedViewModel @Inject constructor(
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(restore())
    val state: StateFlow<UnifiedUiState> = mutable

    fun dispatch(event: UnifiedEvent) {
        val after = SheetReducer.reduce(mutable.value, event)
        mutable.value = after
        persist(after)
    }

    private fun restore(): UnifiedUiState {
        fun read(prefix: String): Place? {
            val lat = saved.get<Double>("${prefix}_lat") ?: return null
            val lon = saved.get<Double>("${prefix}_lon") ?: return null
            val name = saved.get<String>("${prefix}_name") ?: return null
            return Place(prefix, name, lat, lon,
                saved.get<String>("${prefix}_source") ?: "KAYIT")
                .takeIf { it.valid }
        }
        val origin = read("origin")
        val destination = read("destination")
        val mode = saved.get<String>("sheet_mode") ?: "Idle"
        val detent = runCatching {
            SheetDetent.valueOf(saved.get<String>("sheet_detent") ?: "PEEK")
        }.getOrDefault(SheetDetent.PEEK)
        val storedSelected = (saved.get<Int>("selected_index") ?: 0).coerceAtLeast(0)
        // Rota yalnız yeni ağ sorgusuyla üretilebilir. Kayıtlı seçimi gösteremeyiz.
        val staleRoute = mode in setOf("RoutePreview", "Navigating")
        return UnifiedUiState(
            origin = origin,
            destination = destination,
            mode = SheetMode.Idle,
            detent = if (staleRoute) SheetDetent.HALF else detent,
            routes = if (staleRoute) Loadable.Failed(UserMessage(
                "Önceki rota yeniden hesaplanmalı.", "Yeniden dene"))
                else Loadable.Idle,
            banners = if (staleRoute) listOf(Banner(
                UserMessage("Kayıtlı rota yeniden doğrulanmalı.", "Rota hesapla"),
                BannerKind.INFO))
                else emptyList(),
        )
    }

    private fun persist(s: UnifiedUiState) {
        fun write(prefix: String, place: Place?) {
            saved["${prefix}_lat"] = place?.latitude
            saved["${prefix}_lon"] = place?.longitude
            saved["${prefix}_name"] = place?.name
            saved["${prefix}_source"] = place?.source
        }
        write("origin", s.origin)
        write("destination", s.destination)
        saved["sheet_detent"] = s.detent.name
        saved["sheet_mode"] = when (s.mode) {
            is SheetMode.Idle -> "Idle"
            is SheetMode.RoutePreview -> "RoutePreview"
            is SheetMode.Navigating -> "Navigating"
            is SheetMode.StopDetail -> "StopDetail"
            is SheetMode.Energy -> "Energy"
        }
        saved["selected_index"] = (s.mode as? SheetMode.RoutePreview)?.selected ?: 0
    }
}
