package app.launcher.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.StatFs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.launcher.data.CalendarGlanceProvider
import app.launcher.model.BuiltinWidgetType
import app.launcher.model.CalendarEvent
import app.launcher.model.HabitItem
import app.launcher.model.NoteItem
import app.launcher.model.TaskItem
import app.launcher.model.TimerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Built-in widget suite (#113, #115, #118, #119, #126): first-party
 * widgets, fed by the same data layer as the rest of the launcher
 * (system calendar for #126, DataStore for notes / tasks / timers /
 * habits). They are Compose surfaces, not AppWidgets — they render
 * inside a HomeItem of kind BUILTIN_WIDGET.
 *
 * #126 weather shows the same Open-Meteo data as At a Glance: no
 * city set yet, so it says so and points at Settings. #116
 * communications and #117 finance panels are denied and are not
 * here. #118 Quick Tiles are NOT included (quick settings in the
 * launcher = denied #43).
 */
@Composable
fun BuiltinWidgetSurface(
    type: BuiltinWidgetType,
    viewModel: HomeViewModel,
    todayEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier,
) {
    val notes = viewModel.notes.collectAsStateValue()
    val tasks = viewModel.tasks.collectAsStateValue()
    val timers = viewModel.timers.collectAsStateValue()
    val habits = viewModel.habits.collectAsStateValue()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (type) {
                BuiltinWidgetType.NOTES -> NotesWidgetContent(
                    notes = notes,
                    onAdd = { viewModel.addNote(it) },
                    onDelete = { viewModel.deleteNote(it) },
                )
                BuiltinWidgetType.TASKS -> TasksWidgetContent(
                    tasks = tasks,
                    onAdd = { viewModel.addTask(it) },
                    onToggle = { viewModel.toggleTask(it) },
                    onDelete = { viewModel.deleteTask(it) },
                )
                BuiltinWidgetType.TIMERS -> TimersWidgetContent(
                    timers = timers,
                    onAdd = { label, seconds -> viewModel.addTimer(label, seconds) },
                    onStartStop = { viewModel.toggleTimer(it) },
                    onDelete = { viewModel.deleteTimer(it) },
                )
                BuiltinWidgetType.HABITS -> HabitsWidgetContent(
                    habits = habits,
                    onAdd = { viewModel.addHabit(it) },
                    onToggle = { viewModel.toggleHabit(it) },
                    onDelete = { viewModel.deleteHabit(it) },
                )
                BuiltinWidgetType.CALENDAR -> CalendarWidgetContent(events = todayEvents)
                BuiltinWidgetType.WEATHER -> WeatherWidgetContent(
                    weatherNow = viewModel.weatherNow.collectAsStateValue(),
                    citySet = viewModel.settings.collectAsStateValue().weatherCityName.isNotBlank(),
                    onRefresh = { viewModel.refreshWeather(force = true) },
                )
                BuiltinWidgetType.SYSTEM -> SystemWidgetContent()
                // Audit fix (2026-11-26): the home clock — big live
                // time + date, seeded full-width at the top of page 0.
                BuiltinWidgetType.CLOCK -> ClockWidgetContent()
            }
        }
    }
}

@Composable
private fun WidgetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun WidgetEmpty(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** #119 notes widget: add a line, delete one. */
@Composable
private fun NotesWidgetContent(
    notes: List<NoteItem>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    WidgetTitle("Notes")
    if (notes.isEmpty()) {
        WidgetEmpty("No notes yet — add one below.")
    }
    for (note in notes.take(5)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onDelete(note.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete note")
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("New note") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            if (draft.isNotBlank()) {
                onAdd(draft)
                draft = ""
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add note")
        }
    }
}

/** #118 tasks widget: add, check off, delete. */
@Composable
private fun TasksWidgetContent(
    tasks: List<TaskItem>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    WidgetTitle("Tasks")
    if (tasks.isEmpty()) {
        WidgetEmpty("No tasks yet — add one below.")
    }
    for (task in tasks.take(6)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(task.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (task.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (task.done) "Done" else "Not done",
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = { onDelete(task.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task")
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("New task") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            if (draft.isNotBlank()) {
                onAdd(draft)
                draft = ""
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add task")
        }
    }
}

/** #118 timers widget: presets + custom label, start/stop, delete. */
@Composable
private fun TimersWidgetContent(
    timers: List<TimerItem>,
    onAdd: (String, Int) -> Unit,
    onStartStop: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    var draft by remember { mutableStateOf("") }
    WidgetTitle("Timers")
    if (timers.isEmpty()) {
        WidgetEmpty("No timers yet — add one below.")
    }
    for (timer in timers.take(4)) {
        val running = timer.endsAtMillis > now
        val remaining = if (running) {
            ((timer.endsAtMillis - now) / 1000).toInt().coerceAtLeast(0)
        } else {
            timer.totalSeconds
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timer.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatTimer(remaining), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onStartStop(timer.id) }) {
                Text(if (running) "Stop" else "Start")
            }
            IconButton(onClick = { onDelete(timer.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete timer")
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Timer label") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        for (preset in listOf(1, 5, 10, 25)) {
            TextButton(onClick = { onAdd(draft.ifBlank { "Timer" }, preset * 60) }) {
                Text("${preset}m")
            }
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "%d:%02d".format(minutes, seconds)
    } else {
        "%ds".format(seconds)
    }
}

/** #118 habits widget: daily check-off + streak. */
@Composable
private fun HabitsWidgetContent(
    habits: List<HabitItem>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    WidgetTitle("Habits")
    if (habits.isEmpty()) {
        WidgetEmpty("No habits yet — add one below.")
    }
    for (habit in habits.take(6)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(habit.id) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (habit.doneToday) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (habit.doneToday) "Done today" else "Not done today",
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Text(text = "${habit.streak}d streak", style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { onDelete(habit.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete habit")
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("New habit") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            if (draft.isNotBlank()) {
                onAdd(draft)
                draft = ""
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add habit")
        }
    }
}

/** Rule 21 / §4.2: the home clock widget is TIME ONLY. The At a
 *  Glance strip already shows the date — a date line here duplicates
 *  it. Big live time, ticking once a second like the timers widget.
 *  It reads only the system clock, so the time it shows is always
 *  real. One row high at full width (see HomeViewModel.seedHome).
 *  The ticker runs on Dispatchers.Default, not the Main dispatcher:
 *  under Robolectric the main looper is paused and setup() idles it
 *  (ActivityController.visible -> idleMainLooper), which fast-forwards
 *  through every posted delay — a Main-dispatcher while(true) ticker
 *  reschedules forever and idle() never returns, so the startup gate
 *  hangs with zero output (run 20). On Default the delay runs in real
 *  time on a real thread, the state write is thread-safe and still
 *  schedules recomposition, and on-device ticking is identical.
 *  (ClockIcon in AppIcon.kt has the same shape and the same fix.) */
@Composable
private fun ClockWidgetContent() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
            }
        }
    }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = timeFormat.format(now),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** #126 calendar widget: today's events from the system calendar
 *  (the same source as At a Glance). */
@Composable
private fun CalendarWidgetContent(events: List<CalendarEvent>) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    WidgetTitle(dateFormat.format(System.currentTimeMillis()))
    if (events.isEmpty()) {
        WidgetEmpty("Nothing on the calendar today.")
    }
    for (event in events.take(5)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = CalendarGlanceProvider.formatEventTime(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * #126 weather widget: the same Open-Meteo data as At a Glance
 * (#10). No city set yet → it says so and points at Settings; a
 * city set but nothing fetched yet → fetching; otherwise the live
 * temperature + condition + city. Tapping refreshes. Never a
 * made-up temperature or condition.
 */
@Composable
private fun WeatherWidgetContent(
    weatherNow: app.launcher.data.WeatherNow?,
    citySet: Boolean,
    onRefresh: () -> Unit,
) {
    WidgetTitle("Weather")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onRefresh() },
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = when {
                !citySet -> "No location set yet — set one in Settings → At a Glance."
                weatherNow == null -> "Fetching the forecast… (tap to retry)"
                else -> "${weatherNow.summary} · ${weatherNow.cityName} (tap to refresh)"
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** #115 system-monitor widget: battery, memory, storage — all read
 *  live from the system, no background service. The values refresh
 *  every 5 seconds while the widget is on screen, so a charging
 *  phone or a filling disk shows up without leaving the page. */
@Composable
private fun SystemWidgetContent() {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(readSystemStats(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            stats = readSystemStats(context)
        }
    }
    WidgetTitle("System")
    SystemStatRow("Battery", stats.batteryText)
    SystemStatRow("Memory", stats.memoryText)
    SystemStatRow("Storage", stats.storageText)
}

@Composable
private fun SystemStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class SystemStats(
    val batteryText: String,
    val memoryText: String,
    val storageText: String,
)

private fun readSystemStats(context: Context): SystemStats {
    val batteryText = try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (level < 0 || scale <= 0) {
            "Unknown"
        } else {
            val percent = level * 100 / scale
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            "$percent%" + if (charging) " (charging)" else ""
        }
    } catch (e: Exception) {
        "Unknown"
    }
    val memoryText = try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024 * 1024)
        val availMb = info.availMem / (1024 * 1024)
        val usedMb = totalMb - availMb
        "$usedMb MB / $totalMb MB"
    } catch (e: Exception) {
        "Unknown"
    }
    val storageText = try {
        val stat = StatFs(context.filesDir.path)
        val totalGb = stat.totalBytes / (1024 * 1024 * 1024)
        val freeGb = stat.freeBytes / (1024 * 1024 * 1024)
        "${totalGb - freeGb} GB / $totalGb GB used"
    } catch (e: Exception) {
        "Unknown"
    }
    return SystemStats(batteryText, memoryText, storageText)
}
