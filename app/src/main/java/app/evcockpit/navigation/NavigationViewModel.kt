package app.evcockpit.navigation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.CockpitApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

        mutable.value = RouteUiState(destination = destination, busy = true, message = "Rota isteniyor...")
        viewModelScope.launch {
            try {
                val userKey = keys.read()
                    ?: throw RouteServiceException("HeiGIT API anahtarı yok. Rota Ayarları'ndan gir.")
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
        mutable.value = RouteUiState(
            destination = point,
            message = "Haritada hedef seçildi. Rota sekmesinde Rota hesapla düğmesine bas.",
        )
    }

    fun clear() {
        mutable.value = RouteUiState(message = "Rota temizlendi.")
    }
}
