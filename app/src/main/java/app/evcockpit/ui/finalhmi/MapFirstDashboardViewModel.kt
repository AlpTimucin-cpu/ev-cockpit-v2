package app.evcockpit.ui.finalhmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A0: İlk açılış GERÇEK boş durumdur, sahte telemetri gösterilmez.
 * Demo yalnız debug kaynak setindeki ayrı sağlayıcıdan, kullanıcı açarsa gelir.
 */
class MapFirstDashboardViewModel : ViewModel() {
    private val repository = DashboardRepository()
    private val mutable = MutableStateFlow(
        RealDashboardMapper.map(repository.realInputs.value, System.currentTimeMillis()))
    val state: StateFlow<DashboardUiState> = mutable
    val demoAvailable: Boolean = BuildTypeDemoProvider.available
    private var showDemo: Boolean = false

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(15_000L)
            }
        }
    }

    fun showDemo(enabled: Boolean) {
        showDemo = demoAvailable && enabled
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        mutable.value = if (showDemo) {
            BuildTypeDemoProvider.state(now)
                ?: RealDashboardMapper.map(repository.realInputs.value, now)
        } else {
            RealDashboardMapper.map(repository.realInputs.value, now)
        }
    }
}
