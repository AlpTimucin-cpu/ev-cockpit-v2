package app.evcockpit.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.util.Locale

/** No identifiers or network voice synthesis are requested by this inspector. */
object TtsVoiceStatusStore {
    @Volatile private var status: String = "KONTROL EDİLMEDİ / DOĞRULANMADI"
    fun read(): String = status
    fun update(value: String) {
        status = value.take(160)
    }
}

object TtsOfflineEvidence {
    fun describe(
        initialized: Boolean,
        installedOfflineTurkishVoice: Boolean?,
    ): String = when {
        !initialized -> "TTS başlatılamadı / DOĞRULANMADI"
        installedOfflineTurkishVoice == true ->
            "Türkçe çevrimdışı ses mevcut (TTS motoru bildirimi; ses çıkışı DOĞRULANMADI)"
        installedOfflineTurkishVoice == false ->
            "Türkçe çevrimdışı ses bulunamadı; sistem TTS dil paketlerini kontrol et"
        else -> "TTS ses listesi alınamadı / DOĞRULANMADI"
    }
}

/** User-visible probe, with one TTS engine; always shuts down on panel disposal. */
class TtsOfflineProbe(
    private val context: Context,
    private val onStatus: (String) -> Unit,
) {
    private var tts: TextToSpeech? = null
    private var closed = false
    private val main = Handler(Looper.getMainLooper())

    fun start() {
        if (closed || tts != null) return
        tts = TextToSpeech(context.applicationContext) { initialized ->
            main.post {
                if (closed) return@post
                val engine = tts
                val status = if (initialized != TextToSpeech.SUCCESS || engine == null) {
                    TtsOfflineEvidence.describe(false, null)
                } else {
                    val availability = runCatching {
                        engine.voices?.any { voice ->
                            voice.locale.language == Locale.forLanguageTag("tr-TR").language &&
                                !voice.isNetworkConnectionRequired &&
                                !voice.features.orEmpty()
                                    .contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                        }
                    }.getOrNull()
                    TtsOfflineEvidence.describe(true, availability)
                }
                TtsVoiceStatusStore.update(status)
                onStatus(status)
            }
        }
    }

    fun close() {
        closed = true
        tts?.shutdown()
        tts = null
    }
}

object TtsSettingsShortcut {
    /** Opens a system UI; never changes a setting or installs a voice automatically. */
    fun open(context: Context): Boolean {
        val intents = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in intents) {
            if (runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)) return true
        }
        return false
    }
}
