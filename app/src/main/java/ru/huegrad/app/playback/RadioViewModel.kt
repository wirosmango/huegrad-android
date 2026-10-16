package ru.huegrad.app.playback

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.huegrad.app.data.Content

data class RadioState(
    val title: String = "",
    val artist: String = "",
    val artwork: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val ready: Boolean = false,
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
)

class RadioViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    private var controller: MediaController? = null

    // Избранные треки хранятся по uri трека
    private val prefs = app.getSharedPreferences("huegrad_radio", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            if (c.mediaItemCount == 0) {
                c.setMediaItems(Content.tracks.map { it.toMediaItem() })
                c.prepare()
            }
            sync(c)
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                delay(100)
                controller?.takeIf { it.isPlaying }?.let { sync(it) }
            }
        }
    }

    private fun sync(p: Player) {
        val md = p.mediaMetadata
        _state.value = RadioState(
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            artwork = md.artworkUri,
            isPlaying = p.isPlaying,
            positionMs = p.currentPosition.coerceAtLeast(0),
            durationMs = p.duration.coerceAtLeast(0),
            ready = true,
            currentIndex = p.currentMediaItemIndex.coerceAtLeast(0),
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            speed = p.playbackParameters.speed,
        )
    }

    fun togglePlay() = controller?.let { if (it.isPlaying) it.pause() else it.play() }
    fun next() = controller?.seekToNext()
    fun playTrack(index: Int) {
        val c = controller ?: return
        if (index in Content.tracks.indices) {
            c.seekTo(index, 0L)
            c.play()
        }
    }
    fun previous() = controller?.seekToPrevious()
    fun back10() = controller?.seekBack()
    fun forward10() = controller?.seekForward()
    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    /** Повтор: выкл -> весь плейлист -> один трек -> выкл. */
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /** Скорость по кругу: 1x -> 1.25x -> 1.5x -> 2x -> 0.75x -> 1x. */
    fun cycleSpeed() {
        val c = controller ?: return
        val current = c.playbackParameters.speed
        c.setPlaybackSpeed(SPEEDS.firstOrNull { it > current + 0.001f } ?: SPEEDS.first())
    }

    fun toggleFavorite(uri: String) {
        val updated = _favorites.value.let { if (uri in it) it - uri else it + uri }
        _favorites.value = updated
        prefs.edit().putStringSet("favorites", updated).apply()
    }

    fun seekToFraction(f: Float) {
        val c = controller ?: return
        if (c.duration > 0) c.seekTo((c.duration * f).toLong())
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

private fun ru.huegrad.app.data.Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(cover?.let(Uri::parse))
                .build()
        )
        .build()
