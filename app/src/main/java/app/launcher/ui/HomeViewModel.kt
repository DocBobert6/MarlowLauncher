package app.launcher.ui

import android.app.Application
import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.launcher.data.AppRepository
import app.launcher.data.BackupData
import app.launcher.data.BackupManager
import app.launcher.data.CalendarGlanceProvider
import app.launcher.data.DeviceLock
import app.launcher.data.FeedRepository
import app.launcher.data.GlanceSlotProvider
import app.launcher.data.GoogleFeedBridge
import app.launcher.data.LauncherStore
import app.launcher.data.MediaSessionBus
import app.launcher.data.NotificationBus
import app.launcher.data.PinCrypto
import app.launcher.data.SearchEngine
import app.launcher.data.WallpaperHelper
import app.launcher.data.WeatherCity
import app.launcher.data.WeatherNow
import app.launcher.data.WidgetHost
import app.launcher.data.fetchCurrent
import app.launcher.data.geocodeCity
import app.launcher.data.glanceProviders
import app.launcher.model.AppCategory
import app.launcher.model.AppEntry
import app.launcher.model.AppLanguageOverride
import app.launcher.model.AppPair
import app.launcher.model.AppShortcut
import app.launcher.model.BuiltinWidgetType
import app.launcher.model.CalendarEvent
import app.launcher.model.DOCK_MAX_ITEMS
import app.launcher.model.DrawerFolder
import app.launcher.model.FeedItem
import app.launcher.model.FeedSource
import app.launcher.model.FolderContact
import app.launcher.model.GlanceItem
import app.launcher.model.GridSize
import app.launcher.model.HabitItem
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.HomePage
import app.launcher.model.IconPack
import app.launcher.model.LauncherSettings
import app.launcher.model.NoteItem
import app.launcher.model.PAGE_IN_STACK
import app.launcher.model.PinnedContact
import app.launcher.model.SearchProvider
import app.launcher.model.SearchResult
import app.launcher.model.ShortcutSecondaryAction
import app.launcher.model.TaskItem
import app.launcher.model.TimerItem
import app.launcher.model.WidgetStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Phase 1 state holder: settings, installed apps, home/dock layout,
 * folders, pairs and pinned contacts, all persisted in DataStore.
 * The UI stays thin; every mutation goes through here and is saved.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repo = AppRepository(context)

    /** The application context, for wallpaper offsets and similar
     *  calls that need a Context outside the ViewModel. */
    fun applicationContext(): Context = context

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItem>>(emptyList())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private val _homeFolders = MutableStateFlow<List<HomeFolder>>(emptyList())
    val homeFolders: StateFlow<List<HomeFolder>> = _homeFolders.asStateFlow()

    private val _homePages = MutableStateFlow<List<HomePage>>(listOf(HomePage("page-0")))
    val homePages: StateFlow<List<HomePage>> = _homePages.asStateFlow()

    private val _drawerFolders = MutableStateFlow<List<DrawerFolder>>(emptyList())
    val drawerFolders: StateFlow<List<DrawerFolder>> = _drawerFolders.asStateFlow()

    private val _appPairs = MutableStateFlow<List<AppPair>>(emptyList())
    val appPairs: StateFlow<List<AppPair>> = _appPairs.asStateFlow()

    private val _pinnedContacts = MutableStateFlow<List<PinnedContact>>(emptyList())
    val pinnedContacts: StateFlow<List<PinnedContact>> = _pinnedContacts.asStateFlow()

    private val _iconPacks = MutableStateFlow<List<IconPack>>(emptyList())
    val iconPacks: StateFlow<List<IconPack>> = _iconPacks.asStateFlow()

    private val _packMapping = MutableStateFlow<Map<String, String>>(emptyMap())
    val packMapping: StateFlow<Map<String, String>> = _packMapping.asStateFlow()

    private val _renames = MutableStateFlow<Map<String, String>>(emptyMap())
    val renames: StateFlow<Map<String, String>> = _renames.asStateFlow()

    private val _iconOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val iconOverrides: StateFlow<Map<String, String>> = _iconOverrides.asStateFlow()

    private val _drawerQuery = MutableStateFlow("")
    val drawerQuery: StateFlow<String> = _drawerQuery.asStateFlow()

    private val _drawerScrollIndex = MutableStateFlow(0)
    val drawerScrollIndex: StateFlow<Int> = _drawerScrollIndex.asStateFlow()

    // #13 the manual drawer order (DrawerSort.CUSTOM), as flattened
    // ComponentNames. Empty until the user reorders once.
    private val _drawerCustomOrder = MutableStateFlow<List<String>>(emptyList())
    val drawerCustomOrder: StateFlow<List<String>> = _drawerCustomOrder.asStateFlow()

    // ---- Phase 2 state ----
    private val _glanceItems = MutableStateFlow<List<GlanceItem>>(emptyList())
    val glanceItems: StateFlow<List<GlanceItem>> = _glanceItems.asStateFlow()

    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()

    private val _glanceNeedsCalendarPermission = MutableStateFlow(false)
    val glanceNeedsCalendarPermission: StateFlow<Boolean> = _glanceNeedsCalendarPermission.asStateFlow()

    // #10 / #126 weather (Open-Meteo): the cached current conditions
    // for the user's chosen city, plus geocoding candidates while the
    // user types in Settings → At a Glance. 30-minute cache.
    private val _weatherNow = MutableStateFlow<WeatherNow?>(null)
    val weatherNow: StateFlow<WeatherNow?> = _weatherNow.asStateFlow()
    private val _weatherCities = MutableStateFlow<List<WeatherCity>>(emptyList())
    val weatherCities: StateFlow<List<WeatherCity>> = _weatherCities.asStateFlow()

    // #50 Ambient: bumped whenever a wallpaper is applied, so the
    // theme re-derives its accent from the new wallpaper.
    private val _wallpaperVersion = MutableStateFlow(0)
    val wallpaperVersion: StateFlow<Int> = _wallpaperVersion.asStateFlow()

    // #37 Now Brief morning delivery: whether the app may post the
    // morning notification (POST_NOTIFICATIONS, API 33+).
    private val _notificationsGranted = MutableStateFlow(false)
    val notificationsGranted: StateFlow<Boolean> = _notificationsGranted.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _customProviders = MutableStateFlow<List<SearchProvider>>(emptyList())
    val customProviders: StateFlow<List<SearchProvider>> = _customProviders.asStateFlow()

    private val _searchProviders = MutableStateFlow<List<SearchProvider>>(SearchEngine.defaultProviders())
    val searchProviders: StateFlow<List<SearchProvider>> = _searchProviders.asStateFlow()

    private val _headphoneMusicVisible = MutableStateFlow(false)
    val headphoneMusicVisible: StateFlow<Boolean> = _headphoneMusicVisible.asStateFlow()

    private val _musicApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val musicApps: StateFlow<List<AppEntry>> = _musicApps.asStateFlow()

    // ---- Phase 3 state ----
    // #113 / #118 / #119 built-in widget data (DataStore-backed).
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _habits = MutableStateFlow<List<HabitItem>>(emptyList())
    val habits: StateFlow<List<HabitItem>> = _habits.asStateFlow()

    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers.asStateFlow()

    // #7 widget stacks.
    private val _widgetStacks = MutableStateFlow<List<WidgetStack>>(emptyList())
    val widgetStacks: StateFlow<List<WidgetStack>> = _widgetStacks.asStateFlow()

    // #6 the widget currently in resize edit mode (item id, or null).
    private val _editingWidgetId = MutableStateFlow<String?>(null)
    val editingWidgetId: StateFlow<String?> = _editingWidgetId.asStateFlow()

    // #38 RSS feed: sources + fetched entries + last error.
    private val _feedSources = MutableStateFlow<List<FeedSource>>(emptyList())
    val feedSources: StateFlow<List<FeedSource>> = _feedSources.asStateFlow()

    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems.asStateFlow()

    private val _feedRefreshing = MutableStateFlow(false)
    val feedRefreshing: StateFlow<Boolean> = _feedRefreshing.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    // #120 media player state, fed by the notification-listener bus
    // (see MediaMonitor.kt); the launcher plays nothing itself.
    val nowPlaying = MediaSessionBus.nowPlaying
    val mediaListenerConnected = MediaSessionBus.listenerConnected

    // ---- Phase 4 state ----
    // #31 / #127 / #128 / #132 live notification previews, fed by
    // the same listener (see NotificationMonitor.kt). Empty when the
    // listener is not granted — badges are simply absent then.
    val notificationPreviews = NotificationBus.previews

    // #34 hidden apps (flattened ComponentNames) + the visible list
    // the drawer, search and home actually use.
    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _visibleApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val visibleApps: StateFlow<List<AppEntry>> = _visibleApps.asStateFlow()

    // #34 vault state: whether a PIN exists, whether the vault is
    // currently unlocked, and a hide request waiting on PIN setup.
    private val _hasPin = MutableStateFlow(false)
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked: StateFlow<Boolean> = _vaultUnlocked.asStateFlow()

    private val _pendingHideComponent = MutableStateFlow<String?>(null)
    val pendingHideComponent: StateFlow<String?> = _pendingHideComponent.asStateFlow()

    // #31 badge state (rules 49/50): when an app was last opened —
    // the dot clears on open, and only a notification posted AFTER
    // that open re-badges it (NotificationPreview.postedAt is the
    // comparison; the caller filters the counts).
    private val _clearedBadgeAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val clearedBadgeAt: StateFlow<Map<String, Long>> = _clearedBadgeAt.asStateFlow()

    // Rule 22: grid-change notice — set when a grid migration drops
    // items, consumed by the home once shown.
    private val _gridNotice = MutableStateFlow<String?>(null)
    val gridNotice: StateFlow<String?> = _gridNotice.asStateFlow()

    fun consumeGridNotice() {
        _gridNotice.value = null
    }

    // Rule 41: widget placement refusal — set when a widget or
    // stack does not fit the page (or the wrong footprint for a
    // stack), consumed by the home once shown as a snackbar. A
    // placement that fails must be VISIBLE, never a silent drop.
    private val _widgetAddError = MutableStateFlow<String?>(null)
    val widgetAddError: StateFlow<String?> = _widgetAddError.asStateFlow()

    fun consumeWidgetAddError() {
        _widgetAddError.value = null
    }

    // Decision F5: dock-full refusal — set when an add or move to
    // the dock is refused because the dock already holds
    // DOCK_MAX_ITEMS icons, consumed by the home once shown as a
    // snackbar (or inline in the long-press sheet). A refused add
    // must be VISIBLE, never a silent drop.
    private val dockFullMessage = "Dock is full — remove an icon from the dock first."
    private val _dockAddError = MutableStateFlow<String?>(null)
    val dockAddError: StateFlow<String?> = _dockAddError.asStateFlow()

    fun consumeDockAddError() {
        _dockAddError.value = null
    }

    /** Sets the dock-full refusal for a caller that checked the cap
     *  up front (the overview's "Contact to dock" row). */
    fun refuseDockAdd() {
        _dockAddError.value = dockFullMessage
    }

    /** True when the dock has a free slot for one more icon. */
    fun canAddToDock(): Boolean =
        _homeItems.value.count { it.page == -1 } < DOCK_MAX_ITEMS

    // ---- first-run default-home prompt (decision F2) ----

    /** True while the home should fire the single first-run
     *  default-home prompt. Set once by [maybePromptDefaultHome];
     *  the home launches the intent and calls
     *  [consumeDefaultHomePrompt]. */
    private val _defaultHomePromptPending = MutableStateFlow(false)
    val defaultHomePromptPending: StateFlow<Boolean> = _defaultHomePromptPending.asStateFlow()

    /**
     * Home first (decision F2): on first launch the user lands
     * straight on the home screen, and the ONLY first-run prompt is
     * the single system default-home prompt. This decides once
     * whether that prompt is owed: if the flag is already set, or
     * Marlow is already the default home, nothing is owed. Otherwise
     * the flag is set (so the prompt never repeats, even on a
     * decline) and the pending flow fires for the home to launch.
     */
    fun maybePromptDefaultHome() {
        viewModelScope.launch {
            if (LauncherStore.loadDefaultHomePromptShown(context)) return@launch
            if (isDefaultHome()) {
                LauncherStore.saveDefaultHomePromptShown(context, true)
                return@launch
            }
            LauncherStore.saveDefaultHomePromptShown(context, true)
            _defaultHomePromptPending.value = true
        }
    }

    fun consumeDefaultHomePrompt() {
        _defaultHomePromptPending.value = false
    }

    /** True when Marlow is the system's default home app (rule 66)
     *  — the same resolveActivity check Settings uses. */
    fun isDefaultHome(): Boolean {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolve = context.packageManager.resolveActivity(
                homeIntent, PackageManager.MATCH_DEFAULT_ONLY,
            )
            resolve?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }
    private val _appLanguages = MutableStateFlow<List<AppLanguageOverride>>(emptyList())
    val appLanguages: StateFlow<List<AppLanguageOverride>> = _appLanguages.asStateFlow()

    private val _swipeUpActions = MutableStateFlow<Map<String, ShortcutSecondaryAction>>(emptyMap())
    val swipeUpActions: StateFlow<Map<String, ShortcutSecondaryAction>> = _swipeUpActions.asStateFlow()

    // #28 a long-press sheet the swipe-up secondary action asked for.
    private val _pendingLongPressApp = MutableStateFlow<AppEntry?>(null)
    val pendingLongPressApp: StateFlow<AppEntry?> = _pendingLongPressApp.asStateFlow()

    // #47 last backup export / import result, shown in settings.
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private var pinCredentials: Pair<String, String>? = null

    /** #38 the Google feed bridge status for the current toggle. */
    fun googleFeedStatus(): GoogleFeedBridge.BridgeStatus =
        GoogleFeedBridge.status(context, _settings.value.googleFeedBridgeEnabled)

    private var launchStats: Map<String, Pair<Int, Long>> = emptyMap()
    private var knownPackages: Set<String> = emptySet()
    private val iconDrawableCache = HashMap<String, android.graphics.drawable.Drawable?>()

    init {
        viewModelScope.launch {
            _settings.value = LauncherStore.loadSettings(context)
            // Audit fix (2026-11-26): glance weather is ON by
            // default now, but existing installs stored the old
            // default (off). One-time migration: turn it on once
            // when no city is set yet and the flag is absent, so
            // the strip offers "Add weather for your city" instead
            // of leaving Robert with no weather after he complained
            // about it. A user who deliberately turned weather off
            // pre-fix gets it turned back on once — recorded, and
            // they can turn it off again; the flag stops it
            // repeating. Fresh installs already default ON, so the
            // migration is a no-op for them.
            if (!LauncherStore.loadGlanceWeatherMigrated(context) &&
                _settings.value.weatherCityName.isBlank()
            ) {
                _settings.value = _settings.value.copy(glanceWeather = true)
                LauncherStore.saveSettings(context, _settings.value)
                LauncherStore.saveGlanceWeatherMigrated(context, true)
            }
            // Decision F5: an old multi-page dock can hold more than
            // five icons — normalizeDock caps it at five (extras
            // move to page 0) and the store is rewritten so the
            // load stays consistent.
            val loadedItems = LauncherStore.loadHomeItems(context)
            _homeItems.value = normalizeDock(loadedItems)
            if (_homeItems.value != loadedItems) {
                LauncherStore.saveHomeItems(context, _homeItems.value)
            }
            _homeFolders.value = LauncherStore.loadHomeFolders(context)
            _homePages.value = LauncherStore.loadHomePages(context)
            _drawerFolders.value = LauncherStore.loadDrawerFolders(context)
            _appPairs.value = LauncherStore.loadAppPairs(context)
            _pinnedContacts.value = LauncherStore.loadPinnedContacts(context)
            _renames.value = LauncherStore.loadRenames(context)
            _iconOverrides.value = LauncherStore.loadIconOverrides(context)
            launchStats = LauncherStore.loadLaunchStats(context)
            knownPackages = LauncherStore.loadKnownPackages(context)
            _drawerScrollIndex.value = LauncherStore.loadDrawerScroll(context)
            _drawerCustomOrder.value = LauncherStore.loadDrawerCustomOrder(context)
            _drawerQuery.value = LauncherStore.loadDrawerQuery(context)
            _customProviders.value = LauncherStore.loadCustomProviders(context)
            _searchProviders.value = SearchEngine.defaultProviders() + _customProviders.value
            _notes.value = LauncherStore.loadNotes(context)
            _tasks.value = LauncherStore.loadTasks(context)
            val loadedHabits = LauncherStore.loadHabits(context)
            _habits.value = resetHabitsForToday(loadedHabits)
            _timers.value = LauncherStore.loadTimers(context)
            _widgetStacks.value = LauncherStore.loadWidgetStacks(context)
            _feedSources.value = LauncherStore.loadFeedSources(context)
            // Phase 4: vault, hidden apps, per-app languages, swipe-up
            // secondary actions.
            _hiddenApps.value = LauncherStore.loadHiddenApps(context)
            _appLanguages.value = LauncherStore.loadAppLanguages(context)
            _swipeUpActions.value = LauncherStore.loadSwipeUpActions(context)
            pinCredentials = LauncherStore.loadPinCredentials(context)
            _hasPin.value = pinCredentials != null
            refreshVisibleApps()
            if (_habits.value != loadedHabits) {
                LauncherStore.saveHabits(context, _habits.value)
            }
            refreshApps()
            refreshIconPacks()
            refreshGlance()
            refreshNotificationsGranted()
            scheduleNowBrief()
            if (_feedSources.value.isNotEmpty()) refreshFeed()
        }
    }

    /** #118 habits: a new day clears doneToday (the streak stands). */
    private fun resetHabitsForToday(habits: List<HabitItem>): List<HabitItem> {
        val today = System.currentTimeMillis() / 86_400_000L
        return habits.map {
            if (it.doneToday && it.lastDoneDay != today) it.copy(doneToday = false) else it
        }
    }

    // ---- Phase 2: At a Glance (#10, #121) + Now Brief (#37) ----

    /** Reloads the glance strip and today's events from the current
     *  settings. Runs on IO: the calendar query is a content-provider
     *  read. Placeholder slots contribute nothing (no fake data). */
    fun refreshGlance() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _settings.value
            val calendarProvider = CalendarGlanceProvider { s.glanceCalendar }
            val providers: List<GlanceSlotProvider> = glanceProviders(
                calendarEnabled = { s.glanceCalendar },
                weatherEnabled = { s.glanceWeather },
                commuteEnabled = { s.glanceCommute },
                financeEnabled = { s.glanceFinance },
                sportsEnabled = { s.glanceSports },
                smartspacerEnabled = { s.smartspacerEnabled },
                weatherCitySet = { s.weatherCityName.isNotBlank() },
                weatherNow = { _weatherNow.value },
            )
            val items = ArrayList<GlanceItem>()
            for (provider in providers) {
                // The calendar provider is loaded once below (it also
                // feeds today's events); the list's copy is skipped.
                if (provider is CalendarGlanceProvider) continue
                if (provider.isEnabled()) items.addAll(provider.load(context))
            }
            if (s.glanceCalendar) items.addAll(calendarProvider.load(context))
            _glanceItems.value = items
            _todayEvents.value = if (s.glanceCalendar || s.nowBriefEnabled) {
                calendarProvider.readTodaysEvents(context)
            } else {
                emptyList()
            }
            _glanceNeedsCalendarPermission.value = calendarProvider.needsCalendarPermission
        }
        maybeRefreshWeather()
    }

    // ---- #10 / #126 weather (Open-Meteo, no key, no location) ----

    /** Fetches the current weather for the chosen city, unless the
     *  cache is younger than 30 minutes. A fresh value re-renders
     *  the glance strip; the freshness guard is what stops the
     *  refreshGlance → maybeRefreshWeather → refreshGlance chain
     *  from looping. Failures leave the old value in place (or
     *  nothing, on first run) — never fake weather. */
    fun refreshWeather(force: Boolean = false) {
        val s = _settings.value
        if (s.weatherCityName.isBlank()) return
        val cached = _weatherNow.value
        if (!force && cached != null &&
            System.currentTimeMillis() - cached.fetchedAtMillis < 30L * 60 * 1000
        ) return
        val city = WeatherCity(
            name = s.weatherCityName,
            country = "",
            latitude = s.weatherCityLat,
            longitude = s.weatherCityLon,
        )
        viewModelScope.launch(Dispatchers.IO) {
            val now = fetchCurrent(city)
            if (now != null) {
                _weatherNow.value = now
                refreshGlance()
            }
        }
    }

    private fun maybeRefreshWeather() = refreshWeather(force = false)

    /** Geocodes the typed city name for the Settings picker. */
    fun searchWeatherCities(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _weatherCities.value = geocodeCity(query)
        }
    }

    /** Picks a geocoded city: persists it, clears the picker, fetches. */
    fun setWeatherCity(city: WeatherCity) {
        _weatherCities.value = emptyList()
        updateSettings { s ->
            s.copy(
                weatherCityName = city.displayName,
                weatherCityLat = city.latitude,
                weatherCityLon = city.longitude,
            )
        }
        _weatherNow.value = null
        refreshWeather(force = true)
    }

    /** Clears the weather city; the slot and widget say so plainly. */
    fun clearWeatherCity() {
        updateSettings { s -> s.copy(weatherCityName = "", weatherCityLat = 0.0, weatherCityLon = 0.0) }
        _weatherNow.value = null
        refreshGlance()
    }

    // ---- #37 Now Brief morning delivery (alarm + notification) ----

    /** Whether POST_NOTIFICATIONS is granted (always true below
     *  API 33, where the permission does not exist). */
    fun refreshNotificationsGranted() {
        _notificationsGranted.value = if (Build.VERSION.SDK_INT < 33) {
            true
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /** Called when the notification-permission request returns. */
    fun onNotificationsPermissionResult(granted: Boolean) {
        _notificationsGranted.value = granted
        if (granted) scheduleNowBrief()
    }

    /** (Re)schedules the daily Now Brief alarm — the alarm setup
     *  itself lives in NowBriefReceiver.schedule, shared with the
     *  boot receiver, so a reboot re-schedules it too. The alarm
     *  does not survive a reboot on its own. */
    fun scheduleNowBrief() {
        app.launcher.data.NowBriefReceiver.schedule(context, _settings.value)
    }

    // ---- Phase 2: universal search (#18, #104, #106) ----

    /** Runs one universal-search query; results land in searchResults.
     *  Hidden apps (#34) are excluded from search results. */
    private var lastSearchQuery = ""

    fun runSearch(query: String) {
        lastSearchQuery = query
        viewModelScope.launch {
            val s = _settings.value
            _searchResults.value = SearchEngine.search(
                context = context,
                rawQuery = query,
                apps = _visibleApps.value,
                providers = _searchProviders.value,
                defaultProviderId = s.searchProviderId,
                suggestionsEnabled = s.searchSuggestions,
            )
        }
    }

    fun clearSearch() {
        lastSearchQuery = ""
        _searchResults.value = emptyList()
    }

    /** Re-runs the last search query — used after the contacts
     *  permission is granted, so the rows appear without retyping. */
    fun rerunSearch() {
        if (lastSearchQuery.isNotBlank()) runSearch(lastSearchQuery)
    }

    /** #104 add a custom search engine (name + query URL with %s). */
    fun addCustomSearchProvider(name: String, queryUrl: String, suggestionUrl: String = "") {
        if (name.isBlank() || "%s" !in queryUrl) return
        viewModelScope.launch {
            val provider = SearchProvider(UUID.randomUUID().toString(), name.trim(), queryUrl.trim(), suggestionUrl.trim())
            val next = _customProviders.value + provider
            _customProviders.value = next
            _searchProviders.value = SearchEngine.defaultProviders() + next
            LauncherStore.saveCustomProviders(context, next)
        }
    }

    fun removeCustomSearchProvider(id: String) {
        viewModelScope.launch {
            val next = _customProviders.value.filter { it.id != id }
            _customProviders.value = next
            _searchProviders.value = SearchEngine.defaultProviders() + next
            LauncherStore.saveCustomProviders(context, next)
            if (_settings.value.searchProviderId == id) {
                updateSettings { it.copy(searchProviderId = "duckduckgo") }
            }
        }
    }

    /** Opens a web URL from a search result (browser intent). */
    fun launchUrl(url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No browser can open the URL; nothing to do.
        }
    }

    /** #106 direct-dial from a search result. */
    fun dial(number: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No dialler; nothing to do.
        }
    }

    /** #106 timer from a search result (system clock app). */
    fun setTimer(seconds: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // No clock app handles the timer intent; nothing to do.
        }
    }

    fun openFileUri(uriString: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // Nothing can open the file; nothing to do.
        }
    }

    // ---- Phase 2: wallpaper (#53, #77) ----

    /** Applies a picked image as the system wallpaper and turns the
     *  home's wallpaper layer on. Runs on IO (stream copy). */
    fun applyWallpaper(uri: Uri) {
        viewModelScope.launch {
            val applied = withContext(Dispatchers.IO) { WallpaperHelper.applyWallpaper(context, uri) }
            if (applied) {
                updateSettings { it.copy(wallpaperVisible = true) }
                // #50 a new wallpaper means a new Ambient accent.
                _wallpaperVersion.value += 1
            }
        }
    }

    // ---- Phase 2: stickers (#58) ----

    /** Adds a free-text sticker to a home page (2 half-cells tall so
     *  the text fits; spans 4 half-cells = 2 full cells wide). */
    fun addSticker(text: String, page: Int = 0) {
        if (_settings.value.desktopLocked || text.isBlank()) return
        viewModelScope.launch {
            val items = _homeItems.value.toMutableList()
            val pos = firstFreeCell(items, page) ?: return@launch
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.STICKER,
                    page = page, subX = pos.first, subY = pos.second,
                    spanX = 4, spanY = 2, label = text.trim(),
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    fun updateSticker(itemId: String, text: String) {
        viewModelScope.launch {
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) it.copy(label = text.trim()) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    // ---- Phase 2: headphone music (#61) ----

    /** Headphones connected: if the toggle is on, show the music-app
     *  sheet. Music apps = MEDIA-category apps whose label/package
     *  suggests music or audio playback. */
    fun onHeadphonesConnected() {
        if (!_settings.value.headphoneMusicEnabled) return
        val musicKeywords = listOf("music", "spotify", "audio", "player", "podcast", "tune", "radio", "sound")
        _musicApps.value = _apps.value.filter { app ->
            repo.categorize(app) == AppCategory.MEDIA &&
                musicKeywords.any { it in (app.label + " " + app.packageName).lowercase() }
        }
        _headphoneMusicVisible.value = true
    }

    fun dismissHeadphoneMusic() {
        _headphoneMusicVisible.value = false
    }

    // ---- Phase 3: built-in widget data (#113, #118, #119) ----

    /** #119 add a note. */
    fun addNote(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val next = _notes.value + NoteItem(UUID.randomUUID().toString(), text.trim(), System.currentTimeMillis())
            _notes.value = next
            LauncherStore.saveNotes(context, next)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            val next = _notes.value.filter { it.id != id }
            _notes.value = next
            LauncherStore.saveNotes(context, next)
        }
    }

    /** #118 add / toggle / delete a task. */
    fun addTask(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val next = _tasks.value + TaskItem(UUID.randomUUID().toString(), text.trim(), false, System.currentTimeMillis())
            _tasks.value = next
            LauncherStore.saveTasks(context, next)
        }
    }

    fun toggleTask(id: String) {
        viewModelScope.launch {
            val next = _tasks.value.map { if (it.id == id) it.copy(done = !it.done) else it }
            _tasks.value = next
            LauncherStore.saveTasks(context, next)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            val next = _tasks.value.filter { it.id != id }
            _tasks.value = next
            LauncherStore.saveTasks(context, next)
        }
    }

    /** #118 add / toggle / delete a habit. Toggling done today
     *  moves the streak up (or back down when un-checked); the day
     *  rollover in init clears doneToday for a new day. */
    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val next = _habits.value + HabitItem(UUID.randomUUID().toString(), name.trim())
            _habits.value = next
            LauncherStore.saveHabits(context, next)
        }
    }

    fun toggleHabit(id: String) {
        viewModelScope.launch {
            val today = System.currentTimeMillis() / 86_400_000L
            val next = _habits.value.map {
                if (it.id != id) return@map it
                if (it.doneToday) {
                    it.copy(doneToday = false, streak = (it.streak - 1).coerceAtLeast(0))
                } else {
                    it.copy(doneToday = true, streak = it.streak + 1, lastDoneDay = today)
                }
            }
            _habits.value = next
            LauncherStore.saveHabits(context, next)
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            val next = _habits.value.filter { it.id != id }
            _habits.value = next
            LauncherStore.saveHabits(context, next)
        }
    }

    /** #118 add a timer (label + length in seconds), start/stop it,
     *  delete it. Starting sets endsAtMillis; stopping clears it. */
    fun addTimer(label: String, totalSeconds: Int) {
        if (totalSeconds <= 0) return
        viewModelScope.launch {
            val next = _timers.value + TimerItem(
                UUID.randomUUID().toString(),
                label.ifBlank { "Timer" }.trim(),
                totalSeconds,
            )
            _timers.value = next
            LauncherStore.saveTimers(context, next)
        }
    }

    fun toggleTimer(id: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val next = _timers.value.map {
                if (it.id != id) return@map it
                if (it.endsAtMillis > now) it.copy(endsAtMillis = 0L)
                else it.copy(endsAtMillis = now + it.totalSeconds * 1000L)
            }
            _timers.value = next
            LauncherStore.saveTimers(context, next)
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            val next = _timers.value.filter { it.id != id }
            _timers.value = next
            LauncherStore.saveTimers(context, next)
        }
    }

    // ---- Phase 3: widgets (#6, #7, #113, #122) ----

    /** #1 the effective column count for placement math (see
     *  GridSize.effectiveCols). Screen width, not the pane width,
     *  so it agrees with the grid composable; in two-pane mode the
     *  grid keeps the full-width column count, as before. */
    private fun effectiveCols(): Int =
        _settings.value.gridSize.effectiveCols(
            context.resources.configuration.screenWidthDp,
        )

    /** First free rectangle of the given half-cell span on a page,
     *  or null when the page is full. Positions step by full cells
     *  (2 half-cells) so widgets stay grid-aligned. */
    fun firstFreeRect(items: List<HomeItem>, page: Int, spanX: Int, spanY: Int): Pair<Int, Int>? {
        val grid = _settings.value.gridSize
        val maxX = effectiveCols() * 2
        val maxY = grid.rows * 2
        for (y in 0..maxY - spanY step 2) {
            for (x in 0..maxX - spanX step 2) {
                val overlaps = items.filter { it.page == page }.any { other ->
                    rectsOverlap(x, y, spanX, spanY, other.subX, other.subY, other.spanX, other.spanY)
                }
                if (!overlaps) return Pair(x, y)
            }
        }
        return null
    }

    private fun rectsOverlap(
        x1: Int, y1: Int, w1: Int, h1: Int,
        x2: Int, y2: Int, w2: Int, h2: Int,
    ): Boolean = x1 < x2 + w2 && x2 < x1 + w1 && y1 < y2 + h2 && y2 < y1 + h1

    /** #113 place a built-in suite widget on a page. Default size
     *  4 x 4 half-cells (2 x 2 cells) — except the clock, which is
     *  full-width and one row high, like the first-run seed. */
    fun addBuiltinWidget(type: BuiltinWidgetType, page: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val items = _homeItems.value.toMutableList()
            val spanX = if (type == BuiltinWidgetType.CLOCK) effectiveCols() * 2 else 4
            val spanY = if (type == BuiltinWidgetType.CLOCK) 2 else 4
            val pos = firstFreeRect(items, page, spanX, spanY) ?: run {
                _widgetAddError.value = "No free space on this page — remove something first."
                return@launch
            }
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.BUILTIN_WIDGET,
                    page = page, subX = pos.first, subY = pos.second,
                    spanX = spanX, spanY = spanY, builtinWidgetId = type.id, label = type.label,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** The intent the first-run default-home prompt (and Settings →
     *  About, on user action) uses to ask the system to make Marlow
     *  the default home — RoleManager ROLE_HOME on API 29+, the home
     *  settings screen below that. Null only when neither route
     *  exists; the caller launches it for a result (the result
     *  itself needs no handling — the grant is the system's to
     *  record). */
    fun defaultHomeIntent(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                roleManager?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
            } else {
                Intent(Settings.ACTION_HOME_SETTINGS)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** #6 installed third-party widget providers, for the picker. */
    fun installedWidgetProviders() = WidgetHost.installedProviders(context)

    /** #6 allocates a host id for a provider the user just picked;
     *  the caller binds it (user confirmation flow when needed). */
    fun allocateWidgetId(): Int = WidgetHost.allocateId(context)

    /** #6 tries to bind a host id to a provider; false means the
     *  system wants the user to confirm via ACTION_APPWIDGET_BIND. */
    fun bindWidget(appWidgetId: Int, provider: ComponentName): Boolean =
        WidgetHost.bindWidget(context, appWidgetId, provider)

    /** #6 places a bound third-party widget. Its span comes from the
     *  provider's minimum size through the platform table
     *  ([WidgetHost.spanForMinSizeDp]), or 2 x 1 cells when unknown. */
    fun addThirdPartyWidget(appWidgetId: Int, provider: ComponentName, page: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val info = WidgetHost.providerInfo(context, appWidgetId)
            val grid = _settings.value.gridSize
            val (cellsX, cellsY) = if (info != null) {
                WidgetHost.spanForMinSizeDp(info.minWidth, info.minHeight)
            } else {
                Pair(2, 1)
            }
            val spanX = cellsX.coerceIn(1, effectiveCols()) * 2
            val spanY = cellsY.coerceIn(1, grid.rows) * 2
            val items = _homeItems.value.toMutableList()
            val pos = firstFreeRect(items, page, spanX, spanY) ?: run {
                // Rule 41: a failed placement is visible, and the
                // allocated host id is freed — nothing is placed.
                _widgetAddError.value = "No free space on this page — remove something first."
                WidgetHost.deleteId(context, appWidgetId)
                return@launch
            }
            WidgetHost.updateWidgetSize(context, appWidgetId, spanX, spanY)
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.WIDGET,
                    page = page, subX = pos.first, subY = pos.second,
                    spanX = spanX, spanY = spanY,
                    appWidgetId = appWidgetId,
                    providerComponent = provider.flattenToString(),
                    label = info?.loadLabel(context.packageManager)?.toString(),
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** #6 resize a widget in edit mode. Spans clamp to the grid;
     *  the provider is told the new size. */
    fun resizeWidget(itemId: String, spanX: Int, spanY: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val grid = _settings.value.gridSize
            val clampedX = spanX.coerceIn(2, effectiveCols() * 2)
            val clampedY = spanY.coerceIn(2, grid.rows * 2)
            val item = _homeItems.value.firstOrNull { it.id == itemId } ?: return@launch
            if (item.kind == HomeItemKind.WIDGET && item.appWidgetId >= 0) {
                WidgetHost.updateWidgetSize(context, item.appWidgetId, clampedX, clampedY)
            }
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) it.copy(spanX = clampedX, spanY = clampedY) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
            _editingWidgetId.value = null
        }
    }

    /** #6 anchored resize (rule 42): the same clamping and
     *  provider-size update as [resizeWidget], but the left/top
     *  handles also move the origin — the widget grows toward the
     *  top-left instead of only toward the bottom-right. */
    fun resizeWidgetAt(itemId: String, spanX: Int, spanY: Int, subX: Int, subY: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val grid = _settings.value.gridSize
            val clampedX = spanX.coerceIn(2, effectiveCols() * 2)
            val clampedY = spanY.coerceIn(2, grid.rows * 2)
            val clampedSubX = subX.coerceIn(0, (effectiveCols() * 2 - clampedX).coerceAtLeast(0))
            val clampedSubY = subY.coerceIn(0, (grid.rows * 2 - clampedY).coerceAtLeast(0))
            val item = _homeItems.value.firstOrNull { it.id == itemId } ?: return@launch
            if (item.kind == HomeItemKind.WIDGET && item.appWidgetId >= 0) {
                WidgetHost.updateWidgetSize(context, item.appWidgetId, clampedX, clampedY)
            }
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) {
                    it.copy(spanX = clampedX, spanY = clampedY, subX = clampedSubX, subY = clampedSubY)
                } else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
            _editingWidgetId.value = null
        }
    }

    fun setEditingWidget(itemId: String?) {
        _editingWidgetId.value = itemId
    }

    /** #6 / #7 removes a placed widget (or a whole stack with its
     *  members). Host ids are released so providers can rebind. */
    fun removeWidget(itemId: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val item = _homeItems.value.firstOrNull { it.id == itemId } ?: return@launch
            val removedIds = ArrayList<String>()
            removedIds.add(itemId)
            if (item.kind == HomeItemKind.WIDGET_STACK && item.stackId != null) {
                val stack = _widgetStacks.value.firstOrNull { it.id == item.stackId }
                if (stack != null) removedIds.addAll(stack.memberIds)
                _widgetStacks.value = _widgetStacks.value.filter { it.id != item.stackId }
                LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            }
            if (item.stackId != null && item.kind != HomeItemKind.WIDGET_STACK) {
                // A stack member removed directly: drop it from its stack.
                _widgetStacks.value = _widgetStacks.value.map { stack ->
                    if (stack.id == item.stackId) {
                        stack.copy(
                            memberIds = stack.memberIds.filter { it != itemId },
                            currentIndex = 0,
                        )
                    } else stack
                }
                LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            }
            val removed = _homeItems.value.filter { it.id in removedIds }
            for (gone in removed) {
                if (gone.kind == HomeItemKind.WIDGET && gone.appWidgetId >= 0) {
                    WidgetHost.deleteId(context, gone.appWidgetId)
                }
            }
            _homeItems.value = _homeItems.value.filter { it.id !in removedIds }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    /** #7 creates an empty Smart Stack on a page (2 x 2 cells). */
    fun createStack(page: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val stackId = UUID.randomUUID().toString()
            val items = _homeItems.value.toMutableList()
            val pos = firstFreeRect(items, page, 4, 4) ?: run {
                _widgetAddError.value = "No free space on this page — remove something first."
                return@launch
            }
            val stack = WidgetStack(stackId, emptyList())
            _widgetStacks.value = _widgetStacks.value + stack
            LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.WIDGET_STACK,
                    page = page, subX = pos.first, subY = pos.second,
                    spanX = 4, spanY = 4, stackId = stackId, label = "Smart Stack",
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** #7 moves a placed widget into an existing stack. Rule 44:
     *  stacks hold widgets of the same footprint only — a widget
     *  whose span differs from the stack's is refused visibly, and
     *  the widget stays where it was. */
    fun addWidgetToStack(widgetItemId: String, stackId: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val widget = _homeItems.value.firstOrNull { it.id == widgetItemId } ?: return@launch
            if (widget.kind != HomeItemKind.WIDGET && widget.kind != HomeItemKind.BUILTIN_WIDGET) return@launch
            val stackItem = _homeItems.value.firstOrNull {
                it.kind == HomeItemKind.WIDGET_STACK && it.stackId == stackId
            }
            if (stackItem != null &&
                (widget.spanX != stackItem.spanX || widget.spanY != stackItem.spanY)
            ) {
                _widgetAddError.value = "Stacks hold widgets of the same size only."
                return@launch
            }
            _widgetStacks.value = _widgetStacks.value.map { stack ->
                if (stack.id == stackId && widgetItemId !in stack.memberIds) {
                    stack.copy(memberIds = stack.memberIds + widgetItemId)
                } else stack
            }
            LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            _homeItems.value = _homeItems.value.map {
                if (it.id == widgetItemId) it.copy(page = PAGE_IN_STACK, stackId = stackId) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    /** #7 moves a placed widget into a brand-new stack at its spot. */
    fun addWidgetToNewStack(widgetItemId: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val widget = _homeItems.value.firstOrNull { it.id == widgetItemId } ?: return@launch
            if (widget.kind != HomeItemKind.WIDGET && widget.kind != HomeItemKind.BUILTIN_WIDGET) return@launch
            val stackId = UUID.randomUUID().toString()
            val stack = WidgetStack(stackId, listOf(widgetItemId))
            _widgetStacks.value = _widgetStacks.value + stack
            LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            val items = _homeItems.value
                .filter { it.id != widgetItemId }
                .toMutableList()
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.WIDGET_STACK,
                    page = widget.page, subX = widget.subX, subY = widget.subY,
                    spanX = widget.spanX, spanY = widget.spanY,
                    stackId = stackId, label = "Smart Stack",
                ),
            )
            items.add(widget.copy(page = PAGE_IN_STACK, stackId = stackId))
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** #7 which stack member is shown. */
    fun setStackIndex(stackId: String, index: Int) {
        viewModelScope.launch {
            _widgetStacks.value = _widgetStacks.value.map { stack ->
                if (stack.id == stackId) {
                    stack.copy(currentIndex = index.coerceIn(0, (stack.memberIds.size - 1).coerceAtLeast(0)))
                } else stack
            }
            LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
        }
    }

    /** #7 removes the currently shown member from a stack. One
     *  member left dissolves the stack into that plain widget. */
    fun removeCurrentWidgetFromStack(stackId: String) {
        viewModelScope.launch {
            val stack = _widgetStacks.value.firstOrNull { it.id == stackId } ?: return@launch
            val memberId = stack.memberIds.getOrNull(stack.currentIndex) ?: return@launch
            val remaining = stack.memberIds.filter { it != memberId }
            val stackItem = _homeItems.value.firstOrNull {
                it.kind == HomeItemKind.WIDGET_STACK && it.stackId == stackId
            }
            if (remaining.size <= 1) {
                // Dissolve: the last member takes the stack's place.
                val lastId = remaining.firstOrNull()
                _widgetStacks.value = _widgetStacks.value.filter { it.id != stackId }
                LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
                _homeItems.value = _homeItems.value
                    .filter { it.id != stackItem?.id }
                    .map {
                        if (it.id == lastId && stackItem != null) {
                            it.copy(page = stackItem.page, subX = stackItem.subX, subY = stackItem.subY,
                                spanX = stackItem.spanX, spanY = stackItem.spanY, stackId = null)
                        } else it
                    }
            } else {
                _widgetStacks.value = _widgetStacks.value.map {
                    if (it.id == stackId) it.copy(memberIds = remaining, currentIndex = 0) else it
                }
                LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            }
            // The removed member itself goes in both branches.
            val gone = _homeItems.value.firstOrNull { it.id == memberId }
            if (gone?.kind == HomeItemKind.WIDGET && gone.appWidgetId >= 0) {
                WidgetHost.deleteId(context, gone.appWidgetId)
            }
            _homeItems.value = _homeItems.value.filter { it.id != memberId }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    /** #7 dissolves a stack: every member returns to the stack's
     *  page as a plain widget (first free spots). */
    fun dissolveStack(stackId: String) {
        if (stackId.isBlank()) return
        viewModelScope.launch {
            val stack = _widgetStacks.value.firstOrNull { it.id == stackId } ?: return@launch
            val stackItem = _homeItems.value.firstOrNull {
                it.kind == HomeItemKind.WIDGET_STACK && it.stackId == stackId
            }
            _widgetStacks.value = _widgetStacks.value.filter { it.id != stackId }
            LauncherStore.saveWidgetStacks(context, _widgetStacks.value)
            val page = stackItem?.page ?: 0
            var items = _homeItems.value.filter { it.id != stackItem?.id }
            for (memberId in stack.memberIds) {
                val member = items.firstOrNull { it.id == memberId } ?: continue
                val pos = firstFreeRect(items, page, member.spanX, member.spanY)
                    ?: Pair(member.subX, member.subY)
                items = items.map {
                    if (it.id == memberId) {
                        it.copy(page = page, subX = pos.first, subY = pos.second, stackId = null)
                    } else it
                }
            }
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** #122 moves a placed widget (or stack) to another page —
     *  home page 0 or the widget page — at the first free spot. */
    fun moveWidgetToPage(itemId: String, page: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val item = _homeItems.value.firstOrNull { it.id == itemId } ?: return@launch
            if (item.kind != HomeItemKind.WIDGET &&
                item.kind != HomeItemKind.BUILTIN_WIDGET &&
                item.kind != HomeItemKind.WIDGET_STACK
            ) return@launch
            val others = _homeItems.value.filter { it.id != itemId }
            val pos = firstFreeRect(others, page, item.spanX, item.spanY)
                ?: Pair(item.subX, item.subY)
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) it.copy(page = page, subX = pos.first, subY = pos.second) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    // ---- Phase 3: RSS feed (#38) ----

    /** Refreshes every feed source on IO; failures leave the error
     *  text (never invented entries). */
    fun refreshFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _feedRefreshing.value = true
            val result = FeedRepository.fetchAll(_feedSources.value)
            _feedItems.value = result.items
            _feedError.value = result.error
            _feedRefreshing.value = false
        }
    }

    /** Adds an RSS source. The title defaults to the feed's host. */
    fun addFeedSource(url: String) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return
        if (_feedSources.value.any { it.url == trimmed }) return
        viewModelScope.launch {
            val title = try {
                java.net.URL(trimmed).host.removePrefix("www.")
            } catch (e: Exception) {
                trimmed
            }
            val next = _feedSources.value + FeedSource(UUID.randomUUID().toString(), trimmed, title)
            _feedSources.value = next
            LauncherStore.saveFeedSources(context, next)
            refreshFeed()
        }
    }

    fun removeFeedSource(id: String) {
        viewModelScope.launch {
            val next = _feedSources.value.filter { it.id != id }
            _feedSources.value = next
            LauncherStore.saveFeedSources(context, next)
            _feedItems.value = _feedItems.value.filter { it.sourceId != id }
        }
    }

    fun openFeedItem(item: FeedItem) {
        if (item.link.isNotBlank()) launchUrl(item.link)
    }

    // ---- Phase 3: media player (#120) ----

    fun mediaPlay() = MediaSessionBus.play()
    fun mediaPause() = MediaSessionBus.pause()
    fun mediaNext() = MediaSessionBus.skipToNext()
    fun mediaPrevious() = MediaSessionBus.skipToPrevious()

    /** Opens the system notification-access settings so the user can
     *  grant the listener the media player needs. */
    fun openNotificationListenerSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No settings screen for it; the hint just stays.
        }
    }

    // ---- Phase 4: badges / previews (#31, #127, #128, #132) ----

    /** #127 direct reply for one notification (RemoteInput). */
    fun replyToNotification(key: String, text: String): Boolean {
        if (text.isBlank()) return false
        return NotificationBus.reply(context, key, text.trim())
    }

    /** #132 previews for one app, newest first. */
    fun previewsFor(packageName: String) =
        NotificationBus.previews.value[packageName].orEmpty()

    // ---- Phase 4: hide apps + PIN vault (#34) ----

    /** The drawer / search / home list: all apps minus hidden ones. */
    fun refreshVisibleApps() {
        _visibleApps.value = _apps.value.filter { it.component !in _hiddenApps.value }
    }

    fun isHidden(component: String): Boolean = component in _hiddenApps.value

    /**
     * Hides an app behind the vault. When no PIN exists yet, the
     * request waits (pendingHideComponent) until the user sets one —
     * the HomeScreen shows the PIN setup then.
     */
    fun hideApp(component: String) {
        if (!_hasPin.value) {
            _pendingHideComponent.value = component
            return
        }
        viewModelScope.launch {
            val next = _hiddenApps.value + component
            _hiddenApps.value = next
            LauncherStore.saveHiddenApps(context, next)
            refreshVisibleApps()
        }
    }

    fun unhideApp(component: String) {
        viewModelScope.launch {
            val next = _hiddenApps.value - component
            _hiddenApps.value = next
            LauncherStore.saveHiddenApps(context, next)
            refreshVisibleApps()
        }
    }

    /** Hidden apps, as entries, for the vault list. */
    fun hiddenAppEntries(): List<AppEntry> =
        _apps.value.filter { it.component in _hiddenApps.value }

    /** Abandons a hide request that was waiting on PIN setup (the
     *  user dismissed the vault without setting a PIN). */
    fun clearPendingHide() {
        _pendingHideComponent.value = null
    }

    /** Sets (or changes) the vault PIN and hides any pending app. */
    fun setPin(pin: String) {
        if (pin.length < 4 || pin.length > 8 || pin.any { !it.isDigit() }) return
        viewModelScope.launch {
            val salt = PinCrypto.generateSaltHex()
            val hash = PinCrypto.hashPin(pin, salt)
            pinCredentials = Pair(salt, hash)
            LauncherStore.savePinCredentials(context, salt, hash)
            _hasPin.value = true
            val pending = _pendingHideComponent.value
            _pendingHideComponent.value = null
            if (pending != null) hideApp(pending)
        }
    }

    /** Checks the PIN; on success the vault unlocks for this session. */
    fun verifyPinAndUnlock(pin: String): Boolean {
        val credentials = pinCredentials ?: return false
        val ok = PinCrypto.verify(pin, credentials.first, credentials.second)
        if (ok) _vaultUnlocked.value = true
        return ok
    }

    fun lockVault() {
        _vaultUnlocked.value = false
    }

    /** Removes the PIN. Hidden apps stay hidden (they are only
     *  listed again once a new PIN is set and entered). */
    fun clearPin() {
        viewModelScope.launch {
            pinCredentials = null
            LauncherStore.clearPinCredentials(context)
            _hasPin.value = false
            _vaultUnlocked.value = false
        }
    }

    // ---- Phase 4: gestures (#26, #28) ----

    /** #26 / #89 locks the device via the device-admin route
     *  (DeviceLock): lockNow() when the admin is active, otherwise
     *  the system admin-enrollment screen. Returns false when the
     *  lock did not run. */
    fun lockDevice(): Boolean = DeviceLock.lock(context)

    /** #28 swipe-up secondary action for one home item. */
    fun swipeUpActionFor(itemId: String): ShortcutSecondaryAction =
        _swipeUpActions.value[itemId] ?: ShortcutSecondaryAction.NONE

    fun setSwipeUpAction(itemId: String, action: ShortcutSecondaryAction) {
        viewModelScope.launch {
            val next = _swipeUpActions.value.toMutableMap()
            if (action == ShortcutSecondaryAction.NONE) next.remove(itemId)
            else next[itemId] = action
            _swipeUpActions.value = next
            LauncherStore.saveSwipeUpActions(context, next)
        }
    }

    /**
     * Runs a shortcut's swipe-up secondary action. The long-press
     * sheet case reports back through pendingLongPressApp, which the
     * HomeScreen observes and opens.
     */
    fun performSecondaryAction(item: HomeItem, onOpenLongPressSheet: (AppEntry) -> Unit) {
        when (swipeUpActionFor(item.id)) {
            ShortcutSecondaryAction.NONE -> Unit
            ShortcutSecondaryAction.APP_INFO -> {
                val app = _apps.value.firstOrNull { it.component == item.component } ?: return
                try {
                    context.startActivity(
                        appInfoIntent(app.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (e: Exception) {
                    // No app-info screen; nothing to do.
                }
            }
            ShortcutSecondaryAction.LONG_PRESS_SHEET -> {
                val app = _apps.value.firstOrNull { it.component == item.component } ?: return
                _pendingLongPressApp.value = app
                onOpenLongPressSheet(app)
                _pendingLongPressApp.value = null
            }
            ShortcutSecondaryAction.FIRST_SHORTCUT -> {
                val component = item.component ?: return
                val first = shortcutsFor(component).firstOrNull() ?: return
                launchShortcut(first)
            }
        }
    }

    // ---- Phase 4: per-app language (#155) ----

    fun languageFor(packageName: String): AppLanguageOverride? =
        _appLanguages.value.firstOrNull { it.packageName == packageName }

    fun setAppLanguage(packageName: String, languageTag: String) {
        if (languageTag.isBlank()) return
        viewModelScope.launch {
            val next = _appLanguages.value.filter { it.packageName != packageName } +
                AppLanguageOverride(packageName, languageTag.trim())
            _appLanguages.value = next
            LauncherStore.saveAppLanguages(context, next)
            // The launcher's own locale can be applied directly
            // (LocaleManager, API 33+); other apps are the system's
            // job — see openAppLanguageSettings.
            if (packageName == context.packageName) applyOwnLocale(languageTag.trim())
        }
    }

    fun clearAppLanguage(packageName: String) {
        viewModelScope.launch {
            val next = _appLanguages.value.filter { it.packageName != packageName }
            _appLanguages.value = next
            LauncherStore.saveAppLanguages(context, next)
        }
    }

    /** Applies a locale tag to the launcher itself (API 33+). */
    private fun applyOwnLocale(languageTag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        try {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales = LocaleList.forLanguageTags(languageTag)
        } catch (e: Exception) {
            // LocaleManager unavailable; the stored choice stands.
        }
    }

    /** Opens the system per-app language screen for an app. There
     *  is no public API for a launcher to set another app's locale
     *  directly; the system screen is where the user picks it. */
    fun openAppLanguageSettings(packageName: String) {
        try {
            val intent = Intent("android.settings.APP_LOCALES_SETTINGS").apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fall back to the app-details screen.
            try {
                context.startActivity(
                    appInfoIntent(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (ignored: Exception) {
                // Nothing can open it; nothing to do.
            }
        }
    }

    // ---- Phase 4: backup & restore (#47, manual local file only) ----

    /** Exports the layout + settings to the user-picked file. */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val data = BackupData(
                settings = _settings.value,
                homeItems = _homeItems.value,
                homeFolders = _homeFolders.value,
                homePages = _homePages.value,
                drawerFolders = _drawerFolders.value,
                appPairs = _appPairs.value,
                pinnedContacts = _pinnedContacts.value,
                renames = _renames.value,
                iconOverrides = _iconOverrides.value,
                hiddenApps = _hiddenApps.value,
                appLanguages = _appLanguages.value,
                swipeUpActions = _swipeUpActions.value,
                widgetStacks = _widgetStacks.value,
                feedSources = _feedSources.value,
                notes = _notes.value,
                tasks = _tasks.value,
                habits = _habits.value,
                timers = _timers.value,
            )
            val ok = withContext(Dispatchers.IO) { BackupManager.exportToUri(context, uri, data) }
            _backupStatus.value = if (ok) "Backup saved." else "Could not write the backup file."
        }
    }

    /** Imports a backup file, replacing the layout + settings. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { BackupManager.importFromUri(context, uri) }
            if (data == null) {
                _backupStatus.value = "That file is not a launcher backup."
                return@launch
            }
            applyBackup(data)
            _backupStatus.value = "Backup restored."
        }
    }

    private fun applyBackup(data: BackupData) {
        viewModelScope.launch {
            updateSettings { data.settings }
            // Decision F5: a backup written by the old multi-page
            // dock can hold more than five dock icons — normalizeDock
            // caps it at five (extras move to page 0).
            val restoredItems = normalizeDock(data.homeItems)
            _homeItems.value = restoredItems
            LauncherStore.saveHomeItems(context, restoredItems)
            _homeFolders.value = data.homeFolders
            LauncherStore.saveHomeFolders(context, data.homeFolders)
            _homePages.value = data.homePages.ifEmpty { listOf(HomePage("page-0")) }
            LauncherStore.saveHomePages(context, _homePages.value)
            _drawerFolders.value = data.drawerFolders
            LauncherStore.saveDrawerFolders(context, data.drawerFolders)
            _appPairs.value = data.appPairs
            LauncherStore.saveAppPairs(context, data.appPairs)
            _pinnedContacts.value = data.pinnedContacts
            LauncherStore.savePinnedContacts(context, data.pinnedContacts)
            _renames.value = data.renames
            LauncherStore.saveRenames(context, data.renames)
            _iconOverrides.value = data.iconOverrides
            LauncherStore.saveIconOverrides(context, data.iconOverrides)
            iconDrawableCache.clear()
            _hiddenApps.value = data.hiddenApps
            LauncherStore.saveHiddenApps(context, data.hiddenApps)
            _appLanguages.value = data.appLanguages
            LauncherStore.saveAppLanguages(context, data.appLanguages)
            _swipeUpActions.value = data.swipeUpActions
            LauncherStore.saveSwipeUpActions(context, data.swipeUpActions)
            _widgetStacks.value = data.widgetStacks
            LauncherStore.saveWidgetStacks(context, data.widgetStacks)
            _feedSources.value = data.feedSources
            LauncherStore.saveFeedSources(context, data.feedSources)
            _notes.value = data.notes
            LauncherStore.saveNotes(context, data.notes)
            _tasks.value = data.tasks
            LauncherStore.saveTasks(context, data.tasks)
            _habits.value = resetHabitsForToday(data.habits)
            LauncherStore.saveHabits(context, _habits.value)
            _timers.value = data.timers
            LauncherStore.saveTimers(context, data.timers)
            refreshApps()
            refreshGlance()
        }
    }

    // ---- apps ----

    /** Reload the installed list; seed the home on first run and
     *  auto-add newly installed apps when #172 is on. */
    fun refreshApps() {
        viewModelScope.launch {
            val firstRun = knownPackages.isEmpty()
            val raw = repo.loadApps()
            val merged = raw.map { app ->
                val stats = launchStats[app.component]
                app.copy(
                    customLabel = _renames.value[app.component],
                    launchCount = stats?.first ?: 0,
                    lastUsed = stats?.second ?: 0L,
                )
            }
            _apps.value = merged
            refreshVisibleApps()
            val currentPackages = raw.map { it.packageName }.toSet()
            val newPackages = currentPackages - knownPackages
            if (firstRun && raw.isNotEmpty() && _homeItems.value.isEmpty()) {
                seedHome(raw)
            } else {
                if (_settings.value.autoAddApps && newPackages.isNotEmpty()) {
                    val newApps = raw.filter { it.packageName in newPackages }
                    val items = _homeItems.value.toMutableList()
                    for (app in newApps) {
                        val pos = firstFreeCell(items, 0) ?: continue
                        items.add(
                            HomeItem(
                                id = UUID.randomUUID().toString(),
                                kind = HomeItemKind.APP,
                                page = 0,
                                subX = pos.first,
                                subY = pos.second,
                                component = app.component,
                            ),
                        )
                    }
                    _homeItems.value = items
                    LauncherStore.saveHomeItems(context, items)
                }
                // Audit fix (2026-11-26): one-time self-healing for
                // homes seeded while the launcher could only see
                // system apps. Runs only when there is a home to
                // heal (empty homes seed through seedHome above).
                if (_homeItems.value.isNotEmpty()) repairHomeSeedIfNeeded(raw)
            }
            knownPackages = currentPackages
            LauncherStore.saveKnownPackages(context, currentPackages)
        }
    }

    /** Audit fix (2026-11-26): one-time self-healing for a home
     *  seeded from a 2-app list (the pre-fix manifest had no
     *  QUERY_ALL_PACKAGES, so on Android 11+ only system-image apps
     *  were visible and the seed never re-ran). Guarded by the
     *  seed_repaired DataStore flag: it runs at most once per
     *  install, and only when the installed list is big enough to
     *  top up from (>= 10 apps). It never moves or removes anything
     *  the user placed — it only adds into free cells, and it
     *  removes a page-0 copy of an app that is also in the dock
     *  (the dock copy is the one kept).
     *
     *  When the installed list is still under 10 apps the flag
     *  stays false, so the top-up gets its chance on a later
     *  refresh once more apps are installed. When page 0 already
     *  holds 5+ apps there is nothing to top up, so the flag is
     *  set and the check stops running. */
    private suspend fun repairHomeSeedIfNeeded(raw: List<AppEntry>) {
        if (LauncherStore.loadSeedRepaired(context)) return
        var items = _homeItems.value.toMutableList()
        // (a) De-duplicate: an app in both the dock and page 0 keeps
        // only the dock copy (Robert's current state).
        val dockComponents = items.filter { it.page == -1 }
            .mapNotNull { it.component }.toSet()
        val deduped = items.filterNot {
            it.page == 0 && it.kind == HomeItemKind.APP && it.component in dockComponents
        }
        var changed = deduped.size != items.size
        items = deduped.toMutableList()
        val page0AppCount = items.count { it.page == 0 && it.kind == HomeItemKind.APP }
        if (page0AppCount < 5 && raw.size >= 10) {
            val ordered = orderedApps(raw)
            // (b) Clock first, so it takes the top row when one is
            // free (full-width, one row high, like the first-run seed).
            if (items.none {
                    it.page == 0 && it.kind == HomeItemKind.BUILTIN_WIDGET &&
                        it.builtinWidgetId == BuiltinWidgetType.CLOCK.id
                }
            ) {
                val clockPos = firstFreeRect(items, 0, effectiveCols() * 2, 2)
                if (clockPos != null) {
                    items.add(
                        HomeItem(
                            id = UUID.randomUUID().toString(),
                            kind = HomeItemKind.BUILTIN_WIDGET,
                            page = 0,
                            subX = clockPos.first,
                            subY = clockPos.second,
                            spanX = effectiveCols() * 2,
                            spanY = 2,
                            builtinWidgetId = BuiltinWidgetType.CLOCK.id,
                            label = BuiltinWidgetType.CLOCK.label,
                        ),
                    )
                    changed = true
                }
            }
            // (c) Top page 0 up to 5 apps, in curated order, into
            // free cells — never a dock app, never an app already
            // on the page.
            for (app in ordered) {
                if (items.count { it.page == 0 && it.kind == HomeItemKind.APP } >= 5) break
                if (items.any { it.page == -1 && it.component == app.component }) continue
                if (items.any { it.page == 0 && it.kind == HomeItemKind.APP && it.component == app.component }) continue
                val pos = firstFreeCell(items, 0) ?: break
                items.add(
                    HomeItem(
                        id = UUID.randomUUID().toString(),
                        kind = HomeItemKind.APP,
                        page = 0,
                        subX = pos.first,
                        subY = pos.second,
                        component = app.component,
                    ),
                )
                changed = true
            }
            // (d) Top the dock up to 5 from the fixed kind seed —
            // Phone / Messages / browser / mail / Camera, in order —
            // skipping apps already in the dock or on page 0.
            for (app in dockSeedApps(raw)) {
                if (items.count { it.page == -1 } >= DOCK_MAX_ITEMS) break
                if (items.any { it.page == -1 && it.component == app.component }) continue
                if (items.any { it.page == 0 && it.kind == HomeItemKind.APP && it.component == app.component }) continue
                items.add(
                    HomeItem(
                        id = UUID.randomUUID().toString(),
                        kind = HomeItemKind.APP,
                        page = -1,
                        subX = items.count { it.page == -1 } * 2,
                        subY = 0,
                        component = app.component,
                    ),
                )
                changed = true
            }
            LauncherStore.saveSeedRepaired(context, true)
        } else if (page0AppCount >= 5) {
            LauncherStore.saveSeedRepaired(context, true)
        }
        if (changed) {
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** The curated first-run order, shared by seedHome and the
     *  repair top-up: the well-known essentials (phone, messages,
     *  browser, camera, photos, clock, mail, settings, store)
     *  come first when installed, then the rest in label order —
     *  so the first home is a usable home, not the top of an
     *  alphabetical list. */
    private fun orderedApps(raw: List<AppEntry>): List<AppEntry> {
        val preferredPackages = listOf(
            "com.google.android.dialer", "com.android.dialer",
            "com.google.android.apps.messaging", "com.android.mms",
            "com.samsung.android.messaging",
            "com.android.chrome", "org.mozilla.firefox", "com.brave.browser",
            "com.google.android.GoogleCamera", "com.android.camera",
            "com.google.android.apps.photos",
            "com.google.android.deskclock", "com.android.deskclock",
            "com.google.android.gm",
            "com.android.settings",
            "com.android.vending",
        )
        return raw.sortedWith(
            compareBy<AppEntry> { app ->
                val idx = preferredPackages.indexOf(app.packageName)
                if (idx < 0) Int.MAX_VALUE else idx
            }.thenBy { it.displayLabel.lowercase() },
        )
    }

    /** The fixed five-icon dock seed (rules 25/26, decision F5),
     *  in kind order: Phone / Messages / browser / mail / Camera.
     *  Each kind resolves to the device's default app for that kind
     *  (PackageManager.resolveActivity honours the user's chosen
     *  default), falling back to the curated seed the home stream
     *  used when the kind has no resolvable default. A kind with no
     *  installed app is skipped, so a fresh install shows at most
     *  five icons and never a dead slot. */
    private fun dockSeedApps(raw: List<AppEntry>): List<AppEntry> {
        val byPackage = raw.associateBy { it.packageName }
        fun installed(vararg packages: String): AppEntry? =
            packages.firstNotNullOfOrNull { byPackage[it] }
        fun resolved(intent: Intent): AppEntry? {
            val info = try {
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            } catch (e: Exception) {
                null
            } ?: return null
            return byPackage[info.activityInfo.packageName]
        }
        val phone = resolved(Intent(Intent.ACTION_DIAL))
            ?: installed("com.google.android.dialer", "com.android.dialer")
        val messages = resolved(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
            ?: installed(
                "com.google.android.apps.messaging", "com.android.mms",
                "com.samsung.android.messaging",
            )
        val browser = resolved(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")))
            ?: installed("com.android.chrome", "org.mozilla.firefox", "com.brave.browser")
        val mail = resolved(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL))
            ?: installed("com.google.android.gm")
        val camera = resolved(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            ?: installed("com.google.android.GoogleCamera", "com.android.camera")
        return listOfNotNull(phone, messages, browser, mail, camera)
            .distinctBy { it.component }
    }

    /** Enforces the five-icon dock cap (decision F5) on a list
     *  loaded from storage or a backup: an old multi-page dock can
     *  hold more than five icons. The first five stay in the dock,
     *  in order; the extras move to free cells on page 0 where one
     *  exists, and are dropped only when page 0 is full. Mutation
     *  paths refuse a sixth icon outright instead (see
     *  [addAppToDock] / [moveItemToDock]). */
    private fun normalizeDock(items: List<HomeItem>): List<HomeItem> {
        val dock = items.filter { it.page == -1 }
        if (dock.size <= DOCK_MAX_ITEMS) return items
        val rest = items.filter { it.page != -1 }.toMutableList()
        for (extra in dock.drop(DOCK_MAX_ITEMS)) {
            val pos = firstFreeRect(rest, 0, extra.spanX, extra.spanY) ?: continue
            rest.add(extra.copy(page = 0, subX = pos.first, subY = pos.second))
        }
        return rest + dock.take(DOCK_MAX_ITEMS)
    }

    /** First-run seed: the clock widget full-width at the top of
     *  page 0, then the first apps on page 0, the fixed five-kind
     *  dock. Page 0 EXCLUDES the dock's apps: the dock must not
     *  duplicate the grid. Apps start one row below the clock
     *  (subY offset +2). */
    private fun seedHome(raw: List<AppEntry>) {
        val ordered = orderedApps(raw)
        val dockApps = dockSeedApps(raw)
        val dockComponents = dockApps.map { it.component }.toSet()
        val items = ArrayList<HomeItem>()
        val cols = effectiveCols()
        items.add(
            HomeItem(
                id = UUID.randomUUID().toString(),
                kind = HomeItemKind.BUILTIN_WIDGET,
                page = 0,
                subX = 0,
                subY = 0,
                spanX = cols * 2,
                spanY = 2,
                builtinWidgetId = BuiltinWidgetType.CLOCK.id,
                label = BuiltinWidgetType.CLOCK.label,
            ),
        )
        var index = 0
        for (app in ordered.filter { it.component !in dockComponents }.take(cols * 2)) {
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(),
                    kind = HomeItemKind.APP,
                    page = 0,
                    subX = (index % cols) * 2,
                    subY = 2 + (index / cols) * 2,
                    component = app.component,
                ),
            )
            index++
        }
        for (app in dockApps) {
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(),
                    kind = HomeItemKind.APP,
                    page = -1,
                    subX = items.count { it.page == -1 } * 2,
                    subY = 0,
                    component = app.component,
                ),
            )
        }
        _homeItems.value = items
        viewModelScope.launch { LauncherStore.saveHomeItems(context, items) }
    }

    fun recordLaunch(component: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = launchStats[component] ?: Pair(0, 0L)
            launchStats = launchStats + (component to Pair(prev.first + 1, now))
            LauncherStore.saveLaunchStats(context, launchStats)
            _apps.value = _apps.value.map {
                if (it.component == component) it.copy(launchCount = prev.first + 1, lastUsed = now) else it
            }
        }
    }

    fun launchApp(component: String) {
        if (repo.launchApp(component)) recordLaunch(component)
        // Rules 49/50: the dot clears when the app is opened — the
        // open time is recorded, and only a notification posted
        // after it re-badges the icon.
        val packageName = component.substringBefore("/")
        _clearedBadgeAt.value = _clearedBadgeAt.value + (packageName to System.currentTimeMillis())
    }

    /** Rules 49/50: the numeric count for the long-press menu ONLY
     *  (never drawn on the icon). Counts only notifications posted
     *  after the last open, and honours the per-app off switch. */
    fun badgeCountFor(packageName: String): Int {
        if (packageName in _settings.value.badgeOffPackages) return 0
        val clearedAt = _clearedBadgeAt.value[packageName] ?: 0L
        return NotificationBus.previews.value[packageName]
            ?.count { it.postedAt > clearedAt } ?: 0
    }

    /** Rule 50: per-app badge off switch. The setting itself is on
     *  LauncherSettings.badgeOffPackages, persisted in LauncherStore
     *  and carried in backups. */
    fun setBadgeEnabledForPackage(packageName: String, enabled: Boolean) {
        updateSettings { settings ->
            val off = settings.badgeOffPackages.toMutableSet()
            if (enabled) off.remove(packageName) else off.add(packageName)
            settings.copy(badgeOffPackages = off)
        }
    }

    fun launchShortcut(shortcut: AppShortcut) {
        if (repo.launchShortcut(shortcut)) {
            // #80/#83: a shortcut launch counts toward its app's usage,
            // so the frequent row and most-used sort see it.
            val component = _apps.value.firstOrNull {
                it.packageName == shortcut.packageName
            }?.component
            if (component != null) recordLaunch(component)
        }
    }

    fun shortcutsFor(component: String): List<AppShortcut> = repo.loadShortcuts(component)

    fun loadIcon(component: String) = repo.loadIcon(component)

    /**
     * The drawable for an app after the active icon pack (#19) and any
     * per-app override (#98) are applied. Results are cached per
     * component+pack+override; the cache is cleared when the pack or an
     * override changes, so the UI never serves a stale icon.
     */
    fun drawableFor(app: AppEntry): android.graphics.drawable.Drawable? {
        val pack = _settings.value.iconPack
        val packDrawableName = _iconOverrides.value[app.component]
            ?: _packMapping.value[app.component]
        val key = app.component + "|" + pack + "|" + (packDrawableName ?: "")
        // #176 bounded cache: the drawer can hold hundreds of apps,
        // so the cache is capped instead of growing without limit.
        // 256 entries covers a large drawer; clearing it just means
        // icons are re-read from the package manager.
        if (iconDrawableCache.size > 256) iconDrawableCache.clear()
        return iconDrawableCache.getOrPut(key) {
            if (packDrawableName != null && pack.isNotBlank()) {
                packDrawable(packDrawableName) ?: repo.loadIcon(app.component)
            } else {
                repo.loadIcon(app.component)
            }
        }
    }

    fun categorize(app: AppEntry): AppCategory = repo.categorize(app)

    fun uninstallIntent(packageName: String): Intent = repo.uninstallIntent(packageName)
    fun appInfoIntent(packageName: String): Intent = repo.appInfoIntent(packageName)
    fun screenTimeIntent(): Intent = repo.screenTimeIntent()

    // ---- settings ----

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val previous = _settings.value
        val next = transform(previous)
        _settings.value = next
        viewModelScope.launch {
            LauncherStore.saveSettings(context, next)
            if (next.iconPack != previous.iconPack) {
                iconDrawableCache.clear()
                refreshIconPacks()
            }
            // Rule 22: changing grid re-flows without overlap or
            // loss, migrates and drops overflow WITH a notice.
            if (next.gridSize != previous.gridSize) {
                migrateGrid(next.gridSize)
            }
            if (next.glanceVisible != previous.glanceVisible ||
                next.glanceCalendar != previous.glanceCalendar ||
                next.glanceWeather != previous.glanceWeather ||
                next.glanceCommute != previous.glanceCommute ||
                next.glanceFinance != previous.glanceFinance ||
                next.glanceSports != previous.glanceSports ||
                next.smartspacerEnabled != previous.smartspacerEnabled ||
                next.nowBriefEnabled != previous.nowBriefEnabled
            ) {
                refreshGlance()
            }
            if (next.nowBriefEnabled != previous.nowBriefEnabled ||
                next.nowBriefHour != previous.nowBriefHour ||
                next.nowBriefMinute != previous.nowBriefMinute
            ) {
                scheduleNowBrief()
            }
        }
    }

    /**
     * Rule 22: re-flow every home-page item onto the new grid.
     * Items keep relative positions (clamped into the new bounds);
     * a colliding item moves to the nearest free rectangle; an item
     * that cannot fit anywhere is dropped — apps stay in the drawer,
     * widgets shrink to one cell first — and the user is told
     * (gridNotice), never silently.
     */
    private suspend fun migrateGrid(next: GridSize) {
        val newCols = next.effectiveCols(context.resources.configuration.screenWidthDp)
        val maxX = newCols * 2
        val maxY = next.rows * 2
        val placed = ArrayList<HomeItem>()
        var dropped = 0
        for (item in _homeItems.value.filter { it.page >= 0 }) {
            var spanX = item.spanX.coerceAtMost(maxX)
            var spanY = item.spanY.coerceAtMost(maxY)
            if (item.kind != HomeItemKind.APP) {
                // Widgets shrink to one cell rather than being lost.
                spanX = minOf(spanX, 2)
                spanY = minOf(spanY, 2)
            }
            val startX = item.subX.coerceIn(0, (maxX - spanX).coerceAtLeast(0))
            val startY = item.subY.coerceIn(0, (maxY - spanY).coerceAtLeast(0))
            val pos = findFreeRectFrom(placed, maxX, maxY, spanX, spanY, startX, startY)
            if (pos == null) {
                dropped++
                continue
            }
            placed.add(item.copy(subX = pos.first, subY = pos.second, spanX = spanX, spanY = spanY))
        }
        val kept = _homeItems.value.filter { it.page < 0 } + placed
        _homeItems.value = kept
        LauncherStore.saveHomeItems(context, kept)
        if (dropped > 0) {
            _gridNotice.value = if (dropped == 1) {
                "1 item didn't fit the new grid and was removed from the home screen. Apps stay in the app drawer."
            } else {
                "$dropped items didn't fit the new grid and were removed from the home screen. Apps stay in the app drawer."
            }
        }
    }

    /** First free rectangle of the given span, scanning row-major
     *  from (startX, startY) over the placed items. */
    private fun findFreeRectFrom(
        placed: List<HomeItem>, maxX: Int, maxY: Int,
        spanX: Int, spanY: Int, startX: Int, startY: Int,
    ): Pair<Int, Int>? {
        for (y in startY..maxY - spanY step 2) {
            val xFrom = if (y == startY) startX else 0
            for (x in xFrom..maxX - spanX step 2) {
                val overlaps = placed.any { other ->
                    rectsOverlap(x, y, spanX, spanY, other.subX, other.subY, other.spanX, other.spanY)
                }
                if (!overlaps) return Pair(x, y)
            }
        }
        return null
    }

    /** Rule 56: drag-to-top Uninstall — opens the system uninstall
     *  flow for the app. */
    fun uninstallApp(packageName: String) {
        try {
            context.startActivity(
                uninstallIntent(packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No uninstall activity (e.g. a system app) — the item
            // stays where it was rather than disappearing.
        }
    }

    /** Rule 29: empty trailing pages auto-remove — but only the
     *  ones BEHIND the page the user is on, so a page just added
     *  from Overview is not deleted before it can be visited.
     *  Called when the current page changes. */
    fun pruneEmptyTrailingPages(currentPage: Int) {
        viewModelScope.launch {
            var pages = _homePages.value
            val items = _homeItems.value
            while (pages.size > 1) {
                val last = pages.size - 1
                if (last == currentPage) break
                if (items.any { it.page == last }) break
                pages = pages.dropLast(1)
            }
            if (pages.size != _homePages.value.size) {
                _homePages.value = pages
                LauncherStore.saveHomePages(context, pages)
                val def = _settings.value.defaultPage
                if (def >= pages.size) {
                    updateSettings { it.copy(defaultPage = pages.size - 1) }
                }
            }
        }
    }

    // ---- reset & debug (#171) ----

    /**
     * #171 reset the layout to defaults: settings back to the
     * Phase 1–4 defaults, one home page, and the first-run
     * seed re-run so the home is populated again. Hidden apps and
     * the vault PIN are left alone: this resets the layout, not
     * the user's private data. (A layout reset is not a first run
     * — the default-home prompt does not fire again.)
     */
    fun resetLayoutToDefaults() {
        viewModelScope.launch {
            updateSettings { LauncherSettings() }
            _homeFolders.value = emptyList()
            LauncherStore.saveHomeFolders(context, emptyList())
            _homePages.value = listOf(HomePage("page-0"))
            LauncherStore.saveHomePages(context, _homePages.value)
            _homeItems.value = emptyList()
            LauncherStore.saveHomeItems(context, emptyList())
            iconDrawableCache.clear()
            // Re-seed: refreshApps treats empty knownPackages as a
            // first run and seeds the home from the installed apps.
            knownPackages = emptySet()
            refreshApps()
        }
    }

    /**
     * #171 clear ALL launcher data: every DataStore key (settings,
     * layout, vault credentials, hidden apps, notes/tasks/habits/
     * timers, stacks, feed sources, usage history) plus the
     * in-memory state. The next launch is a first launch again —
     * home first, with the single default-home prompt.
     */
    fun clearAllData() {
        viewModelScope.launch {
            LauncherStore.clearAll(context)
            _settings.value = LauncherSettings()
            _homeItems.value = emptyList()
            _homeFolders.value = emptyList()
            _homePages.value = listOf(HomePage("page-0"))
            _drawerFolders.value = emptyList()
            _appPairs.value = emptyList()
            _pinnedContacts.value = emptyList()
            _renames.value = emptyMap()
            _iconOverrides.value = emptyMap()
            _notes.value = emptyList()
            _tasks.value = emptyList()
            _habits.value = emptyList()
            _timers.value = emptyList()
            _widgetStacks.value = emptyList()
            _feedSources.value = emptyList()
            _feedItems.value = emptyList()
            _hiddenApps.value = emptySet()
            _appLanguages.value = emptyList()
            _swipeUpActions.value = emptyMap()
            _customProviders.value = emptyList()
            _searchProviders.value = SearchEngine.defaultProviders()
            pinCredentials = null
            _hasPin.value = false
            _vaultUnlocked.value = false
            launchStats = emptyMap()
            iconDrawableCache.clear()
            _backupStatus.value = "All launcher data cleared."
            knownPackages = emptySet()
            refreshApps()
            refreshGlance()
        }
    }

    /** #171 / #176 how many icons the drawable cache holds. The
     *  cache is bounded (see drawableFor): it never grows without
     *  limit as the user scrolls the drawer. */
    fun iconCacheSize(): Int = iconDrawableCache.size

    fun setDrawerQuery(query: String) {
        _drawerQuery.value = query
        viewModelScope.launch { LauncherStore.saveDrawerQuery(context, query) }
    }

    fun setDrawerScrollIndex(index: Int) {
        _drawerScrollIndex.value = index
        viewModelScope.launch { LauncherStore.saveDrawerScroll(context, index) }
    }

    /** #13 move an app one step in the manual drawer order
     *  (DrawerSort.CUSTOM). The order seeds from the current name
     *  order the first time it is used, so a first move does not
     *  scramble the list. */
    fun moveDrawerApp(component: String, delta: Int) {
        val current = _drawerCustomOrder.value.toMutableList()
        if (current.isEmpty()) {
            current.addAll(_apps.value.sortedBy { it.displayLabel.lowercase() }.map { it.component })
        }
        val index = current.indexOf(component)
        if (index < 0) return
        val target = (index + delta).coerceIn(0, current.size - 1)
        if (target == index) return
        current.removeAt(index)
        current.add(target, component)
        _drawerCustomOrder.value = current
        viewModelScope.launch { LauncherStore.saveDrawerCustomOrder(context, current) }
    }

    // ---- icon packs (#19, #99) ----

    fun refreshIconPacks() {
        viewModelScope.launch {
            _iconPacks.value = repo.discoverIconPacks()
            val pack = _settings.value.iconPack
            _packMapping.value = if (pack.isBlank()) emptyMap() else repo.loadPackMapping(pack)
        }
    }

    fun packDrawable(drawableName: String) =
        repo.loadPackDrawable(_settings.value.iconPack, drawableName)

    fun quickEditSuggestions(app: AppEntry): List<String> {
        val pack = _settings.value.iconPack
        if (pack.isBlank()) return emptyList()
        return repo.quickEditSuggestions(pack, app, _iconOverrides.value[app.component])
    }

    // ---- home layout (#1, #3, #62, #64, #65) ----

    /** First free half-cell position on a page, or null when full. */
    fun firstFreeCell(items: List<HomeItem>, page: Int): Pair<Int, Int>? {
        val grid = _settings.value.gridSize
        val maxX = effectiveCols() * 2
        val maxY = grid.rows * 2
        val occupied = items.filter { it.page == page }.flatMap { item ->
            val cells = ArrayList<Pair<Int, Int>>()
            for (x in item.subX until item.subX + item.spanX step 2) {
                for (y in item.subY until item.subY + item.spanY step 2) {
                    cells.add(Pair(x, y))
                }
            }
            cells
        }.toSet()
        for (y in 0 until maxY step 2) {
            for (x in 0 until maxX step 2) {
                if (Pair(x, y) !in occupied) return Pair(x, y)
            }
        }
        return null
    }

    fun addAppToHome(component: String, page: Int = 0) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val items = _homeItems.value.toMutableList()
            if (items.any { it.kind == HomeItemKind.APP && it.component == component && it.page == page }) return@launch
            val pos = firstFreeCell(items, page) ?: return@launch
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.APP,
                    page = page, subX = pos.first, subY = pos.second, component = component,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    fun moveHomeItem(itemId: String, page: Int, subX: Int, subY: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val grid = _settings.value.gridSize
            val clampedX = subX.coerceIn(0, effectiveCols() * 2 - 2)
            val clampedY = subY.coerceIn(0, grid.rows * 2 - 2)
            val item = _homeItems.value.firstOrNull { it.id == itemId } ?: return@launch
            // #63 widget overlap: when the toggle is off, a drop that
            // would overlap another item on the page snaps back (the
            // item stays where it was); when it is on, the item lands
            // on top of whatever is there, still on the grid.
            if (!_settings.value.widgetOverlapEnabled) {
                val overlap = _homeItems.value.firstOrNull { other ->
                    other.id != itemId && other.page == page &&
                        rectsOverlap(
                            clampedX, clampedY, item.spanX, item.spanY,
                            other.subX, other.subY, other.spanX, other.spanY,
                        )
                }
                if (overlap != null) {
                    // Rule 56 live reorder: two single-cell apps
                    // SWAP places instead of refusing — this is what
                    // the 650 ms hover commits. Anything bigger
                    // (widgets, multi-cell items) still snaps back.
                    if (item.kind == HomeItemKind.APP && overlap.kind == HomeItemKind.APP &&
                        item.spanX == 2 && item.spanY == 2 &&
                        overlap.spanX == 2 && overlap.spanY == 2
                    ) {
                        _homeItems.value = _homeItems.value.map {
                            when (it.id) {
                                itemId -> it.copy(page = page, subX = clampedX, subY = clampedY)
                                overlap.id -> it.copy(subX = item.subX, subY = item.subY)
                                else -> it
                            }
                        }
                        LauncherStore.saveHomeItems(context, _homeItems.value)
                    }
                    return@launch
                }
            }
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) it.copy(page = page, subX = clampedX, subY = clampedY) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    /** Move a home item into the dock (long-press sheet). The dock
     *  holds exactly [DOCK_MAX_ITEMS] icons on one page (decision
     *  F5): a sixth icon is REFUSED — false, with the refusal
     *  surfaced (see [dockAddError]) — never silently dropped.
     *  True when the item is in the dock afterwards (including the
     *  no-op when it was already there). */
    fun moveItemToDock(itemId: String): Boolean {
        if (_settings.value.desktopLocked) return false
        if (_homeItems.value.any { it.id == itemId && it.page == -1 }) return true
        if (!canAddToDock()) {
            _dockAddError.value = dockFullMessage
            return false
        }
        viewModelScope.launch {
            _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) it.copy(page = -1) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
        return true
    }

    /** #4 add an app from the drawer straight to the dock
     *  (long-press sheet). Same cap as [moveItemToDock]: a sixth
     *  icon is refused with a visible error, never silently
     *  dropped. True when the app is in the dock afterwards
     *  (including the no-op when it already is). Dock slots are
     *  counted like the first-run seed — the dock Row lays items
     *  out evenly, so subX is bookkeeping only. */
    fun addAppToDock(component: String): Boolean {
        if (_settings.value.desktopLocked) return false
        if (_homeItems.value.any {
                it.kind == HomeItemKind.APP && it.component == component && it.page == -1
            }
        ) return true
        if (!canAddToDock()) {
            _dockAddError.value = dockFullMessage
            return false
        }
        viewModelScope.launch {
            val items = _homeItems.value.toMutableList()
            val slot = items.count { it.page == -1 }
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.APP,
                    page = -1,
                    subX = slot * 2, subY = 0, component = component,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
        return true
    }

    fun removeHomeItem(itemId: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homeItems.value = _homeItems.value.filter { it.id != itemId }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    fun addShortcutToHome(shortcut: AppShortcut, page: Int = 0) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val items = _homeItems.value.toMutableList()
            val pos = firstFreeCell(items, page) ?: return@launch
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.SHORTCUT,
                    page = page, subX = pos.first, subY = pos.second,
                    shortcutId = shortcut.id, shortcutPackage = shortcut.packageName,
                    label = shortcut.shortLabel,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    // ---- folders (#14) ----

    /** Drop an app onto another app: make a folder holding both (#14).
     *  Rule 39: the name is SHORT and TASK-BASED, derived from the
     *  contents' kind — NEVER the target app's name, never "Folder".
     *  Two apps of one kind take that kind's name (e.g. "Games");
     *  mixed kinds take "Apps". */
    fun createFolderFromDrop(draggedComponent: String, targetItem: HomeItem) {
        if (_settings.value.desktopLocked) return
        if (targetItem.kind != HomeItemKind.APP || targetItem.component == draggedComponent) return
        viewModelScope.launch {
            val folderId = UUID.randomUUID().toString()
            val targetApp = _apps.value.firstOrNull { it.component == targetItem.component }
            val draggedApp = _apps.value.firstOrNull { it.component == draggedComponent }
            val name = folderNameFor(targetApp, draggedApp)
            val folder = HomeFolder(folderId, name, listOfNotNull(targetItem.component, draggedComponent))
            _homeFolders.value = _homeFolders.value + folder
            LauncherStore.saveHomeFolders(context, _homeFolders.value)
            val items = _homeItems.value
                .filter { !(it.kind == HomeItemKind.APP && it.component == draggedComponent) }
                .map {
                    if (it.id == targetItem.id) {
                        it.copy(kind = HomeItemKind.FOLDER, folderId = folderId, component = null)
                    } else it
                }
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** Rule 39 folder naming: the kind the contents share, or
     *  "Apps" when they share none. Never an app's name, never
     *  "Folder". */
    fun folderNameFor(first: AppEntry?, second: AppEntry?): String {
        val firstCategory = first?.let { categorize(it) }
        val secondCategory = second?.let { categorize(it) }
        if (firstCategory != null && firstCategory == secondCategory) {
            return when (firstCategory) {
                AppCategory.COMMUNICATION -> "Communication"
                AppCategory.GAMES -> "Games"
                AppCategory.MEDIA -> "Media"
                AppCategory.SOCIAL -> "Social"
                AppCategory.PRODUCTIVITY -> "Productivity"
                AppCategory.FINANCE -> "Finance"
                AppCategory.SHOPPING -> "Shopping"
                AppCategory.TOOLS -> "Tools"
                AppCategory.ALL, AppCategory.OTHER -> "Apps"
            }
        }
        return "Apps"
    }

    /** Rule 37: dropping an app onto a closed folder ADDS it to
     *  the folder (the old drop search only matched APP items). */
    fun addAppToFolder(component: String, folderId: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homeFolders.value = _homeFolders.value.map {
                if (it.id == folderId && component !in it.apps) {
                    it.copy(apps = it.apps + component)
                } else {
                    it
                }
            }
            LauncherStore.saveHomeFolders(context, _homeFolders.value)
            _homeItems.value = _homeItems.value.filterNot {
                it.kind == HomeItemKind.APP && it.component == component
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    fun renameFolder(folderId: String, name: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homeFolders.value = _homeFolders.value.map {
                if (it.id == folderId) it.copy(name = name) else it
            }
            LauncherStore.saveHomeFolders(context, _homeFolders.value)
        }
    }

    fun removeAppFromFolder(folderId: String, component: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            applyFolderRemoval(folderId) { folder ->
                folder.copy(apps = folder.apps.filter { app -> app != component })
            }
        }
    }

    /** #95 add a pinned contact to a home folder (from the contact's
     *  long-press sheet). The same contact cannot be added twice. */
    fun addContactToFolder(folderId: String, contactUri: String, contactName: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homeFolders.value = _homeFolders.value.map {
                if (it.id == folderId && it.contacts.none { contact -> contact.uri == contactUri }) {
                    it.copy(contacts = it.contacts + FolderContact(contactUri, contactName))
                } else {
                    it
                }
            }
            LauncherStore.saveHomeFolders(context, _homeFolders.value)
        }
    }

    /** #95 remove a contact from a home folder. The folder dissolves
     *  by the same rule as for apps (see applyFolderRemoval). */
    fun removeContactFromFolder(folderId: String, contactUri: String) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            applyFolderRemoval(folderId) { folder ->
                folder.copy(contacts = folder.contacts.filter { contact -> contact.uri != contactUri })
            }
        }
    }

    /**
     * #14 / #95 one removal rule for folders, counting apps AND
     * contacts together: a folder with two or more members left
     * just loses the member; with one member left it dissolves
     * back into that member's tile (an app tile, or a contact tile
     * when the last member is a contact); empty, the folder and
     * its tile are removed. Counting only apps (the old rule)
     * would have dissolved a folder with one app and one contact
     * into the app and silently dropped the contact.
     */
    private suspend fun applyFolderRemoval(
        folderId: String,
        remove: (HomeFolder) -> HomeFolder,
    ) {
        val folders = _homeFolders.value.map {
            if (it.id == folderId) remove(it) else it
        }
        val remaining = folders.firstOrNull { it.id == folderId }
            ?: return
        val memberCount = remaining.apps.size + remaining.contacts.size
        if (memberCount <= 1) {
            _homeFolders.value = folders.filter { it.id != folderId }
            val lastApp = remaining.apps.firstOrNull()
            val lastContact = remaining.contacts.firstOrNull()
            _homeItems.value = _homeItems.value.mapNotNull {
                if (it.folderId != folderId) {
                    it
                } else if (lastApp != null) {
                    it.copy(kind = HomeItemKind.APP, folderId = null, component = lastApp)
                } else if (lastContact != null) {
                    it.copy(
                        kind = HomeItemKind.CONTACT, folderId = null, component = null,
                        contactUri = lastContact.uri, contactName = lastContact.name,
                    )
                } else {
                    // Empty folder: drop the tile rather than
                    // leaving an app tile with no component.
                    null
                }
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        } else {
            _homeFolders.value = folders
        }
        LauncherStore.saveHomeFolders(context, _homeFolders.value)
    }

    // ---- pages (#3, #75) ----

    fun addPage() {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homePages.value = _homePages.value + HomePage("page-" + UUID.randomUUID().toString().take(8))
            LauncherStore.saveHomePages(context, _homePages.value)
        }
    }

    fun deletePage(index: Int) {
        if (_settings.value.desktopLocked) return
        if (_homePages.value.size <= 1) return
        viewModelScope.launch {
            val pages = _homePages.value.toMutableList()
            if (index !in pages.indices) return@launch
            pages.removeAt(index)
            _homePages.value = pages
            LauncherStore.saveHomePages(context, pages)
            // Items on the deleted page move to page 0; items on
            // later pages shift down one so they stay on their page.
            // Dock (-1) and widget pages (negative) are untouched.
            _homeItems.value = _homeItems.value.map {
                when {
                    it.page == index -> it.copy(page = 0)
                    it.page > index -> it.copy(page = it.page - 1)
                    else -> it
                }
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
            // The default page follows the same shift.
            val def = _settings.value.defaultPage
            val newDef = when {
                def == index -> 0
                def > index -> def - 1
                else -> def
            }
            if (newDef != def) updateSettings { it.copy(defaultPage = newDef) }
        }
    }

    fun setPageHidden(index: Int, hidden: Boolean) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            _homePages.value = _homePages.value.mapIndexed { i, page ->
                if (i == index) page.copy(hidden = hidden) else page
            }
            LauncherStore.saveHomePages(context, _homePages.value)
        }
    }

    fun movePage(from: Int, to: Int) {
        if (_settings.value.desktopLocked) return
        viewModelScope.launch {
            val pages = _homePages.value.toMutableList()
            if (from !in pages.indices || to !in pages.indices) return@launch
            val page = pages.removeAt(from)
            pages.add(to, page)
            _homePages.value = pages
            LauncherStore.saveHomePages(context, pages)
            // Items follow their page: the moved page's items take
            // `to`, and the pages between shift one in the opposite
            // direction — otherwise a reorder would swap the contents
            // of two pages instead of moving a page.
            _homeItems.value = _homeItems.value.map {
                val newPage = when {
                    it.page == from -> to
                    from < to && it.page in (from + 1)..to -> it.page - 1
                    from > to && it.page in to until from -> it.page + 1
                    else -> it.page
                }
                if (newPage != it.page) it.copy(page = newPage) else it
            }
            LauncherStore.saveHomeItems(context, _homeItems.value)
            // The default page follows the same shift.
            val def = _settings.value.defaultPage
            val newDef = when {
                def == from -> to
                from < to && def in (from + 1)..to -> def - 1
                from > to && def in to until from -> def + 1
                else -> def
            }
            if (newDef != def) updateSettings { it.copy(defaultPage = newDef) }
        }
    }

    fun setDefaultPage(index: Int) {
        if (_settings.value.desktopLocked) return
        updateSettings { it.copy(defaultPage = index) }
    }

    // ---- drawer folders (#13, #87) ----

    fun createDrawerFolder(name: String, apps: List<String>) {
        viewModelScope.launch {
            val folder = DrawerFolder(UUID.randomUUID().toString(), name, apps)
            _drawerFolders.value = _drawerFolders.value + folder
            LauncherStore.saveDrawerFolders(context, _drawerFolders.value)
        }
    }

    fun reorderInDrawerFolder(folderId: String, from: Int, to: Int) {
        viewModelScope.launch {
            _drawerFolders.value = _drawerFolders.value.map { folder ->
                if (folder.id != folderId) return@map folder
                val list = folder.apps.toMutableList()
                if (from !in list.indices || to !in list.indices) return@map folder
                val app = list.removeAt(from)
                list.add(to, app)
                folder.copy(apps = list)
            }
            LauncherStore.saveDrawerFolders(context, _drawerFolders.value)
        }
    }

    fun removeFromDrawerFolder(folderId: String, component: String) {
        viewModelScope.launch {
            _drawerFolders.value = _drawerFolders.value.map {
                if (it.id == folderId) it.copy(apps = it.apps.filter { app -> app != component }) else it
            }.filter { it.apps.isNotEmpty() }
            LauncherStore.saveDrawerFolders(context, _drawerFolders.value)
        }
    }

    fun deleteDrawerFolder(folderId: String) {
        viewModelScope.launch {
            _drawerFolders.value = _drawerFolders.value.filter { it.id != folderId }
            LauncherStore.saveDrawerFolders(context, _drawerFolders.value)
        }
    }

    // ---- rename / icon swap (#98, #99) ----

    fun renameApp(component: String, newLabel: String) {
        viewModelScope.launch {
            val renames = _renames.value.toMutableMap()
            if (newLabel.isBlank()) renames.remove(component) else renames[component] = newLabel.trim()
            _renames.value = renames
            LauncherStore.saveRenames(context, renames)
            _apps.value = _apps.value.map {
                if (it.component == component) it.copy(customLabel = renames[component]) else it
            }
            refreshVisibleApps()
        }
    }

    fun setIconOverride(component: String, drawableName: String?) {
        viewModelScope.launch {
            val overrides = _iconOverrides.value.toMutableMap()
            if (drawableName.isNullOrBlank()) overrides.remove(component) else overrides[component] = drawableName
            _iconOverrides.value = overrides
            iconDrawableCache.clear()
            LauncherStore.saveIconOverrides(context, overrides)
        }
    }

    /** #13 add an app to a drawer folder (from the long-press sheet). */
    fun addToDrawerFolder(folderId: String, component: String) {
        viewModelScope.launch {
            _drawerFolders.value = _drawerFolders.value.map {
                if (it.id == folderId && component !in it.apps) it.copy(apps = it.apps + component) else it
            }
            LauncherStore.saveDrawerFolders(context, _drawerFolders.value)
        }
    }

    // ---- app pairs (#96) ----

    fun createAppPair(first: String, second: String, name: String) {
        if (first == second) return
        viewModelScope.launch {
            val pair = AppPair(UUID.randomUUID().toString(), name.ifBlank { "App Pair" }, first, second)
            _appPairs.value = _appPairs.value + pair
            LauncherStore.saveAppPairs(context, _appPairs.value)
            val items = _homeItems.value.toMutableList()
            val pos = firstFreeCell(items, 0) ?: return@launch
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.APP_PAIR,
                    page = 0, subX = pos.first, subY = pos.second, pairId = pair.id,
                    label = pair.name,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
    }

    /** #96 Android-native split-screen: the first app is launched
     *  normally, the second with FLAG_ACTIVITY_LAUNCH_ADJACENT, which
     *  asks the system to place it beside the first in split-screen.
     *  Note: forcing a windowing mode needs ActivityOptions
     *  .setLaunchWindowingMode, a hidden API that is not in the public
     *  SDK (android.jar has no such method), so the pair relies on the
     *  system's adjacent-launch behaviour — the device test will show
     *  whether the Pixel splits them. If the system declines, both
     *  apps still launch (fallback below). */
    fun launchAppPair(pair: AppPair) {
        val first = ComponentName.unflattenFromString(pair.first) ?: return
        val second = ComponentName.unflattenFromString(pair.second) ?: return
        try {
            val user = android.os.Process.myUserHandle()
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
            launcherApps.startMainActivity(first, user, null, null)
            recordLaunch(pair.first)
            val secondIntent = context.packageManager.getLaunchIntentForPackage(second.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (secondIntent != null) {
                context.startActivity(secondIntent)
                recordLaunch(pair.second)
            }
        } catch (e: Exception) {
            // Fall back to launching the two apps normally.
            launchApp(pair.first)
            launchApp(pair.second)
        }
    }

    fun deleteAppPair(pairId: String) {
        viewModelScope.launch {
            _appPairs.value = _appPairs.value.filter { it.id != pairId }
            LauncherStore.saveAppPairs(context, _appPairs.value)
            _homeItems.value = _homeItems.value.filter { it.pairId != pairId }
            LauncherStore.saveHomeItems(context, _homeItems.value)
        }
    }

    // ---- pinned contacts (#95) ----

    /** #95 a pinned contact on the home or in the dock. Dock
     *  placement ([toDock]) is capped like every other dock add
     *  (decision F5): a full dock refuses the contact outright —
     *  false, with the refusal surfaced — and the contact is not
     *  added anywhere. True when the contact was added. */
    fun addPinnedContact(uri: Uri, name: String?, page: Int = 0, toDock: Boolean = false): Boolean {
        if (toDock && !canAddToDock()) {
            _dockAddError.value = dockFullMessage
            return false
        }
        viewModelScope.launch {
            val displayName = name ?: repo.resolveContactName(uri) ?: "Contact"
            val contact = PinnedContact(UUID.randomUUID().toString(), displayName, uri.toString())
            _pinnedContacts.value = _pinnedContacts.value + contact
            LauncherStore.savePinnedContacts(context, _pinnedContacts.value)
            val items = _homeItems.value.toMutableList()
            // Dock placement (#95): counted like the first-run seed,
            // not a grid cell.
            val targetPage = if (toDock) -1 else page
            val pos = if (toDock) {
                Pair(items.count { it.page == -1 } * 2, 0)
            } else {
                firstFreeCell(items, page) ?: return@launch
            }
            items.add(
                HomeItem(
                    id = UUID.randomUUID().toString(), kind = HomeItemKind.CONTACT,
                    page = targetPage,
                    subX = pos.first, subY = pos.second,
                    contactUri = uri.toString(), contactName = displayName,
                ),
            )
            _homeItems.value = items
            LauncherStore.saveHomeItems(context, items)
        }
        return true
    }

    fun openContact(uriString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // No contacts app can open the Uri; nothing to do.
        }
    }

    fun removePinnedContact(contactId: String) {
        viewModelScope.launch {
            _pinnedContacts.value = _pinnedContacts.value.filter { it.id != contactId }
            LauncherStore.savePinnedContacts(context, _pinnedContacts.value)
        }
    }
}
