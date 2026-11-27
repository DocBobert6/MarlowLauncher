package app.launcher.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.launcher.MainActivity
import app.launcher.model.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * #37 Now Brief morning delivery: the daily alarm (scheduled by
 * HomeViewModel.scheduleNowBrief) fires here, and the receiver posts
 * a notification that opens the launcher with the Now Brief sheet
 * (EXTRA_OPEN_NOW_BRIEF) — the notification opens the brief itself,
 * not just the home.
 *
 * If POST_NOTIFICATIONS was revoked after the alarm was scheduled,
 * the post is skipped silently (and the try/catch covers the
 * SecurityException on API 33+).
 *
 * The alarm does not survive a reboot on its own: NowBriefBootReceiver
 * below re-schedules it when the device boots, and the launcher
 * re-schedules it on start and whenever the toggle or time changes.
 */
class NowBriefReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Now Brief",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Your morning Now Brief summary is ready."
                }
                manager.createNotificationChannel(channel)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_NOW_BRIEF, true)
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Good morning — your Now Brief is ready")
                .setContentText("Today's schedule, weather and reminders at a glance.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Permission revoked, or notifications unavailable on this
            // device. The tap-to-open sheet still works.
        }
    }

    companion object {
        const val CHANNEL_ID = "now_brief"
        const val NOTIFICATION_ID = 3701

        /** Launch extra: open the Now Brief sheet on arrival.
         *  Absent (the normal launch, including the Robolectric
         *  startup gate) the home opens as usual. */
        const val EXTRA_OPEN_NOW_BRIEF = "openNowBrief"

        /**
         * (Re)schedules the daily Now Brief alarm for the chosen
         * time, or cancels it when the summary is off. Inexact
         * repeating: the summary may land within a window of the
         * chosen time, and no exact-alarm permission is needed.
         * Shared by HomeViewModel (start / toggle / time change)
         * and the boot receiver below, so the alarm setup lives
         * in exactly one place.
         */
        fun schedule(context: Context, settings: LauncherSettings) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            val intent = Intent(context, NowBriefReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (!settings.nowBriefEnabled) {
                alarmManager.cancel(pending)
                return
            }
            val next = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, settings.nowBriefHour)
                set(java.util.Calendar.MINUTE, settings.nowBriefMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }.timeInMillis
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    next,
                    AlarmManager.INTERVAL_DAY,
                    pending,
                )
            } catch (e: Exception) {
                // Alarms unavailable on this device; the tap-to-open
                // sheet still works.
            }
        }
    }
}

/**
 * #37 boot rescheduling: alarms are cleared on reboot, so when the
 * device boots this receiver re-schedules the daily Now Brief alarm
 * from the stored settings. It does nothing else, and does nothing
 * when the summary is off (schedule() cancels in that case).
 * Exported: BOOT_COMPLETED is a system broadcast, and this receiver
 * ignores any other action.
 */
class NowBriefBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = LauncherStore.loadSettings(context.applicationContext)
                NowBriefReceiver.schedule(context.applicationContext, settings)
            } catch (e: Exception) {
                // Settings unreadable this early in boot; the alarm
                // is re-scheduled again when the launcher starts.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
