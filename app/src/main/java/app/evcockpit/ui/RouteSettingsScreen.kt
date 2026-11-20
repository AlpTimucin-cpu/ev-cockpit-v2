package app.evcockpit.ui

import android.content.Intent
import app.evcockpit.ui.NightlyUpdatePanel
import android.os.Build
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.evcockpit.navigation.EncryptedRouteKeyStore
import app.evcockpit.obd.ManualSocInput
import app.evcockpit.location.GpsSnapshot
import kotlinx.coroutines.launch

@Composable
fun RouteSettingsScreen(
    gps: GpsSnapshot,
    manualSocPct: Double?,
    onSaveManualSoc: (Double) -> Unit,
    onClearManualSoc: () -> Unit,
    onStartGps: () -> Unit,
    onStopGps: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { EncryptedRouteKeyStore(context) }
    val configured by store.isConfigured.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Manuel batarya SoC (OBD yanıt vermezse)")
        Text("Kayıtlı manuel SoC: " +
            (manualSocPct?.let { "%.1f %%".format(it) } ?: "yok"))
        OutlinedTextField(
            value = manualInput, onValueChange = { manualInput = it },
            label = { Text("Gösterge SoC (%) — 0-100") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            val soc = ManualSocInput.parse(manualInput)
            if (soc == null) {
                manualError = "0 ile 100 arasında geçerli SoC girin."
            } else {
                onSaveManualSoc(soc)
                manualError = null
                manualInput = ""
            }
        }) { Text("Manuel SoC kaydet") }
        OutlinedButton(onClick = onClearManualSoc, enabled = manualSocPct != null) {
            Text("Manuel SoC sil")
        }
        manualError?.let { Text(it) }
        Text("OBD SoC yanıt verirse öncelik OBD'dedir; PID ölçeği doğrulanmadı etiketi korunur.")
        NightlyUpdatePanel()
        Text("Konum: ${gps.status}")
        Text("Hassasiyet: ${gps.accuracyMeters?.toInt()?.toString() ?: "--"} m • "
            + (gps.provider ?: "Konum sağlayıcı yok"))
        Button(onClick = onStartGps) { Text("GPS takibini başlat") }
        OutlinedButton(onClick = onStopGps) { Text("GPS takibini durdur") }
        Text("GPS izni bu düğmeyle istenir. Konum işlenmesi için görünür bildirimli konum servisi kullanılır.")
        Text("Arka plan servisleri için pil / otomatik başlatma ayarları")
        Text(BackgroundSettingsGuide.explanation(Build.MANUFACTURER))
        OutlinedButton(onClick = {
            status = if (BackgroundSettingsGuide.openBatteryOptimizationSettings(context)) {
                "Pil optimizasyonu sistem sayfası istendi; ayarı kendiniz kontrol edin."
            } else "Pil optimizasyonu ayarı açılamadı; telefon Ayarlar'dan açın."
        }) { Text("Pil optimizasyonu ayarlarını aç") }
        OutlinedButton(onClick = {
            status = if (BackgroundSettingsGuide.openAutostartOrAppDetails(context)) {
                "Otomatik başlatma / uygulama bilgileri sayfası istendi."
            } else "Otomatik başlatma ayarı açılamadı; telefon Ayarlar'dan açın."
        }) { Text("Otomatik başlatma / uygulama ayarları") }
        Text("Bu düğmeler izni değiştirmez, yalnızca sistem sayfasını açar. " +
            "Arka planda sorunsuz çalışma DOĞRULANMADI.")
        Text("Rota servisi: openrouteservice (HeiGIT)")
        Text("API anahtarı: " + if (configured) "kayıtlı (doğrulanmadı)" else "kayıtlı değil")
        Text("Anahtarını kendi HeiGIT hesabından oluştur. Bu uygulama anahtar üretmez.")
        OutlinedButton(onClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://account.heigit.org/")),
                )
            } catch (_: Exception) {
                status = "Hesap sayfası açılamadı."
            }
        }) { Text("HeiGIT hesap sayfasını aç") }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Kendi API anahtarın") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    try {
                        store.save(input)
                        input = "" // Remove plaintext from Compose state immediately.
                        status = "Anahtar Android Keystore + DataStore ile şifreli kaydedildi."
                    } catch (_: Exception) {
                        status = "Anahtar kaydedilemedi; geçerliliği ve cihaz güvenlik ayarlarını kontrol et."
                    }
                }
            },
            enabled = input.isNotBlank(),
        ) { Text("Anahtarı şifreli kaydet") }
        if (configured) {
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        store.clear()
                        input = ""
                        status = "Kaydedilmiş anahtar silindi."
                    } catch (_: Exception) {
                        status = "Anahtar silinemedi."
                    }
                }
            }) { Text("Kaydedilmiş anahtarı sil") }
        }
        status?.let { Text(it) }
        Text("Gizlilik: veri dosyasına yalnızca AES-GCM şifreli içerik yazılır. "
            + "Android Keystore anahtarı dışarı aktarılmaz. "
            + "Telefonun ele geçirilmesine karşı mutlak güvenlik garantisi yoktur.")
        Text("Anahtarın doğruluğu ancak HeiGIT isteğiyle test edilebilir; "
            + "API kotası ve servis erişilebilirliği sağlayıcıya bağlıdır.")
    }
}
