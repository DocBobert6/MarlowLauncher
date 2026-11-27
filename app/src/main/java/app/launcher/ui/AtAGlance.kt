package app.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.launcher.data.CalendarGlanceProvider
import app.launcher.model.CalendarEvent
import app.launcher.model.GlanceItem
import app.launcher.model.GlanceSlot
import app.launcher.model.LauncherSettings
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * At a Glance (#10, #121) and Now Brief (#37) — rule 27, decision F6.
 *
 * Text directly on the wallpaper: NO card background, NO border.
 * Resting state is two lines — day + date, and weather (condition +
 * temperature). An upcoming calendar event REPLACES the date line
 * (never a second row, never a badge). The strip is a swipeable
 * complication carousel: resting → today's events → weather detail;
 * a page with no data is skipped, never blank. Split tap targets:
 * tap the date/event line → Calendar, tap the weather line →
 * weather. Long-press opens per-feature toggles plus where the
 * content comes from. Turning features off reduces the strip to
 * day/date — never blank, never an error. No news, no greeting, no
 * promo, no battery percentage.
 *
 * #121 Smartspacer is OFF by default and contributes nothing yet
 * (the plugin protocol is not bound — see GlanceProvider.kt); the
 * toggle lives in settings and arms the provider slot.
 */

/** #10 the strip itself, pinned above the home grid. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtAGlanceStrip(
    items: List<GlanceItem>,
    settings: LauncherSettings,
    todayEvents: List<CalendarEvent>,
    onDateTap: () -> Unit,
    onWeatherTap: () -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onToggleCalendar: (Boolean) -> Unit,
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
    modifier: Modifier = Modifier,
) {
    if (!settings.glanceVisible) return
    var showStripSettings by remember { mutableStateOf(false) }
    val dateItem = items.firstOrNull { it.slot == GlanceSlot.DATE }
    val weatherItem = items.firstOrNull { it.slot == GlanceSlot.WEATHER }
    val now = System.currentTimeMillis()
    // Rule 27: the next upcoming event replaces the date line.
    val nextEvent = todayEvents.firstOrNull { it.startMillis + 60L * 60 * 1000 > now }
    val dateLine = nextEvent?.let {
        it.title + " · " + CalendarGlanceProvider.formatEventTime(it)
    } ?: (dateItem?.title ?: SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now))
    val weatherLine = weatherItem?.let {
        it.title + (it.subtitle?.let { sub -> " · $sub" } ?: "")
    }
    // Carousel pages: resting is always there; events and weather
    // detail join only when they have something to show.
    val pages = buildList {
        add("rest")
        if (todayEvents.isNotEmpty()) add("events")
        if (weatherItem != null) add("weather")
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val textColor = if (overWallpaper) {
        val wallColor = labelColorForWallpaper(wallpaperLuminance)
        if (wallColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else wallColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subColor = if (overWallpaper) textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showStripSettings = true },
            )
            .semantics { contentDescription = "At a Glance" },
    ) {
        if (pages.size <= 1) {
            GlanceRestingLines(
                dateLine = dateLine,
                isEvent = nextEvent != null,
                weatherLine = weatherLine,
                weatherEnabled = settings.glanceWeather,
                textColor = textColor,
                subColor = subColor,
                onDateTap = onDateTap,
                onWeatherTap = onWeatherTap,
            )
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                when (pages[page]) {
                    "rest" -> GlanceRestingLines(
                        dateLine = dateLine,
                        isEvent = nextEvent != null,
                        weatherLine = weatherLine,
                        weatherEnabled = settings.glanceWeather,
                        textColor = textColor,
                        subColor = subColor,
                        onDateTap = onDateTap,
                        onWeatherTap = onWeatherTap,
                    )
                    "events" -> Column {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                        )
                        for (event in todayEvents.take(3)) {
                            Text(
                                text = event.title + " · " + CalendarGlanceProvider.formatEventTime(event),
                                style = MaterialTheme.typography.bodyMedium,
                                color = subColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { onDateTap() },
                            )
                        }
                    }
                    "weather" -> Column {
                        Text(
                            text = weatherLine ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            modifier = Modifier.clickable { onWeatherTap() },
                        )
                        Text(
                            text = "Weather for ${settings.weatherCityName.ifBlank { "your city" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    // Long-press: per-feature toggles + where content comes from.
    // Turning everything off leaves day/date — the resting lines
    // above render from the system clock alone.
    if (showStripSettings) {
        AlertDialog(
            onDismissRequest = { showStripSettings = false },
            title = { Text("At a Glance", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Weather", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.glanceWeather,
                            onCheckedChange = { onToggleWeather(it) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Calendar events", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.glanceCalendar,
                            onCheckedChange = { onToggleCalendar(it) },
                        )
                    }
                    Text(
                        text = "Date comes from the system clock. Weather comes from " +
                            "Open-Meteo for the city you chose in Settings. Events " +
                            "come from the system calendar, and only appear when the " +
                            "calendar permission is granted. With weather and events " +
                            "off, the strip shows the day and date only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStripSettings = false }) { Text("Done") }
            },
        )
    }
}

/** The resting two lines, with split tap targets: the date/event
 *  line opens Calendar, the weather line opens weather. */
@Composable
private fun GlanceRestingLines(
    dateLine: String,
    isEvent: Boolean,
    weatherLine: String?,
    weatherEnabled: Boolean,
    textColor: Color,
    subColor: Color,
    onDateTap: () -> Unit,
    onWeatherTap: () -> Unit,
) {
    Column {
        Text(
            text = dateLine,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onDateTap() },
        )
        if (weatherEnabled && weatherLine != null) {
            Text(
                text = weatherLine,
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { onWeatherTap() },
            )
        }
    }
}

/** #37 the morning summary: today's schedule over the same
 *  calendar data as At a Glance, plus the enabled slots. No health
 *  data — there is no Health Connect dependency in the plan.
 *  Reminders have no source chosen yet, so the sheet says so plainly
 *  instead of inventing any. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowBriefSheet(
    events: List<CalendarEvent>,
    glanceItems: List<GlanceItem>,
    remindersAvailable: Boolean,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text("Now Brief", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            item {
                Text(
                    text = glanceItems.firstOrNull { it.slot == GlanceSlot.DATE }?.title ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                Text("Today's schedule", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            if (events.isEmpty()) {
                item { Text("Nothing scheduled today.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(events, key = { it.title + it.startMillis }) { event ->
                    Text(
                        text = event.title + " · " + CalendarGlanceProvider.formatEventTime(event),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            val weatherItem = glanceItems.firstOrNull { it.slot == GlanceSlot.WEATHER }
            if (weatherItem != null) {
                item {
                    Text("Weather", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
                item {
                    Text(
                        text = weatherItem.title +
                            (weatherItem.subtitle?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Text("Reminders", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            item {
                Text(
                    text = if (remindersAvailable) {
                        "Reminders are connected."
                    } else {
                        "No reminders source connected yet — a reminders provider has not been chosen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null)
                    Text(
                        text = "Commute, finance and sports need a provider " +
                            "before they can appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
