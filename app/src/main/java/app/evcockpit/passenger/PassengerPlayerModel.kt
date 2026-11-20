package app.evcockpit.passenger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PassengerState(
    val source: PassengerSource? = null,
    val message: String = "Yolcu oynatıcısı (telefon): cihazda DOĞRULANMADI",
)

/**
 * Activity-scoped ViewModel owns one ExoPlayer across all Compose layout switches.
 * setAudioAttributes(true) opts Media3 into system AudioFocus, including ducking.
 */
class PassengerPlayerModel(app: Application) : AndroidViewModel(app) {
    val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(), true)
    }

    private val mutable = MutableStateFlow(PassengerState())
    val state = mutable.asStateFlow()

    fun playLocalDocument(uri: Uri) {
        val source = PassengerSourcePolicy.parseLocal(uri.toString())
        if (source == null) {
            mutable.value = PassengerState(message = "Yalnız seçilmiş yerel video dosyaları kabul edilir.")
            return
        }
        playMedia(source, uri)
    }

    fun playDirectUrl(raw: String) {
        val source = PassengerSourcePolicy.parseDirectHttps(raw)
        if (source == null) {
            mutable.value = PassengerState(message = "Doğrudan DRM'siz HTTPS video URL'si gir.")
            return
        }
        playMedia(source, Uri.parse(source.url))
    }

    fun playYouTube(raw: String) {
        val source = PassengerSourcePolicy.parseYouTube(raw)
        if (source == null) {
            mutable.value = PassengerState(message = "Geçerli bir YouTube video sayfası bağlantısı gir.")
            return
        }
        player.pause()
        player.clearMediaItems()
        mutable.value = PassengerState(source, "Resmî YouTube IFrame • telefon testi DOĞRULANMADI")
    }

    private fun playMedia(source: PassengerSource, uri: Uri) {
        try {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
            mutable.value = PassengerState(source, "Media3 oynatılıyor (cihaz testi DOĞRULANMADI)")
        } catch (_: Exception) {
            mutable.value = PassengerState(message = "Medya açılamadı; format/erişim kontrolü gerekli.")
        }
    }

    fun pause() = player.pause()

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
