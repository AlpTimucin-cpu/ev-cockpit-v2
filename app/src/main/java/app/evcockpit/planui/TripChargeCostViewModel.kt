package app.evcockpit.planui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.planner.charging.ManualChargeTariff
import app.evcockpit.planner.charging.TripChargeCost
import app.evcockpit.planner.charging.TripExpenseEstimate
import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.solver.RouteEnergyPrefix
import app.evcockpit.storage.ManualChargeTariffStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripCostUiState(
    val tariffs: Map<String, ManualChargeTariff> = emptyMap(),
    val estimate: TripExpenseEstimate? = null,
    val message: String = "TL/kWh fiyatı girilmeden gerçek maliyet hesaplanamaz.",
)

/** Only ViewModel calculates route energy and pricing; UI receives read-only state. */
class TripChargeCostViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ManualChargeTariffStore(application)
    private val mutable = MutableStateFlow(TripCostUiState())
    val state = mutable.asStateFlow()
    private var plan: PlanResult.Success? = null
    private var request: PlanRequest? = null

    init {
        viewModelScope.launch {
            store.tariffs.collect { prices ->
                mutable.value = mutable.value.copy(tariffs = prices)
                recalculate()
            }
        }
    }

    fun setTrip(result: PlanResult.Success?, parameters: PlanRequest?) {
        plan = result
        request = parameters
        recalculate()
    }

    private fun recalculate() {
        val current = plan ?: run {
            mutable.value = mutable.value.copy(estimate = null)
            return
        }
        val config = request ?: run {
            mutable.value = mutable.value.copy(estimate = null)
            return
        }
        val routeEnergy = runCatching {
            RouteEnergyPrefix(config.route, config.car, config.temperatureC, config.hvac)
                .totalKwh
        }.getOrNull()
        mutable.value = mutable.value.copy(
            estimate = TripChargeCost.calculate(
                current, config.car.capacityAtSohKwh,
                mutable.value.tariffs, routeEnergy,
            ),
        )
    }

    fun saveManualTariff(operator: String, rawPrice: String) {
        val normalized = TripChargeCost.normalizedOperator(operator)
        val input = rawPrice.trim().replace(',', '.').toDoubleOrNull()
        if (normalized == "operator bilinmiyor" || normalized.length > 100 ||
            input == null || !input.isFinite() || input <= 0.0 || input > 150.0) {
            mutable.value = mutable.value.copy(
                message = "Operatör veya fiyat geçersiz. TL/kWh 0'dan büyük, en fazla 150 olmalı.")
            return
        }
        viewModelScope.launch {
            try {
                store.save(operator, input)
                mutable.value = mutable.value.copy(
                    message = "Kullanıcının MANUEL tarifesi kaydedildi; canlı fiyat DOĞRULANMADI.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    message = "Fiyat kaydedilemedi. Telefon depolamasını kontrol et.")
            }
        }
    }

    fun clearTariff(operator: String) {
        viewModelScope.launch {
            store.remove(operator)
            mutable.value = mutable.value.copy(
                message = "İşletmeci fiyatı silindi; bilinmeyen bedel 0 TL gösterilmez.")
        }
    }
}
