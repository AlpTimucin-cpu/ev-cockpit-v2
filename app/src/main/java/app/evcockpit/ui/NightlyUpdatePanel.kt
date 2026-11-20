package app.evcockpit.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.evcockpit.BuildConfig
import app.evcockpit.updates.NightlyReleaseParser
import app.evcockpit.updates.NightlyUpdateChecker
import kotlinx.coroutines.launch

/** UI notification only. No APK download, PackageInstaller, or auto-install. */
@Composable
fun NightlyUpdatePanel() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val checker = remember { NightlyUpdateChecker() }
    var message by remember { mutableStateOf("Nightly güncellemesi henüz kontrol edilmedi — DOĞRULANMADI") }
    var available by remember { mutableStateOf(false) }

    fun lookup(force: Boolean) {
        scope.launch {
            try {
                val status = checker.check(BuildConfig.CI_COMMIT, force)
                available = status?.newBuildAvailable == true
                message = when {
                    status == null -> "Güncelleme kontrolü DOĞRULANMADI (ağ/API/sürüm bilgisi)."
                    available -> "Farklı bir Nightly sürümü yayımlanmış; indirmeyi açabilirsin."
                    else -> "Bu APK ile yayımlanan Nightly commit'i eşleşiyor."
                }
            } catch (_: Exception) {
                available = false
                message = "Nightly sürümü sorgulanamadı — DOĞRULANMADI"
            }
        }
    }

    LaunchedEffect(Unit) { lookup(false) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("Nightly güncelleme bildirimi (uygulama içi)")
        Text(message)
        OutlinedButton(onClick = { lookup(true) }) {
            Text("Nightly sürümünü kontrol et")
        }
        if (available) {
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse(NightlyReleaseParser.EXPECTED_URL)))
            }) { Text("Yeni APK indirme bağlantısını aç") }
        }
        Text("APK otomatik indirilmez veya kurulmaz. Kaynak: GitHub Releases.")
    }
}
