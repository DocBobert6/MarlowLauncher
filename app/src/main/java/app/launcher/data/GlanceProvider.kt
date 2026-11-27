package app.launcher.data

import android.content.Context
import android.provider.CalendarContract
import app.launcher.model.CalendarEvent
import app.launcher.model.GlanceItem
import app.launcher.model.GlanceSlot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * At a Glance data (#10, #121, #37).
 *
 * The strip is fed by small [GlanceSlotProvider]s, one per slot, so a
 * provider can be added or removed without touching the UI. Two slots
 * are real today: DATE (system clock, always available) and CALENDAR
 * (the system calendar, needs READ_CALENDAR — a denied permission
 * yields no rows and sets [needsCalendarPermission]).
 *
 * WEATHER is real since 2026-11-26 (Open-Meteo, no key — see
 * WeatherProvider.kt). COMMUTE / FINANCE / SPORTS have NO provider
 * chosen yet (BUILD_LOG.md open question 4). They are served by
 * [PlaceholderGlanceProvider], which returns no items and says why —
 * the strip never shows fake traffic, prices or scores.
 *
 * #121 Smartspacer is the same shape: [SmartspacerGlanceProvider]
 * is the integration point, OFF by default, and currently returns
 * nothing — binding the Smartspacer plugin protocol is deferred
 * (see BUILD_LOG.md). Turning the toggle on today changes nothing
 * visible; it only arms the provider slot.
 */
interface GlanceSlotProvider {
    val slot: GlanceSlot
    fun isEnabled(): Boolean
    fun load(context: Context): List<GlanceItem>
    /** Why this slot shows nothing, when it has a reason to give. */
    fun unavailableReason(): String? = null
}

/** #10 the date line — always available, from the system clock. */
class DateGlanceProvider : GlanceSlotProvider {
    override val slot = GlanceSlot.DATE
    override fun isEnabled(): Boolean = true
    override fun load(context: Context): List<GlanceItem> {
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return listOf(GlanceItem(GlanceSlot.DATE, format.format(System.currentTimeMillis())))
    }
}

/** #10 calendar events from the system calendar (today only). */
class CalendarGlanceProvider(private val enabled: () -> Boolean) : GlanceSlotProvider {
    override val slot = GlanceSlot.CALENDAR
    override fun isEnabled(): Boolean = enabled()
    override fun load(context: Context): List<GlanceItem> =
        readTodaysEvents(context).map { event ->
            GlanceItem(
                slot = GlanceSlot.CALENDAR,
                title = event.title,
                subtitle = formatEventTime(event),
            )
        }

    override fun unavailableReason(): String? =
        if (needsCalendarPermission) "Calendar permission not granted" else null

    var needsCalendarPermission: Boolean = false
        private set

    /**
     * Today's events, soonest first, capped at 5. A missing
     * READ_CALENDAR permission (SecurityException) yields no rows and
     * records the gap instead of crashing.
     */
    fun readTodaysEvents(context: Context): List<CalendarEvent> {
        needsCalendarPermission = false
        return try {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + 24L * 60 * 60 * 1000
            val out = ArrayList<CalendarEvent>()
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.ALL_DAY,
                ),
                CalendarContract.Events.DTSTART + " >= ? AND " +
                    CalendarContract.Events.DTSTART + " < ?",
                arrayOf(start.toString(), end.toString()),
                CalendarContract.Events.DTSTART + " ASC",
            )?.use { cursor ->
                while (cursor.moveToNext() && out.size < 5) {
                    val title = cursor.getString(0) ?: continue
                    out.add(
                        CalendarEvent(
                            title = title,
                            startMillis = cursor.getLong(1),
                            location = cursor.getString(2)?.takeIf { it.isNotBlank() },
                            allDay = cursor.getInt(3) == 1,
                        ),
                    )
                }
            }
            out
        } catch (e: SecurityException) {
            needsCalendarPermission = true
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun formatEventTime(event: CalendarEvent): String {
            if (event.allDay) return "All day" + (event.location?.let { " · $it" } ?: "")
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(event.startMillis) + (event.location?.let { " · $it" } ?: "")
        }
    }
}

/** #10 placeholder for a slot with no provider chosen yet. */
class PlaceholderGlanceProvider(
    override val slot: GlanceSlot,
    private val enabled: () -> Boolean,
    private val reason: String,
) : GlanceSlotProvider {
    override fun isEnabled(): Boolean = enabled()
    override fun load(context: Context): List<GlanceItem> = emptyList()
    override fun unavailableReason(): String = reason
}

/** #121 Smartspacer integration point. OFF by default; the plugin
 *  protocol is not bound in this phase, so it contributes no items.
 *  When the protocol is bound, this is where Smartspacer target /
 *  complication data will enter the strip. */
class SmartspacerGlanceProvider(private val enabled: () -> Boolean) : GlanceSlotProvider {
    override val slot = GlanceSlot.DATE // Smartspacer feeds the strip as a whole, not one slot.
    override fun isEnabled(): Boolean = enabled()
    override fun load(context: Context): List<GlanceItem> = emptyList()
    override fun unavailableReason(): String =
        "Smartspacer plugin protocol not bound yet — no Smartspacer data"
}

/** Builds the provider set for the current settings. */
fun glanceProviders(
    calendarEnabled: () -> Boolean,
    weatherEnabled: () -> Boolean,
    commuteEnabled: () -> Boolean,
    financeEnabled: () -> Boolean,
    sportsEnabled: () -> Boolean,
    smartspacerEnabled: () -> Boolean,
    weatherCitySet: () -> Boolean = { false },
    weatherNow: () -> WeatherNow? = { null },
): List<GlanceSlotProvider> = listOf(
    DateGlanceProvider(),
    CalendarGlanceProvider(calendarEnabled),
    WeatherGlanceProvider(weatherEnabled, weatherCitySet, weatherNow),
    PlaceholderGlanceProvider(GlanceSlot.COMMUTE, commuteEnabled, "No commute provider chosen yet"),
    PlaceholderGlanceProvider(GlanceSlot.FINANCE, financeEnabled, "No finance provider chosen yet"),
    PlaceholderGlanceProvider(GlanceSlot.SPORTS, sportsEnabled, "No sports provider chosen yet"),
    SmartspacerGlanceProvider(smartspacerEnabled),
)
