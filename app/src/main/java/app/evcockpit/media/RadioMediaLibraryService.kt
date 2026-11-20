package app.evcockpit.media

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Audio-only Media3 library for supported Android Auto clients.
 * NEVER exposes videos or custom Android Auto screen surfaces.
 *
 * Library data is a local snapshot refreshed from the phone screen;
 * on first launch, the user must load stations on the phone while parked.
 */
@OptIn(UnstableApi::class)
class RadioMediaLibraryService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    private fun root(): MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Türkiye Radyoları")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .build()

    private fun stationItem(source: MediaStream): MediaItem = MediaItem.Builder()
        .setMediaId(PREFIX + source.id)
        .setUri(source.url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(source.title)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()

    private fun findStation(mediaId: String): MediaStream? {
        if (!mediaId.startsWith(PREFIX)) return null
        val id = mediaId.removePrefix(PREFIX)
        return RadioCatalogStore.load(this).firstOrNull { it.id == id }
    }

    private val callback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val stations = if (parentId == ROOT_ID) {
                RadioCatalogStore.load(this@RadioMediaLibraryService)
                    .drop(page.toLong().times(pageSize).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    .take(pageSize)
                    .map(::stationItem)
            } else emptyList()
            return Futures.immediateFuture(LibraryResult.ofItemList(stations, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = if (mediaId == ROOT_ID) root()
                else findStation(mediaId)?.let(::stationItem)
            return Futures.immediateFuture(
                if (item == null) LibraryResult.ofError(
                    androidx.media3.session.SessionError.ERROR_BAD_VALUE,
                )
                else LibraryResult.ofItem(item, null),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val available = mediaItems.mapNotNull { findStation(it.mediaId)?.let(::stationItem) }
            return Futures.immediateFuture(available)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(),
                true,
            )
        }
        session = MediaLibrarySession.Builder(this, requireNotNull(player), callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session

    override fun onDestroy() {
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "evcockpit-radio-root"
        private const val PREFIX = "evcockpit-radio:"
    }
}
