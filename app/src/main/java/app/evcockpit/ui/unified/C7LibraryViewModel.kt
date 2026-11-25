package app.evcockpit.ui.unified

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.R
import app.evcockpit.navigation.DestinationsStore
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.SavedDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class C7LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val addresses = DestinationsStore(application)
    private val routes = C7RouteStore(application)
    private val addressMutable = MutableStateFlow<List<SavedDestination>>(emptyList())
    private val routeMutable = MutableStateFlow<List<C7SavedRoute>>(emptyList())
    private val noticeMutable = MutableStateFlow<Int?>(null)
    val entries = addressMutable.asStateFlow()
    val savedRoutes = routeMutable.asStateFlow()
    val notice = noticeMutable.asStateFlow()

    init {
        viewModelScope.launch {
            try { addresses.entries.collect { addressMutable.value = it } }
            catch (c: CancellationException) { throw c }
            catch (_: Exception) { noticeMutable.value = R.string.a0b_c7_storage_failed }
        }
        viewModelScope.launch {
            try { routes.routes.collect { routeMutable.value = it } }
            catch (c: CancellationException) { throw c }
            catch (_: Exception) { noticeMutable.value = R.string.a0b_c7_storage_failed }
        }
    }

    private fun write(showSuccess: Boolean = true, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                if (showSuccess) noticeMutable.value = R.string.a0b_c7_saved
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { noticeMutable.value = R.string.a0b_c7_storage_failed }
        }
    }

    fun saveAddress(type: String, place: Place) {
        if (!place.valid || place.name.isBlank()) return
        write(type != "RECENT") {
            addresses.save(SavedDestination(type, place.name,
                GeoPoint(place.latitude, place.longitude)))
        }
    }
    fun removeAddress(entry: SavedDestination) = write { addresses.remove(entry) }
    fun clearRecent() = write { addresses.clear("RECENT") }
    fun saveRoute(state: UnifiedUiState, title: String) {
        val route = C7RouteCodec.from(state, title, System.currentTimeMillis())
        if (route == null) { noticeMutable.value = R.string.a0b_c7_storage_failed; return }
        write { routes.save(route) }
    }
    fun removeRoute(route: C7SavedRoute) = write { routes.delete(route.id) }
}
