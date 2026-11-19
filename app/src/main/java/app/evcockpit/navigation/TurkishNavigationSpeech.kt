package app.evcockpit.navigation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * User-opted-in Turkish TTS guidance. Requests a transient MAY_DUCK focus
 * only while actually speaking; returns it after completion or failure.
 * App/vehicle support must still be tested on the phone.
 */
class TurkishNavigationSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sequence = AtomicLong()
    val status = MutableStateFlow("Türkçe ses motoru hazırlanıyor")
    @Volatile private var ready = false
    private var speech: TextToSpeech? = null
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setOnAudioFocusChangeListener { newFocus ->
            if (newFocus == AudioManager.AUDIOFOCUS_LOSS) stop()
        }
        .build()

    init {
        speech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(initStatus: Int) {
        if (initStatus != TextToSpeech.SUCCESS) {
            status.value = "Türkçe TTS başlatılamadı"
            return
        }
        val engine = speech ?: run {
            status.value = "Türkçe TTS motoru kullanılamıyor"
            return
        }
        val localeResult = engine.setLanguage(Locale.forLanguageTag("tr-TR"))
        if (localeResult == TextToSpeech.LANG_NOT_SUPPORTED ||
            localeResult == TextToSpeech.LANG_MISSING_DATA
        ) {
            status.value = "Türkçe TTS dil verisi yok; telefona Türkçe ses paketi yüklenmeli"
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                audioManager.abandonAudioFocusRequest(focus)
            }
            @Deprecated("Older TTS versions")
            override fun onError(utteranceId: String?) {
                audioManager.abandonAudioFocusRequest(focus)
                status.value = "Sesli tarif başarısız"
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                audioManager.abandonAudioFocusRequest(focus)
                status.value = "Sesli tarif başarısız"
            }
        })
        ready = true
        status.value = "Türkçe TTS hazır (telefonda doğrulanmadı)"
    }

    fun speak(text: String): Boolean {
        if (!ready) {
            status.value = "TTS hazır değil: ${status.value}"
            return false
        }
        if (text.isBlank()) return false
        val result = audioManager.requestAudioFocus(focus)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            status.value = "Ses odağı alınamadı, tarif atlandı"
            return false
        }
        val id = "ev-route-${sequence.incrementAndGet()}"
        val outcome = speech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (outcome != TextToSpeech.SUCCESS) {
            audioManager.abandonAudioFocusRequest(focus)
            status.value = "TTS motoru ses üretemedi"
            return false
        }
        return true
    }

    fun stop() {
        speech?.stop()
        audioManager.abandonAudioFocusRequest(focus)
    }

    fun shutdown() {
        ready = false
        stop()
        speech?.shutdown()
        speech = null
    }
}
