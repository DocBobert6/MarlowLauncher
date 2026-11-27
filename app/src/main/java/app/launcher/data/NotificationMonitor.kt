package app.launcher.data

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One posted notification, as the launcher shows it (#31 badges,
 * #127 inline previews + direct reply, #132 long-press preview).
 *
 * The large icon is kept only so #128 dynamic badges can show the
 * sender's avatar / artwork; it is never persisted. The RemoteInput
 * is kept (not just its key) because direct reply needs the exact
 * RemoteInput the app posted to attach results to.
 */
data class NotificationPreview(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val largeIcon: Bitmap?,
    val replyAction: PendingIntent?,
    val replyRemoteInput: RemoteInput?,
) {
    val hasRemoteInput: Boolean get() = replyAction != null && replyRemoteInput != null
}

/**
 * Process-local bus for posted notifications (#31, #127, #128,
 * #132). It is fed by [MediaNotificationListener] — the same
 * NotificationListenerService that already feeds the media player
 * (#120) — so badges and previews need no second service and no
 * second grant: when the listener is not granted, the map is simply
 * empty and badges are absent. Nothing is ever swallowed, cancelled
 * or re-posted here; the launcher only reads.
 *
 * Previews are keyed by package name; the count for a package is
 * its badge count (#31). The map is not persisted — it is rebuilt
 * from the live notification set whenever the listener connects.
 */
object NotificationBus {

    private val _previews = MutableStateFlow<Map<String, List<NotificationPreview>>>(emptyMap())
    val previews: StateFlow<Map<String, List<NotificationPreview>>> = _previews.asStateFlow()

    /** Badge count per package (0 when the listener is not granted). */
    fun badgeCount(packageName: String): Int = _previews.value[packageName]?.size ?: 0

    /** The first preview's large icon for a package, for #128
     *  dynamic badges (null when the app posted no large icon). */
    fun largeIconFor(packageName: String): Bitmap? =
        _previews.value[packageName]?.firstNotNullOfOrNull { it.largeIcon }

    fun onPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val preview = toPreview(sbn) ?: return
        val current = _previews.value.toMutableMap()
        val list = current[preview.packageName].orEmpty()
            .filter { it.key != preview.key } + preview
        current[preview.packageName] = list.sortedByDescending { it.postedAt }
        _previews.value = current
    }

    fun onRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val current = _previews.value.toMutableMap()
        val list = current[sbn.packageName].orEmpty().filter { it.key != sbn.key }
        if (list.isEmpty()) current.remove(sbn.packageName) else current[sbn.packageName] = list
        _previews.value = current
    }

    fun clear() {
        _previews.value = emptyMap()
    }

    /**
     * #127 direct reply: sends the text through the notification's
     * own reply action (RemoteInput), exactly as the system shade
     * does. Returns false when the app posted no reply action.
     */
    fun reply(context: Context, key: String, text: String): Boolean {
        val preview = _previews.value.values.flatten().firstOrNull { it.key == key }
            ?: return false
        val action = preview.replyAction ?: return false
        val remoteInput = preview.replyRemoteInput ?: return false
        return try {
            val intent = Intent()
            val results = Bundle()
            results.putCharSequence(remoteInput.resultKey, text)
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, results)
            action.send(context, 0, intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun toPreview(sbn: StatusBarNotification): NotificationPreview? {
        val notification = sbn.notification ?: return null
        // Ongoing system / media notifications are not app badges:
        // skip notifications the system marks ongoing, and the
        // launcher's own notifications.
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        if (sbn.packageName == "app.launcher") return null
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val largeIcon = try {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
        } catch (e: Exception) {
            null
        }
        var replyAction: PendingIntent? = null
        var replyRemoteInput: RemoteInput? = null
        for (action in notification.actions.orEmpty()) {
            val inputs = action.remoteInputs
            if (action.actionIntent != null && !inputs.isNullOrEmpty()) {
                replyAction = action.actionIntent
                replyRemoteInput = inputs.first()
                break
            }
        }
        return NotificationPreview(
            key = sbn.key,
            packageName = sbn.packageName,
            title = title,
            text = text,
            postedAt = sbn.postTime,
            largeIcon = largeIcon,
            replyAction = replyAction,
            replyRemoteInput = replyRemoteInput,
        )
    }
}
