package app.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.launcher.model.AppPair
import app.launcher.model.DrawerFolder
import app.launcher.model.DrawerSort
import app.launcher.model.DrawerStyle
import app.launcher.model.FeedSource
import app.launcher.model.FolderContact
import app.launcher.model.GridSize
import app.launcher.model.HabitItem
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.HomePage
import app.launcher.model.IconShape
import app.launcher.model.IndicatorColour
import app.launcher.model.LauncherSettings
import app.launcher.model.NoteItem
import app.launcher.model.PageIndicatorStyle
import app.launcher.model.PinnedContact
import app.launcher.model.SearchBarPlacement
import app.launcher.model.SearchProvider
import app.launcher.model.SurfaceStyle
import app.launcher.model.TaskItem
import app.launcher.model.ThemeMode
import app.launcher.model.LauncherThemeChoice
import app.launcher.model.AccentColour
import app.launcher.model.AppLanguageOverride
import app.launcher.model.BadgePosition
import app.launcher.model.BadgeStyle
import app.launcher.model.BarVisibility
import app.launcher.model.FontChoice
import app.launcher.model.GestureAction
import app.launcher.model.ShortcutSecondaryAction
import app.launcher.model.TimerItem
import app.launcher.model.WidgetStack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "launcher")

/**
 * Persistence for Phase 1: settings + home layout in DataStore
 * Preferences (androidx.datastore:datastore-preferences:1.1.1).
 *
 * Layout is stored as delimited text (no serialisation library is a
 * declared dependency): one record per line, fields separated by
 * U+001F, with backslash escaping for the separator, backslash and
 * newline inside values.
 */
object LauncherStore {

    private val KEY_GRID = stringPreferencesKey("grid_size")
    private val KEY_GRID_MARGIN = intPreferencesKey("grid_margin_dp")
    private val KEY_GRID_SPACING = intPreferencesKey("grid_spacing_dp")
    private val KEY_INFINITE = booleanPreferencesKey("infinite_scroll")
    private val KEY_INDICATOR_STYLE = stringPreferencesKey("indicator_style")
    private val KEY_INDICATOR_COLOUR = stringPreferencesKey("indicator_colour")
    private val KEY_DEFAULT_PAGE = intPreferencesKey("default_page")
    private val KEY_DOCK_VISIBLE = booleanPreferencesKey("dock_visible")
    private val KEY_DOCK_HEIGHT = intPreferencesKey("dock_height_dp")
    private val KEY_DOCK_BG = booleanPreferencesKey("dock_background")
    private val KEY_SEARCH_PILL = booleanPreferencesKey("search_pill_visible")
    private val KEY_DRAWER_STYLE = stringPreferencesKey("drawer_style")
    private val KEY_DRAWER_SORT = stringPreferencesKey("drawer_sort")
    private val KEY_SEARCH_PLACEMENT = stringPreferencesKey("search_bar_placement")
    private val KEY_PULL_TO_SEARCH = booleanPreferencesKey("pull_to_search")
    private val KEY_FREQUENT_ROW = booleanPreferencesKey("frequent_row_visible")
    private val KEY_REMEMBER_DRAWER = booleanPreferencesKey("remember_drawer_position")
    private val KEY_DRAWER_AUTO_CLOSE = booleanPreferencesKey("drawer_auto_close")
    private val KEY_TWO_COLUMN = booleanPreferencesKey("two_column_drawer")
    // Rule 32/33 drawer extensions (integration, 2026-11-26):
    // swipe-up-to-search + the two opt-in extension toggles.
    private val KEY_SWIPE_UP_TO_SEARCH = booleanPreferencesKey("swipe_up_to_search")
    private val KEY_DRAWER_SHOW_CATEGORIES = booleanPreferencesKey("drawer_show_categories")
    private val KEY_DRAWER_SHOW_FOLDERS = booleanPreferencesKey("drawer_show_drawer_folders")
    private val KEY_ICON_SHAPE = stringPreferencesKey("icon_shape")
    private val KEY_ICON_SIZE = intPreferencesKey("icon_size_dp")
    private val KEY_SHOW_LABELS = booleanPreferencesKey("show_labels")
    private val KEY_THEMED_ICONS = booleanPreferencesKey("themed_icons")
    private val KEY_SMARTSIZE = booleanPreferencesKey("smartsize_icons")
    private val KEY_LIVE_CLOCK = booleanPreferencesKey("live_clock_icon")
    private val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
    private val KEY_ONE_HANDED = booleanPreferencesKey("one_handed")
    private val KEY_LOCKED = booleanPreferencesKey("desktop_locked")
    private val KEY_AUTO_ADD = booleanPreferencesKey("auto_add_apps")
    private val KEY_MENU_ORDER = stringPreferencesKey("long_press_menu_order")
    private val KEY_OVERVIEW_ACTIONS = stringPreferencesKey("overview_actions")
    // ---- Phase 2 keys ----
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_LAUNCHER_THEME = stringPreferencesKey("launcher_theme")
    private val KEY_ACCENT = stringPreferencesKey("accent_colour")
    private val KEY_FONT = stringPreferencesKey("font_choice")
    private val KEY_SURFACE_STYLE = stringPreferencesKey("surface_style")
    private val KEY_AMOLED = booleanPreferencesKey("amoled_black")
    private val KEY_GLANCE_VISIBLE = booleanPreferencesKey("glance_visible")
    private val KEY_GLANCE_CALENDAR = booleanPreferencesKey("glance_calendar")
    private val KEY_GLANCE_WEATHER = booleanPreferencesKey("glance_weather")
    private val KEY_GLANCE_COMMUTE = booleanPreferencesKey("glance_commute")
    private val KEY_GLANCE_FINANCE = booleanPreferencesKey("glance_finance")
    private val KEY_GLANCE_SPORTS = booleanPreferencesKey("glance_sports")
    private val KEY_WEATHER_CITY = stringPreferencesKey("weather_city_name")
    private val KEY_WEATHER_LAT = doublePreferencesKey("weather_city_lat")
    private val KEY_WEATHER_LON = doublePreferencesKey("weather_city_lon")
    private val KEY_SURFACE_ALPHA = floatPreferencesKey("surface_alpha")
    private val KEY_NOW_BRIEF_HOUR = intPreferencesKey("now_brief_hour")
    private val KEY_NOW_BRIEF_MINUTE = intPreferencesKey("now_brief_minute")
    private val KEY_SMARTSPACER = booleanPreferencesKey("smartspacer_enabled")
    private val KEY_NOW_BRIEF = booleanPreferencesKey("now_brief_enabled")
    private val KEY_SEARCH_PROVIDER = stringPreferencesKey("search_provider_id")
    private val KEY_SEARCH_SUGGESTIONS = booleanPreferencesKey("search_suggestions")
    private val KEY_HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
    private val KEY_WALLPAPER_VISIBLE = booleanPreferencesKey("wallpaper_visible")
    private val KEY_SCROLLING_WALLPAPER = booleanPreferencesKey("scrolling_wallpaper")
    private val KEY_WALLPAPER_GRADIENT = booleanPreferencesKey("wallpaper_gradient")
    private val KEY_WALLPAPER_DIM = intPreferencesKey("wallpaper_dim_percent")
    private val KEY_HEADPHONE_MUSIC = booleanPreferencesKey("headphone_music_enabled")
    private val KEY_CUSTOM_PROVIDERS = stringPreferencesKey("custom_search_providers")
    // ---- Phase 3 keys ----
    private val KEY_WIDGET_PAGE = booleanPreferencesKey("widget_page_enabled")
    private val KEY_DESKTOP_WIDGET_PAGE = booleanPreferencesKey("desktop_widget_page_enabled")
    private val KEY_WIDGET_OVERLAP = booleanPreferencesKey("widget_overlap_enabled")
    private val KEY_STACK_AUTO_ROTATE = booleanPreferencesKey("stack_auto_rotate")
    private val KEY_MEDIA_PLAYER = booleanPreferencesKey("media_player_enabled")
    private val KEY_FEED_ENABLED = booleanPreferencesKey("feed_enabled")
    private val KEY_GOOGLE_FEED_BRIDGE = booleanPreferencesKey("google_feed_bridge_enabled")
    // ---- Phase 4 keys ----
    private val KEY_GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
    // Rule 59 haptics — OFF by default (see LauncherModels).
    private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    private val KEY_DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
    private val KEY_TRIPLE_TAP_ACTION = stringPreferencesKey("triple_tap_action")
    private val KEY_TWO_FINGER_SWIPE_ACTION = stringPreferencesKey("two_finger_swipe_action")
    private val KEY_TWO_FINGER_DOUBLE_TAP_ACTION = stringPreferencesKey("two_finger_double_tap_action")
    private val KEY_SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
    private val KEY_PINCH_IN_ACTION = stringPreferencesKey("pinch_in_action")
    private val KEY_PINCH_OUT_ACTION = stringPreferencesKey("pinch_out_action")
    private val KEY_BADGES_ENABLED = booleanPreferencesKey("badges_enabled")
    private val KEY_BADGE_STYLE = stringPreferencesKey("badge_style")
    private val KEY_BADGE_POSITION = stringPreferencesKey("badge_position")
    private val KEY_BADGE_SIZE = intPreferencesKey("badge_size_dp")
    private val KEY_BADGE_CORNER_RADIUS = intPreferencesKey("badge_corner_radius_dp")
    // Rule 50 per-app badge off switch (integration, 2026-11-26):
    // comma-joined package names, like the hidden-apps key.
    private val KEY_BADGE_OFF_PACKAGES = stringPreferencesKey("badge_off_packages")
    private val KEY_DYNAMIC_BADGES = booleanPreferencesKey("dynamic_badges")
    private val KEY_INLINE_PREVIEWS = booleanPreferencesKey("inline_previews_enabled")
    private val KEY_LONG_PRESS_PREVIEW = booleanPreferencesKey("long_press_preview_enabled")
    private val KEY_BAR_VISIBILITY = stringPreferencesKey("bar_visibility")
    private val KEY_LANDSCAPE_ENABLED = booleanPreferencesKey("landscape_enabled")
    private val KEY_SEPARATE_SCREEN_LAYOUTS = booleanPreferencesKey("separate_screen_layouts")
    private val KEY_TWO_PANE_TABLET = booleanPreferencesKey("two_pane_tablet")
    // ---- Phase 5 keys ----
    // #34 PIN vault: only the salt + salted hash are stored, never
    // the PIN itself (see VaultStore / PinCrypto).
    private val KEY_PIN_SALT = stringPreferencesKey("vault_pin_salt")
    private val KEY_PIN_HASH = stringPreferencesKey("vault_pin_hash")
    private val KEY_HIDDEN_APPS = stringPreferencesKey("hidden_apps")
    private val KEY_APP_LANGUAGES = stringPreferencesKey("app_languages")
    private val KEY_SWIPE_UP_ACTIONS = stringPreferencesKey("swipe_up_actions")
    private val KEY_NOTES = stringPreferencesKey("notes")
    private val KEY_TASKS = stringPreferencesKey("tasks")
    private val KEY_HABITS = stringPreferencesKey("habits")
    private val KEY_TIMERS = stringPreferencesKey("timers")
    private val KEY_WIDGET_STACKS = stringPreferencesKey("widget_stacks")
    private val KEY_FEED_SOURCES = stringPreferencesKey("feed_sources")

    private val KEY_HOME_ITEMS = stringPreferencesKey("home_items")
    private val KEY_HOME_FOLDERS = stringPreferencesKey("home_folders")
    private val KEY_HOME_PAGES = stringPreferencesKey("home_pages")
    private val KEY_DRAWER_FOLDERS = stringPreferencesKey("drawer_folders")
    private val KEY_APP_PAIRS = stringPreferencesKey("app_pairs")
    private val KEY_PINNED_CONTACTS = stringPreferencesKey("pinned_contacts")
    private val KEY_RENAMES = stringPreferencesKey("renames")
    private val KEY_ICON_OVERRIDES = stringPreferencesKey("icon_overrides")
    private val KEY_LAUNCH_STATS = stringPreferencesKey("launch_stats")
    private val KEY_KNOWN_PACKAGES = stringPreferencesKey("known_packages")
    // Audit fix (2026-11-26): one-time flags. seed_repaired marks
    // the home-seed self-healing in HomeViewModel as done; the
    // glance-weather migration marks the one-time turn-on of
    // glanceWeather for installs that predate its default-ON.
    private val KEY_SEED_REPAIRED = booleanPreferencesKey("seed_repaired")
    private val KEY_GLANCE_WEATHER_MIGRATED = booleanPreferencesKey("glance_weather_migrated")
    private val KEY_DRAWER_SCROLL = intPreferencesKey("drawer_scroll_index")
    private val KEY_DRAWER_QUERY = stringPreferencesKey("drawer_query")
    private val KEY_DRAWER_CUSTOM_ORDER = stringPreferencesKey("drawer_custom_order")

    fun settingsFlow(context: Context): Flow<LauncherSettings> =
        context.launcherDataStore.data.map { prefs ->
            gateForDevice(context, readSettings(prefs))
        }

    suspend fun loadSettings(context: Context): LauncherSettings =
        gateForDevice(context, readSettings(context.launcherDataStore.data.first()))

    /** Device gate for the foldable / tablet flags: on a phone
     *  (smallest width < 600dp) they are forced OFF in the settings
     *  object every consumer sees, whatever is stored. The stored
     *  values are left alone — a wide device that later reads the
     *  same store still sees them — so this is a use-time gate, not
     *  a migration. New installs get OFF from the model defaults. */
    private fun gateForDevice(context: Context, settings: LauncherSettings): LauncherSettings {
        if (AdaptiveLayout.isWideDevice(context.resources.configuration.smallestScreenWidthDp)) {
            return settings
        }
        return settings.copy(separateScreenLayouts = false, twoPaneTablet = false)
    }

    suspend fun saveSettings(context: Context, s: LauncherSettings) {
        context.launcherDataStore.edit { prefs ->
            prefs[KEY_GRID] = s.gridSize.name
            prefs[KEY_GRID_MARGIN] = s.gridMarginDp
            prefs[KEY_GRID_SPACING] = s.gridSpacingDp
            prefs[KEY_INFINITE] = s.infiniteScroll
            prefs[KEY_INDICATOR_STYLE] = s.indicatorStyle.name
            prefs[KEY_INDICATOR_COLOUR] = s.indicatorColour.name
            prefs[KEY_DEFAULT_PAGE] = s.defaultPage
            prefs[KEY_DOCK_VISIBLE] = s.dockVisible
            prefs[KEY_DOCK_HEIGHT] = s.dockHeightDp
            prefs[KEY_DOCK_BG] = s.dockBackground
            prefs[KEY_SEARCH_PILL] = s.searchPillVisible
            prefs[KEY_DRAWER_STYLE] = s.drawerStyle.name
            prefs[KEY_DRAWER_SORT] = s.drawerSort.name
            prefs[KEY_SEARCH_PLACEMENT] = s.searchBarPlacement.name
            prefs[KEY_PULL_TO_SEARCH] = s.pullToSearch
            prefs[KEY_FREQUENT_ROW] = s.frequentRowVisible
            prefs[KEY_REMEMBER_DRAWER] = s.rememberDrawerPosition
            prefs[KEY_DRAWER_AUTO_CLOSE] = s.drawerAutoClose
            prefs[KEY_TWO_COLUMN] = s.twoColumnDrawer
            prefs[KEY_SWIPE_UP_TO_SEARCH] = s.swipeUpToSearch
            prefs[KEY_DRAWER_SHOW_CATEGORIES] = s.drawerShowCategories
            prefs[KEY_DRAWER_SHOW_FOLDERS] = s.drawerShowDrawerFolders
            prefs[KEY_ICON_SHAPE] = s.iconShape.name
            prefs[KEY_ICON_SIZE] = s.iconSizeDp
            prefs[KEY_SHOW_LABELS] = s.showLabels
            prefs[KEY_THEMED_ICONS] = s.themedIcons
            prefs[KEY_SMARTSIZE] = s.smartSizeIcons
            prefs[KEY_LIVE_CLOCK] = s.liveClockIcon
            prefs[KEY_ICON_PACK] = s.iconPack
            prefs[KEY_ONE_HANDED] = s.oneHanded
            prefs[KEY_LOCKED] = s.desktopLocked
            prefs[KEY_AUTO_ADD] = s.autoAddApps
            prefs[KEY_MENU_ORDER] = s.longPressMenuOrder.joinToString(",")
            prefs[KEY_OVERVIEW_ACTIONS] = s.overviewActions.joinToString(",")
            prefs[KEY_THEME_MODE] = s.themeMode.name
            prefs[KEY_LAUNCHER_THEME] = s.launcherTheme.name
            prefs[KEY_ACCENT] = s.accent.name
            prefs[KEY_FONT] = s.fontChoice.name
            prefs[KEY_SURFACE_STYLE] = s.surfaceStyle.name
            prefs[KEY_AMOLED] = s.amoledBlack
            prefs[KEY_GLANCE_VISIBLE] = s.glanceVisible
            prefs[KEY_GLANCE_CALENDAR] = s.glanceCalendar
            prefs[KEY_GLANCE_WEATHER] = s.glanceWeather
            prefs[KEY_GLANCE_COMMUTE] = s.glanceCommute
            prefs[KEY_GLANCE_FINANCE] = s.glanceFinance
            prefs[KEY_GLANCE_SPORTS] = s.glanceSports
            prefs[KEY_WEATHER_CITY] = s.weatherCityName
            prefs[KEY_WEATHER_LAT] = s.weatherCityLat
            prefs[KEY_WEATHER_LON] = s.weatherCityLon
            prefs[KEY_SURFACE_ALPHA] = s.surfaceAlpha
            prefs[KEY_NOW_BRIEF_HOUR] = s.nowBriefHour
            prefs[KEY_NOW_BRIEF_MINUTE] = s.nowBriefMinute
            prefs[KEY_SMARTSPACER] = s.smartspacerEnabled
            prefs[KEY_NOW_BRIEF] = s.nowBriefEnabled
            prefs[KEY_SEARCH_PROVIDER] = s.searchProviderId
            prefs[KEY_SEARCH_SUGGESTIONS] = s.searchSuggestions
            prefs[KEY_HIDE_STATUS_BAR] = s.hideStatusBar
            prefs[KEY_WALLPAPER_VISIBLE] = s.wallpaperVisible
            prefs[KEY_SCROLLING_WALLPAPER] = s.scrollingWallpaper
            prefs[KEY_WALLPAPER_GRADIENT] = s.wallpaperGradient
            prefs[KEY_WALLPAPER_DIM] = s.wallpaperDimPercent
            prefs[KEY_HEADPHONE_MUSIC] = s.headphoneMusicEnabled
            prefs[KEY_WIDGET_PAGE] = s.widgetPageEnabled
            prefs[KEY_DESKTOP_WIDGET_PAGE] = s.desktopWidgetPageEnabled
            prefs[KEY_WIDGET_OVERLAP] = s.widgetOverlapEnabled
            prefs[KEY_STACK_AUTO_ROTATE] = s.stackAutoRotate
            prefs[KEY_MEDIA_PLAYER] = s.mediaPlayerEnabled
            prefs[KEY_FEED_ENABLED] = s.feedEnabled
            prefs[KEY_GOOGLE_FEED_BRIDGE] = s.googleFeedBridgeEnabled
            prefs[KEY_GESTURES_ENABLED] = s.gesturesEnabled
            prefs[KEY_HAPTICS_ENABLED] = s.hapticsEnabled
            prefs[KEY_DOUBLE_TAP_ACTION] = s.doubleTapAction.name
            prefs[KEY_TRIPLE_TAP_ACTION] = s.tripleTapAction.name
            prefs[KEY_TWO_FINGER_SWIPE_ACTION] = s.twoFingerSwipeAction.name
            prefs[KEY_TWO_FINGER_DOUBLE_TAP_ACTION] = s.twoFingerDoubleTapAction.name
            prefs[KEY_SWIPE_DOWN_ACTION] = s.swipeDownAction.name
            prefs[KEY_PINCH_IN_ACTION] = s.pinchInAction.name
            prefs[KEY_PINCH_OUT_ACTION] = s.pinchOutAction.name
            prefs[KEY_BADGES_ENABLED] = s.badgesEnabled
            prefs[KEY_BADGE_STYLE] = s.badgeStyle.name
            prefs[KEY_BADGE_POSITION] = s.badgePosition.name
            prefs[KEY_BADGE_SIZE] = s.badgeSizeDp
            prefs[KEY_BADGE_CORNER_RADIUS] = s.badgeCornerRadiusDp
            prefs[KEY_BADGE_OFF_PACKAGES] = s.badgeOffPackages.joinToString(",")
            prefs[KEY_DYNAMIC_BADGES] = s.dynamicBadges
            prefs[KEY_INLINE_PREVIEWS] = s.inlinePreviewsEnabled
            prefs[KEY_LONG_PRESS_PREVIEW] = s.longPressPreviewEnabled
            prefs[KEY_BAR_VISIBILITY] = s.barVisibility.name
            prefs[KEY_LANDSCAPE_ENABLED] = s.landscapeEnabled
            prefs[KEY_SEPARATE_SCREEN_LAYOUTS] = s.separateScreenLayouts
            prefs[KEY_TWO_PANE_TABLET] = s.twoPaneTablet
        }
    }

    /** #171 clear-all: wipes every DataStore key (settings, layout,
     *  vault credentials, hidden apps, notes/tasks/habits/timers,
     *  stacks, feed sources, usage history). The caller also resets
     *  its in-memory state; after this the next launch is a first
     *  launch again — home first, with the single default-home
     *  prompt if Marlow isn't the default home. */
    suspend fun clearAll(context: Context) {
        context.launcherDataStore.edit { it.clear() }
    }

    /** #104 custom search providers, one record per line, fields
     *  separated by U+001F (same escaping as the layout records):
     *  id, name, queryUrl, suggestionUrl. */
    suspend fun loadCustomProviders(context: Context): List<SearchProvider> {
        val raw = context.launcherDataStore.data.first()[KEY_CUSTOM_PROVIDERS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else SearchProvider(f[0], f[1], f[2], if (f.size > 3) f[3] else "")
        }
    }

    suspend fun saveCustomProviders(context: Context, providers: List<SearchProvider>) {
        val lines = providers.map { p ->
            listOf(p.id, p.name, p.queryUrl, p.suggestionUrl).joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_CUSTOM_PROVIDERS] = encode(lines) }
    }

    private fun readSettings(p: Preferences): LauncherSettings {
        val d = LauncherSettings()
        return d.copy(
            gridSize = enumValue(p[KEY_GRID], d.gridSize),
            gridMarginDp = p[KEY_GRID_MARGIN] ?: d.gridMarginDp,
            gridSpacingDp = p[KEY_GRID_SPACING] ?: d.gridSpacingDp,
            infiniteScroll = p[KEY_INFINITE] ?: d.infiniteScroll,
            indicatorStyle = enumValue(p[KEY_INDICATOR_STYLE], d.indicatorStyle),
            indicatorColour = enumValue(p[KEY_INDICATOR_COLOUR], d.indicatorColour),
            defaultPage = p[KEY_DEFAULT_PAGE] ?: d.defaultPage,
            dockVisible = p[KEY_DOCK_VISIBLE] ?: d.dockVisible,
            dockHeightDp = p[KEY_DOCK_HEIGHT] ?: d.dockHeightDp,
            dockBackground = p[KEY_DOCK_BG] ?: d.dockBackground,
            searchPillVisible = p[KEY_SEARCH_PILL] ?: d.searchPillVisible,
            drawerStyle = enumValue(p[KEY_DRAWER_STYLE], d.drawerStyle),
            drawerSort = enumValue(p[KEY_DRAWER_SORT], d.drawerSort),
            searchBarPlacement = enumValue(p[KEY_SEARCH_PLACEMENT], d.searchBarPlacement),
            pullToSearch = p[KEY_PULL_TO_SEARCH] ?: d.pullToSearch,
            frequentRowVisible = p[KEY_FREQUENT_ROW] ?: d.frequentRowVisible,
            rememberDrawerPosition = p[KEY_REMEMBER_DRAWER] ?: d.rememberDrawerPosition,
            drawerAutoClose = p[KEY_DRAWER_AUTO_CLOSE] ?: d.drawerAutoClose,
            twoColumnDrawer = p[KEY_TWO_COLUMN] ?: d.twoColumnDrawer,
            swipeUpToSearch = p[KEY_SWIPE_UP_TO_SEARCH] ?: d.swipeUpToSearch,
            drawerShowCategories = p[KEY_DRAWER_SHOW_CATEGORIES] ?: d.drawerShowCategories,
            drawerShowDrawerFolders = p[KEY_DRAWER_SHOW_FOLDERS] ?: d.drawerShowDrawerFolders,
            iconShape = enumValue(p[KEY_ICON_SHAPE], d.iconShape),
            iconSizeDp = p[KEY_ICON_SIZE] ?: d.iconSizeDp,
            showLabels = p[KEY_SHOW_LABELS] ?: d.showLabels,
            themedIcons = p[KEY_THEMED_ICONS] ?: d.themedIcons,
            smartSizeIcons = p[KEY_SMARTSIZE] ?: d.smartSizeIcons,
            liveClockIcon = p[KEY_LIVE_CLOCK] ?: d.liveClockIcon,
            iconPack = p[KEY_ICON_PACK] ?: d.iconPack,
            oneHanded = p[KEY_ONE_HANDED] ?: d.oneHanded,
            desktopLocked = p[KEY_LOCKED] ?: d.desktopLocked,
            autoAddApps = p[KEY_AUTO_ADD] ?: d.autoAddApps,
            longPressMenuOrder = splitList(p[KEY_MENU_ORDER], d.longPressMenuOrder),
            overviewActions = splitList(p[KEY_OVERVIEW_ACTIONS], d.overviewActions),
            themeMode = enumValue(p[KEY_THEME_MODE], d.themeMode),
            launcherTheme = enumValue(p[KEY_LAUNCHER_THEME], d.launcherTheme),
            accent = enumValue(p[KEY_ACCENT], d.accent),
            fontChoice = enumValue(p[KEY_FONT], d.fontChoice),
            surfaceStyle = enumValue(p[KEY_SURFACE_STYLE], d.surfaceStyle),
            amoledBlack = p[KEY_AMOLED] ?: d.amoledBlack,
            glanceVisible = p[KEY_GLANCE_VISIBLE] ?: d.glanceVisible,
            glanceCalendar = p[KEY_GLANCE_CALENDAR] ?: d.glanceCalendar,
            glanceWeather = p[KEY_GLANCE_WEATHER] ?: d.glanceWeather,
            glanceCommute = p[KEY_GLANCE_COMMUTE] ?: d.glanceCommute,
            glanceFinance = p[KEY_GLANCE_FINANCE] ?: d.glanceFinance,
            glanceSports = p[KEY_GLANCE_SPORTS] ?: d.glanceSports,
            weatherCityName = p[KEY_WEATHER_CITY] ?: d.weatherCityName,
            weatherCityLat = p[KEY_WEATHER_LAT] ?: d.weatherCityLat,
            weatherCityLon = p[KEY_WEATHER_LON] ?: d.weatherCityLon,
            surfaceAlpha = p[KEY_SURFACE_ALPHA] ?: d.surfaceAlpha,
            nowBriefHour = p[KEY_NOW_BRIEF_HOUR] ?: d.nowBriefHour,
            nowBriefMinute = p[KEY_NOW_BRIEF_MINUTE] ?: d.nowBriefMinute,
            smartspacerEnabled = p[KEY_SMARTSPACER] ?: d.smartspacerEnabled,
            nowBriefEnabled = p[KEY_NOW_BRIEF] ?: d.nowBriefEnabled,
            searchProviderId = p[KEY_SEARCH_PROVIDER] ?: d.searchProviderId,
            searchSuggestions = p[KEY_SEARCH_SUGGESTIONS] ?: d.searchSuggestions,
            hideStatusBar = p[KEY_HIDE_STATUS_BAR] ?: d.hideStatusBar,
            wallpaperVisible = p[KEY_WALLPAPER_VISIBLE] ?: d.wallpaperVisible,
            scrollingWallpaper = p[KEY_SCROLLING_WALLPAPER] ?: d.scrollingWallpaper,
            wallpaperGradient = p[KEY_WALLPAPER_GRADIENT] ?: d.wallpaperGradient,
            wallpaperDimPercent = (p[KEY_WALLPAPER_DIM] ?: d.wallpaperDimPercent).coerceIn(0, 70),
            headphoneMusicEnabled = p[KEY_HEADPHONE_MUSIC] ?: d.headphoneMusicEnabled,
            widgetPageEnabled = p[KEY_WIDGET_PAGE] ?: d.widgetPageEnabled,
            desktopWidgetPageEnabled = p[KEY_DESKTOP_WIDGET_PAGE] ?: d.desktopWidgetPageEnabled,
            widgetOverlapEnabled = p[KEY_WIDGET_OVERLAP] ?: d.widgetOverlapEnabled,
            stackAutoRotate = p[KEY_STACK_AUTO_ROTATE] ?: d.stackAutoRotate,
            mediaPlayerEnabled = p[KEY_MEDIA_PLAYER] ?: d.mediaPlayerEnabled,
            feedEnabled = p[KEY_FEED_ENABLED] ?: d.feedEnabled,
            googleFeedBridgeEnabled = p[KEY_GOOGLE_FEED_BRIDGE] ?: d.googleFeedBridgeEnabled,
            gesturesEnabled = p[KEY_GESTURES_ENABLED] ?: d.gesturesEnabled,
            hapticsEnabled = p[KEY_HAPTICS_ENABLED] ?: d.hapticsEnabled,
            doubleTapAction = enumValue(p[KEY_DOUBLE_TAP_ACTION], d.doubleTapAction),
            tripleTapAction = enumValue(p[KEY_TRIPLE_TAP_ACTION], d.tripleTapAction),
            twoFingerSwipeAction = enumValue(p[KEY_TWO_FINGER_SWIPE_ACTION], d.twoFingerSwipeAction),
            twoFingerDoubleTapAction = enumValue(p[KEY_TWO_FINGER_DOUBLE_TAP_ACTION], d.twoFingerDoubleTapAction),
            swipeDownAction = enumValue(p[KEY_SWIPE_DOWN_ACTION], d.swipeDownAction),
            pinchInAction = enumValue(p[KEY_PINCH_IN_ACTION], d.pinchInAction),
            pinchOutAction = enumValue(p[KEY_PINCH_OUT_ACTION], d.pinchOutAction),
            badgesEnabled = p[KEY_BADGES_ENABLED] ?: d.badgesEnabled,
            badgeStyle = enumValue(p[KEY_BADGE_STYLE], d.badgeStyle),
            badgePosition = enumValue(p[KEY_BADGE_POSITION], d.badgePosition),
            badgeSizeDp = (p[KEY_BADGE_SIZE] ?: d.badgeSizeDp).coerceIn(8, 28),
            badgeCornerRadiusDp = (p[KEY_BADGE_CORNER_RADIUS] ?: d.badgeCornerRadiusDp).coerceIn(0, 14),
            badgeOffPackages = splitList(p[KEY_BADGE_OFF_PACKAGES], d.badgeOffPackages.toList()).toSet(),
            dynamicBadges = p[KEY_DYNAMIC_BADGES] ?: d.dynamicBadges,
            inlinePreviewsEnabled = p[KEY_INLINE_PREVIEWS] ?: d.inlinePreviewsEnabled,
            longPressPreviewEnabled = p[KEY_LONG_PRESS_PREVIEW] ?: d.longPressPreviewEnabled,
            barVisibility = enumValue(p[KEY_BAR_VISIBILITY], d.barVisibility),
            landscapeEnabled = p[KEY_LANDSCAPE_ENABLED] ?: d.landscapeEnabled,
            separateScreenLayouts = p[KEY_SEPARATE_SCREEN_LAYOUTS] ?: d.separateScreenLayouts,
            twoPaneTablet = p[KEY_TWO_PANE_TABLET] ?: d.twoPaneTablet,
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String?, fallback: T): T =
        if (name == null) fallback else runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)

    private fun splitList(raw: String?, fallback: List<String>): List<String> =
        if (raw.isNullOrBlank()) fallback else raw.split(",").filter { it.isNotBlank() }

    // ---- escaping helpers ----

    private fun esc(value: String?): String =
        (value ?: "").replace("\\", "\\\\").replace("\u001F", "\\u").replace("\n", "\\n")

    private fun unesc(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> out.append('\\')
                    'u' -> out.append('\u001F')
                    'n' -> out.append('\n')
                    else -> out.append(value[i + 1])
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun splitRecord(line: String): List<String> {
        val parts = ArrayList<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                cur.append(c).append(line[i + 1])
                i += 2
            } else if (c == '\u001F') {
                parts.add(unesc(cur.toString()))
                cur.clear()
                i++
            } else {
                cur.append(c)
                i++
            }
        }
        parts.add(unesc(cur.toString()))
        return parts
    }

    private fun encode(lines: List<String>): String = lines.joinToString("\n")
    private fun decode(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }

    // ---- layout ----

    suspend fun loadHomeItems(context: Context): List<HomeItem> {
        val raw = context.launcherDataStore.data.first()[KEY_HOME_ITEMS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 13) null else HomeItem(
                id = f[0],
                kind = runCatching { HomeItemKind.valueOf(f[1]) }.getOrDefault(HomeItemKind.APP),
                page = f[2].toIntOrNull() ?: 0,
                // Records written before the dock rebuild (decision
                // F5) carry the retired dockPage field at index 3
                // and are 13–20 fields long; records written since
                // omit it and are exactly 19 fields. The offset
                // below shifts every field after the retired slot,
                // so both shapes load into the same HomeItem.
                subX = f[3 + dockPageOffset(f)].toIntOrNull() ?: 0,
                subY = f[4 + dockPageOffset(f)].toIntOrNull() ?: 0,
                spanX = f[5 + dockPageOffset(f)].toIntOrNull() ?: 2,
                spanY = f[6 + dockPageOffset(f)].toIntOrNull() ?: 2,
                component = f[7 + dockPageOffset(f)].ifEmpty { null },
                folderId = f[8 + dockPageOffset(f)].ifEmpty { null },
                shortcutId = f[9 + dockPageOffset(f)].ifEmpty { null },
                shortcutPackage = f[10 + dockPageOffset(f)].ifEmpty { null },
                label = f[11 + dockPageOffset(f)].ifEmpty { null },
                contactUri = if (f.size > 12 + dockPageOffset(f) && f[12 + dockPageOffset(f)].isNotEmpty()) f[12 + dockPageOffset(f)] else null,
                contactName = if (f.size > 13 + dockPageOffset(f)) f[13 + dockPageOffset(f)].ifEmpty { null } else null,
                pairId = if (f.size > 14 + dockPageOffset(f)) f[14 + dockPageOffset(f)].ifEmpty { null } else null,
                // Phase 3 fields, appended after the Phase 1/2 record;
                // older records simply lack them (defaults apply).
                appWidgetId = if (f.size > 15 + dockPageOffset(f)) f[15 + dockPageOffset(f)].toIntOrNull() ?: -1 else -1,
                providerComponent = if (f.size > 16 + dockPageOffset(f)) f[16 + dockPageOffset(f)].ifEmpty { null } else null,
                builtinWidgetId = if (f.size > 17 + dockPageOffset(f)) f[17 + dockPageOffset(f)].ifEmpty { null } else null,
                stackId = if (f.size > 18 + dockPageOffset(f)) f[18 + dockPageOffset(f)].ifEmpty { null } else null,
            )
        }
    }

    /** 1 when the record still carries the retired dockPage slot
     *  at index 3 (pre-rebuild records, 13–20 fields), 0 when it
     *  was written without it (exactly 19 fields). */
    private fun dockPageOffset(f: List<String>): Int = if (f.size == 19) 0 else 1

    suspend fun saveHomeItems(context: Context, items: List<HomeItem>) {
        val lines = items.map { it ->
            listOf(
                it.id, it.kind.name, it.page.toString(),
                it.subX.toString(), it.subY.toString(), it.spanX.toString(), it.spanY.toString(),
                it.component, it.folderId, it.shortcutId, it.shortcutPackage,
                it.label,
                it.contactUri, it.contactName, it.pairId,
                it.appWidgetId.toString(), it.providerComponent, it.builtinWidgetId, it.stackId,
            ).joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_HOME_ITEMS] = encode(lines) }
    }

    // ---- Phase 3 collections (#113, #118, #119, #7, #38) ----
    // Same delimited-record pattern as the layout records above.

    suspend fun loadNotes(context: Context): List<NoteItem> {
        val raw = context.launcherDataStore.data.first()[KEY_NOTES]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 2) null else NoteItem(f[0], f[1], f.getOrNull(2)?.toLongOrNull() ?: 0L)
        }
    }

    suspend fun saveNotes(context: Context, notes: List<NoteItem>) {
        val lines = notes.map { listOf(it.id, it.text, it.createdAt.toString()).joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_NOTES] = encode(lines) }
    }

    suspend fun loadTasks(context: Context): List<TaskItem> {
        val raw = context.launcherDataStore.data.first()[KEY_TASKS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 2) null else TaskItem(f[0], f[1], f.getOrNull(2) == "1", f.getOrNull(3)?.toLongOrNull() ?: 0L)
        }
    }

    suspend fun saveTasks(context: Context, tasks: List<TaskItem>) {
        val lines = tasks.map {
            listOf(it.id, it.text, if (it.done) "1" else "0", it.createdAt.toString())
                .joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_TASKS] = encode(lines) }
    }

    suspend fun loadHabits(context: Context): List<HabitItem> {
        val raw = context.launcherDataStore.data.first()[KEY_HABITS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 2) null else HabitItem(
                f[0], f[1],
                f.getOrNull(2)?.toIntOrNull() ?: 0,
                f.getOrNull(3) == "1",
                f.getOrNull(4)?.toLongOrNull() ?: 0L,
            )
        }
    }

    suspend fun saveHabits(context: Context, habits: List<HabitItem>) {
        val lines = habits.map {
            listOf(
                it.id, it.name, it.streak.toString(), if (it.doneToday) "1" else "0",
                it.lastDoneDay.toString(),
            ).joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_HABITS] = encode(lines) }
    }

    suspend fun loadTimers(context: Context): List<TimerItem> {
        val raw = context.launcherDataStore.data.first()[KEY_TIMERS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else TimerItem(
                f[0], f[1],
                f[2].toIntOrNull() ?: 0,
                f.getOrNull(3)?.toLongOrNull() ?: 0L,
            )
        }
    }

    suspend fun saveTimers(context: Context, timers: List<TimerItem>) {
        val lines = timers.map {
            listOf(it.id, it.label, it.totalSeconds.toString(), it.endsAtMillis.toString())
                .joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_TIMERS] = encode(lines) }
    }

    suspend fun loadWidgetStacks(context: Context): List<WidgetStack> {
        val raw = context.launcherDataStore.data.first()[KEY_WIDGET_STACKS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 2) null else WidgetStack(
                f[0],
                f[1].split(",").filter { it.isNotBlank() },
                f.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }
    }

    suspend fun saveWidgetStacks(context: Context, stacks: List<WidgetStack>) {
        val lines = stacks.map {
            listOf(it.id, it.memberIds.joinToString(","), it.currentIndex.toString())
                .joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_WIDGET_STACKS] = encode(lines) }
    }

    suspend fun loadFeedSources(context: Context): List<FeedSource> {
        val raw = context.launcherDataStore.data.first()[KEY_FEED_SOURCES]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else FeedSource(f[0], f[1], f[2])
        }
    }

    suspend fun saveFeedSources(context: Context, sources: List<FeedSource>) {
        val lines = sources.map { listOf(it.id, it.url, it.title).joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_FEED_SOURCES] = encode(lines) }
    }

    suspend fun loadHomeFolders(context: Context): List<HomeFolder> {
        val raw = context.launcherDataStore.data.first()[KEY_HOME_FOLDERS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else HomeFolder(
                f[0],
                f[1],
                f[2].split(",").filter { it.isNotBlank() },
                // #95 folder contacts, 4th field (absent in records
                // written before contacts existed): flat
                // uri/name pairs separated by U+001E. U+001E is not
                // a field separator and esc() leaves it alone, so
                // it survives the record round-trip.
                contacts = f.getOrNull(3)
                    ?.split("\u001E")?.filter { it.isNotBlank() }
                    ?.chunked(2)?.mapNotNull { pair ->
                        if (pair.size < 2) null else FolderContact(pair[0], pair[1])
                    } ?: emptyList(),
            )
        }
    }

    suspend fun saveHomeFolders(context: Context, folders: List<HomeFolder>) {
        val lines = folders.map {
            listOf(
                it.id,
                it.name,
                it.apps.joinToString(","),
                it.contacts.joinToString("\u001E") { contact -> "${contact.uri}\u001E${contact.name}" },
            ).joinToString("\u001F") { field -> esc(field) }
        }
        context.launcherDataStore.edit { it[KEY_HOME_FOLDERS] = encode(lines) }
    }

    suspend fun loadHomePages(context: Context): List<HomePage> {
        val raw = context.launcherDataStore.data.first()[KEY_HOME_PAGES]
        val pages = decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 2) null else HomePage(f[0], f[1] == "1")
        }
        return pages.ifEmpty { listOf(HomePage("page-0")) }
    }

    suspend fun saveHomePages(context: Context, pages: List<HomePage>) {
        val lines = pages.map { listOf(it.id, if (it.hidden) "1" else "0").joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_HOME_PAGES] = encode(lines) }
    }

    suspend fun loadDrawerFolders(context: Context): List<DrawerFolder> {
        val raw = context.launcherDataStore.data.first()[KEY_DRAWER_FOLDERS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else DrawerFolder(f[0], f[1], f[2].split(",").filter { it.isNotBlank() })
        }
    }

    suspend fun saveDrawerFolders(context: Context, folders: List<DrawerFolder>) {
        val lines = folders.map { listOf(it.id, it.name, it.apps.joinToString(",")).joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_DRAWER_FOLDERS] = encode(lines) }
    }

    suspend fun loadAppPairs(context: Context): List<AppPair> {
        val raw = context.launcherDataStore.data.first()[KEY_APP_PAIRS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 4) null else AppPair(f[0], f[1], f[2], f[3])
        }
    }

    suspend fun saveAppPairs(context: Context, pairs: List<AppPair>) {
        val lines = pairs.map { listOf(it.id, it.name, it.first, it.second).joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_APP_PAIRS] = encode(lines) }
    }

    suspend fun loadPinnedContacts(context: Context): List<PinnedContact> {
        val raw = context.launcherDataStore.data.first()[KEY_PINNED_CONTACTS]
        return decode(raw).mapNotNull { line ->
            val f = splitRecord(line)
            if (f.size < 3) null else PinnedContact(f[0], f[1], f[2])
        }
    }

    suspend fun savePinnedContacts(context: Context, contacts: List<PinnedContact>) {
        val lines = contacts.map { listOf(it.id, it.name, it.lookupUri).joinToString("\u001F") { field -> esc(field) } }
        context.launcherDataStore.edit { it[KEY_PINNED_CONTACTS] = encode(lines) }
    }

    // ---- per-app overrides + usage (#98, #99, #80, #83) ----

    /** component -> custom display label */
    suspend fun loadRenames(context: Context): Map<String, String> =
        readStringMap(context, KEY_RENAMES)

    suspend fun saveRenames(context: Context, renames: Map<String, String>) =
        writeStringMap(context, KEY_RENAMES, renames)

    /** component -> icon-pack drawable name ("" = default icon) */
    suspend fun loadIconOverrides(context: Context): Map<String, String> =
        readStringMap(context, KEY_ICON_OVERRIDES)

    suspend fun saveIconOverrides(context: Context, overrides: Map<String, String>) =
        writeStringMap(context, KEY_ICON_OVERRIDES, overrides)

    /** component -> "launchCount,lastUsedEpochMs" — our own usage history,
     *  used for the frequent row (#80) and most-used / last-used sorts
     *  (#83). No UsageStats permission is needed. */
    suspend fun loadLaunchStats(context: Context): Map<String, Pair<Int, Long>> {
        val map = readStringMap(context, KEY_LAUNCH_STATS)
        return map.mapValues { (_, v) ->
            val parts = v.split(",")
            Pair(parts.getOrNull(0)?.toIntOrNull() ?: 0, parts.getOrNull(1)?.toLongOrNull() ?: 0L)
        }
    }

    suspend fun saveLaunchStats(context: Context, stats: Map<String, Pair<Int, Long>>) =
        writeStringMap(context, KEY_LAUNCH_STATS, stats.mapValues { (_, v) -> "${v.first},${v.second}" })

    suspend fun loadKnownPackages(context: Context): Set<String> {
        val raw = context.launcherDataStore.data.first()[KEY_KNOWN_PACKAGES]
        return if (raw.isNullOrBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun saveKnownPackages(context: Context, packages: Set<String>) {
        context.launcherDataStore.edit { it[KEY_KNOWN_PACKAGES] = packages.joinToString(",") }
    }

    /** Audit fix (2026-11-26): whether the one-time home-seed
     *  self-healing has run (see HomeViewModel.repairHomeSeedIfNeeded). */
    suspend fun loadSeedRepaired(context: Context): Boolean =
        context.launcherDataStore.data.first()[KEY_SEED_REPAIRED] ?: false

    suspend fun saveSeedRepaired(context: Context, repaired: Boolean) {
        context.launcherDataStore.edit { it[KEY_SEED_REPAIRED] = repaired }
    }

    // Decision F2 (home first): whether the single first-run
    // default-home prompt has been shown. One-time per install —
    // the prompt fires once on first launch and never again, even
    // if the user declines; Settings → About still offers it on
    // user action. Not a LauncherSettings field: it is a one-time
    // flag like seed_repaired, not a setting, and it is not
    // carried in backups (a restored install prompts once).
    private val KEY_DEFAULT_HOME_PROMPT_SHOWN = booleanPreferencesKey("default_home_prompt_shown")

    suspend fun loadDefaultHomePromptShown(context: Context): Boolean =
        context.launcherDataStore.data.first()[KEY_DEFAULT_HOME_PROMPT_SHOWN] ?: false

    suspend fun saveDefaultHomePromptShown(context: Context, shown: Boolean) {
        context.launcherDataStore.edit { it[KEY_DEFAULT_HOME_PROMPT_SHOWN] = shown }
    }

    /** Audit fix (2026-11-26): whether the one-time glance-weather
     *  migration has run (see HomeViewModel init). */
    suspend fun loadGlanceWeatherMigrated(context: Context): Boolean =
        context.launcherDataStore.data.first()[KEY_GLANCE_WEATHER_MIGRATED] ?: false

    suspend fun saveGlanceWeatherMigrated(context: Context, migrated: Boolean) {
        context.launcherDataStore.edit { it[KEY_GLANCE_WEATHER_MIGRATED] = migrated }
    }

    suspend fun loadDrawerScroll(context: Context): Int =
        context.launcherDataStore.data.first()[KEY_DRAWER_SCROLL] ?: 0

    suspend fun saveDrawerScroll(context: Context, index: Int) {
        context.launcherDataStore.edit { it[KEY_DRAWER_SCROLL] = index }
    }

    suspend fun loadDrawerQuery(context: Context): String =
        context.launcherDataStore.data.first()[KEY_DRAWER_QUERY] ?: ""

    suspend fun saveDrawerQuery(context: Context, query: String) {
        context.launcherDataStore.edit { it[KEY_DRAWER_QUERY] = query }
    }

    // ---- Phase 4: PIN vault (#34), hidden apps, per-app languages
    // (#155), swipe-up secondary actions (#28) ----

    /** The stored PIN credentials: (salt hex, hash hex), or null
     *  when no PIN has been set. The PIN itself is never stored. */
    suspend fun loadPinCredentials(context: Context): Pair<String, String>? {
        val prefs = context.launcherDataStore.data.first()
        val salt = prefs[KEY_PIN_SALT] ?: return null
        val hash = prefs[KEY_PIN_HASH] ?: return null
        return Pair(salt, hash)
    }

    suspend fun savePinCredentials(context: Context, saltHex: String, hashHex: String) {
        context.launcherDataStore.edit {
            it[KEY_PIN_SALT] = saltHex
            it[KEY_PIN_HASH] = hashHex
        }
    }

    suspend fun clearPinCredentials(context: Context) {
        context.launcherDataStore.edit {
            it.remove(KEY_PIN_SALT)
            it.remove(KEY_PIN_HASH)
        }
    }

    /** Hidden apps, as flattened ComponentNames. */
    suspend fun loadHiddenApps(context: Context): Set<String> {
        val raw = context.launcherDataStore.data.first()[KEY_HIDDEN_APPS]
        return if (raw.isNullOrBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun saveHiddenApps(context: Context, hidden: Set<String>) {
        context.launcherDataStore.edit { it[KEY_HIDDEN_APPS] = hidden.joinToString(",") }
    }

    /** #13 the manual drawer order (DrawerSort.CUSTOM), as flattened
     *  ComponentNames in order. Apps missing from the list sort by
     *  name after the ordered ones. */
    suspend fun loadDrawerCustomOrder(context: Context): List<String> {
        val raw = context.launcherDataStore.data.first()[KEY_DRAWER_CUSTOM_ORDER]
        return if (raw.isNullOrBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun saveDrawerCustomOrder(context: Context, order: List<String>) {
        context.launcherDataStore.edit { it[KEY_DRAWER_CUSTOM_ORDER] = order.joinToString(",") }
    }

    /** #155 per-app language overrides, keyed by package name. */
    suspend fun loadAppLanguages(context: Context): List<AppLanguageOverride> =
        readStringMap(context, KEY_APP_LANGUAGES).map { (pkg, tag) -> AppLanguageOverride(pkg, tag) }

    suspend fun saveAppLanguages(context: Context, overrides: List<AppLanguageOverride>) =
        writeStringMap(context, KEY_APP_LANGUAGES, overrides.associate { it.packageName to it.languageTag })

    /** #28 swipe-up secondary action per home item, keyed by item id. */
    suspend fun loadSwipeUpActions(context: Context): Map<String, ShortcutSecondaryAction> =
        readStringMap(context, KEY_SWIPE_UP_ACTIONS).mapValues { (_, name) ->
            runCatching { ShortcutSecondaryAction.valueOf(name) }.getOrDefault(ShortcutSecondaryAction.NONE)
        }

    suspend fun saveSwipeUpActions(context: Context, actions: Map<String, ShortcutSecondaryAction>) =
        writeStringMap(context, KEY_SWIPE_UP_ACTIONS, actions.mapValues { (_, action) -> action.name })

    private suspend fun readStringMap(context: Context, key: Preferences.Key<String>): Map<String, String> {
        val raw = context.launcherDataStore.data.first()[key] ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (line in raw.split("\n")) {
            if (line.isBlank()) continue
            val f = splitRecord(line)
            if (f.size >= 2) out[f[0]] = f[1]
        }
        return out
    }

    private suspend fun writeStringMap(context: Context, key: Preferences.Key<String>, map: Map<String, String>) {
        val raw = map.entries.joinToString("\n") { (k, v) -> esc(k) + "\u001F" + esc(v) }
        context.launcherDataStore.edit { it[key] = raw }
    }
}
