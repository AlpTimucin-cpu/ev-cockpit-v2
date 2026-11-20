package app.evcockpit.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import app.evcockpit.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Phone-only audio / IPTV hub.
 * Android Auto video surfaces, Mola's private sources and privileged services are not used.
 */
@Composable
fun MediaHubScreen(vm: MediaHubViewModel, onNavigateTab: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    var radioSelected by remember { mutableStateOf(true) }
    var spokenOrTyped by remember { mutableStateOf("") }
    var voiceStatus by remember { mutableStateOf("") }

    val handleCommand: (String) -> Unit = { phrase ->
        spokenOrTyped = phrase
        when (TurkishMediaVoice.parse(phrase)) {
            MediaVoiceAction.FETCH_RADIO -> { radioSelected = true; vm.loadTurkishRadio() }
            MediaVoiceAction.NEXT_RADIO -> vm.nextRadio()
            MediaVoiceAction.PREVIOUS_RADIO -> vm.previousRadio()
            MediaVoiceAction.STOP -> vm.stop()
            MediaVoiceAction.ROUTE -> onNavigateTab(2)
            MediaVoiceAction.PLAN -> onNavigateTab(3)
            MediaVoiceAction.VIDEO -> onNavigateTab(4)
            MediaVoiceAction.MEDIA -> radioSelected = false
            MediaVoiceAction.UNKNOWN -> voiceStatus = "Komut tanınmadı: $phrase"
        }
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val recognized = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (recognized.isNullOrBlank()) voiceStatus = "Sesli komut alınamadı."
        else { voiceStatus = "Algılanan: $recognized"; handleCommand(recognized) }
    }
    val m3uPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { source ->
                            val buffer = ByteArrayOutputStream()
                            val bytes = ByteArray(8192)
                            while (true) {
                                val count = source.read(bytes)
                                if (count < 0) break
                                if (buffer.size() + count > 512 * 1024) {
                                    error("M3U dosyası çok büyük")
                                }
                                buffer.write(bytes, 0, count)
                            }
                            buffer.toString(Charsets.UTF_8.name())
                        }
                    }.getOrNull()
                }
                if (content == null) voiceStatus = "M3U okunamadı (en fazla 512 KB)."
                else { vm.importM3u(content); radioSelected = false }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("MEDYA MERKEZİ", style = MaterialTheme.typography.titleLarge)
        Text("Mola kodu kullanılmaz • Radyo ve izinli IPTV • Telefon ekranı",
            style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { radioSelected = true }) {
                Text("Türkiye radyoları ${if (radioSelected) "✓" else ""}")
            }
            OutlinedButton(onClick = { radioSelected = false }) {
                Text("IPTV ${if (!radioSelected) "✓" else ""}")
            }
            Button(onClick = { vm.stop() }) { Text("Durdur") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (radioSelected) {
                Button(onClick = { vm.loadTurkishRadio() }, enabled = !state.loading) {
                    Text(if (state.loading) "Yükleniyor" else "Radyoları getir")
                }
                OutlinedButton(onClick = vm::previousRadio) { Text("Önceki") }
                OutlinedButton(onClick = vm::nextRadio) { Text("Sonraki") }
            } else {
                Button(onClick = { m3uPicker.launch(arrayOf("*/*")) }) { Text("M3U dosyası seç") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = spokenOrTyped,
                onValueChange = { spokenOrTyped = it },
                label = { Text("Türkçe komut (örn. sonraki radyo)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { handleCommand(spokenOrTyped) }) { Text("Uygula") }
            OutlinedButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                runCatching { voiceLauncher.launch(intent) }.onFailure {
                    voiceStatus = "Ses tanıma uygulaması bulunamadı."
                }
            }) { Text("Sesli") }
        }
        if (voiceStatus.isNotBlank()) Text(voiceStatus,
            style = MaterialTheme.typography.bodySmall)
        Text(state.message, style = MaterialTheme.typography.bodySmall)
        state.current?.let { current ->
            Text("Şimdi: ${current.title}", style = MaterialTheme.typography.titleSmall)
            if (current.kind == StreamKind.IPTV) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = vm.player } },
                    update = { view -> view.player = vm.player },
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )
            }
        }
        val entries = if (radioSelected) state.radio else state.tv
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(entries, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, modifier = Modifier.weight(1f), maxLines = 1)
                    OutlinedButton(onClick = { vm.play(item) }) { Text("Oynat") }
                }
            }
        }
        Text("Radyo kaynağı: Radio Browser. Kullanıcı seçimi olmadan medya başlatılmaz.",
            style = MaterialTheme.typography.labelSmall)
    }
}
