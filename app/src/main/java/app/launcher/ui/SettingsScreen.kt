@file:OptIn(ExperimentalFoundationApi::class)

package app.launcher.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.launcher.data.AdaptiveLayout
import app.launcher.data.BackupManager
import app.launcher.data.appVersionInfo
import app.launcher.model.AccentColour
import app.launcher.model.BadgePosition
import app.launcher.model.BadgeStyle
import app.launcher.model.BarVisibility
import app.launcher.model.DrawerSort
import app.launcher.model.DrawerStyle
import app.launcher.model.FontChoice
import app.launcher.model.GestureAction
import app.launcher.model.GridSize
import app.launcher.model.IconPack
import app.launcher.model.IconShape
import app.launcher.model.IndicatorColour
import app.launcher.model.LauncherSettings
import app.launcher.model.LauncherThemeChoice
import app.launcher.model.PageIndicatorStyle
import app.launcher.model.SearchBarPlacement
import app.launcher.model.SearchProvider
import app.launcher.model.SurfaceStyle
import app.launcher.model.ThemeMode

/**
 * Rebuilt settings (rules 61–65, 67).
 *
 * Information architecture (§4.3 map, rule 61): the top level has
 * seven sections — Wallpaper & style, Home screen, App drawer, Dock,
 * Gestures, Appearance, About — plus verb rows at the bottom (Hidden
 * apps vault, Back up home screen, Restore backup, Reset &
 * debugging). No screen shows more than 15 rows. Every sub-screen
 * title matches its entry label exactly. The entry point is exactly
 * "Settings" (the long-press menu calls it "Home settings") — never
 * "Options", "Preferences", or "Launcher options". There is no
 * "Advanced" (no screen has ≥3 rarely-changed items to hide) and no
 * vague section title ("General", "Misc", "Other settings").
 *
 * Row wording (rule 62): every row shows its current value as
 * supporting text ("5 × 5", "On", "System default") — subtext states
 * the VALUE, never a description. Labels are brief, most-important-
 * word-first, sentence case; no generic-verb labels, no row repeats
 * its section title's words (so inside the Dock screen the rows are
 * "Visible", "Height", "Background" — not "Dock height"), no negative
 * labels, no first-person labels, no parentheticals. Buttons are Verb
 * + Object ("Reset home screen", not "Reset"). No user-visible string
 * contains a plan/feature number or a developer aside.
 *
 * Row types (rule 63): settings (control or chevron), status lines
 * (no switch, no chevron, no ripple; bottom of a screen or About),
 * and actions (one-off verbs; destructive ones separated, grey /
 * negative, confirmed). A dependent setting sits directly below its
 * parent, disabled WITH a reason ("Turn on At a Glance to change
 * this") rather than disappearing.
 *
 * Controls (rule 64): switches over checkboxes; single-choice
 * settings use radio on a sub-screen ([ChoiceKey]); toggles apply
 * immediately, no Save button. No device-level preference is
 * duplicated (no Marlow toggle for system dark-theme scheduling,
 * system font size, system animation scale, or system notification
 * sound).
 *
 * Search (rule 65): live over the index below; matches title +
 * synonyms + effect words ("dark" → dark theme); each result shows
 * its breadcrumb ("Home settings › App drawer"); tapping a result
 * lands DIRECTLY on that setting's sub-screen with the row visible
 * (scrolled to) AND highlighted. No-match shows an honest empty
 * state. Reachable in ≤2 taps from home (long-press empty space →
 * "Home settings").
 *
 * Back (rule 58): one [BackHandler] for settings navigation — it pops
 * one level while a sub-screen is open and dismisses settings at the
 * top level. Its behaviour follows the observable nav state; dialogs
 * (priming, rename, pickers) consume Back themselves via
 * onDismissRequest. No predictive-back preview animation here — a
 * later motion pass refines it.
 *
 * Permissions (rule 67): every ask goes through the priming dialog
 * ([PermissionPrimingDialog]) — one sentence stating what the user
 * gets, cancellable without the system dialog ever firing; the system
 * dialog fires only after opt-in. A denial degrades only the
 * dependent feature, with a one-line explanation on its row or as a
 * status line at the bottom of its screen.
 *
 * Dex-guard discipline: the screen is split into one small composable
 * per sub-screen — no single composable anywhere near 255 registers.
 */

/**
 * The settings areas (kept for source compatibility: HomeScreen
 * passes an initial area when it opens Settings for one job, e.g.
 * HOME from the At a Glance setup row). The ten old areas are mapped
 * onto the new seven-section map by [initialNavFor] — SEARCH lands on
 * the App drawer screen (search settings live there now),
 * NOTIFICATIONS on the Notifications screen, THEMES on Appearance,
 * PRIVACY / BACKUP on the top level with their action row
 * highlighted.
 */
enum class SettingsArea(val label: String) {
    HOME("Home"),
    DRAWER("Drawer"),
    DOCK("Dock"),
    SEARCH("Search"),
    THEMES("Themes"),
    GESTURES("Gestures"),
    NOTIFICATIONS("Notifications"),
    PRIVACY("Privacy"),
    BACKUP("Backup"),
    ABOUT("About");
}

/** One destination in the settings navigation. */
private sealed interface SettingsNav {
    data object Top : SettingsNav
    data object Home : SettingsNav
    data object Grid : SettingsNav
    data object Icons : SettingsNav
    data object AtAGlance : SettingsNav
    data object WidgetsFeed : SettingsNav
    data object Notifications : SettingsNav
    data object Drawer : SettingsNav
    data object Dock : SettingsNav
    data object Gestures : SettingsNav
    data object Appearance : SettingsNav
    data object About : SettingsNav
    data class Choice(val key: ChoiceKey) : SettingsNav
    data class Reorder(val kind: ReorderKind) : SettingsNav
    data object WeatherCity : SettingsNav
    data object CustomEngine : SettingsNav
}

private enum class ReorderKind { LONG_PRESS_MENU, OVERVIEW_ACTIONS }

/** A single-choice setting, shown as radio on its own sub-screen. */
private enum class ChoiceKey(val title: String, val parent: SettingsNav) {
    GRID_SIZE("Grid size", SettingsNav.Grid),
    INDICATOR_STYLE("Page indicators", SettingsNav.Grid),
    INDICATOR_COLOUR("Indicator colour", SettingsNav.Grid),
    ICON_SHAPE("Icon shape", SettingsNav.Icons),
    ICON_PACK("Icon pack", SettingsNav.Icons),
    DRAWER_STYLE("Drawer style", SettingsNav.Drawer),
    DRAWER_SORT("Sort by", SettingsNav.Drawer),
    SEARCH_BAR_PLACEMENT("Search bar", SettingsNav.Drawer),
    SEARCH_PROVIDER("Search provider", SettingsNav.Drawer),
    THEME_MODE("Theme mode", SettingsNav.Appearance),
    THEME("Theme", SettingsNav.Appearance),
    ACCENT("Accent colour", SettingsNav.Appearance),
    FONT("Font", SettingsNav.Appearance),
    SURFACE_STYLE("Surface style", SettingsNav.Appearance),
    BADGE_STYLE("Badge style", SettingsNav.Notifications),
    BADGE_POSITION("Badge position", SettingsNav.Notifications),
    BAR_VISIBILITY("Status and navigation bars", SettingsNav.Appearance),
    DOUBLE_TAP("Double-tap empty space", SettingsNav.Gestures),
    TRIPLE_TAP("Triple-tap empty space", SettingsNav.Gestures),
    TWO_FINGER_SWIPE("Two-finger swipe", SettingsNav.Gestures),
    TWO_FINGER_DOUBLE_TAP("Two-finger double-tap", SettingsNav.Gestures),
    SWIPE_DOWN("Swipe down", SettingsNav.Gestures),
    PINCH_IN("Pinch in", SettingsNav.Gestures),
    PINCH_OUT("Pinch out", SettingsNav.Gestures),
}

/** Which permission the priming dialog is asking about. */
private enum class PrimingKind { NOTIFICATIONS, BLUETOOTH, CONTACTS, LISTENER, CALENDAR }

/** The parent destination Back returns to from [nav]. */
private fun parentOf(nav: SettingsNav): SettingsNav? = when (nav) {
    SettingsNav.Top -> null
    SettingsNav.Home, SettingsNav.Drawer, SettingsNav.Dock,
    SettingsNav.Gestures, SettingsNav.Appearance, SettingsNav.About -> SettingsNav.Top
    SettingsNav.Grid, SettingsNav.Icons, SettingsNav.AtAGlance,
    SettingsNav.WidgetsFeed, SettingsNav.Notifications -> SettingsNav.Home
    is SettingsNav.Choice -> nav.key.parent
    is SettingsNav.Reorder -> SettingsNav.Home
    SettingsNav.WeatherCity -> SettingsNav.AtAGlance
    SettingsNav.CustomEngine -> SettingsNav.Drawer
}

/** The opening destination for an [initialArea] from HomeScreen. */
private fun initialNavFor(area: SettingsArea?): SettingsNav = when (area) {
    null -> SettingsNav.Top
    SettingsArea.HOME -> SettingsNav.Home
    SettingsArea.DRAWER -> SettingsNav.Drawer
    SettingsArea.DOCK -> SettingsNav.Dock
    SettingsArea.SEARCH -> SettingsNav.Drawer
    SettingsArea.THEMES -> SettingsNav.Appearance
    SettingsArea.GESTURES -> SettingsNav.Gestures
    SettingsArea.NOTIFICATIONS -> SettingsNav.Notifications
    SettingsArea.PRIVACY -> SettingsNav.Top
    SettingsArea.BACKUP -> SettingsNav.Top
    SettingsArea.ABOUT -> SettingsNav.About
}

/** One searchable setting: display name, synonyms/effect words, the
 *  breadcrumb shown under the result, the sub-screen it lands on,
 *  and the row key to scroll to and highlight there (null = the
 *  screen itself, e.g. a section entry). */
private data class SettingIndexEntry(
    val label: String,
    val keywords: String,
    val breadcrumb: String,
    val target: SettingsNav,
    val rowKey: String?,
)

/** The settings search index (rule 65): one entry per setting. */
private val settingsSearchIndex: List<SettingIndexEntry> = listOf(
    SettingIndexEntry("Wallpaper & style", "wallpaper pick style background", "Home settings", SettingsNav.Top, "wallpaper"),
    SettingIndexEntry("Home screen", "home grid icons glance", "Home settings", SettingsNav.Home, null),
    SettingIndexEntry("App drawer", "drawer apps list", "Home settings", SettingsNav.Drawer, null),
    SettingIndexEntry("Dock", "dock favourites bar", "Home settings", SettingsNav.Dock, null),
    SettingIndexEntry("Gestures", "gestures swipe tap", "Home settings", SettingsNav.Gestures, null),
    SettingIndexEntry("Appearance", "appearance theme colours dark", "Home settings", SettingsNav.Appearance, null),
    SettingIndexEntry("About", "about version home app", "Home settings", SettingsNav.About, null),
    SettingIndexEntry("Hidden apps vault", "privacy hide apps pin vault", "Home settings", SettingsNav.Top, "vault"),
    SettingIndexEntry("Back up home screen", "backup export save", "Home settings", SettingsNav.Top, "backup"),
    SettingIndexEntry("Restore backup", "backup import restore", "Home settings", SettingsNav.Top, "restore"),
    SettingIndexEntry("Reset & debugging", "reset debug clear all", "Home settings", SettingsNav.Top, "reset_debug"),
    SettingIndexEntry("Grid size", "grid columns rows 4x5 5x5 5x6 fluid", "Home settings › Home screen › Grid", SettingsNav.Grid, "size"),
    SettingIndexEntry("Grid margin", "grid margin padding edges", "Home settings › Home screen › Grid", SettingsNav.Grid, "margin"),
    SettingIndexEntry("Icon spacing", "grid spacing gap icons", "Home settings › Home screen › Grid", SettingsNav.Grid, "spacing"),
    SettingIndexEntry("Infinite scroll", "scroll wrap pages", "Home settings › Home screen › Grid", SettingsNav.Grid, "infinite_scroll"),
    SettingIndexEntry("Page indicators", "dots numbers lines page indicator", "Home settings › Home screen › Grid", SettingsNav.Grid, "indicator_style"),
    SettingIndexEntry("Indicator colour", "page indicator accent white black", "Home settings › Home screen › Grid", SettingsNav.Grid, "indicator_colour"),
    SettingIndexEntry("Icon shape", "circle rounded square icons", "Home settings › Home screen › Icons", SettingsNav.Icons, "shape"),
    SettingIndexEntry("Icon size", "icons big small", "Home settings › Home screen › Icons", SettingsNav.Icons, "size"),
    SettingIndexEntry("Show app labels", "icons labels names", "Home settings › Home screen › Icons", SettingsNav.Icons, "labels"),
    SettingIndexEntry("Themed icons", "material you icons monochrome silhouette dark", "Home settings › Home screen › Icons", SettingsNav.Icons, "themed"),
    SettingIndexEntry("Smartsize icons", "icons normalised uniform", "Home settings › Home screen › Icons", SettingsNav.Icons, "smartsize"),
    SettingIndexEntry("Live clock icon", "clock animated icon time", "Home settings › Home screen › Icons", SettingsNav.Icons, "live_clock"),
    SettingIndexEntry("Icon pack", "icons theme pack nova adw", "Home settings › Home screen › Icons", SettingsNav.Icons, "pack"),
    SettingIndexEntry("At a Glance", "glance date weather calendar", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "strip"),
    SettingIndexEntry("Calendar events", "glance calendar", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "calendar"),
    SettingIndexEntry("Weather", "glance weather forecast", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "weather"),
    SettingIndexEntry("Weather city", "weather city location", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "weather_city"),
    SettingIndexEntry("Morning summary", "brief summary schedule morning time alarm notification", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "now_brief"),
    SettingIndexEntry("Morning summary hour", "morning summary time hour", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "brief_hour"),
    SettingIndexEntry("Morning summary minute", "morning summary time minute", "Home settings › Home screen › At a Glance", SettingsNav.AtAGlance, "brief_minute"),
    SettingIndexEntry("Widget page", "widgets swipe left", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "widget_page"),
    SettingIndexEntry("Desktop widget page", "widgets desktop", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "desktop_widget_page"),
    SettingIndexEntry("Widget overlap", "widgets overlap grid", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "widget_overlap"),
    SettingIndexEntry("Smart Stack auto-rotate", "widgets stack rotate", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "smart_stack"),
    SettingIndexEntry("Auto-hiding media player", "music media player", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "media_player"),
    SettingIndexEntry("Discover feed", "rss feed news swipe right", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "discover"),
    SettingIndexEntry("Google feed bridge", "google feed discover", "Home settings › Home screen › Widgets & feed", SettingsNav.WidgetsFeed, "google_bridge"),
    SettingIndexEntry("Notification access", "notification access listener badges", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "access"),
    SettingIndexEntry("Notification badges", "badges dots numbers", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "badges"),
    SettingIndexEntry("Badge style", "badges dot number", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "badge_style"),
    SettingIndexEntry("Badge position", "badges corner top right", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "badge_position"),
    SettingIndexEntry("Badge size", "badges size", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "badge_size"),
    SettingIndexEntry("Badge corner radius", "badges rounded corners", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "badge_corner"),
    SettingIndexEntry("Dynamic badges", "badges avatar artwork", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "dynamic_badges"),
    SettingIndexEntry("Inline notification previews", "notifications previews reply", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "inline_previews"),
    SettingIndexEntry("Notification preview in long-press popup", "notifications preview long press", "Home settings › Home screen › Notifications", SettingsNav.Notifications, "long_press_preview"),
    SettingIndexEntry("One-handed layout", "bottom third reachability", "Home settings › Home screen", SettingsNav.Home, "one_handed"),
    SettingIndexEntry("Lock home-screen layout", "desktop lock moves", "Home settings › Home screen", SettingsNav.Home, "lock_layout"),
    SettingIndexEntry("Add new apps to Home automatically", "auto add new apps", "Home settings › Home screen", SettingsNav.Home, "auto_add"),
    SettingIndexEntry("Show music apps on headphone connect", "headphones music bluetooth", "Home settings › Home screen", SettingsNav.Home, "headphone_music"),
    SettingIndexEntry("Add a home-screen sticker", "sticker text free", "Home settings › Home screen", SettingsNav.Home, "sticker"),
    SettingIndexEntry("Long-press menu order", "menu reorder long press", "Home settings › Home screen", SettingsNav.Home, "long_press_order"),
    SettingIndexEntry("Overview actions", "overview menu page actions", "Home settings › Home screen", SettingsNav.Home, "overview_actions"),
    SettingIndexEntry("Drawer style", "drawer list pages", "Home settings › App drawer", SettingsNav.Drawer, "drawer_style"),
    SettingIndexEntry("Sort by", "drawer sort name install most used size", "Home settings › App drawer", SettingsNav.Drawer, "drawer_sort"),
    SettingIndexEntry("Search bar", "drawer search top bottom", "Home settings › App drawer", SettingsNav.Drawer, "search_bar"),
    SettingIndexEntry("Pull down for search", "drawer pull search", "Home settings › App drawer", SettingsNav.Drawer, "pull_to_search"),
    SettingIndexEntry("Swipe up to start search", "drawer swipe up search keyboard", "Home settings › App drawer", SettingsNav.Drawer, "swipe_up_search"),
    SettingIndexEntry("Show categories", "drawer categories tabs", "Home settings › App drawer", SettingsNav.Drawer, "show_categories"),
    SettingIndexEntry("Drawer folders", "drawer folders", "Home settings › App drawer", SettingsNav.Drawer, "drawer_folders"),
    SettingIndexEntry("Frequently used row", "drawer frequent recent", "Home settings › App drawer", SettingsNav.Drawer, "frequent_row"),
    SettingIndexEntry("Remember drawer position", "drawer position scroll", "Home settings › App drawer", SettingsNav.Drawer, "remember_position"),
    SettingIndexEntry("Close drawer after launching", "drawer auto close", "Home settings › App drawer", SettingsNav.Drawer, "auto_close"),
    SettingIndexEntry("Two-column drawer on wide screens", "drawer columns wide", "Home settings › App drawer", SettingsNav.Drawer, "two_column"),
    SettingIndexEntry("Search provider", "search engine duckduckgo google bing brave", "Home settings › App drawer", SettingsNav.Drawer, "search_provider"),
    SettingIndexEntry("Search suggestions", "search suggestions", "Home settings › App drawer", SettingsNav.Drawer, "search_suggestions"),
    SettingIndexEntry("Contacts in search", "search contacts permission", "Home settings › App drawer", SettingsNav.Drawer, "contacts_search"),
    SettingIndexEntry("Custom search engine", "search engine add custom", "Home settings › App drawer", SettingsNav.Drawer, "custom_engine"),
    SettingIndexEntry("Show dock", "dock favourites hide", "Home settings › Dock", SettingsNav.Dock, "visible"),
    SettingIndexEntry("Dock height", "dock size tall", "Home settings › Dock", SettingsNav.Dock, "height"),
    SettingIndexEntry("Dock background", "dock background", "Home settings › Dock", SettingsNav.Dock, "background"),
    SettingIndexEntry("Dock shadow", "dock shadow", "Home settings › Dock", SettingsNav.Dock, "shadow"),
    SettingIndexEntry("Search pill in dock", "dock search pill bar", "Home settings › Dock", SettingsNav.Dock, "search_pill"),
    SettingIndexEntry("Theme mode", "theme auto light dark", "Home settings › Appearance", SettingsNav.Appearance, "theme_mode"),
    SettingIndexEntry("Theme", "theme material you stock ambient wallpaper dark", "Home settings › Appearance", SettingsNav.Appearance, "theme"),
    SettingIndexEntry("Accent colour", "theme accent colour blue", "Home settings › Appearance", SettingsNav.Appearance, "accent"),
    SettingIndexEntry("Font", "theme font serif monospace", "Home settings › Appearance", SettingsNav.Appearance, "font"),
    SettingIndexEntry("Surface style", "theme opaque blur glass transparency", "Home settings › Appearance", SettingsNav.Appearance, "surface_style"),
    SettingIndexEntry("Blur-glass transparency", "blur glass transparency", "Home settings › Appearance", SettingsNav.Appearance, "transparency"),
    SettingIndexEntry("AMOLED black", "theme amoled black pure dark", "Home settings › Appearance", SettingsNav.Appearance, "amoled"),
    SettingIndexEntry("Hide status bar", "immersive status bar", "Home settings › Appearance", SettingsNav.Appearance, "hide_status_bar"),
    SettingIndexEntry("Status and navigation bars", "bars navigation hide", "Home settings › Appearance", SettingsNav.Appearance, "bar_visibility"),
    SettingIndexEntry("Landscape home screen", "landscape rotate", "Home settings › Appearance", SettingsNav.Appearance, "landscape"),
    SettingIndexEntry("Separate layouts per screen", "foldable layouts", "Home settings › Appearance", SettingsNav.Appearance, "separate_layouts"),
    SettingIndexEntry("Two-pane tablet layout", "tablet two pane", "Home settings › Appearance", SettingsNav.Appearance, "two_pane"),
    SettingIndexEntry("Gestures", "gestures swipe double tap triple", "Home settings › Gestures", SettingsNav.Gestures, "enabled"),
    SettingIndexEntry("Haptics", "haptics vibration feedback", "Home settings › Gestures", SettingsNav.Gestures, "haptics"),
    SettingIndexEntry("Double-tap empty space", "gesture double tap lock", "Home settings › Gestures", SettingsNav.Gestures, "double_tap"),
    SettingIndexEntry("Triple-tap empty space", "gesture triple tap", "Home settings › Gestures", SettingsNav.Gestures, "triple_tap"),
    SettingIndexEntry("Two-finger swipe", "gesture two finger swipe", "Home settings › Gestures", SettingsNav.Gestures, "two_finger_swipe"),
    SettingIndexEntry("Two-finger double-tap", "gesture two finger double tap", "Home settings › Gestures", SettingsNav.Gestures, "two_finger_double_tap"),
    SettingIndexEntry("Swipe down", "gesture swipe down notifications", "Home settings › Gestures", SettingsNav.Gestures, "swipe_down"),
    SettingIndexEntry("Pinch in", "gesture pinch overview", "Home settings › Gestures", SettingsNav.Gestures, "pinch_in"),
    SettingIndexEntry("Pinch out", "gesture pinch drawer", "Home settings › Gestures", SettingsNav.Gestures, "pinch_out"),
    SettingIndexEntry("Home app", "home app default launcher", "Home settings › About", SettingsNav.About, "home_app"),
    SettingIndexEntry("Version", "version app", "Home settings › About", SettingsNav.About, "version"),
)

/** "On" / "Off" for a switch row's value subtext (rule 62). */
private fun onOff(checked: Boolean): String = if (checked) "On" else "Off"

/** Everything the sub-screen composables need, in one holder. */
private class SettingsScope(
    val settings: LauncherSettings,
    val viewModel: HomeViewModel,
    val iconPacks: List<IconPack>,
    val searchProviders: List<SearchProvider>,
    val customProviders: List<SearchProvider>,
    val backupStatus: String?,
    val versionName: String,
    val versionCode: Long,
    val isWideDevice: Boolean,
    val highlightKey: String?,
    val navigate: (SettingsNav) -> Unit,
    val onPrime: (PrimingKind) -> Unit,
    val onOpenWallpaper: () -> Unit,
    val onAddSticker: () -> Unit,
    val onExportBackup: () -> Unit,
    val onImportBackup: () -> Unit,
    val onOpenVault: () -> Unit,
    val onOpenDebug: () -> Unit,
    val bluetoothGranted: Boolean,
    val contactsGranted: Boolean,
    val listenerGranted: Boolean,
    val notificationsGranted: Boolean,
    val calendarGranted: Boolean,
)

/**
 * A LazyListState that scrolls to [highlightKey] when it names one of
 * [rowKeys] (rule 65: a search result lands with its row visible AND
 * highlighted). The highlight itself is rendered by the row's
 * `highlighted` flag.
 */
@Composable
private fun rememberHighlightListState(
    highlightKey: String?,
    rowKeys: List<String>,
): LazyListState {
    val listState = rememberLazyListState()
    LaunchedEffect(highlightKey) {
        val index = highlightKey?.let { rowKeys.indexOf(it) } ?: -1
        if (index >= 0) listState.scrollToItem(index)
    }
    return listState
}

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    // ColumnScope receiver: screen content uses weight(1f) for its list.
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            content()
        }
    }
}

/** The full-screen settings surface. Shown from HomeScreen; the
 *  system back button navigates within settings, then closes it. */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    iconPacks: List<IconPack>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onOpenWallpaper: () -> Unit = {},
    onAddSticker: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenDebug: () -> Unit = {},
    // #18 re-request the contacts permission for universal search.
    // Fires the system dialog — only ever called from the priming
    // dialog's Allow (rule 67), never directly from a row.
    onRequestContactsPermission: () -> Unit = {},
    // Rule 67 / decision F22: the calendar permission for At a
    // Glance events. Same pattern as contacts — only from the
    // priming dialog's Allow, never directly from a row.
    onRequestCalendarPermission: () -> Unit = {},
    // Opens Settings filtered to one area (e.g. HOME from the At a
    // Glance setup row): the caller passes the area when it opens
    // Settings for one job. Null (the default) opens the top level.
    initialArea: SettingsArea? = null,
) {
    var query by remember { mutableStateOf("") }
    var nav by remember(initialArea) { mutableStateOf(initialNavFor(initialArea)) }
    var highlightKey by remember { mutableStateOf<String?>(null) }
    var priming by remember { mutableStateOf<PrimingKind?>(null) }
    val searchProviders = viewModel.searchProviders.collectAsStateValue()
    val customProviders = viewModel.customProviders.collectAsStateValue()
    val backupStatus = viewModel.backupStatus.collectAsStateValue()
    val notificationsGranted = viewModel.notificationsGranted.collectAsStateValue()
    val version = appVersionInfo(LocalContext.current)
    val context = LocalContext.current
    // #37 the morning summary needs POST_NOTIFICATIONS (API 33+);
    // #61 headphone detection needs BLUETOOTH_CONNECT (API 31+).
    // Both are requested only after opt-in on the priming dialog; a
    // denial just leaves the feature quiet. Below the API where the
    // permission exists, the request is skipped entirely.
    val notificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onNotificationsPermissionResult(granted) }
    val bluetoothPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }
    val fireNotificationsPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val fireBluetoothPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 31) {
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
    val bluetoothGranted = if (Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val contactsGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED
    val calendarGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED
    val listenerGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
    // Foldable / tablet rows are gated on the expanded breakpoint
    // (rule 68/70: two-pane at ≥840dp, not 600dp).
    val isWideDevice = AdaptiveLayout.isWideDevice(LocalConfiguration.current.smallestScreenWidthDp)

    // One BackHandler for settings navigation (rule 58): pop one
    // level while a sub-screen is open, dismiss at the top level.
    BackHandler {
        val parent = parentOf(nav)
        if (parent != null) {
            nav = parent
            highlightKey = null
        } else {
            onDismiss()
        }
    }
    val navigate: (SettingsNav) -> Unit = { destination ->
        nav = destination
        highlightKey = null
    }
    val back: () -> Unit = {
        val parent = parentOf(nav)
        if (parent != null) {
            nav = parent
            highlightKey = null
        } else {
            onDismiss()
        }
    }
    val scope = SettingsScope(
        settings = settings,
        viewModel = viewModel,
        iconPacks = iconPacks,
        searchProviders = searchProviders,
        customProviders = customProviders,
        backupStatus = backupStatus,
        versionName = version.first,
        versionCode = version.second,
        isWideDevice = isWideDevice,
        highlightKey = highlightKey,
        navigate = navigate,
        onPrime = { kind -> priming = kind },
        onOpenWallpaper = onOpenWallpaper,
        onAddSticker = onAddSticker,
        onExportBackup = onExportBackup,
        onImportBackup = onImportBackup,
        onOpenVault = onOpenVault,
        onOpenDebug = onOpenDebug,
        bluetoothGranted = bluetoothGranted,
        contactsGranted = contactsGranted,
        listenerGranted = listenerGranted,
        notificationsGranted = notificationsGranted,
        calendarGranted = calendarGranted,
    )
    when (val current = nav) {
        SettingsNav.Top -> TopSettingsContent(scope, query, onQueryChange = { query = it },
            onDeepLink = { destination, row ->
                nav = destination
                highlightKey = row
                query = ""
            }, onBack = back)
        SettingsNav.Home -> HomeSettingsContent(scope, back)
        SettingsNav.Grid -> GridSettingsContent(scope, back)
        SettingsNav.Icons -> IconsSettingsContent(scope, back)
        SettingsNav.AtAGlance -> AtAGlanceSettingsContent(scope, back)
        SettingsNav.WidgetsFeed -> WidgetsFeedSettingsContent(scope, back)
        SettingsNav.Notifications -> NotificationsSettingsContent(scope, back)
        SettingsNav.Drawer -> DrawerSettingsContent(scope, back)
        SettingsNav.Dock -> DockSettingsContent(scope, back)
        SettingsNav.Gestures -> GesturesSettingsContent(scope, back)
        SettingsNav.Appearance -> AppearanceSettingsContent(scope, back)
        SettingsNav.About -> AboutSettingsContent(scope, back)
        is SettingsNav.Choice -> ChoiceSettingsContent(scope, current.key, back)
        is SettingsNav.Reorder -> ReorderSettingsContent(scope, current.kind, back)
        SettingsNav.WeatherCity -> WeatherCitySettingsContent(scope, back)
        SettingsNav.CustomEngine -> CustomEngineSettingsContent(scope, back)
    }
    // The priming dialog (rule 67): one sentence, cancellable without
    // the system dialog firing; the system dialog fires only on Allow.
    val primingKind = priming
    if (primingKind != null) {
        when (primingKind) {
            PrimingKind.NOTIFICATIONS -> PermissionPrimingDialog(
                title = "Morning summary",
                message = "The morning summary arrives as a notification.",
                allowLabel = "Allow notifications",
                onAllow = fireNotificationsPermission,
                onDismiss = { priming = null },
            )
            PrimingKind.BLUETOOTH -> PermissionPrimingDialog(
                title = "Music on headphone connect",
                message = "Marlow listens for headphone connections to offer music apps.",
                allowLabel = "Allow Bluetooth",
                onAllow = fireBluetoothPermission,
                onDismiss = { priming = null },
            )
            PrimingKind.CONTACTS -> PermissionPrimingDialog(
                title = "Contacts in search",
                message = "Contacts appear in search results when you allow access.",
                allowLabel = "Allow contacts",
                onAllow = onRequestContactsPermission,
                onDismiss = { priming = null },
            )
            PrimingKind.LISTENER -> PermissionPrimingDialog(
                title = "Notification access",
                message = "Badges and previews need notification access to show anything.",
                allowLabel = "Open system settings",
                onAllow = { viewModel.openNotificationListenerSettings() },
                onDismiss = { priming = null },
            )
            PrimingKind.CALENDAR -> PermissionPrimingDialog(
                title = "Calendar events",
                message = "At a Glance shows your upcoming events when you allow calendar access.",
                allowLabel = "Allow calendar",
                onAllow = onRequestCalendarPermission,
                onDismiss = { priming = null },
            )
        }
    }
}

/** The top level — "Settings". Search sits at the top (rule 65). */
@Composable
private fun TopSettingsContent(
    scope: SettingsScope,
    query: String,
    onQueryChange: (String) -> Unit,
    onDeepLink: (SettingsNav, String?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val rowKeys = listOf(
        "wallpaper", "home", "drawer", "dock", "gestures", "appearance",
        "about", "vault", "backup", "restore", "reset_debug",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    SettingsScaffold(title = "Settings", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val trimmed = query.trim()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                state = listState,
                // Bottom inset so the last row never slides under the
                // gesture bar.
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (trimmed.isNotEmpty()) {
                    val matches = settingsSearchIndex.filter { entry ->
                        entry.label.contains(trimmed, ignoreCase = true) ||
                            entry.keywords.contains(trimmed.lowercase())
                    }
                    if (matches.isEmpty()) {
                        item {
                            Text(
                                text = "No settings match “$trimmed”.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    items(matches, key = { it.label + it.breadcrumb }) { entry ->
                        NavigationRow(
                            label = entry.label,
                            value = entry.breadcrumb,
                            onClick = { onDeepLink(entry.target, entry.rowKey) },
                        )
                    }
                } else {
                    item {
                        NavigationRow(
                            label = "Wallpaper & style",
                            value = "System",
                            onClick = { openSystemWallpaper(context, scope.onOpenWallpaper) },
                            highlighted = scope.highlightKey == "wallpaper",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "Home screen", value = null,
                            onClick = { scope.navigate(SettingsNav.Home) },
                            highlighted = scope.highlightKey == "home",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "App drawer", value = null,
                            onClick = { scope.navigate(SettingsNav.Drawer) },
                            highlighted = scope.highlightKey == "drawer",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "Dock", value = null,
                            onClick = { scope.navigate(SettingsNav.Dock) },
                            highlighted = scope.highlightKey == "dock",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "Gestures", value = null,
                            onClick = { scope.navigate(SettingsNav.Gestures) },
                            highlighted = scope.highlightKey == "gestures",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "Appearance", value = null,
                            onClick = { scope.navigate(SettingsNav.Appearance) },
                            highlighted = scope.highlightKey == "appearance",
                        )
                    }
                    item {
                        NavigationRow(
                            label = "About", value = null,
                            onClick = { scope.navigate(SettingsNav.About) },
                            highlighted = scope.highlightKey == "about",
                        )
                    }
                    item {
                        ActionRow(
                            label = "Hidden apps vault",
                            onClick = scope.onOpenVault,
                            highlighted = scope.highlightKey == "vault",
                        )
                    }
                    item {
                        ActionRow(
                            label = "Back up home screen",
                            onClick = scope.onExportBackup,
                            highlighted = scope.highlightKey == "backup",
                        )
                    }
                    item {
                        ActionRow(
                            label = "Restore backup",
                            onClick = scope.onImportBackup,
                            highlighted = scope.highlightKey == "restore",
                        )
                    }
                    item {
                        ActionRow(
                            label = "Reset & debugging",
                            onClick = scope.onOpenDebug,
                            highlighted = scope.highlightKey == "reset_debug",
                        )
                    }
                    val status = scope.backupStatus
                    if (status != null) {
                        item {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Opens the system Wallpaper & style flow (§4.3: not a Marlow
 *  re-implementation). Falls back to Marlow's own wallpaper sheet
 *  when the system flow is absent. */
private fun openSystemWallpaper(context: Context, onFallback: () -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
    } catch (e: ActivityNotFoundException) {
        onFallback()
    }
}

/** True when Marlow is the system's default home app (rule 66). */
private fun isMarlowDefaultHome(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolve = context.packageManager.resolveActivity(
        homeIntent, PackageManager.MATCH_DEFAULT_ONLY,
    )
    return resolve?.activityInfo?.packageName == context.packageName
}

/** "Home screen" — navigation rows into its groups plus the
 *  home-screen behaviours that live here directly. 12 rows. */
@Composable
private fun HomeSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "grid", "icons", "glance", "widgets_feed", "notifications",
        "one_handed", "lock_layout", "auto_add", "headphone_music",
        "sticker", "long_press_order", "overview_actions",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    SettingsScaffold(title = "Home screen", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                NavigationRow(
                    label = "Grid", value = s.gridSize.label,
                    onClick = { scope.navigate(SettingsNav.Grid) },
                    highlighted = scope.highlightKey == "grid",
                )
            }
            item {
                NavigationRow(
                    label = "Icons", value = null,
                    onClick = { scope.navigate(SettingsNav.Icons) },
                    highlighted = scope.highlightKey == "icons",
                )
            }
            item {
                NavigationRow(
                    label = "At a Glance", value = onOff(s.glanceVisible),
                    onClick = { scope.navigate(SettingsNav.AtAGlance) },
                    highlighted = scope.highlightKey == "glance",
                )
            }
            item {
                NavigationRow(
                    label = "Widgets & feed", value = null,
                    onClick = { scope.navigate(SettingsNav.WidgetsFeed) },
                    highlighted = scope.highlightKey == "widgets_feed",
                )
            }
            item {
                NavigationRow(
                    label = "Notifications", value = onOff(scope.listenerGranted),
                    onClick = { scope.navigate(SettingsNav.Notifications) },
                    highlighted = scope.highlightKey == "notifications",
                )
            }
            item {
                SwitchRow(
                    label = "One-handed layout", value = onOff(s.oneHanded),
                    checked = s.oneHanded,
                    onChange = { checked -> vm.updateSettings { it.copy(oneHanded = checked) } },
                    highlighted = scope.highlightKey == "one_handed",
                )
            }
            item {
                SwitchRow(
                    label = "Lock layout", value = onOff(s.desktopLocked),
                    checked = s.desktopLocked,
                    onChange = { checked -> vm.updateSettings { it.copy(desktopLocked = checked) } },
                    highlighted = scope.highlightKey == "lock_layout",
                )
            }
            item {
                SwitchRow(
                    label = "Add new apps automatically", value = onOff(s.autoAddApps),
                    checked = s.autoAddApps,
                    onChange = { checked -> vm.updateSettings { it.copy(autoAddApps = checked) } },
                    highlighted = scope.highlightKey == "auto_add",
                )
            }
            item {
                SwitchRow(
                    label = "Show music apps on headphone connect",
                    value = onOff(s.headphoneMusicEnabled),
                    checked = s.headphoneMusicEnabled,
                    onChange = { checked ->
                        vm.updateSettings { it.copy(headphoneMusicEnabled = checked) }
                        // #61 the A2DP broadcast is only delivered
                        // when BLUETOOTH_CONNECT is granted (API 31+)
                        // — prime first; a denial just leaves the
                        // sheet unshown.
                        if (checked && !scope.bluetoothGranted) scope.onPrime(PrimingKind.BLUETOOTH)
                    },
                    highlighted = scope.highlightKey == "headphone_music",
                )
            }
            item {
                ActionRow(
                    label = "Add a sticker",
                    onClick = scope.onAddSticker,
                    highlighted = scope.highlightKey == "sticker",
                )
            }
            item {
                NavigationRow(
                    label = "Long-press menu order", value = null,
                    onClick = { scope.navigate(SettingsNav.Reorder(ReorderKind.LONG_PRESS_MENU)) },
                    highlighted = scope.highlightKey == "long_press_order",
                )
            }
            item {
                NavigationRow(
                    label = "Overview actions", value = null,
                    onClick = { scope.navigate(SettingsNav.Reorder(ReorderKind.OVERVIEW_ACTIONS)) },
                    highlighted = scope.highlightKey == "overview_actions",
                )
            }
            if (s.headphoneMusicEnabled && !scope.bluetoothGranted) {
                item {
                    Text(
                        text = "Bluetooth permission is off, so music apps will not appear on headphone connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** "Grid" — size, margins, scroll, indicators. 6 rows. */
@Composable
private fun GridSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf("size", "margin", "spacing", "infinite_scroll", "indicator_style", "indicator_colour")
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    // #66 infinite scroll only applies when the pager is pages-only:
    // the feed page and the widget page sit at the ends of the pager,
    // so wrapping past them would skip them.
    val infiniteScrollBlocked = s.feedEnabled || s.widgetPageEnabled
    SettingsScaffold(title = "Grid", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                NavigationRow(
                    label = "Size", value = s.gridSize.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.GRID_SIZE)) },
                    highlighted = scope.highlightKey == "size",
                )
            }
            item {
                SliderRow(
                    label = "Margin", value = s.gridMarginDp.toFloat(), range = 0f..24f,
                    valueText = s.gridMarginDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(gridMarginDp = value.toInt()) } },
                    highlighted = scope.highlightKey == "margin",
                )
            }
            item {
                SliderRow(
                    label = "Spacing", value = s.gridSpacingDp.toFloat(), range = 0f..24f,
                    valueText = s.gridSpacingDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(gridSpacingDp = value.toInt()) } },
                    highlighted = scope.highlightKey == "spacing",
                )
            }
            item {
                SwitchRow(
                    label = "Infinite scroll", value = onOff(s.infiniteScroll),
                    checked = s.infiniteScroll,
                    onChange = { checked -> vm.updateSettings { it.copy(infiniteScroll = checked) } },
                    enabled = !infiniteScrollBlocked,
                    disabledReason = "Turn off the feed page and widget page to use this",
                    highlighted = scope.highlightKey == "infinite_scroll",
                )
            }
            item {
                NavigationRow(
                    label = "Page indicators", value = s.indicatorStyle.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.INDICATOR_STYLE)) },
                    highlighted = scope.highlightKey == "indicator_style",
                )
            }
            item {
                NavigationRow(
                    label = "Indicator colour", value = s.indicatorColour.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.INDICATOR_COLOUR)) },
                    highlighted = scope.highlightKey == "indicator_colour",
                )
            }
        }
    }
}

/** "Icons" — shape, size, labels, themed, smartsize, clock, pack. 7 rows. */
@Composable
private fun IconsSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf("shape", "size", "labels", "themed", "smartsize", "live_clock", "pack")
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val packLabel = if (s.iconPack.isBlank()) "System" else {
        scope.iconPacks.firstOrNull { it.packageName == s.iconPack }?.label ?: "System"
    }
    SettingsScaffold(title = "Icons", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                NavigationRow(
                    label = "Shape", value = s.iconShape.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.ICON_SHAPE)) },
                    highlighted = scope.highlightKey == "shape",
                )
            }
            item {
                SliderRow(
                    label = "Size", value = s.iconSizeDp.toFloat(), range = 40f..72f,
                    valueText = s.iconSizeDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(iconSizeDp = value.toInt()) } },
                    highlighted = scope.highlightKey == "size",
                )
            }
            item {
                SwitchRow(
                    label = "App labels", value = onOff(s.showLabels),
                    checked = s.showLabels,
                    onChange = { checked -> vm.updateSettings { it.copy(showLabels = checked) } },
                    highlighted = scope.highlightKey == "labels",
                )
            }
            item {
                SwitchRow(
                    label = "Themed", value = onOff(s.themedIcons),
                    checked = s.themedIcons,
                    onChange = { checked -> vm.updateSettings { it.copy(themedIcons = checked) } },
                    highlighted = scope.highlightKey == "themed",
                )
            }
            item {
                SwitchRow(
                    label = "Smartsize", value = onOff(s.smartSizeIcons),
                    checked = s.smartSizeIcons,
                    onChange = { checked -> vm.updateSettings { it.copy(smartSizeIcons = checked) } },
                    highlighted = scope.highlightKey == "smartsize",
                )
            }
            item {
                SwitchRow(
                    label = "Live clock", value = onOff(s.liveClockIcon),
                    checked = s.liveClockIcon,
                    onChange = { checked -> vm.updateSettings { it.copy(liveClockIcon = checked) } },
                    highlighted = scope.highlightKey == "live_clock",
                )
            }
            item {
                NavigationRow(
                    label = "Pack", value = packLabel,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.ICON_PACK)) },
                    highlighted = scope.highlightKey == "pack",
                )
            }
        }
    }
}

/** "At a Glance" — strip, calendar, weather, city, morning summary. 7 rows. */
@Composable
private fun AtAGlanceSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf("strip", "calendar", "weather", "weather_city", "now_brief", "brief_hour", "brief_minute")
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val briefTime = "%02d:%02d".format(s.nowBriefHour, s.nowBriefMinute)
    SettingsScaffold(title = "At a Glance", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SwitchRow(
                    label = "Show strip", value = onOff(s.glanceVisible),
                    checked = s.glanceVisible,
                    onChange = { checked -> vm.updateSettings { it.copy(glanceVisible = checked) } },
                    highlighted = scope.highlightKey == "strip",
                )
            }
            item {
                SwitchRow(
                    label = "Calendar events", value = onOff(s.glanceCalendar),
                    checked = s.glanceCalendar,
                    onChange = { checked ->
                        vm.updateSettings { it.copy(glanceCalendar = checked) }
                        // Rule 67 / decision F22: the calendar
                        // permission is asked here, when the user
                        // enables events — never at launch. A denial
                        // leaves the strip on date + weather only.
                        if (checked && !scope.calendarGranted) scope.onPrime(PrimingKind.CALENDAR)
                    },
                    enabled = s.glanceVisible,
                    disabledReason = "Turn on At a Glance to change this",
                    highlighted = scope.highlightKey == "calendar",
                )
            }
            item {
                SwitchRow(
                    label = "Weather", value = onOff(s.glanceWeather),
                    checked = s.glanceWeather,
                    onChange = { checked -> vm.updateSettings { it.copy(glanceWeather = checked) } },
                    enabled = s.glanceVisible,
                    disabledReason = "Turn on At a Glance to change this",
                    highlighted = scope.highlightKey == "weather",
                )
            }
            item {
                NavigationRow(
                    label = "Weather city",
                    value = if (s.weatherCityName.isBlank()) "Not set" else s.weatherCityName,
                    onClick = { scope.navigate(SettingsNav.WeatherCity) },
                    highlighted = scope.highlightKey == "weather_city",
                    enabled = s.glanceVisible && s.glanceWeather,
                    disabledReason = "Turn on Weather to change this",
                )
            }
            item {
                SwitchRow(
                    label = "Morning summary",
                    value = if (s.nowBriefEnabled) "On, $briefTime" else "Off",
                    checked = s.nowBriefEnabled,
                    onChange = { checked ->
                        vm.updateSettings { it.copy(nowBriefEnabled = checked) }
                        // #37 the summary arrives as a notification —
                        // prime first; a denial just means it never
                        // appears.
                        if (checked && !scope.notificationsGranted) scope.onPrime(PrimingKind.NOTIFICATIONS)
                    },
                    highlighted = scope.highlightKey == "now_brief",
                )
            }
            item {
                SliderRow(
                    label = "Hour", value = s.nowBriefHour.toFloat(), range = 0f..23f,
                    valueText = "%02d".format(s.nowBriefHour),
                    onChange = { value -> vm.updateSettings { it.copy(nowBriefHour = value.toInt()) } },
                    enabled = s.nowBriefEnabled,
                    disabledReason = "Turn on the morning summary to change this",
                    highlighted = scope.highlightKey == "brief_hour",
                )
            }
            item {
                SliderRow(
                    label = "Minute", value = s.nowBriefMinute.toFloat(), range = 0f..55f,
                    valueText = "%02d".format(s.nowBriefMinute),
                    onChange = { value ->
                        vm.updateSettings { it.copy(nowBriefMinute = value.toInt() / 5 * 5) }
                    },
                    enabled = s.nowBriefEnabled,
                    disabledReason = "Turn on the morning summary to change this",
                    highlighted = scope.highlightKey == "brief_minute",
                )
            }
            if (s.nowBriefEnabled && !scope.notificationsGranted) {
                item {
                    Text(
                        text = "Notifications are off, so the morning summary will not arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** "Widgets & feed" — widget pages, overlap, stack, media, feeds. 7 rows. */
@Composable
private fun WidgetsFeedSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "widget_page", "desktop_widget_page", "widget_overlap",
        "smart_stack", "media_player", "discover", "google_bridge",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    SettingsScaffold(title = "Widgets & feed", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SwitchRow(
                    label = "Widget page", value = onOff(s.widgetPageEnabled),
                    checked = s.widgetPageEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(widgetPageEnabled = checked) } },
                    highlighted = scope.highlightKey == "widget_page",
                )
            }
            item {
                SwitchRow(
                    label = "Desktop widget page", value = onOff(s.desktopWidgetPageEnabled),
                    checked = s.desktopWidgetPageEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(desktopWidgetPageEnabled = checked) } },
                    enabled = s.widgetPageEnabled,
                    disabledReason = "Turn on the widget page to change this",
                    highlighted = scope.highlightKey == "desktop_widget_page",
                )
            }
            item {
                SwitchRow(
                    label = "Widget overlap", value = onOff(s.widgetOverlapEnabled),
                    checked = s.widgetOverlapEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(widgetOverlapEnabled = checked) } },
                    highlighted = scope.highlightKey == "widget_overlap",
                )
            }
            item {
                SwitchRow(
                    label = "Smart Stack auto-rotate", value = onOff(s.stackAutoRotate),
                    checked = s.stackAutoRotate,
                    onChange = { checked -> vm.updateSettings { it.copy(stackAutoRotate = checked) } },
                    highlighted = scope.highlightKey == "smart_stack",
                )
            }
            item {
                SwitchRow(
                    label = "Auto-hiding media player", value = onOff(s.mediaPlayerEnabled),
                    checked = s.mediaPlayerEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(mediaPlayerEnabled = checked) } },
                    highlighted = scope.highlightKey == "media_player",
                )
            }
            item {
                SwitchRow(
                    label = "Discover", value = onOff(s.feedEnabled),
                    checked = s.feedEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(feedEnabled = checked) } },
                    highlighted = scope.highlightKey == "discover",
                )
            }
            item {
                SwitchRow(
                    label = "Google bridge", value = onOff(s.googleFeedBridgeEnabled),
                    checked = s.googleFeedBridgeEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(googleFeedBridgeEnabled = checked) } },
                    highlighted = scope.highlightKey == "google_bridge",
                )
            }
        }
    }
}

/** "Notifications" — access, badges, previews. 9 rows. */
@Composable
private fun NotificationsSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "access", "badges", "badge_style", "badge_position", "badge_size",
        "badge_corner", "dynamic_badges", "inline_previews", "long_press_preview",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val access = scope.listenerGranted
    val badgeReason = if (!access) "Turn on notification access to change this"
    else "Turn on Notification badges to change this"
    val badgesEnabled = access && s.badgesEnabled
    SettingsScaffold(title = "Notifications", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                // The notification-listener grant lives in system
                // settings, so this row opens it (via priming when
                // it is off). (Also reachable from the notification
                // panel.)
                NavigationRow(
                    label = "Access", value = onOff(access),
                    onClick = {
                        if (access) vm.openNotificationListenerSettings()
                        else scope.onPrime(PrimingKind.LISTENER)
                    },
                    highlighted = scope.highlightKey == "access",
                )
            }
            item {
                SwitchRow(
                    label = "Badges", value = onOff(s.badgesEnabled),
                    checked = s.badgesEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(badgesEnabled = checked) } },
                    enabled = access,
                    disabledReason = "Turn on notification access to change this",
                    highlighted = scope.highlightKey == "badges",
                )
            }
            item {
                NavigationRow(
                    label = "Badge style", value = s.badgeStyle.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.BADGE_STYLE)) },
                    highlighted = scope.highlightKey == "badge_style",
                    enabled = badgesEnabled,
                    disabledReason = badgeReason,
                )
            }
            item {
                NavigationRow(
                    label = "Badge position", value = s.badgePosition.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.BADGE_POSITION)) },
                    highlighted = scope.highlightKey == "badge_position",
                    enabled = badgesEnabled,
                    disabledReason = badgeReason,
                )
            }
            item {
                SliderRow(
                    label = "Badge size", value = s.badgeSizeDp.toFloat(), range = 8f..28f,
                    valueText = s.badgeSizeDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(badgeSizeDp = value.toInt()) } },
                    enabled = badgesEnabled,
                    disabledReason = badgeReason,
                    highlighted = scope.highlightKey == "badge_size",
                )
            }
            item {
                SliderRow(
                    label = "Badge corner radius", value = s.badgeCornerRadiusDp.toFloat(), range = 0f..14f,
                    valueText = s.badgeCornerRadiusDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(badgeCornerRadiusDp = value.toInt()) } },
                    enabled = badgesEnabled,
                    disabledReason = badgeReason,
                    highlighted = scope.highlightKey == "badge_corner",
                )
            }
            item {
                SwitchRow(
                    label = "Dynamic badges", value = onOff(s.dynamicBadges),
                    checked = s.dynamicBadges,
                    onChange = { checked -> vm.updateSettings { it.copy(dynamicBadges = checked) } },
                    enabled = badgesEnabled,
                    disabledReason = badgeReason,
                    highlighted = scope.highlightKey == "dynamic_badges",
                )
            }
            item {
                SwitchRow(
                    label = "Inline previews", value = onOff(s.inlinePreviewsEnabled),
                    checked = s.inlinePreviewsEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(inlinePreviewsEnabled = checked) } },
                    enabled = access,
                    disabledReason = "Turn on notification access to change this",
                    highlighted = scope.highlightKey == "inline_previews",
                )
            }
            item {
                SwitchRow(
                    label = "Long-press preview", value = onOff(s.longPressPreviewEnabled),
                    checked = s.longPressPreviewEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(longPressPreviewEnabled = checked) } },
                    enabled = access,
                    disabledReason = "Turn on notification access to change this",
                    highlighted = scope.highlightKey == "long_press_preview",
                )
            }
            if (!access) {
                item {
                    Text(
                        text = "Notification access is off — badges and previews are hidden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** "App drawer" — layout, sorting, search, providers. 15 rows. */
@Composable
private fun DrawerSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "drawer_style", "drawer_sort", "search_bar", "pull_to_search",
        "swipe_up_search", "show_categories", "drawer_folders",
        "frequent_row", "remember_position", "auto_close", "two_column",
        "search_provider", "search_suggestions", "contacts_search", "custom_engine",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val providerName = scope.searchProviders.firstOrNull { it.id == s.searchProviderId }?.name ?: ""
    SettingsScaffold(title = "App drawer", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                NavigationRow(
                    label = "Style", value = s.drawerStyle.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.DRAWER_STYLE)) },
                    highlighted = scope.highlightKey == "drawer_style",
                )
            }
            item {
                NavigationRow(
                    label = "Sort by", value = s.drawerSort.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.DRAWER_SORT)) },
                    highlighted = scope.highlightKey == "drawer_sort",
                )
            }
            item {
                NavigationRow(
                    label = "Search bar", value = s.searchBarPlacement.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.SEARCH_BAR_PLACEMENT)) },
                    highlighted = scope.highlightKey == "search_bar",
                )
            }
            item {
                SwitchRow(
                    label = "Pull down for search", value = onOff(s.pullToSearch),
                    checked = s.pullToSearch,
                    onChange = { checked -> vm.updateSettings { it.copy(pullToSearch = checked) } },
                    highlighted = scope.highlightKey == "pull_to_search",
                )
            }
            item {
                // Rule 33: swipe up opens the drawer with the search
                // field focused and the keyboard up.
                SwitchRow(
                    label = "Swipe up to start search", value = onOff(s.swipeUpToSearch),
                    checked = s.swipeUpToSearch,
                    onChange = { checked -> vm.updateSettings { it.copy(swipeUpToSearch = checked) } },
                    highlighted = scope.highlightKey == "swipe_up_search",
                )
            }
            item {
                // Rule 32 extension (Marlow, not stock): category tabs
                // in the drawer. Off by default.
                SwitchRow(
                    label = "Show categories", value = onOff(s.drawerShowCategories),
                    checked = s.drawerShowCategories,
                    onChange = { checked -> vm.updateSettings { it.copy(drawerShowCategories = checked) } },
                    highlighted = scope.highlightKey == "show_categories",
                )
            }
            item {
                // Rule 32 extension (Marlow, not stock): drawer
                // folders. Off by default.
                SwitchRow(
                    label = "Drawer folders", value = onOff(s.drawerShowDrawerFolders),
                    checked = s.drawerShowDrawerFolders,
                    onChange = { checked -> vm.updateSettings { it.copy(drawerShowDrawerFolders = checked) } },
                    highlighted = scope.highlightKey == "drawer_folders",
                )
            }
            item {
                SwitchRow(
                    label = "Frequently used row", value = onOff(s.frequentRowVisible),
                    checked = s.frequentRowVisible,
                    onChange = { checked -> vm.updateSettings { it.copy(frequentRowVisible = checked) } },
                    highlighted = scope.highlightKey == "frequent_row",
                )
            }
            item {
                SwitchRow(
                    label = "Remember drawer position", value = onOff(s.rememberDrawerPosition),
                    checked = s.rememberDrawerPosition,
                    onChange = { checked -> vm.updateSettings { it.copy(rememberDrawerPosition = checked) } },
                    highlighted = scope.highlightKey == "remember_position",
                )
            }
            item {
                SwitchRow(
                    label = "Close drawer after launching", value = onOff(s.drawerAutoClose),
                    checked = s.drawerAutoClose,
                    onChange = { checked -> vm.updateSettings { it.copy(drawerAutoClose = checked) } },
                    highlighted = scope.highlightKey == "auto_close",
                )
            }
            item {
                SwitchRow(
                    label = "Two-column on wide screens", value = onOff(s.twoColumnDrawer),
                    checked = s.twoColumnDrawer,
                    onChange = { checked -> vm.updateSettings { it.copy(twoColumnDrawer = checked) } },
                    highlighted = scope.highlightKey == "two_column",
                )
            }
            item {
                NavigationRow(
                    label = "Search provider", value = providerName,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.SEARCH_PROVIDER)) },
                    highlighted = scope.highlightKey == "search_provider",
                )
            }
            item {
                SwitchRow(
                    label = "Search suggestions", value = onOff(s.searchSuggestions),
                    checked = s.searchSuggestions,
                    onChange = { checked -> vm.updateSettings { it.copy(searchSuggestions = checked) } },
                    highlighted = scope.highlightKey == "search_suggestions",
                )
            }
            item {
                // #18 contacts in universal search: the grant state
                // is read live (no permission state is stored). Tap
                // primes first (rule 67); a denial leaves contacts
                // skipped in search, nothing else.
                NavigationRow(
                    label = "Contacts in search",
                    value = if (scope.contactsGranted) "On"
                    else "Off — contacts are skipped in search",
                    onClick = {
                        if (!scope.contactsGranted) scope.onPrime(PrimingKind.CONTACTS)
                    },
                    highlighted = scope.highlightKey == "contacts_search",
                )
            }
            item {
                NavigationRow(
                    label = "Custom search engine", value = null,
                    onClick = { scope.navigate(SettingsNav.CustomEngine) },
                    highlighted = scope.highlightKey == "custom_engine",
                )
            }
        }
    }
}

/** "Dock" — visibility, height, background, pill. 4 rows. */
@Composable
private fun DockSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf("visible", "height", "background", "search_pill")
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val dockReason = "Turn on the dock to change this"
    SettingsScaffold(title = "Dock", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SwitchRow(
                    label = "Visible", value = onOff(s.dockVisible),
                    checked = s.dockVisible,
                    onChange = { checked -> vm.updateSettings { it.copy(dockVisible = checked) } },
                    highlighted = scope.highlightKey == "visible",
                )
            }
            item {
                SliderRow(
                    label = "Height", value = s.dockHeightDp.toFloat(), range = 56f..96f,
                    valueText = s.dockHeightDp.toString(),
                    onChange = { value -> vm.updateSettings { it.copy(dockHeightDp = value.toInt()) } },
                    enabled = s.dockVisible,
                    disabledReason = dockReason,
                    highlighted = scope.highlightKey == "height",
                )
            }
            item {
                SwitchRow(
                    label = "Background", value = onOff(s.dockBackground),
                    checked = s.dockBackground,
                    onChange = { checked -> vm.updateSettings { it.copy(dockBackground = checked) } },
                    enabled = s.dockVisible,
                    disabledReason = dockReason,
                    highlighted = scope.highlightKey == "background",
                )
            }
            item {
                SwitchRow(
                    label = "Search pill", value = onOff(s.searchPillVisible),
                    checked = s.searchPillVisible,
                    onChange = { checked -> vm.updateSettings { it.copy(searchPillVisible = checked) } },
                    enabled = s.dockVisible,
                    disabledReason = dockReason,
                    highlighted = scope.highlightKey == "search_pill",
                )
            }
        }
    }
}

/** "Gestures" — master switch plus one choice row per gesture. 8 rows. */
@Composable
private fun GesturesSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "enabled", "haptics", "double_tap", "triple_tap", "two_finger_swipe",
        "two_finger_double_tap", "swipe_down", "pinch_in", "pinch_out",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    val gestureReason = "Turn on Gestures to change this"
    SettingsScaffold(title = "Gestures", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SwitchRow(
                    label = "Enabled", value = onOff(s.gesturesEnabled),
                    checked = s.gesturesEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(gesturesEnabled = checked) } },
                    highlighted = scope.highlightKey == "enabled",
                )
            }
            item {
                // Rule 59: haptics are OFF by default and never forced
                // on. Independent of the Gestures master switch —
                // haptics are global feedback, not a gesture.
                SwitchRow(
                    label = "Haptics", value = onOff(s.hapticsEnabled),
                    checked = s.hapticsEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(hapticsEnabled = checked) } },
                    highlighted = scope.highlightKey == "haptics",
                )
            }
            item {
                NavigationRow(
                    label = "Double-tap empty space", value = s.doubleTapAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.DOUBLE_TAP)) },
                    highlighted = scope.highlightKey == "double_tap",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Triple-tap empty space", value = s.tripleTapAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.TRIPLE_TAP)) },
                    highlighted = scope.highlightKey == "triple_tap",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Two-finger swipe", value = s.twoFingerSwipeAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.TWO_FINGER_SWIPE)) },
                    highlighted = scope.highlightKey == "two_finger_swipe",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Two-finger double-tap", value = s.twoFingerDoubleTapAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.TWO_FINGER_DOUBLE_TAP)) },
                    highlighted = scope.highlightKey == "two_finger_double_tap",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Swipe down", value = s.swipeDownAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.SWIPE_DOWN)) },
                    highlighted = scope.highlightKey == "swipe_down",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Pinch in", value = s.pinchInAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.PINCH_IN)) },
                    highlighted = scope.highlightKey == "pinch_in",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
            item {
                NavigationRow(
                    label = "Pinch out", value = s.pinchOutAction.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.PINCH_OUT)) },
                    highlighted = scope.highlightKey == "pinch_out",
                    enabled = s.gesturesEnabled,
                    disabledReason = gestureReason,
                )
            }
        }
    }
}

/** "Appearance" — theme, accent, surfaces, bars, wide layouts. 13 rows. */
@Composable
private fun AppearanceSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val rowKeys = listOf(
        "theme_mode", "theme", "accent", "font", "surface_style",
        "transparency", "amoled", "hide_status_bar", "bar_visibility",
        "landscape", "separate_layouts", "two_pane",
    )
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    SettingsScaffold(title = "Appearance", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                NavigationRow(
                    label = "Theme mode", value = s.themeMode.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.THEME_MODE)) },
                    highlighted = scope.highlightKey == "theme_mode",
                )
            }
            item {
                NavigationRow(
                    label = "Theme", value = s.launcherTheme.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.THEME)) },
                    highlighted = scope.highlightKey == "theme",
                )
            }
            item {
                NavigationRow(
                    label = "Accent colour", value = s.accent.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.ACCENT)) },
                    highlighted = scope.highlightKey == "accent",
                    // Rule 5: under Material You the wallpaper is
                    // the seed — the accent picker applies to the
                    // Stock theme only.
                    enabled = s.launcherTheme == LauncherThemeChoice.STOCK,
                    disabledReason = "Stock theme only — Material You follows your wallpaper",
                )
            }
            item {
                NavigationRow(
                    label = "Font", value = s.fontChoice.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.FONT)) },
                    highlighted = scope.highlightKey == "font",
                )
            }
            item {
                NavigationRow(
                    label = "Surface style", value = s.surfaceStyle.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.SURFACE_STYLE)) },
                    highlighted = scope.highlightKey == "surface_style",
                )
            }
            item {
                // #24 transparency for blur glass only — Opaque
                // ignores it. 55% is the original fixed value; 30%
                // is barely there, 90% is nearly solid. Shown
                // disabled with its reason when Opaque is chosen,
                // rather than disappearing (rule 63).
                SliderRow(
                    label = "Blur-glass transparency",
                    value = s.surfaceAlpha, range = 0.3f..0.9f,
                    valueText = "${(s.surfaceAlpha * 100).toInt()}%",
                    onChange = { value -> vm.updateSettings { it.copy(surfaceAlpha = value) } },
                    enabled = s.surfaceStyle == SurfaceStyle.BLUR_GLASS,
                    disabledReason = "Choose Blur glass to change this",
                    highlighted = scope.highlightKey == "transparency",
                )
            }
            item {
                SwitchRow(
                    label = "AMOLED black", value = onOff(s.amoledBlack),
                    checked = s.amoledBlack,
                    onChange = { checked -> vm.updateSettings { it.copy(amoledBlack = checked) } },
                    highlighted = scope.highlightKey == "amoled",
                )
            }
            item {
                SwitchRow(
                    label = "Hide status bar", value = onOff(s.hideStatusBar),
                    checked = s.hideStatusBar,
                    onChange = { checked -> vm.updateSettings { it.copy(hideStatusBar = checked) } },
                    highlighted = scope.highlightKey == "hide_status_bar",
                )
            }
            item {
                NavigationRow(
                    label = "Status and navigation bars", value = s.barVisibility.label,
                    onClick = { scope.navigate(SettingsNav.Choice(ChoiceKey.BAR_VISIBILITY)) },
                    highlighted = scope.highlightKey == "bar_visibility",
                )
            }
            item {
                SwitchRow(
                    label = "Landscape home screen", value = onOff(s.landscapeEnabled),
                    checked = s.landscapeEnabled,
                    onChange = { checked -> vm.updateSettings { it.copy(landscapeEnabled = checked) } },
                    highlighted = scope.highlightKey == "landscape",
                )
            }
            // Foldable / tablet rows: only on expanded widths
            // (smallest width ≥ 840dp, rule 68/70) and OFF by
            // default. A phone install that stored them ON is gated
            // at load time in LauncherStore, so the stored value can
            // never misbehave on a phone.
            if (scope.isWideDevice) {
                item {
                    SwitchRow(
                        label = "Separate layouts per screen",
                        value = onOff(s.separateScreenLayouts),
                        checked = s.separateScreenLayouts,
                        onChange = { checked -> vm.updateSettings { it.copy(separateScreenLayouts = checked) } },
                        highlighted = scope.highlightKey == "separate_layouts",
                    )
                }
                item {
                    SwitchRow(
                        label = "Two-pane tablet layout", value = onOff(s.twoPaneTablet),
                        checked = s.twoPaneTablet,
                        onChange = { checked -> vm.updateSettings { it.copy(twoPaneTablet = checked) } },
                        highlighted = scope.highlightKey == "two_pane",
                    )
                }
            }
        }
    }
}

/** "About" — status lines only (rule 63): home app, version,
 *  package, and the backup restore scope at the bottom. */
@Composable
private fun AboutSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val context = LocalContext.current
    val isDefault = isMarlowDefaultHome(context)
    val rowKeys = listOf("home_app", "version", "package", "backup_scope")
    val listState = rememberHighlightListState(scope.highlightKey, rowKeys)
    SettingsScaffold(title = "About", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                StatusRow(
                    label = "Home app",
                    value = if (isDefault) "Marlow" else "Not Marlow",
                    highlighted = scope.highlightKey == "home_app",
                )
            }
            if (!isDefault) {
                item {
                    // Rule 66 fallback: a plain row that opens
                    // Settings → Apps → Default apps → Home app.
                    ActionRow(
                        label = "Change the default home app",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                            } catch (e: ActivityNotFoundException) {
                                // System settings is always present;
                                // nothing else to fall back to.
                            }
                        },
                    )
                }
            }
            item {
                StatusRow(
                    label = "Version",
                    value = "${scope.versionName} (${scope.versionCode})",
                    highlighted = scope.highlightKey == "version",
                )
            }
            item {
                StatusRow(
                    label = "Package",
                    value = context.packageName,
                    highlighted = scope.highlightKey == "package",
                )
            }
            item {
                Text(
                    text = BackupManager.RESTORE_SCOPE_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

/** One radio option on a choice sub-screen. */
private data class ChoiceOption(val id: String, val label: String, val supportingText: String? = null)

/** A single-choice setting as radio on its own sub-screen (rule 64).
 *  Tapping an option applies it immediately and returns to the
 *  parent screen — the standard single-choice settings behaviour. */
@Composable
private fun ChoiceSettingsContent(scope: SettingsScope, key: ChoiceKey, onBack: () -> Unit) {
    val options = choiceOptions(scope, key)
    val currentId = choiceCurrentId(scope, key)
    SettingsScaffold(title = key.title, onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(options, key = { it.id }) { option ->
                RadioChoiceRow(
                    label = option.label,
                    supportingText = option.supportingText,
                    selected = option.id == currentId,
                    onClick = {
                        applyChoice(scope, key, option.id)
                        onBack()
                    },
                )
            }
        }
    }
}

private fun choiceOptions(scope: SettingsScope, key: ChoiceKey): List<ChoiceOption> = when (key) {
    ChoiceKey.GRID_SIZE -> GridSize.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.INDICATOR_STYLE -> PageIndicatorStyle.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.INDICATOR_COLOUR -> IndicatorColour.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.ICON_SHAPE -> IconShape.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.ICON_PACK -> listOf(ChoiceOption("", "System")) +
        scope.iconPacks.map { ChoiceOption(it.packageName, it.label) }
    ChoiceKey.DRAWER_STYLE -> DrawerStyle.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.DRAWER_SORT -> DrawerSort.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.SEARCH_BAR_PLACEMENT -> SearchBarPlacement.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.SEARCH_PROVIDER -> scope.searchProviders.map { ChoiceOption(it.id, it.name) }
    ChoiceKey.THEME_MODE -> ThemeMode.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.THEME -> LauncherThemeChoice.values().map { choice ->
        ChoiceOption(
            id = choice.name,
            label = choice.label,
            supportingText = when (choice) {
                LauncherThemeChoice.MATERIAL_YOU -> "Follows your system colours"
                LauncherThemeChoice.STOCK -> "Marlow's own look"
                LauncherThemeChoice.AMBIENT -> "Accent from your wallpaper"
            },
        )
    }
    ChoiceKey.ACCENT -> AccentColour.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.FONT -> FontChoice.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.SURFACE_STYLE -> SurfaceStyle.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.BADGE_STYLE -> BadgeStyle.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.BADGE_POSITION -> BadgePosition.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.BAR_VISIBILITY -> BarVisibility.values().map { ChoiceOption(it.name, it.label) }
    ChoiceKey.DOUBLE_TAP, ChoiceKey.TRIPLE_TAP, ChoiceKey.TWO_FINGER_SWIPE,
    ChoiceKey.TWO_FINGER_DOUBLE_TAP, ChoiceKey.SWIPE_DOWN, ChoiceKey.PINCH_IN,
    ChoiceKey.PINCH_OUT -> GestureAction.values().map { ChoiceOption(it.name, it.label) }
}

private fun choiceCurrentId(scope: SettingsScope, key: ChoiceKey): String {
    val s = scope.settings
    return when (key) {
        ChoiceKey.GRID_SIZE -> s.gridSize.name
        ChoiceKey.INDICATOR_STYLE -> s.indicatorStyle.name
        ChoiceKey.INDICATOR_COLOUR -> s.indicatorColour.name
        ChoiceKey.ICON_SHAPE -> s.iconShape.name
        ChoiceKey.ICON_PACK -> s.iconPack
        ChoiceKey.DRAWER_STYLE -> s.drawerStyle.name
        ChoiceKey.DRAWER_SORT -> s.drawerSort.name
        ChoiceKey.SEARCH_BAR_PLACEMENT -> s.searchBarPlacement.name
        ChoiceKey.SEARCH_PROVIDER -> s.searchProviderId
        ChoiceKey.THEME_MODE -> s.themeMode.name
        ChoiceKey.THEME -> s.launcherTheme.name
        ChoiceKey.ACCENT -> s.accent.name
        ChoiceKey.FONT -> s.fontChoice.name
        ChoiceKey.SURFACE_STYLE -> s.surfaceStyle.name
        ChoiceKey.BADGE_STYLE -> s.badgeStyle.name
        ChoiceKey.BADGE_POSITION -> s.badgePosition.name
        ChoiceKey.BAR_VISIBILITY -> s.barVisibility.name
        ChoiceKey.DOUBLE_TAP -> s.doubleTapAction.name
        ChoiceKey.TRIPLE_TAP -> s.tripleTapAction.name
        ChoiceKey.TWO_FINGER_SWIPE -> s.twoFingerSwipeAction.name
        ChoiceKey.TWO_FINGER_DOUBLE_TAP -> s.twoFingerDoubleTapAction.name
        ChoiceKey.SWIPE_DOWN -> s.swipeDownAction.name
        ChoiceKey.PINCH_IN -> s.pinchInAction.name
        ChoiceKey.PINCH_OUT -> s.pinchOutAction.name
    }
}

private fun applyChoice(scope: SettingsScope, key: ChoiceKey, id: String) {
    val vm = scope.viewModel
    when (key) {
        ChoiceKey.GRID_SIZE -> vm.updateSettings { it.copy(gridSize = GridSize.valueOf(id)) }
        ChoiceKey.INDICATOR_STYLE -> vm.updateSettings { it.copy(indicatorStyle = PageIndicatorStyle.valueOf(id)) }
        ChoiceKey.INDICATOR_COLOUR -> vm.updateSettings { it.copy(indicatorColour = IndicatorColour.valueOf(id)) }
        ChoiceKey.ICON_SHAPE -> vm.updateSettings { it.copy(iconShape = IconShape.valueOf(id)) }
        ChoiceKey.ICON_PACK -> vm.updateSettings { it.copy(iconPack = id) }
        ChoiceKey.DRAWER_STYLE -> vm.updateSettings { it.copy(drawerStyle = DrawerStyle.valueOf(id)) }
        ChoiceKey.DRAWER_SORT -> vm.updateSettings { it.copy(drawerSort = DrawerSort.valueOf(id)) }
        ChoiceKey.SEARCH_BAR_PLACEMENT -> vm.updateSettings { it.copy(searchBarPlacement = SearchBarPlacement.valueOf(id)) }
        ChoiceKey.SEARCH_PROVIDER -> vm.updateSettings { it.copy(searchProviderId = id) }
        ChoiceKey.THEME_MODE -> vm.updateSettings { it.copy(themeMode = ThemeMode.valueOf(id)) }
        ChoiceKey.THEME -> vm.updateSettings { it.copy(launcherTheme = LauncherThemeChoice.valueOf(id)) }
        ChoiceKey.ACCENT -> vm.updateSettings { it.copy(accent = AccentColour.valueOf(id)) }
        ChoiceKey.FONT -> vm.updateSettings { it.copy(fontChoice = FontChoice.valueOf(id)) }
        ChoiceKey.SURFACE_STYLE -> vm.updateSettings { it.copy(surfaceStyle = SurfaceStyle.valueOf(id)) }
        ChoiceKey.BADGE_STYLE -> vm.updateSettings { it.copy(badgeStyle = BadgeStyle.valueOf(id)) }
        ChoiceKey.BADGE_POSITION -> vm.updateSettings { it.copy(badgePosition = BadgePosition.valueOf(id)) }
        ChoiceKey.BAR_VISIBILITY -> vm.updateSettings { it.copy(barVisibility = BarVisibility.valueOf(id)) }
        ChoiceKey.DOUBLE_TAP -> vm.updateSettings { it.copy(doubleTapAction = GestureAction.valueOf(id)) }
        ChoiceKey.TRIPLE_TAP -> vm.updateSettings { it.copy(tripleTapAction = GestureAction.valueOf(id)) }
        ChoiceKey.TWO_FINGER_SWIPE -> vm.updateSettings { it.copy(twoFingerSwipeAction = GestureAction.valueOf(id)) }
        ChoiceKey.TWO_FINGER_DOUBLE_TAP -> vm.updateSettings { it.copy(twoFingerDoubleTapAction = GestureAction.valueOf(id)) }
        ChoiceKey.SWIPE_DOWN -> vm.updateSettings { it.copy(swipeDownAction = GestureAction.valueOf(id)) }
        ChoiceKey.PINCH_IN -> vm.updateSettings { it.copy(pinchInAction = GestureAction.valueOf(id)) }
        ChoiceKey.PINCH_OUT -> vm.updateSettings { it.copy(pinchOutAction = GestureAction.valueOf(id)) }
    }
}

/** The #173 reorder screens: long-press menu order and overview
 *  actions. Up/down buttons move one step; the order applies
 *  immediately. */
@Composable
private fun ReorderSettingsContent(scope: SettingsScope, kind: ReorderKind, onBack: () -> Unit) {
    val s = scope.settings
    val vm = scope.viewModel
    val title = if (kind == ReorderKind.LONG_PRESS_MENU) "Long-press menu order" else "Overview actions"
    val ids = if (kind == ReorderKind.LONG_PRESS_MENU) s.longPressMenuOrder else s.overviewActions
    val labelOf: (String) -> String = if (kind == ReorderKind.LONG_PRESS_MENU) ::menuLabel else ::overviewLabel
    SettingsScaffold(title = title, onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(ids.size) { index ->
                val id = ids[index]
                ReorderRow(
                    label = labelOf(id),
                    canUp = index > 0,
                    canDown = index < ids.size - 1,
                    onUp = {
                        val order = ids.toMutableList()
                        val item = order.removeAt(index)
                        order.add(index - 1, item)
                        if (kind == ReorderKind.LONG_PRESS_MENU) {
                            vm.updateSettings { it.copy(longPressMenuOrder = order) }
                        } else {
                            vm.updateSettings { it.copy(overviewActions = order) }
                        }
                    },
                    onDown = {
                        val order = ids.toMutableList()
                        val item = order.removeAt(index)
                        order.add(index + 1, item)
                        if (kind == ReorderKind.LONG_PRESS_MENU) {
                            vm.updateSettings { it.copy(longPressMenuOrder = order) }
                        } else {
                            vm.updateSettings { it.copy(overviewActions = order) }
                        }
                    },
                )
            }
        }
    }
}

/** "Weather city" — type a name, pick a geocoded match. No location
 *  permission is used: the city is typed, not read. The current city
 *  is a status line at the bottom (rule 63), with a verb row to clear
 *  it. */
@Composable
private fun WeatherCitySettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val vm = scope.viewModel
    val s = scope.settings
    var cityQuery by remember { mutableStateOf("") }
    val cityCandidates = vm.weatherCities.collectAsStateValue()
    SettingsScaffold(title = "Weather city", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = cityQuery,
                onValueChange = { cityQuery = it; vm.searchWeatherCities(it) },
                label = { Text("Weather city") },
                placeholder = { Text("Search for your city") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(cityCandidates, key = { it.displayName }) { city ->
                    TextButton(
                        onClick = { cityQuery = ""; vm.setWeatherCity(city) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(city.displayName, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (s.weatherCityName.isNotBlank()) {
                StatusRow(label = "Weather location", value = s.weatherCityName)
                ActionRow(label = "Clear weather city", onClick = { vm.clearWeatherCity() })
            } else {
                StatusRow(label = "Weather location", value = "Not set")
            }
        }
    }
}

/** "Custom search engine" — the #104 editor (name + query URL) plus
 *  the list of added engines, each with a verb row to remove it. */
@Composable
private fun CustomEngineSettingsContent(scope: SettingsScope, onBack: () -> Unit) {
    val vm = scope.viewModel
    var newProviderName by remember { mutableStateOf("") }
    var newProviderUrl by remember { mutableStateOf("") }
    SettingsScaffold(title = "Custom search engine", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProviderName,
                        onValueChange = { newProviderName = it },
                        label = { Text("Custom engine name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    OutlinedTextField(
                        value = newProviderUrl,
                        onValueChange = { newProviderUrl = it },
                        label = { Text("Search URL, with %s for the query") },
                        placeholder = { Text("https://example.com/search?q=%s") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    TextButton(
                        onClick = {
                            vm.addCustomSearchProvider(newProviderName, newProviderUrl)
                            newProviderName = ""
                            newProviderUrl = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add custom search engine", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(scope.customProviders, key = { it.id }) { provider ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.removeCustomSearchProvider(provider.id) }) {
                        Text("Remove engine")
                    }
                }
            }
        }
    }
}
