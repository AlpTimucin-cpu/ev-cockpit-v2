package app.evcockpit.ui.finalhmi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F1 deposu salt boş, bağımsız durum taşır.
 * OBD, rota, API veya konum kaynağına BAĞLANMAZ.
 * F3–F5 veri bağlayıcıları eklendiğinde yalnız gerçek girdiler buradan akacak.
 */
class DashboardRepository {
    private val mutable = MutableStateFlow(RealDashboardInput())
    val realInputs: StateFlow<RealDashboardInput> = mutable

    // F1'de veri yazıcı yöntem bulunmaması, önizleme verilerinin sürüşe sızmasını engeller.
}
