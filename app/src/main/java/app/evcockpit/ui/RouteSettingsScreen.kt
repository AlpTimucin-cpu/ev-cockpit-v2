package app.evcockpit.ui

import android.content.Intent
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
import kotlinx.coroutines.launch

@Composable
fun RouteSettingsScreen() {
    val context = LocalContext.current
    val store = remember(context) { EncryptedRouteKeyStore(context) }
    val configured by store.isConfigured.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
