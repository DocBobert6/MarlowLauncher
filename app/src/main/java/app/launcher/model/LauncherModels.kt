package app.launcher.model

/** Grid sizes (rule 22/23, decision F3): the Pixel set 2×2 / 3×3 /
 *  4×4 / 4×5 / 5×5, default 5×5 — PLUS 5×6 and Fluid kept as
 *  explicit non-default choices, labelled as Marlow extensions,
 *  not Pixel options (`isPixelOption = false`).
 *
 *  One grid record (rule 23): workspace rows/cols, folder grid,
 *  drawer columns and the derived icon size all come from here —
 *  folder grid is 4 cols × 3 rows per page (rule 39), drawer
 *  columns equal workspace columns, icon size derives from the
 *  option (the icon-size slider in settings is a user override of
 *  this derived value). Cell size is derived from the record, never
 *  hard-coded; horizontal margin is capped at 14% of screen width
 *  ([MAX_HORIZONTAL_MARGIN_PERCENT], rule 23). */
enum class GridSize(val cols: Int, val rows: Int, val label: String, val isPixelOption: Boolean = true) {
    GRID_2X2(2, 2, "2 × 2"),
    GRID_3X3(3, 3, "3 × 3"),
    GRID_4X4(4, 4, "4 × 4"),
    GRID_4X5(4, 5, "4 × 5"),
    GRID_5X5(5, 5, "5 × 5"),
    GRID_5X6(5, 6, "5 × 6 (Marlow)", isPixelOption = false),
    FLUID(6, 6, "Fluid (Marlow)", isPixelOption = false);

    /** #1 FLUID derives its columns from the screen width (one
     *  column per 64 dp, clamped 4–8); the fixed sizes use their
     *  declared columns. Rows stay as declared. The grid composable
     *  and the ViewModel placement math both use this, so rendering
     *  and placement always agree. */
    fun effectiveCols(screenWidthDp: Int): Int =
        if (this == FLUID) (screenWidthDp / 64).coerceIn(4, 8) else cols

    /** Rule 23/39: the folder grid derives from the grid record —
     *  4 cols × 3 rows per page, independent of the workspace grid. */
    val folderCols: Int get() = 4
    val folderRows: Int get() = 3

    /** Rule 23: drawer columns equal workspace columns. */
    val drawerColumns: Int get() = cols

    /** Rule 23/24: the icon size derived from the grid option —
     *  smaller grids get larger icons, so one apparent size holds
     *  per grid. The settings icon-size slider overrides this. */
    val derivedIconSizeDp: Int
        get() = when (this) {
            GRID_2X2 -> 72
            GRID_3X3 -> 64
            GRID_4X4 -> 60
            GRID_4X5 -> 56
            GRID_5X5 -> 56
            GRID_5X6 -> 52
            FLUID -> 56
        }
}

/** Rule 23: horizontal margin is capped at 14% of screen width
 *  (AOSP `DeviceProfile.MAX_HORIZONTAL_PADDING_PERCENT`). */
const val MAX_HORIZONTAL_MARGIN_PERCENT = 0.14f

/** #67 page-indicator style, including "none". */
enum class PageIndicatorStyle(val label: String) {
    DOTS("Dots"),
    NUMBERS("Numbers"),
    LINES("Lines"),
    NONE("None");
}

/** #67 indicator colour choices. */
enum class IndicatorColour(val label: String) {
    ACCENT("Accent"),
    WHITE("White"),
    BLACK("Black");
}

/** #11 drawer layout: vertical grid (default), list, or pages. */
enum class DrawerStyle(val label: String) {
    GRID("Grid"),
    LIST("List"),
    PAGES("Pages");
}

/** #83 drawer sort orders. */
enum class DrawerSort(val label: String) {
    NAME("Name"),
    INSTALL_DATE("Install date"),
    MOST_USED("Most used"),
    SIZE("Size"),
    LAST_USED("Last used"),
    // #13 a manual order, set from the long-press sheet (move
    // up / down in drawer). Unordered apps fall back to name order.
    CUSTOM("Custom");
}

/** #82 drawer search-bar placement. */
enum class SearchBarPlacement(val label: String) {
    TOP("Top"),
    BOTTOM("Bottom");
}

/** #22 icon shape. */
enum class IconShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED("Rounded square"),
    SQUARE("Square");
}

/** #12 auto-categories for the drawer / App Library. */
enum class AppCategory(val label: String) {
    ALL("All"),
    COMMUNICATION("Communication"),
    GAMES("Games"),
    MEDIA("Media"),
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    FINANCE("Finance"),
    SHOPPING("Shopping"),
    TOOLS("Tools"),
    OTHER("Other");
}

/** One installed app, as listed by LauncherApps. */
data class AppEntry(
    val component: String, // flattened ComponentName: package/class
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val installTime: Long = 0L,
    val sizeBytes: Long = 0L,
    val launchCount: Int = 0,
    val lastUsed: Long = 0L,
) {
    val displayLabel: String get() = customLabel ?: label
}

/** What kind of thing a home/dock item is. WIDGET / BUILTIN_WIDGET /
 *  WIDGET_STACK are Phase 3 (#6, #7, #113). Widgets are never placed
 *  in the dock (#69 removed) — the dock renders them as nothing. */
enum class HomeItemKind {
    APP, FOLDER, SHORTCUT, CONTACT, APP_PAIR, STICKER, WIDGET, BUILTIN_WIDGET, WIDGET_STACK
}

/** Phase 3 page codes for HomeItem.page. Home pages are >= 0, the
 *  dock is -1 (Phase 1). Widgets also live on the swipe-left widget
 *  page (-4, #122), the hidden Desktop widget page (-3, #122), and
 *  inside a stack (-2, #7 — members are not rendered directly). */
const val PAGE_IN_STACK = -2
const val PAGE_DESKTOP_WIDGETS = -3
const val PAGE_WIDGET_PAGE = -4

/** #113 first-party widget types in the built-in suite. */
enum class BuiltinWidgetType(val id: String, val label: String) {
    NOTES("notes", "Notes"),
    TASKS("tasks", "Tasks"),
    TIMERS("timers", "Timers"),
    HABITS("habits", "Habits"),
    CALENDAR("calendar", "Calendar"),
    WEATHER("weather", "Weather"),
    SYSTEM("system", "System monitor"),
    // Audit fix (2026-11-26): the home clock — big live time +
    // date, seeded full-width at the top of page 0. One row high
    // (see HomeViewModel.seedHome / addBuiltinWidget).
    CLOCK("clock", "Clock");
}

/** #119 one note, shown by the notes widget. */
data class NoteItem(
    val id: String,
    val text: String,
    val createdAt: Long = 0L,
)

/** #118 one task, shown by the tasks widget. */
data class TaskItem(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val createdAt: Long = 0L,
)

/** #118 one habit, shown by the habits widget. doneToday resets when
 *  the day changes (see HomeViewModel.loadHabits); streak counts
 *  consecutive done days. */
data class HabitItem(
    val id: String,
    val name: String,
    val streak: Int = 0,
    val doneToday: Boolean = false,
    val lastDoneDay: Long = 0L, // epoch day (millis / 86400000)
)

/** #118 one timer, shown by the timers widget. endsAtMillis == 0
 *  means not running; remaining is derived from it while running. */
data class TimerItem(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val endsAtMillis: Long = 0L,
)

/** #7 a widget stack (Smart Stack): an ordered list of placed-widget
 *  HomeItem ids (members have page == PAGE_IN_STACK). currentIndex is
 *  which member is shown; auto-rotate is the global setting
 *  (#7, stackAutoRotate), not per-stack. */
data class WidgetStack(
    val id: String,
    val memberIds: List<String>,
    val currentIndex: Int = 0,
)

/** #38 one user-added RSS feed source. */
data class FeedSource(
    val id: String,
    val url: String,
    val title: String,
)

/** #38 one fetched feed entry (RSS item or Atom entry). */
data class FeedItem(
    val id: String,
    val sourceId: String,
    val sourceTitle: String,
    val title: String,
    val link: String,
    val publishedMillis: Long = 0L,
    val summary: String? = null,
)

/** #120 what the auto-hiding media player shows: the active session's
 *  metadata + playback state, from the MediaSession APIs. The
 *  launcher plays nothing itself. */
data class MediaNowPlaying(
    val title: String,
    val artist: String,
    val packageName: String,
    val playing: Boolean,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/** #58 a home-screen sticker / free text. Stickers are HomeItems of
 *  kind STICKER; the label holds the text. The sticker renders in
 *  the body-large type role (rule 9 — no hard-coded size). */

/** #23 theme mode at release: Auto, Light, Dark (#177 per-part dark
 *  mode was removed — one global mode only). */
enum class ThemeMode(val label: String) {
    AUTO("Auto"),
    LIGHT("Light"),
    DARK("Dark");
}

/** #23 the two release themes: system Material You (dynamic colour)
 *  and the launcher's curated stock theme. Custom themes are way
 *  later — there is no theme maker here. */
enum class LauncherThemeChoice(val label: String) {
    MATERIAL_YOU("Material You"),
    STOCK("Stock"),
    // #50 Ambient — the accent comes from the wallpaper (Settings
    // only; there is no first-run theme step — home first,
    // decision F2). Falls back to the stock warm palette when the
    // wallpaper yields no usable colour.
    AMBIENT("Ambient");
}

/** #51 surface styles — ONLY Opaque and our custom blur glass.
 *  No Liquid Glass, no Frosted as a separate style. Blur glass is
 *  translucent surfaces plus true backdrop blur: when a wallpaper
 *  is visible, MainActivity blurs the system wallpaper behind the
 *  window (Window.setBackgroundBlurRadius, API 31+). */
enum class SurfaceStyle(val label: String) {
    OPAQUE("Opaque"),
    BLUR_GLASS("Blur glass");
}

/** #23 accent picker: a small curated set plus the Phase 0 default.
 *  Each entry is a SEED, not a primary colour: the light value seeds
 *  the light scheme, the dark value seeds the dark scheme, and the
 *  HCT pipeline regenerates the whole scheme from it (rule 4/5).
 *  BLUE is the P22 CRT phosphor blue (ZnS:Ag, x 0.15, y 0.06),
 *  lifted in lightness so it reads as glowing — DECIDED 2026-11-26:
 *  #3333CC light, #5555FF dark. */
enum class AccentColour(val label: String, val lightArgb: Long, val darkArgb: Long) {
    BLUE("Blue", 0xFF3333CC, 0xFF5555FF),
    INDIGO("Indigo", 0xFF3F51B5, 0xFF9FA8DA),
    VIOLET("Violet", 0xFF7C4DFF, 0xFFB388FF),
    GREEN("Green", 0xFF1E8E3E, 0xFF81C995),
    ORANGE("Orange", 0xFFE8710A, 0xFFF9AB00),
    RED("Red", 0xFFD93025, 0xFFF28B82);
}

/** #23 font choice for the launcher theme (system / serif / mono).
 *  No downloadable fonts — these are the platform families only. */
enum class FontChoice(val label: String) {
    SYSTEM("System"),
    SERIF("Serif"),
    MONOSPACE("Monospace");
}

/** #10 At a Glance slots. DATE is always available (system clock);
 *  CALENDAR reads the system calendar; WEATHER / COMMUTE / FINANCE /
 *  SPORTS have no provider chosen yet and are placeholder slots. */
enum class GlanceSlot(val label: String) {
    DATE("Date"),
    WEATHER("Weather"),
    CALENDAR("Calendar"),
    COMMUTE("Commute"),
    FINANCE("Finance"),
    SPORTS("Sports");
}

/** One At a Glance line: the slot it came from plus display text.
 *  Placeholder slots never produce items — no fake data. */
data class GlanceItem(
    val slot: GlanceSlot,
    val title: String,
    val subtitle: String? = null,
)

/** One calendar event, as read from the system calendar (#10, #37). */
data class CalendarEvent(
    val title: String,
    val startMillis: Long,
    val location: String? = null,
    val allDay: Boolean = false,
)

/** #104 a search engine / provider. queryUrl and suggestionUrl hold
 *  a "%s" where the (URL-encoded) query goes; suggestionUrl may be
 *  blank (no suggestions for that provider). */
data class SearchProvider(
    val id: String,
    val name: String,
    val queryUrl: String,
    val suggestionUrl: String = "",
)

/** #18 what kind of universal-search result a row is. */
enum class SearchResultKind {
    CALCULATOR, TIMER, DIAL, APP, CONTACT, SETTING, FILE, WEB, SUGGESTION
}

/** One universal-search result row. payload meaning depends on kind:
 *  APP = flattened ComponentName, CONTACT/FILE = content Uri string,
 *  WEB/SUGGESTION = full search URL, DIAL = phone number,
 *  TIMER = seconds as a string, CALCULATOR/SETTING = unused. */
data class SearchResult(
    val kind: SearchResultKind,
    val title: String,
    val subtitle: String? = null,
    val payload: String = "",
)

/** #18 one launcher setting, searchable from universal search. */
data class SettingSearchEntry(val label: String, val hint: String)

/** The dock holds exactly five icons (rules 25/26, decision F5):
 *  one page, labels off, seeded by kind — Phone / Messages /
 *  browser / mail / Camera, in that order. Adding a sixth icon is
 *  refused, never silently dropped (see HomeViewModel). */
const val DOCK_MAX_ITEMS = 5

/**
 * One placed item on a home page or in the dock.
 * Position is in sub-grid half-cells (#62): subX/subY are half-cell
 * offsets, spanX/spanY are half-cell spans (2 = one full cell).
 * page >= 0 is a home page index; page == -1 means the dock, which
 * holds exactly [DOCK_MAX_ITEMS] icons on its single page (the old
 * dockPage field — up to 5 dock pages, #68 — is gone with the
 * rebuild; records written before it carry a retired slot at index
 * 3, skipped on load — see LauncherStore / BackupManager).
 */
data class HomeItem(
    val id: String,
    val kind: HomeItemKind,
    val page: Int,
    val subX: Int = 0,
    val subY: Int = 0,
    val spanX: Int = 2,
    val spanY: Int = 2,
    val component: String? = null, // APP: flattened ComponentName
    val folderId: String? = null,
    val shortcutId: String? = null,
    val shortcutPackage: String? = null,
    val label: String? = null, // shortcut / pair display label (#94, #96)
    val contactUri: String? = null,
    val contactName: String? = null,
    val pairId: String? = null,
    // ---- Phase 3: widgets (#6, #7, #113) ----
    // WIDGET: AppWidgetHost id + provider component (flattened).
    val appWidgetId: Int = -1,
    val providerComponent: String? = null,
    // BUILTIN_WIDGET: BuiltinWidgetType.id (see BuiltinWidgetType).
    val builtinWidgetId: String? = null,
    // WIDGET / BUILTIN_WIDGET inside a stack: the stack's id; the
    // stack record itself lives in WidgetStack (memberIds).
    val stackId: String? = null,
)

/** #95 a pinned contact held inside a home folder: the contact's
 *  lookup URI plus the display name shown in the folder. */
data class FolderContact(
    val uri: String,
    val name: String,
)

/** #14 a standard folder on the home screen. Contacts ride in
 *  [contacts] (default empty, so records written before contacts
 *  existed still load); [apps] stays component names only. */
data class HomeFolder(
    val id: String,
    val name: String,
    val apps: List<String>, // flattened ComponentNames, in order
    val contacts: List<FolderContact> = emptyList(),
)

/** #13 a folder inside the drawer. */
data class DrawerFolder(
    val id: String,
    val name: String,
    val apps: List<String>,
)

/** #96 an app pair: two apps launched with one tap. Split-screen
 *  itself is the system's call (see HomeViewModel.launchAppPair):
 *  if the system declines to split them, both apps just open
 *  normally — the pair never claims a split it cannot force. */
data class AppPair(
    val id: String,
    val name: String,
    val first: String, // flattened ComponentName
    val second: String,
)

/** #95 a pinned contact. */
data class PinnedContact(
    val id: String,
    val name: String,
    val lookupUri: String,
)

/** One home page (#3). */
data class HomePage(
    val id: String,
    val hidden: Boolean = false,
)

/** A third-party icon pack (#19). */
data class IconPack(
    val packageName: String,
    val label: String,
)

/** One app shortcut (#30). */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val shortLabel: String,
    val longLabel: String?,
)

/**
 * All Phase 1 launcher settings, persisted in DataStore.
 * Toggle rule: extra / Pro-style features are OFF by default.
 */
data class LauncherSettings(
    // #1 grid
    val gridSize: GridSize = GridSize.GRID_5X5,
    // #62 / #64 sub-grid + margins. Sub-grid positioning itself is always
    // on (it is the positioning model); the margins are adjustable.
    val gridMarginDp: Int = 8,
    val gridSpacingDp: Int = 8,
    // #66 infinite scroll — extra behaviour, OFF by default.
    val infiniteScroll: Boolean = false,
    // #67 indicators
    val indicatorStyle: PageIndicatorStyle = PageIndicatorStyle.DOTS,
    val indicatorColour: IndicatorColour = IndicatorColour.ACCENT,
    // #75 default home page (index into the visible pages)
    val defaultPage: Int = 0,
    // #4 / #68 / #71 dock — exactly five icons, labels off, one
    // page (decision F5). No dock-page indicators: one page has
    // none. The old dockIndicatorStyle setting is gone with the
    // rebuild; the home-page indicatorStyle above stays.
    val dockVisible: Boolean = true,
    val dockHeightDp: Int = 72,
    val dockBackground: Boolean = true,
    // #112 fixed bottom search pill in the dock — ON by default (Pixel parity).
    val searchPillVisible: Boolean = true,
    // #11 / #82 / #83 drawer
    val drawerStyle: DrawerStyle = DrawerStyle.GRID,
    val drawerSort: DrawerSort = DrawerSort.NAME,
    val searchBarPlacement: SearchBarPlacement = SearchBarPlacement.TOP,
    val pullToSearch: Boolean = true,
    // #80 frequently/recently used row — extra, OFF by default.
    val frequentRowVisible: Boolean = false,
    // #81 remember drawer position / auto-close — both ON (plain behaviour).
    val rememberDrawerPosition: Boolean = true,
    val drawerAutoClose: Boolean = true,
    // #84 two-column drawer on wide screens — ON (layout, not an extra).
    val twoColumnDrawer: Boolean = true,
    // Rule 33: swipe up opens the drawer with the search field
    // focused and the keyboard up — OFF by default (extra gesture).
    val swipeUpToSearch: Boolean = false,
    // Rule 32 drawer extensions — opt-in, OFF by default, labelled
    // as Marlow extensions, not stock behaviour: category tabs and
    // drawer folders. The extension code lives in Drawer.kt.
    val drawerShowCategories: Boolean = false,
    val drawerShowDrawerFolders: Boolean = false,
    // #22 icons
    val iconShape: IconShape = IconShape.ROUNDED,
    val iconSizeDp: Int = 56,
    val showLabels: Boolean = true,
    // #20 themed / Material You icons — basic implementation (single-colour
    // silhouette in the accent), OFF by default: tinting every icon one
    // colour hurts recognition, and the full theme system is Phase 2.
    val themedIcons: Boolean = false,
    // #100 Smartsize / icon normalisation — ON (uniform look).
    val smartSizeIcons: Boolean = true,
    // #101 animated / live clock icon — ON by default (audit fix
    // 2026-11-26): the home clock reads live, and a frozen clock
    // icon on a launcher whose clock widget ticks every second
    // would look broken.
    val liveClockIcon: Boolean = true,
    // #19 active third-party icon pack (package name, "" = none/system).
    val iconPack: String = "",
    // #5 one-handed layout — extra, OFF by default.
    val oneHanded: Boolean = false,
    // #65 desktop lock — OFF by default (an unlocked home is the default).
    val desktopLocked: Boolean = false,
    // #172 auto-add new apps — OFF by default (decided).
    val autoAddApps: Boolean = false,
    // #173 reorderable long-press menu: ordered menu-item ids.
    val longPressMenuOrder: List<String> = listOf(
        "shortcuts", "app_info", "screen_time", "rename", "edit_icon",
        "add_to_home", "add_to_folder", "pair_with", "uninstall",
    ),
    // #173 overview-menu customisation: which overview actions are shown.
    val overviewActions: List<String> = listOf(
        "set_default", "hide_page", "move_left", "move_right", "add_page", "delete_page",
    ),
    // ---- Phase 2 ----
    // #23 theme: mode (Auto/Light/Dark), the two release themes, accent
    // picker, font. Defaults: Auto + Stock + blue accent.
    // Robert's decision (2026-11-26): Stock is the default theme;
    // Material You is one tap away in Settings and the wizard. The
    // research (rule 3) recommended dynamic colour on by default,
    // but Robert's explicit default stands — the HCT Stock scheme
    // built from the P22 seed is what a fresh install shows. The
    // accent picker is a seed choice for Stock only — under
    // Material You the wallpaper is the seed.
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val launcherTheme: LauncherThemeChoice = LauncherThemeChoice.STOCK,
    val accent: AccentColour = AccentColour.BLUE,
    val fontChoice: FontChoice = FontChoice.SYSTEM,
    // #51 surface style — Opaque by default; blur glass is the extra.
    val surfaceStyle: SurfaceStyle = SurfaceStyle.OPAQUE,
    // #24 surface transparency for blur glass (0.3–0.9; 0.55 is the
    // original fixed value). Opaque ignores it.
    val surfaceAlpha: Float = 0.55f,
    // #24 AMOLED black mode — extra, OFF by default.
    val amoledBlack: Boolean = false,
    // #10 At a Glance strip — ON by default (Pixel parity), toggleable.
    val glanceVisible: Boolean = true,
    // #10 slots. Calendar is real (system calendar) and ON; weather
    // #10 slots. Calendar is real (system calendar) and ON; weather
    // is real too (Open-Meteo, user-chosen city) and ON by default
    // since the audit fix (2026-11-26) — a fresh home shows the
    // weather slot, and with no city set yet the strip offers to
    // add one instead of showing nothing. Commute / finance /
    // sports still have no provider, so they stay OFF and show
    // nothing until one is chosen. Their settings rows are hidden
    // until a provider exists (see SettingsScreen) — the fields stay
    // so the At a Glance slot machinery keeps working.
    val glanceCalendar: Boolean = true,
    val glanceWeather: Boolean = true,
    val glanceCommute: Boolean = false,
    val glanceFinance: Boolean = false,
    val glanceSports: Boolean = false,
    // #10 weather city (Open-Meteo, DECIDED 2026-11-26): the user types
    // a city; geocoding resolves it to coordinates. Empty name = no
    // location set yet, so the weather slot and widget say so.
    val weatherCityName: String = "",
    val weatherCityLat: Double = 0.0,
    val weatherCityLon: Double = 0.0,
    // #121 Smartspacer integration — optional, OFF by default.
    val smartspacerEnabled: Boolean = false,
    // #37 Now Brief morning summary — extra, OFF by default.
    val nowBriefEnabled: Boolean = false,
    // #37 morning delivery: the alarm time (07:00 default). The
    // notification opens the launcher; the brief itself is one tap
    // away on the At a Glance strip.
    val nowBriefHour: Int = 7,
    val nowBriefMinute: Int = 0,
    // #104 search: default provider + suggestions toggle.
    val searchProviderId: String = "duckduckgo",
    val searchSuggestions: Boolean = true,
    // #57 hide status bar / ultra-immersive — OFF by default.
    val hideStatusBar: Boolean = false,
    // #53 / #77 wallpaper: the picker sets wallpaperVisible when the
    // user picks a local image; scrolling + gradient/dim are extras,
    // OFF by default (dim 0 = no dimming).
    val wallpaperVisible: Boolean = false,
    val scrollingWallpaper: Boolean = false,
    val wallpaperGradient: Boolean = false,
    val wallpaperDimPercent: Int = 0,
    // #61 contextual music apps on headphone connect — OFF by default.
    val headphoneMusicEnabled: Boolean = false,
    // ---- Phase 3 ----
    // #122 dedicated widget page (swipe-left) — ON by default: it is
    // the phase's main widget surface, not a Pro extra.
    val widgetPageEnabled: Boolean = true,
    // #122 hidden Desktop widget page — OFF by default (extra surface;
    // reached from the widget page, never in the swipe sequence).
    val desktopWidgetPageEnabled: Boolean = false,
    // #63 widget overlap — toggleable, OFF by default (decided).
    // When off, a dragged item that would overlap another snaps back;
    // when on, items may overlap, still on the grid.
    val widgetOverlapEnabled: Boolean = false,
    // #7 Smart Stack auto-rotate — toggleable, OFF by default.
    val stackAutoRotate: Boolean = false,
    // #120 auto-hiding media player — ON by default: it only ever
    // appears while something is playing, so it adds no clutter.
    val mediaPlayerEnabled: Boolean = true,
    // #38 RSS / Discover feed page (swipe-right) — ON by default.
    val feedEnabled: Boolean = true,
    // #38 Google feed bridge — optional, OFF by default. It uses
    // unofficial Google-app internals and may break; when it is
    // unavailable the feed degrades gracefully to RSS-only.
    val googleFeedBridgeEnabled: Boolean = false,
    // ---- Phase 4 ----
    // #26 standard gestures are ON by default (swipe up for the
    // drawer is core Phase 1 behaviour). Double-tap to lock defaults
    // to LOCK_DEVICE (#26); triple-tap is mappable and OFF by default
    // (#91 — NONE). Two-finger swipe / double-tap (#90) default to
    // notifications / overview; pinch-in defaults to the overview
    // (#88, Phase 1 #76) and pinch-out to the drawer.
    val gesturesEnabled: Boolean = true,
    // Rule 59 haptics — OFF by default. The haptic map (ui/Haptics.kt)
    // only fires when this is on; Marlow never overrides the system
    // haptics setting and never forces haptics on. An extra, like
    // every other non-stock toggle. There was no haptics setting
    // before this wave — the map was built with the setting, not
    // behind a pre-existing one.
    val hapticsEnabled: Boolean = false,
    val doubleTapAction: GestureAction = GestureAction.LOCK_DEVICE,
    val tripleTapAction: GestureAction = GestureAction.NONE,
    val twoFingerSwipeAction: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
    val twoFingerDoubleTapAction: GestureAction = GestureAction.OPEN_OVERVIEW,
    val swipeDownAction: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
    val pinchInAction: GestureAction = GestureAction.OPEN_OVERVIEW,
    val pinchOutAction: GestureAction = GestureAction.OPEN_DRAWER,
    // #31 / #129 badges — ON by default (Pixel parity). Rules
    // 49/50, decision F9: dot ONLY — a 6dp dot, top-trailing, one
    // colour, one shape. NO numbers on icons, NO 18dp badge, NO
    // error-red. The numeric count lives only in the long-press
    // menu (see HomeViewModel.badgeCountFor); the dot clears when
    // the app is opened (HomeViewModel records the open time).
    // badgeStyle / badgePosition / badgeSizeDp / badgeCornerRadiusDp
    // / dynamicBadges below are KEPT (LauncherStore and
    // BackupManager still read/write them) but the badge UI ignores
    // them — they no longer drive anything on screen.
    val badgesEnabled: Boolean = true,
    val badgeStyle: BadgeStyle = BadgeStyle.DOT,
    val badgePosition: BadgePosition = BadgePosition.TOP_END,
    val badgeSizeDp: Int = 6,
    val badgeCornerRadiusDp: Int = 3,
    val dynamicBadges: Boolean = false,
    // #31 per-app badge off switch: package names whose dot is off.
    // Persisted in LauncherStore (KEY_BADGE_OFF_PACKAGES) and
    // carried in backups.
    val badgeOffPackages: Set<String> = emptySet(),
    // #127 inline previews + direct reply on the home screen —
    // ON by default: it is also what swipe-down (#26) opens, so
    // leaving it off would make the standard swipe-down gesture
    // dead out of the box. It only ever shows notifications the
    // user already sees in the shade, and it can be turned off.
    // #132 preview in the long-press popup is ON by default.
    val inlinePreviewsEnabled: Boolean = true,
    val longPressPreviewEnabled: Boolean = true,
    // #154 status / navigation bar control — system default; #57
    // hide-status-bar still applies on top of it (see MainActivity).
    val barVisibility: BarVisibility = BarVisibility.SYSTEM_DEFAULT,
    // #151 landscape home — ON (layout adaptation, not an extra).
    val landscapeEnabled: Boolean = true,
    // #150 / #152 separate layouts per screen + two-pane tablet —
    // OFF by default and hidden on phones (smallest width < 600dp):
    // they only mean anything on a foldable / tablet, so a phone
    // install must never start with them on. LauncherStore also
    // gates them off at load time on a narrow device, so a stored
    // ON from an earlier install cannot misbehave on a phone.
    val separateScreenLayouts: Boolean = false,
    val twoPaneTablet: Boolean = false,
    // The old #169 first-run wizard is deleted (decision F2):
    // home first, single system default-home prompt. There is no
    // wizardCompleted state any more — nothing to persist, nothing
    // to complete.
)

/** #31 / #129 badge style: a plain dot or a number. */
enum class BadgeStyle(val label: String) {
    DOT("Dot"),
    NUMBER("Number");
}

/** #129 badge position on the icon. Rule 50: the dot is pinned
 *  top-trailing — only the top corners are offered. */
enum class BadgePosition(val label: String) {
    TOP_END("Top right"),
    TOP_START("Top left");
}

/**
 * Phase 4 mappable gesture actions (#26, #89, #90, #91). NONE means
 * the gesture does nothing. LOCK_DEVICE uses the device-admin lock
 * (see DeviceLock.kt); OPEN_NOTIFICATIONS opens the launcher's own
 * inline notification panel (#127) — there is no public API for a
 * third-party app to expand the system notification shade.
 */
enum class GestureAction(val label: String) {
    NONE("None"),
    OPEN_DRAWER("Open drawer"),
    OPEN_NOTIFICATIONS("Notifications"),
    LOCK_DEVICE("Lock device"),
    OPEN_OVERVIEW("Overview"),
    OPEN_SEARCH("Search"),
    OPEN_SETTINGS("Settings"),
    OPEN_VAULT("Hidden apps vault");
}

/** #28 swipe-up secondary action per home shortcut. */
enum class ShortcutSecondaryAction(val label: String) {
    NONE("None"),
    APP_INFO("App info"),
    LONG_PRESS_SHEET("Long-press sheet"),
    FIRST_SHORTCUT("First shortcut");
}

/** #154 status-bar / navigation-bar control. SYSTEM_DEFAULT leaves
 *  both bars as the system shows them (and #57 hide-status-bar still
 *  applies to the status bar); HIDE_BOTH hides both bars
 *  non-immersively (they return on swipe from the edge). */
enum class BarVisibility(val label: String) {
    SYSTEM_DEFAULT("System default"),
    HIDE_NAVIGATION("Hide navigation bar"),
    HIDE_BOTH("Hide both bars");
}

/**
 * #150 / #151 / #152 layout variant, from the window's smallest
 * width + orientation (see AdaptiveLayout.kt). The Pixel 10a's
 * smallest width is about 412dp, so it only ever sees the two PHONE
 * variants — foldable / tablet variants are present but dormant
 * there until a wider device runs the launcher.
 */
enum class LayoutVariant(val label: String) {
    PHONE_PORTRAIT("Phone portrait"),
    PHONE_LANDSCAPE("Phone landscape"),
    FOLDABLE_COVER("Foldable cover"),
    FOLDABLE_INNER("Foldable inner"),
    TABLET_TWO_PANE("Tablet two-pane");
}

/** #155 per-app language override (a BCP 47 tag, e.g. "es"). The
 *  system owns applying another app's locale; the launcher stores
 *  the choice and deep-links to the system per-app language screen
 *  for that app. For the launcher's own package the stored tag is
 *  applied through LocaleManager where the API exists (API 33+). */
data class AppLanguageOverride(
    val packageName: String,
    val languageTag: String,
)

/** Swipe direction, for two-finger swipes (#90). */
enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}
