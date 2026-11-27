package app.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.launcher.data.MIN_TOUCH_TARGET_DP
import app.launcher.data.appVersionInfo
import app.launcher.model.LauncherSettings

/**
 * Phase 5 reset & debugging tools (#171).
 *
 * Debug screen (versions, cache state, toggle states) plus the two
 * destructive actions, each behind its own confirmation dialog:
 *
 * - Reset layout to defaults: settings back to the Phase 1–4
 *   defaults, one home page, home re-seeded. Hidden apps and the
 *   vault PIN are left alone — this resets the layout, not private
 *   data.
 * - Clear all launcher data: wipes every DataStore key (see
 *   LauncherStore.clearAll) and the in-memory state. The next
 *   launch is a first launch again — home first, with the single
 *   default-home prompt if Marlow isn't the default home.
 *
 * The toggle list is built straight from LauncherSettings, so it
 * always shows the real current state. Version comes from the
 * PackageManager (no BuildConfig reference — see Accessibility.kt).
 *
 * Rebuilt (rules 9, 62, 63): type-scale roles only (no `.sp`
 * literals); toggle labels carry no plan/feature numbers; the two
 * destructive actions are separated, error-coloured verb rows, each
 * confirmed by its own dialog — not filled primary buttons.
 */

@Composable
fun DebugScreen(
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    // The debug screen is a full screen: Back always dismisses it
    // while it is open (rule 58 — enabled follows the open state).
    BackHandler(enabled = true, onBack = onDismiss)
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val version = appVersionInfo(context)
    val homeItems = viewModel.homeItems.collectAsStateValue()
    val homePages = viewModel.homePages.collectAsStateValue()
    val apps = viewModel.apps.collectAsStateValue()
    val hiddenApps = viewModel.hiddenApps.collectAsStateValue()
    val notes = viewModel.notes.collectAsStateValue()
    val tasks = viewModel.tasks.collectAsStateValue()
    val habits = viewModel.habits.collectAsStateValue()
    val timers = viewModel.timers.collectAsStateValue()
    val feedSources = viewModel.feedSources.collectAsStateValue()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                item {
                    Text(
                        "Reset & debugging",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                item { SectionTitle("Versions") }
                item { DebugRow("App version", version.first + " (" + version.second + ")") }
                item { DebugRow("Package", context.packageName) }
                item { SectionTitle("State") }
                item { DebugRow("Installed apps", apps.size.toString()) }
                item { DebugRow("Home items", homeItems.size.toString()) }
                item { DebugRow("Home pages", homePages.size.toString()) }
                item { DebugRow("Hidden apps", hiddenApps.size.toString()) }
                item { DebugRow("Notes / tasks / habits / timers", "${notes.size} / ${tasks.size} / ${habits.size} / ${timers.size}") }
                item { DebugRow("Feed sources", feedSources.size.toString()) }
                item { DebugRow("Icon cache entries", viewModel.iconCacheSize().toString() + " (capped at 256)") }
                item { SectionTitle("Toggle states") }
                items(debugToggleStates(settings), key = { it.first }) { (label, on) ->
                    DebugRow(label, if (on) "ON" else "OFF")
                }
                item { SectionTitle("Reset") }
                item {
                    Text(
                        text = "Reset layout to defaults: settings back to defaults, one home page, " +
                            "home re-seeded. Hidden apps and the vault PIN are kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                item {
                    TextButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET_DP.dp),
                    ) {
                        Text(
                            "Reset layout to defaults",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    Text(
                        text = "Clear all launcher data: wipes every setting, the layout, hidden apps, " +
                            "the vault PIN, notes, tasks, habits, timers and feed sources. The next " +
                            "launch is home-first again, with the single default-home prompt if Marlow " +
                            "isn't the default home. This cannot be undone — " +
                            "export a backup first if you want to keep anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                item {
                    TextButton(
                        onClick = { confirmClearAll = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET_DP.dp),
                    ) {
                        Text(
                            "Clear all launcher data",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                val message = actionMessage
                if (message != null) {
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset layout to defaults?") },
            text = { Text("Settings return to defaults and the home is re-seeded. Hidden apps and the vault PIN are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    viewModel.resetLayoutToDefaults()
                    actionMessage = "Layout reset to defaults."
                }) { Text("Reset layout") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all launcher data?") },
            text = { Text("Everything is wiped — settings, layout, hidden apps, vault PIN, notes, tasks, habits, timers and feed sources. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearAll = false
                    viewModel.clearAllData()
                    actionMessage = "All launcher data cleared. The next launch is home-first again."
                }) { Text("Clear everything") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Every boolean toggle in LauncherSettings, as (label, on) pairs,
 *  for the #171 debug screen. Built from the live settings, so it
 *  can never drift from what the app is actually doing. Labels carry
 *  no plan/feature numbers (rule 62). */
fun debugToggleStates(s: LauncherSettings): List<Pair<String, Boolean>> = listOf(
    "Infinite scroll" to s.infiniteScroll,
    "Dock visible" to s.dockVisible,
    "Dock background" to s.dockBackground,
    "Search pill in dock" to s.searchPillVisible,
    "Pull down for search" to s.pullToSearch,
    "Frequently used row" to s.frequentRowVisible,
    "Remember drawer position" to s.rememberDrawerPosition,
    "Close drawer after launching" to s.drawerAutoClose,
    "Two-column drawer on wide screens" to s.twoColumnDrawer,
    "Show app labels" to s.showLabels,
    "Themed icons" to s.themedIcons,
    "Smartsize icons" to s.smartSizeIcons,
    "Live clock icon" to s.liveClockIcon,
    "One-handed layout" to s.oneHanded,
    "Lock home-screen layout" to s.desktopLocked,
    "Add new apps to Home automatically" to s.autoAddApps,
    "Show music apps on headphone connect" to s.headphoneMusicEnabled,
    "AMOLED black" to s.amoledBlack,
    "Show At a Glance strip" to s.glanceVisible,
    "Calendar events" to s.glanceCalendar,
    "Weather" to s.glanceWeather,
    "Commute" to s.glanceCommute,
    "Finance" to s.glanceFinance,
    "Sports" to s.glanceSports,
    "Smartspacer integration" to s.smartspacerEnabled,
    "Now Brief morning summary" to s.nowBriefEnabled,
    "Search suggestions" to s.searchSuggestions,
    "Hide status bar" to s.hideStatusBar,
    "Wallpaper layer on" to s.wallpaperVisible,
    "Scrolling wallpaper" to s.scrollingWallpaper,
    "Wallpaper gradient" to s.wallpaperGradient,
    "Widget page" to s.widgetPageEnabled,
    "Hidden Desktop widget page" to s.desktopWidgetPageEnabled,
    "Widget overlap" to s.widgetOverlapEnabled,
    "Smart Stack auto-rotate" to s.stackAutoRotate,
    "Auto-hiding media player" to s.mediaPlayerEnabled,
    "Discover feed" to s.feedEnabled,
    "Google feed bridge" to s.googleFeedBridgeEnabled,
    "Gestures" to s.gesturesEnabled,
    "Notification badges" to s.badgesEnabled,
    "Dynamic badges" to s.dynamicBadges,
    "Inline notification previews" to s.inlinePreviewsEnabled,
    "Notification preview in long-press popup" to s.longPressPreviewEnabled,
    "Landscape home screen" to s.landscapeEnabled,
    "Separate layouts per screen" to s.separateScreenLayouts,
    "Two-pane tablet layout" to s.twoPaneTablet,
)
