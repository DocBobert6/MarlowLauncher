package app.launcher.data

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.launcher.model.MediaNowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Auto-hiding media player (#120): the launcher plays nothing
 * itself — it mirrors the active MediaSession of whatever app is
 * actually playing, via MediaSessionManager.
 *
 * Reading another app's sessions needs a NotificationListenerService
 * (the only public route for a third-party app); the listener is
 * granted by the user in system settings. Until it is granted, there
 * is no session to mirror and the player stays hidden — the media
 * bar says so plainly when its toggle is on.
 *
 * The listener pushes into [MediaSessionBus], a process-local
 * singleton the ViewModel exposes; transport actions (play / pause /
 * next / previous) go through the active session's controller.
 */
object MediaSessionBus {

    private val _nowPlaying = MutableStateFlow<MediaNowPlaying?>(null)
    val nowPlaying: StateFlow<MediaNowPlaying?> = _nowPlaying.asStateFlow()

    /** Whether the notification listener is currently connected. */
    val listenerConnected = MutableStateFlow(false)

    private var activeController: MediaController? = null

    fun update(nowPlaying: MediaNowPlaying?, controller: MediaController?) {
        activeController = controller
        _nowPlaying.value = nowPlaying
    }

    fun clear() {
        activeController = null
        _nowPlaying.value = null
    }

    fun play() {
        try {
            activeController?.transportControls?.play()
        } catch (e: Exception) {
            // Session gone; the next listener refresh clears the bar.
        }
    }

    fun pause() {
        try {
            activeController?.transportControls?.pause()
        } catch (e: Exception) {
            // Session gone; the next listener refresh clears the bar.
        }
    }

    fun skipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            // Session gone; the next listener refresh clears the bar.
        }
    }

    fun skipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            // Session gone; the next listener refresh clears the bar.
        }
    }
}

/**
 * The notification listener that feeds the media player. It does
 * nothing with notifications themselves — it only re-queries the
 * active media sessions whenever the notification set changes (a
 * media notification appearing / disappearing is the signal that
 * playback started or stopped).
 */
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        MediaSessionBus.listenerConnected.value = true
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        MediaSessionBus.listenerConnected.value = false
        MediaSessionBus.clear()
        // Phase 4 (#31, #127): badges + previews die with the listener.
        NotificationBus.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
        // Phase 4: the same listener feeds badges + inline previews.
        NotificationBus.onPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSessions()
        NotificationBus.onRemoved(sbn)
    }

    /** Picks the active session to mirror: a PLAYING session first,
     *  so a paused session never shadows the one actually playing. */
    fun refreshSessions() {
        try {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(this, MediaNotificationListener::class.java)
            val controllers = manager.getActiveSessions(component)
            val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull { it.playbackState != null }
            if (controller == null) {
                MediaSessionBus.clear()
                return
            }
            val metadata = controller.metadata
            val state = controller.playbackState
            val playing = state?.state == PlaybackState.STATE_PLAYING
            val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            MediaSessionBus.update(
                MediaNowPlaying(
                    title = title?.takeIf { it.isNotBlank() } ?: controller.packageName,
                    artist = artist?.takeIf { it.isNotBlank() } ?: "",
                    packageName = controller.packageName,
                    playing = playing,
                    positionMs = state?.position ?: 0L,
                    durationMs = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                ),
                controller,
            )
        } catch (e: SecurityException) {
            // Listener not granted (any more); nothing to mirror.
            MediaSessionBus.clear()
        } catch (e: Exception) {
            MediaSessionBus.clear()
        }
    }
}
