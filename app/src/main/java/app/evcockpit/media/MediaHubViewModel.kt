package app.evcockpit.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MediaHubState(
    val radio: List<MediaStream> = emptyList(),
    val tv: List<MediaStream> = emptyList(),
    val current: MediaStream? = null,
    val loading: Boolean = false,
    val message: String = "Radyo listesi çevrim içi yüklenir. IPTV yalnız seçtiğin HTTPS M3U dosyasından okunur.",
)

/** Phone player, independent from the existing passenger video player. */
class MediaHubViewModel(app: Application) : AndroidViewModel(app) {
    private val client = RadioBrowserClient()
    private val mutable = MutableStateFlow(MediaHubState())
    val state = mutable.asStateFlow()

    val player = ExoPlayer.Builder(app).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true,
        )
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                mutable.value = mutable.value.copy(
                    message = "Akış oynatılamıyor. Kanal URL'sini ve internet bağlantısını kontrol et.",
                )
            }
        })
    }

    fun loadTurkishRadio() {
        if (mutable.value.loading) return
        mutable.value = mutable.value.copy(loading = true, message = "Türkiye radyoları yükleniyor…")
        viewModelScope.launch {
            try {
                val stations = client.getTurkishStations()
                RadioCatalogStore.save(getApplication(), stations)
                mutable.value = mutable.value.copy(
                    loading = false, radio = stations,
                    message = if (stations.isEmpty()) "HTTPS radyo yayını bulunamadı."
                    else "${stations.size} Türkiye radyosu listelendi.",
                )
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    loading = false, message = "Radyo sunucusuna erişilemedi. Tekrar dene.",
                )
            }
        }
    }

    fun importM3u(raw: String) {
        val channels = M3uPlaylistParser.parse(raw)
        mutable.value = mutable.value.copy(
            tv = channels,
            message = if (channels.isEmpty()) "Geçerli HTTPS M3U kanalı bulunamadı."
                else "${channels.size} IPTV kanalı yüklendi. İçerik erişim hakkından kullanıcı sorumludur.",
        )
    }

    fun play(entry: MediaStream) {
        if (MediaUrlPolicy.validHttps(entry.url) == null) {
            mutable.value = mutable.value.copy(message = "Yalnız HTTPS akışları desteklenir.")
            return
        }
        try {
            val uri = Uri.parse(entry.url)
            val media = MediaItem.Builder().setUri(uri).apply {
                if (uri.path?.endsWith(".m3u8", ignoreCase = true) == true) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }.build()
            player.setMediaItem(media)
            player.prepare()
            player.playWhenReady = true
            mutable.value = mutable.value.copy(current = entry, message = "Oynatılıyor: ${entry.title}")
            if (entry.kind == StreamKind.RADIO) {
                viewModelScope.launch { client.registerClick(entry.id) }
            }
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(message = "Akış başlatılamadı.")
        }
    }

    fun nextRadio() = shiftRadio(1)
    fun previousRadio() = shiftRadio(-1)

    private fun shiftRadio(delta: Int) {
        val items = mutable.value.radio
        if (items.isEmpty()) {
            mutable.value = mutable.value.copy(message = "Önce radyo listesini yükle.")
            return
        }
        val currentIndex = items.indexOfFirst { it.id == mutable.value.current?.id }
        val next = if (currentIndex < 0) 0
            else Math.floorMod(currentIndex + delta, items.size)
        play(items[next])
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        mutable.value = mutable.value.copy(current = null, message = "Oynatma durduruldu.")
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
