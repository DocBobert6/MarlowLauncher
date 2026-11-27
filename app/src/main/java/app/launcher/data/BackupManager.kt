package app.launcher.data

import android.content.Context
import android.net.Uri
import app.launcher.model.AccentColour
import app.launcher.model.AppLanguageOverride
import app.launcher.model.AppPair
import app.launcher.model.BadgePosition
import app.launcher.model.BadgeStyle
import app.launcher.model.BarVisibility
import app.launcher.model.DrawerFolder
import app.launcher.model.DrawerSort
import app.launcher.model.DrawerStyle
import app.launcher.model.FeedSource
import app.launcher.model.FolderContact
import app.launcher.model.FontChoice
import app.launcher.model.GestureAction
import app.launcher.model.GridSize
import app.launcher.model.HabitItem
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.HomePage
import app.launcher.model.IconShape
import app.launcher.model.IndicatorColour
import app.launcher.model.LauncherSettings
import app.launcher.model.LauncherThemeChoice
import app.launcher.model.NoteItem
import app.launcher.model.PinnedContact
import app.launcher.model.SearchBarPlacement
import app.launcher.model.ShortcutSecondaryAction
import app.launcher.model.SurfaceStyle
import app.launcher.model.TaskItem
import app.launcher.model.ThemeMode
import app.launcher.model.TimerItem
import app.launcher.model.WidgetStack

/**
 * #47 backup & restore home layout — MANUAL LOCAL FILE ONLY.
 *
 * Export writes one JSON document (version 1) to a file the user
 * picks through the Storage Access Framework; import reads it back
 * and replaces the layout + settings. That is the whole feature:
 * no schedule (#163 removed), no selective backup (#164 removed),
 * no cloud destinations (#165 removed), no named portable format
 * (#166 removed — the file is plain JSON and the user names it in
 * the system picker), no backup-honesty extras (#167 removed).
 *
 * No serialisation library is a declared dependency, so the JSON
 * writer and the small parser below are hand-rolled. Records are
 * positional arrays in the same field order LauncherStore uses, so
 * an old record (fewer fields) still loads with defaults. Import
 * validates the version field and drops anything it cannot parse
 * rather than failing the whole restore.
 *
 * EXACTLY what a backup restores (rule 72 — no silent gaps):
 * settings, home items (including dock items), home folders, home
 * pages, drawer folders, app pairs, pinned contacts, app renames,
 * icon overrides, hidden apps, per-app languages, swipe-up actions,
 * widget stacks, feed sources, and notes / tasks / habits / timers.
 * Built-in widgets restore (they are identified by builtinWidgetId).
 *
 * What it does NOT restore:
 * - Third-party widgets CANNOT be restored on a new device.
 *   AppWidgetHost ids are per-device: the backup stores the old
 *   device's appWidgetId, and on the new device that id has no
 *   provider behind it, so the spot comes back as "Widget
 *   unavailable" (see ThirdPartyWidgetView in Widgets.kt) until the
 *   user adds the widget again. The home item record itself IS
 *   restored — only the live widget behind it is not.
 * - Import from other launchers is NOT supported — including Nova
 *   import. There is no Nova-backup reader; a Nova file is not a
 *   Marlow backup and import rejects it (wrong version / not JSON).
 * - Three settings fields are NOT carried in the backup file and fall
 *   back to defaults on restore (a known silent gap, recorded here
 *   so it is not silent): blur-glass transparency (surfaceAlpha),
 *   the weather city (weatherCityName/Lat/Lon), and the morning-summary
 *   time (nowBriefHour/Minute). Everything else in LauncherSettings is
 *   carried — see [settingsToMap]. (The deleted first-run wizard state
 *   used to be a fourth gap; there is no wizard state any more.)
 *
 * The user-facing one-paragraph version of this lives in
 * [RESTORE_SCOPE_TEXT] and is shown at the bottom of Settings →
 * About.
 */
data class BackupData(
    val settings: LauncherSettings,
    val homeItems: List<HomeItem>,
    val homeFolders: List<HomeFolder>,
    val homePages: List<HomePage>,
    val drawerFolders: List<DrawerFolder>,
    val appPairs: List<AppPair>,
    val pinnedContacts: List<PinnedContact>,
    val renames: Map<String, String>,
    val iconOverrides: Map<String, String>,
    val hiddenApps: Set<String>,
    val appLanguages: List<AppLanguageOverride>,
    val swipeUpActions: Map<String, ShortcutSecondaryAction>,
    val widgetStacks: List<WidgetStack>,
    val feedSources: List<FeedSource>,
    val notes: List<NoteItem>,
    val tasks: List<TaskItem>,
    val habits: List<HabitItem>,
    val timers: List<TimerItem>,
)

object BackupManager {

    const val BACKUP_VERSION = 1

    /**
     * User-facing statement of exactly what a backup restores (rule
     * 72). Shown at the bottom of Settings → About.
     */
    const val RESTORE_SCOPE_TEXT =
        "Backup restores the home layout, dock, folders, drawer folders, " +
            "app pairs, pinned contacts, renames, icon choices, hidden apps, " +
            "and settings. Third-party widgets are not restored on a new " +
            "device: their widget ids belong to the old device, so those " +
            "spots come back as unavailable until the widget is added again. " +
            "Import from other launchers, including Nova, is not supported."

    /** Writes the backup JSON to the user-picked Uri. */
    fun exportToUri(context: Context, uri: Uri, data: BackupData): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(toJson(data).toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Reads a backup back. Null = not a launcher backup (wrong
     *  version, not JSON, unreadable file). */
    fun importFromUri(context: Context, uri: Uri): BackupData? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return null
            fromJson(text)
        } catch (e: Exception) {
            null
        }
    }

    // ---- JSON writing ----

    fun toJson(data: BackupData): String {
        val sb = StringBuilder()
        sb.append("{\"version\":").append(BACKUP_VERSION)
        sb.append(",\"settings\":").append(mapToJson(settingsToMap(data.settings)))
        sb.append(",\"homeItems\":").append(listToJson(data.homeItems.map { homeItemToJson(it) }))
        sb.append(",\"homeFolders\":").append(listToJson(data.homeFolders.map {
            folderToJson(it.id, it.name, it.apps, it.contacts)
        }))
        sb.append(",\"homePages\":").append(listToJson(data.homePages.map { pageToJson(it) }))
        sb.append(",\"drawerFolders\":").append(listToJson(data.drawerFolders.map { folderToJson(it.id, it.name, it.apps) }))
        sb.append(",\"appPairs\":").append(
            listToJson(data.appPairs.map { listOf(it.id, it.name, it.first, it.second) }),
        )
        sb.append(",\"pinnedContacts\":").append(
            listToJson(data.pinnedContacts.map { listOf(it.id, it.name, it.lookupUri) }),
        )
        sb.append(",\"renames\":").append(mapToJson(data.renames))
        sb.append(",\"iconOverrides\":").append(mapToJson(data.iconOverrides))
        sb.append(",\"hiddenApps\":").append(listToJson(data.hiddenApps.sorted()))
        sb.append(",\"appLanguages\":").append(
            listToJson(data.appLanguages.map { listOf(it.packageName, it.languageTag) }),
        )
        sb.append(",\"swipeUpActions\":").append(
            mapToJson(data.swipeUpActions.mapValues { it.value.name }),
        )
        sb.append(",\"widgetStacks\":").append(
            listToJson(data.widgetStacks.map { listOf(it.id, it.memberIds.joinToString(","), it.currentIndex.toString()) }),
        )
        sb.append(",\"feedSources\":").append(
            listToJson(data.feedSources.map { listOf(it.id, it.url, it.title) }),
        )
        sb.append(",\"notes\":").append(
            listToJson(data.notes.map { listOf(it.id, it.text, it.createdAt.toString()) }),
        )
        sb.append(",\"tasks\":").append(
            listToJson(data.tasks.map { listOf(it.id, it.text, if (it.done) "1" else "0", it.createdAt.toString()) }),
        )
        sb.append(",\"habits\":").append(
            listToJson(data.habits.map {
                listOf(it.id, it.name, it.streak.toString(), if (it.doneToday) "1" else "0", it.lastDoneDay.toString())
            }),
        )
        sb.append(",\"timers\":").append(
            listToJson(data.timers.map { listOf(it.id, it.label, it.totalSeconds.toString(), it.endsAtMillis.toString()) }),
        )
        sb.append("}")
        return sb.toString()
    }

    private fun homeItemToJson(item: HomeItem): List<String> = listOf(
        item.id, item.kind.name, item.page.toString(),
        item.subX.toString(), item.subY.toString(), item.spanX.toString(), item.spanY.toString(),
        item.component ?: "", item.folderId ?: "", item.shortcutId ?: "", item.shortcutPackage ?: "",
        item.label ?: "", item.contactUri ?: "", item.contactName ?: "", item.pairId ?: "",
        item.appWidgetId.toString(), item.providerComponent ?: "",
        item.builtinWidgetId ?: "", item.stackId ?: "",
    )

    private fun folderToJson(
        id: String,
        name: String,
        apps: List<String>,
        contacts: List<FolderContact> = emptyList(),
    ): List<String> =
        listOf(
            id,
            name,
            apps.joinToString(","),
            // #95 folder contacts, same flat U+001E uri/name encoding
            // as LauncherStore; the JSON writer escapes the control
            // character and the parser decodes it back. Old backups
            // (3 fields) still load — the 4th field is optional.
            contacts.joinToString("\u001E") { contact -> "${contact.uri}\u001E${contact.name}" },
        )

    private fun pageToJson(page: HomePage): List<String> =
        listOf(page.id, if (page.hidden) "1" else "0")

    private fun mapToJson(map: Map<String, String>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in map) {
            if (!first) sb.append(",")
            first = false
            sb.append(stringToJson(key)).append(":").append(stringToJson(value))
        }
        return sb.append("}").toString()
    }

    private fun listToJson(items: List<*>): String {
        val sb = StringBuilder("[")
        var first = true
        for (item in items) {
            if (!first) sb.append(",")
            first = false
            when (item) {
                is String -> sb.append(stringToJson(item))
                is List<*> -> sb.append(listToJson(item))
                else -> sb.append(stringToJson(item.toString()))
            }
        }
        return sb.append("]").toString()
    }

    private fun stringToJson(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) sb.append(String.format("\\u%04x", c.code))
                    else sb.append(c)
                }
            }
        }
        return sb.append("\"").toString()
    }

    // ---- settings as a flat string map ----

    fun settingsToMap(s: LauncherSettings): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        fun put(key: String, value: Any) {
            map[key] = value.toString()
        }
        put("grid_size", s.gridSize.name)
        put("grid_margin_dp", s.gridMarginDp)
        put("grid_spacing_dp", s.gridSpacingDp)
        put("infinite_scroll", s.infiniteScroll)
        put("indicator_style", s.indicatorStyle.name)
        put("indicator_colour", s.indicatorColour.name)
        put("default_page", s.defaultPage)
        put("dock_visible", s.dockVisible)
        put("dock_height_dp", s.dockHeightDp)
        put("dock_background", s.dockBackground)
        put("search_pill_visible", s.searchPillVisible)
        put("drawer_style", s.drawerStyle.name)
        put("drawer_sort", s.drawerSort.name)
        put("search_bar_placement", s.searchBarPlacement.name)
        put("pull_to_search", s.pullToSearch)
        put("frequent_row_visible", s.frequentRowVisible)
        put("remember_drawer_position", s.rememberDrawerPosition)
        put("drawer_auto_close", s.drawerAutoClose)
        put("two_column_drawer", s.twoColumnDrawer)
        put("swipe_up_to_search", s.swipeUpToSearch)
        put("drawer_show_categories", s.drawerShowCategories)
        put("drawer_show_drawer_folders", s.drawerShowDrawerFolders)
        put("icon_shape", s.iconShape.name)
        put("icon_size_dp", s.iconSizeDp)
        put("show_labels", s.showLabels)
        put("themed_icons", s.themedIcons)
        put("smartsize_icons", s.smartSizeIcons)
        put("live_clock_icon", s.liveClockIcon)
        put("icon_pack", s.iconPack)
        put("one_handed", s.oneHanded)
        put("desktop_locked", s.desktopLocked)
        put("auto_add_apps", s.autoAddApps)
        put("long_press_menu_order", s.longPressMenuOrder.joinToString(","))
        put("overview_actions", s.overviewActions.joinToString(","))
        put("theme_mode", s.themeMode.name)
        put("launcher_theme", s.launcherTheme.name)
        put("accent_colour", s.accent.name)
        put("font_choice", s.fontChoice.name)
        put("surface_style", s.surfaceStyle.name)
        put("amoled_black", s.amoledBlack)
        put("glance_visible", s.glanceVisible)
        put("glance_calendar", s.glanceCalendar)
        put("glance_weather", s.glanceWeather)
        put("glance_commute", s.glanceCommute)
        put("glance_finance", s.glanceFinance)
        put("glance_sports", s.glanceSports)
        put("smartspacer_enabled", s.smartspacerEnabled)
        put("now_brief_enabled", s.nowBriefEnabled)
        put("search_provider_id", s.searchProviderId)
        put("search_suggestions", s.searchSuggestions)
        put("hide_status_bar", s.hideStatusBar)
        put("wallpaper_visible", s.wallpaperVisible)
        put("scrolling_wallpaper", s.scrollingWallpaper)
        put("wallpaper_gradient", s.wallpaperGradient)
        put("wallpaper_dim_percent", s.wallpaperDimPercent)
        put("headphone_music_enabled", s.headphoneMusicEnabled)
        put("widget_page_enabled", s.widgetPageEnabled)
        put("desktop_widget_page_enabled", s.desktopWidgetPageEnabled)
        put("widget_overlap_enabled", s.widgetOverlapEnabled)
        put("stack_auto_rotate", s.stackAutoRotate)
        put("media_player_enabled", s.mediaPlayerEnabled)
        put("feed_enabled", s.feedEnabled)
        put("google_feed_bridge_enabled", s.googleFeedBridgeEnabled)
        put("gestures_enabled", s.gesturesEnabled)
        put("double_tap_action", s.doubleTapAction.name)
        put("triple_tap_action", s.tripleTapAction.name)
        put("two_finger_swipe_action", s.twoFingerSwipeAction.name)
        put("two_finger_double_tap_action", s.twoFingerDoubleTapAction.name)
        put("swipe_down_action", s.swipeDownAction.name)
        put("pinch_in_action", s.pinchInAction.name)
        put("pinch_out_action", s.pinchOutAction.name)
        put("badges_enabled", s.badgesEnabled)
        put("badge_style", s.badgeStyle.name)
        put("badge_position", s.badgePosition.name)
        put("badge_size_dp", s.badgeSizeDp)
        put("badge_corner_radius_dp", s.badgeCornerRadiusDp)
        put("dynamic_badges", s.dynamicBadges)
        put("badge_off_packages", s.badgeOffPackages.joinToString(","))
        put("inline_previews_enabled", s.inlinePreviewsEnabled)
        put("long_press_preview_enabled", s.longPressPreviewEnabled)
        put("bar_visibility", s.barVisibility.name)
        put("landscape_enabled", s.landscapeEnabled)
        put("separate_screen_layouts", s.separateScreenLayouts)
        put("two_pane_tablet", s.twoPaneTablet)
        return map
    }

    fun settingsFromMap(map: Map<String, String>): LauncherSettings {
        val d = LauncherSettings()
        fun bool(key: String, fallback: Boolean): Boolean =
            map[key]?.toBooleanStrictOrNull() ?: fallback
        fun int(key: String, fallback: Int): Int = map[key]?.toIntOrNull() ?: fallback
        fun list(key: String, fallback: List<String>): List<String> {
            val raw = map[key] ?: return fallback
            return if (raw.isBlank()) fallback else raw.split(",").filter { it.isNotBlank() }
        }
        fun <T : Enum<T>> enum(key: String, fallback: T): T =
            map[key]?.let { name ->
                runCatching { java.lang.Enum.valueOf(fallback.javaClass, name) }.getOrNull()
            } ?: fallback
        return d.copy(
            gridSize = enum("grid_size", d.gridSize),
            gridMarginDp = int("grid_margin_dp", d.gridMarginDp),
            gridSpacingDp = int("grid_spacing_dp", d.gridSpacingDp),
            infiniteScroll = bool("infinite_scroll", d.infiniteScroll),
            indicatorStyle = enum("indicator_style", d.indicatorStyle),
            indicatorColour = enum("indicator_colour", d.indicatorColour),
            defaultPage = int("default_page", d.defaultPage),
            dockVisible = bool("dock_visible", d.dockVisible),
            dockHeightDp = int("dock_height_dp", d.dockHeightDp),
            dockBackground = bool("dock_background", d.dockBackground),
            searchPillVisible = bool("search_pill_visible", d.searchPillVisible),
            drawerStyle = enum("drawer_style", d.drawerStyle),
            drawerSort = enum("drawer_sort", d.drawerSort),
            searchBarPlacement = enum("search_bar_placement", d.searchBarPlacement),
            pullToSearch = bool("pull_to_search", d.pullToSearch),
            frequentRowVisible = bool("frequent_row_visible", d.frequentRowVisible),
            rememberDrawerPosition = bool("remember_drawer_position", d.rememberDrawerPosition),
            drawerAutoClose = bool("drawer_auto_close", d.drawerAutoClose),
            twoColumnDrawer = bool("two_column_drawer", d.twoColumnDrawer),
            swipeUpToSearch = bool("swipe_up_to_search", d.swipeUpToSearch),
            drawerShowCategories = bool("drawer_show_categories", d.drawerShowCategories),
            drawerShowDrawerFolders = bool("drawer_show_drawer_folders", d.drawerShowDrawerFolders),
            iconShape = enum("icon_shape", d.iconShape),
            iconSizeDp = int("icon_size_dp", d.iconSizeDp),
            showLabels = bool("show_labels", d.showLabels),
            themedIcons = bool("themed_icons", d.themedIcons),
            smartSizeIcons = bool("smartsize_icons", d.smartSizeIcons),
            liveClockIcon = bool("live_clock_icon", d.liveClockIcon),
            iconPack = map["icon_pack"] ?: d.iconPack,
            oneHanded = bool("one_handed", d.oneHanded),
            desktopLocked = bool("desktop_locked", d.desktopLocked),
            autoAddApps = bool("auto_add_apps", d.autoAddApps),
            longPressMenuOrder = list("long_press_menu_order", d.longPressMenuOrder),
            overviewActions = list("overview_actions", d.overviewActions),
            themeMode = enum("theme_mode", d.themeMode),
            launcherTheme = enum("launcher_theme", d.launcherTheme),
            accent = enum("accent_colour", d.accent),
            fontChoice = enum("font_choice", d.fontChoice),
            surfaceStyle = enum("surface_style", d.surfaceStyle),
            amoledBlack = bool("amoled_black", d.amoledBlack),
            glanceVisible = bool("glance_visible", d.glanceVisible),
            glanceCalendar = bool("glance_calendar", d.glanceCalendar),
            glanceWeather = bool("glance_weather", d.glanceWeather),
            glanceCommute = bool("glance_commute", d.glanceCommute),
            glanceFinance = bool("glance_finance", d.glanceFinance),
            glanceSports = bool("glance_sports", d.glanceSports),
            smartspacerEnabled = bool("smartspacer_enabled", d.smartspacerEnabled),
            nowBriefEnabled = bool("now_brief_enabled", d.nowBriefEnabled),
            searchProviderId = map["search_provider_id"] ?: d.searchProviderId,
            searchSuggestions = bool("search_suggestions", d.searchSuggestions),
            hideStatusBar = bool("hide_status_bar", d.hideStatusBar),
            wallpaperVisible = bool("wallpaper_visible", d.wallpaperVisible),
            scrollingWallpaper = bool("scrolling_wallpaper", d.scrollingWallpaper),
            wallpaperGradient = bool("wallpaper_gradient", d.wallpaperGradient),
            wallpaperDimPercent = int("wallpaper_dim_percent", d.wallpaperDimPercent).coerceIn(0, 70),
            headphoneMusicEnabled = bool("headphone_music_enabled", d.headphoneMusicEnabled),
            widgetPageEnabled = bool("widget_page_enabled", d.widgetPageEnabled),
            desktopWidgetPageEnabled = bool("desktop_widget_page_enabled", d.desktopWidgetPageEnabled),
            widgetOverlapEnabled = bool("widget_overlap_enabled", d.widgetOverlapEnabled),
            stackAutoRotate = bool("stack_auto_rotate", d.stackAutoRotate),
            mediaPlayerEnabled = bool("media_player_enabled", d.mediaPlayerEnabled),
            feedEnabled = bool("feed_enabled", d.feedEnabled),
            googleFeedBridgeEnabled = bool("google_feed_bridge_enabled", d.googleFeedBridgeEnabled),
            gesturesEnabled = bool("gestures_enabled", d.gesturesEnabled),
            doubleTapAction = enum("double_tap_action", d.doubleTapAction),
            tripleTapAction = enum("triple_tap_action", d.tripleTapAction),
            twoFingerSwipeAction = enum("two_finger_swipe_action", d.twoFingerSwipeAction),
            twoFingerDoubleTapAction = enum("two_finger_double_tap_action", d.twoFingerDoubleTapAction),
            swipeDownAction = enum("swipe_down_action", d.swipeDownAction),
            pinchInAction = enum("pinch_in_action", d.pinchInAction),
            pinchOutAction = enum("pinch_out_action", d.pinchOutAction),
            badgesEnabled = bool("badges_enabled", d.badgesEnabled),
            badgeStyle = enum("badge_style", d.badgeStyle),
            badgePosition = enum("badge_position", d.badgePosition),
            badgeSizeDp = int("badge_size_dp", d.badgeSizeDp).coerceIn(8, 28),
            badgeCornerRadiusDp = int("badge_corner_radius_dp", d.badgeCornerRadiusDp).coerceIn(0, 14),
            dynamicBadges = bool("dynamic_badges", d.dynamicBadges),
            badgeOffPackages = list("badge_off_packages", d.badgeOffPackages.toList()).toSet(),
            inlinePreviewsEnabled = bool("inline_previews_enabled", d.inlinePreviewsEnabled),
            longPressPreviewEnabled = bool("long_press_preview_enabled", d.longPressPreviewEnabled),
            barVisibility = enum("bar_visibility", d.barVisibility),
            landscapeEnabled = bool("landscape_enabled", d.landscapeEnabled),
            separateScreenLayouts = bool("separate_screen_layouts", d.separateScreenLayouts),
            twoPaneTablet = bool("two_pane_tablet", d.twoPaneTablet),
        )
    }

    // ---- JSON reading ----

    fun fromJson(text: String): BackupData? {
        val root = JsonParser(text).parseValue() as? JsonValue.JsonObject ?: return null
        val version = (root.values["version"] as? JsonValue.JsonNumber)?.value?.toIntOrNull()
            ?: return null
        if (version != BACKUP_VERSION) return null
        val settingsMap = (root.values["settings"] as? JsonValue.JsonObject)
            ?.values?.mapValues { (_, v) -> (v as? JsonValue.JsonString)?.value ?: "" }
            ?: emptyMap()
        fun stringList(key: String): List<List<String>> =
            (root.values[key] as? JsonValue.JsonArray)?.items?.mapNotNull { item ->
                (item as? JsonValue.JsonArray)?.items?.map { cell ->
                    (cell as? JsonValue.JsonString)?.value ?: ""
                }
            } ?: emptyList()
        fun flatStringList(key: String): List<String> =
            (root.values[key] as? JsonValue.JsonArray)?.items?.mapNotNull {
                (it as? JsonValue.JsonString)?.value
            } ?: emptyList()
        fun stringMap(key: String): Map<String, String> =
            (root.values[key] as? JsonValue.JsonObject)?.values?.mapValues { (_, v) ->
                (v as? JsonValue.JsonString)?.value ?: ""
            } ?: emptyMap()
        val homeItems = stringList("homeItems").mapNotNull { f ->
            if (f.size < 13) return@mapNotNull null
            // Same retired-dockPage offset as LauncherStore: backup
            // records written before the dock rebuild carry the
            // retired dockPage field at index 3 (13–20 fields);
            // records written since omit it (exactly 19 fields).
            val o = if (f.size == 19) 0 else 1
            HomeItem(
                id = f[0],
                kind = runCatching { HomeItemKind.valueOf(f[1]) }.getOrDefault(HomeItemKind.APP),
                page = f[2].toIntOrNull() ?: 0,
                subX = f[3 + o].toIntOrNull() ?: 0,
                subY = f[4 + o].toIntOrNull() ?: 0,
                spanX = f[5 + o].toIntOrNull() ?: 2,
                spanY = f[6 + o].toIntOrNull() ?: 2,
                component = f[7 + o].ifEmpty { null },
                folderId = f[8 + o].ifEmpty { null },
                shortcutId = f[9 + o].ifEmpty { null },
                shortcutPackage = f[10 + o].ifEmpty { null },
                label = f[11 + o].ifEmpty { null },
                contactUri = f.getOrNull(12 + o)?.ifEmpty { null },
                contactName = f.getOrNull(13 + o)?.ifEmpty { null },
                pairId = f.getOrNull(14 + o)?.ifEmpty { null },
                appWidgetId = f.getOrNull(15 + o)?.toIntOrNull() ?: -1,
                providerComponent = f.getOrNull(16 + o)?.ifEmpty { null },
                builtinWidgetId = f.getOrNull(17 + o)?.ifEmpty { null },
                stackId = f.getOrNull(18 + o)?.ifEmpty { null },
            )
        }
        fun homeFolders(): List<HomeFolder> =
            stringList("homeFolders").mapNotNull { f ->
                if (f.size < 3) null else HomeFolder(
                    f[0],
                    f[1],
                    f[2].split(",").filter { it.isNotBlank() },
                    contacts = f.getOrNull(3)
                        ?.split("\u001E")?.filter { it.isNotBlank() }
                        ?.chunked(2)?.mapNotNull { pair ->
                            if (pair.size < 2) null else FolderContact(pair[0], pair[1])
                        } ?: emptyList(),
                )
            }
        fun drawerFolders(): List<DrawerFolder> =
            stringList("drawerFolders").mapNotNull { f ->
                if (f.size < 3) null else DrawerFolder(f[0], f[1], f[2].split(",").filter { it.isNotBlank() })
            }
        return BackupData(
            settings = settingsFromMap(settingsMap),
            homeItems = homeItems,
            homeFolders = homeFolders(),
            homePages = stringList("homePages").mapNotNull { f ->
                if (f.size < 2) null else HomePage(f[0], f[1] == "1")
            },
            drawerFolders = drawerFolders(),
            appPairs = stringList("appPairs").mapNotNull { f ->
                if (f.size < 4) null else AppPair(f[0], f[1], f[2], f[3])
            },
            pinnedContacts = stringList("pinnedContacts").mapNotNull { f ->
                if (f.size < 3) null else PinnedContact(f[0], f[1], f[2])
            },
            renames = stringMap("renames"),
            iconOverrides = stringMap("iconOverrides"),
            hiddenApps = flatStringList("hiddenApps").toSet(),
            appLanguages = stringList("appLanguages").mapNotNull { f ->
                if (f.size < 2) null else AppLanguageOverride(f[0], f[1])
            },
            swipeUpActions = stringMap("swipeUpActions").mapValues { (_, name) ->
                runCatching { ShortcutSecondaryAction.valueOf(name) }.getOrDefault(ShortcutSecondaryAction.NONE)
            },
            widgetStacks = stringList("widgetStacks").mapNotNull { f ->
                if (f.size < 2) null else WidgetStack(
                    f[0], f[1].split(",").filter { it.isNotBlank() },
                    f.getOrNull(2)?.toIntOrNull() ?: 0,
                )
            },
            feedSources = stringList("feedSources").mapNotNull { f ->
                if (f.size < 3) null else FeedSource(f[0], f[1], f[2])
            },
            notes = stringList("notes").mapNotNull { f ->
                if (f.size < 2) null else NoteItem(f[0], f[1], f.getOrNull(2)?.toLongOrNull() ?: 0L)
            },
            tasks = stringList("tasks").mapNotNull { f ->
                if (f.size < 2) null else TaskItem(
                    f[0], f[1], f.getOrNull(2) == "1", f.getOrNull(3)?.toLongOrNull() ?: 0L,
                )
            },
            habits = stringList("habits").mapNotNull { f ->
                if (f.size < 2) null else HabitItem(
                    f[0], f[1], f.getOrNull(2)?.toIntOrNull() ?: 0,
                    f.getOrNull(3) == "1", f.getOrNull(4)?.toLongOrNull() ?: 0L,
                )
            },
            timers = stringList("timers").mapNotNull { f ->
                if (f.size < 3) null else TimerItem(
                    f[0], f[1], f[2].toIntOrNull() ?: 0, f.getOrNull(3)?.toLongOrNull() ?: 0L,
                )
            },
        )
    }

    // ---- minimal JSON value model + parser (objects, arrays,
    // strings, numbers, booleans, null — enough for our own files) ----

    sealed interface JsonValue {
        data class JsonObject(val values: Map<String, JsonValue>) : JsonValue
        data class JsonArray(val items: List<JsonValue>) : JsonValue
        data class JsonString(val value: String) : JsonValue
        data class JsonNumber(val value: String) : JsonValue
        data class JsonBoolean(val value: Boolean) : JsonValue
        object JsonNull : JsonValue
    }

    class JsonParser(private val text: String) {
        private var pos = 0

        fun parseValue(): JsonValue? {
            skipWhitespace()
            if (pos >= text.length) return null
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.JsonString(parseString() ?: return null)
                't', 'f' -> parseBoolean()
                'n' -> {
                    if (text.startsWith("null", pos)) {
                        pos += 4
                        JsonValue.JsonNull
                    } else null
                }
                else -> parseNumber()
            }
        }

        private fun parseObject(): JsonValue.JsonObject? {
            pos++ // {
            val map = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (pos < text.length && text[pos] == '}') {
                pos++
                return JsonValue.JsonObject(map)
            }
            while (pos < text.length) {
                skipWhitespace()
                val key = parseString() ?: return null
                skipWhitespace()
                if (pos >= text.length || text[pos] != ':') return null
                pos++
                val value = parseValue() ?: return null
                map[key] = value
                skipWhitespace()
                when {
                    pos < text.length && text[pos] == ',' -> pos++
                    pos < text.length && text[pos] == '}' -> {
                        pos++
                        return JsonValue.JsonObject(map)
                    }
                    else -> return null
                }
            }
            return null
        }

        private fun parseArray(): JsonValue.JsonArray? {
            pos++ // [
            val items = ArrayList<JsonValue>()
            skipWhitespace()
            if (pos < text.length && text[pos] == ']') {
                pos++
                return JsonValue.JsonArray(items)
            }
            while (pos < text.length) {
                val value = parseValue() ?: return null
                items.add(value)
                skipWhitespace()
                when {
                    pos < text.length && text[pos] == ',' -> pos++
                    pos < text.length && text[pos] == ']' -> {
                        pos++
                        return JsonValue.JsonArray(items)
                    }
                    else -> return null
                }
            }
            return null
        }

        private fun parseString(): String? {
            if (pos >= text.length || text[pos] != '"') return null
            pos++
            val sb = StringBuilder()
            while (pos < text.length) {
                val c = text[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= text.length) return null
                        when (val esc = text[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > text.length) return null
                                val code = text.substring(pos, pos + 4).toIntOrNull(16) ?: return null
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> return null
                        }
                    }
                    else -> sb.append(c)
                }
            }
            return null
        }

        private fun parseBoolean(): JsonValue.JsonBoolean? {
            return when {
                text.startsWith("true", pos) -> {
                    pos += 4
                    JsonValue.JsonBoolean(true)
                }
                text.startsWith("false", pos) -> {
                    pos += 5
                    JsonValue.JsonBoolean(false)
                }
                else -> null
            }
        }

        private fun parseNumber(): JsonValue.JsonNumber? {
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] in "-+.eE")) pos++
            if (pos == start) return null
            return JsonValue.JsonNumber(text.substring(start, pos))
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }
    }
}
