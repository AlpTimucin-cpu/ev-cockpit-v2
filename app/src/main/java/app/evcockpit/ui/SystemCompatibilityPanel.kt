package app.evcockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** An expandable diagnostic block in existing OBD Test; not a new cockpit tab. */
@Composable
fun SystemCompatibilityPanel() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var snapshot by remember(context) {
        mutableStateOf(DeviceCompatibilityDiagnostics.capture(context))
    }
    var ttsStatus by remember { mutableStateOf(TtsVoiceStatusStore.read()) }
    var settingsStatus by remember { mutableStateOf("") }

    DisposableEffect(expanded, context) {
        val probe = if (expanded) TtsOfflineProbe(context) { ttsStatus = it }.also {
            it.start()
        } else null
        onDispose { probe?.close() }
    }

    OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "Sistem uyumluluk tanısını kapat" else
            "Sistem uyumluluk tanısı (Android / ROM / TTS)")
    }
    if (expanded) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(snapshot.safeReport(), fontSize = 11.sp)
            Text("Türkçe TTS denetimi: $ttsStatus", fontSize = 11.sp)
            OutlinedButton(onClick = {
                snapshot = DeviceCompatibilityDiagnostics.capture(context)
                ttsStatus = TtsVoiceStatusStore.read()
            }, modifier = Modifier.fillMaxWidth()) { Text("Sistem durumunu yenile") }
            OutlinedButton(onClick = {
                settingsStatus = if (TtsSettingsShortcut.open(context))
                    "TTS veya sistem ayarları açıldı; izinler değiştirilmedi."
                else "TTS ayarları açılamadı. Telefon ayarlarından ses motorunu kontrol et."
            }, modifier = Modifier.fillMaxWidth()) { Text("Türkçe TTS ses ayarlarını aç") }
            if (settingsStatus.isNotBlank()) Text(settingsStatus, fontSize = 11.sp)
            Text("Bu ekran donanım ve kurulu hizmetleri gösterir. GPS, OBD, " +
                "Android Auto ve çevrimdışı ses üretiminin çalıştığını kanıtlamaz.",
                fontSize = 11.sp)
        }
    }
}
