package app.evcockpit.ui.finalhmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue

/**
 * F1 bağımsız ekranı: eski ana Activity ve A–C sekmeleri aynen yerinde.
 * Sadece Ayarlar'daki açık kullanıcı dokunuşuyla açılır.
 * Varsayılan DEMO; "Boş veri" düğmesi gerçek modun veri-yok davranışını gösterir.
 */
class MapFirstDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MapFirstDashboardViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            FinalCockpitTheme {
                MapFirstDashboardScreen(
                    state = state,
                    onDemoChange = vm::showDemo,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}
