package app.evcockpit.ui.finalhmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Görünümden bağımsız zamanlayıcı ve Demo/Boş geçişi.
 * Demo, ayrı mapper ile yalnız bellekte oluşturulur; kayıt/OBD servisine gönderilmez.
 */
class MapFirstDashboardViewModel : ViewModel() {
    private val repository = DashboardRepository()
    private val mutable = MutableStateFlow(DemoDashboardMapper.map(System.currentTimeMillis()))
    val state: StateFlow<DashboardUiState> = mutable
    private var showDemo = true

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(15_000L)
            }
        }
    }

    fun showDemo(enabled: Boolean) {
        showDemo = enabled
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        mutable.value = if (showDemo) DemoDashboardMapper.map(now)
            else RealDashboardMapper.map(repository.realInputs.value, now)
    }
}
