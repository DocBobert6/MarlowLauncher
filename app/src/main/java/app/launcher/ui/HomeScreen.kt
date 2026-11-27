package app.launcher.ui

import android.Manifest
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import app.launcher.data.AdaptiveLayout
import app.launcher.data.WidgetHost
import app.launcher.ui.theme.relativeLuminance
import app.launcher.model.GestureAction
import app.launcher.model.LauncherThemeChoice
import app.launcher.model.PAGE_DESKTOP_WIDGETS
import app.launcher.model.PAGE_WIDGET_PAGE
import app.launcher.model.DrawerFolder
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry
import app.launcher.model.AppShortcut
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Phase 1 home: paged grid (#3), dock (#4/#68/#71/#112), drawer
 * (#11, swipe up — the stock gesture), overview on pinch (#76) or
 * long-press on empty space, one-handed layout (#5).
 *
 * Motion wave: this function is only the haptics provider — the
 * body lives in HomeScreenContent. LocalMarlowHaptics is provided
 * here so HomeGrid, FolderDialog, and SwitchRow (all composed
 * below) read the gated instance without a parameter threaded
 * through every call site.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    // #37 the morning notification's launch extra: when true the
    // home opens the Now Brief sheet on arrival. Defaults to false
    // (a plain launch, including the startup gate, is unaffected);
    // consumed via onNowBriefOpened once the sheet is opened.
    openNowBrief: Boolean = false,
    onNowBriefOpened: () -> Unit = {},
) {
    val settings = viewModel.settings.collectAsStateValue()
    val haptics = rememberMarlowHaptics(settings.hapticsEnabled)
    CompositionLocalProvider(LocalMarlowHaptics provides haptics) {
        HomeScreenContent(viewModel, openNowBrief, onNowBriefOpened)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenContent(
    viewModel: HomeViewModel,
    // #37 the morning notification's launch extra: when true the
    // home opens the Now Brief sheet on arrival. Defaults to false
    // (a plain launch, including the startup gate, is unaffected);
    // consumed via onNowBriefOpened once the sheet is opened.
    openNowBrief: Boolean = false,
    onNowBriefOpened: () -> Unit = {},
) {
    val settings = viewModel.settings.collectAsStateValue()
    // Motion wave: the system reduced-motion setting, read once
    // here for the pager, the parallax, and the drawer.
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalMarlowHaptics.current
    val apps = viewModel.apps.collectAsStateValue()
    val homeItems = viewModel.homeItems.collectAsStateValue()
    val homeFolders = viewModel.homeFolders.collectAsStateValue()
    val homePages = viewModel.homePages.collectAsStateValue()
    val appPairs = viewModel.appPairs.collectAsStateValue()
    val drawerFolders = viewModel.drawerFolders.collectAsStateValue()
    val iconPacks = viewModel.iconPacks.collectAsStateValue()
    // Phase 2 state.
    val glanceItems = viewModel.glanceItems.collectAsStateValue()
    val todayEvents = viewModel.todayEvents.collectAsStateValue()
    val searchResults = viewModel.searchResults.collectAsStateValue()
    val headphoneMusicVisible = viewModel.headphoneMusicVisible.collectAsStateValue()
    val musicApps = viewModel.musicApps.collectAsStateValue()
    // Phase 3 state.
    val widgetStacks = viewModel.widgetStacks.collectAsStateValue()
    val editingWidgetId = viewModel.editingWidgetId.collectAsStateValue()
    val feedSources = viewModel.feedSources.collectAsStateValue()
    val feedItems = viewModel.feedItems.collectAsStateValue()
    val feedRefreshing = viewModel.feedRefreshing.collectAsStateValue()
    val feedError = viewModel.feedError.collectAsStateValue()
    val nowPlaying = viewModel.nowPlaying.collectAsStateValue()
    val mediaListenerConnected = viewModel.mediaListenerConnected.collectAsStateValue()
    // Phase 4 state.
    val visibleApps = viewModel.visibleApps.collectAsStateValue()
    val notificationPreviews = viewModel.notificationPreviews.collectAsStateValue()
    val hasPin = viewModel.hasPin.collectAsStateValue()
    val vaultUnlocked = viewModel.vaultUnlocked.collectAsStateValue()
    val pendingHideComponent = viewModel.pendingHideComponent.collectAsStateValue()
    val swipeUpActions = viewModel.swipeUpActions.collectAsStateValue()
    val appsByComponent = remember(visibleApps) { visibleApps.associateBy { it.component } }
    val appsByPackage = remember(visibleApps) { visibleApps.associateBy { it.packageName } }
    // #31 badge counts + #128 dynamic-badge artwork, keyed by package.
    // Rules 49/50: the dot clears when the app is opened (only a
    // notification posted AFTER the last open counts) and honours
    // the per-app off switch. The numeric count itself lives only
    // in the long-press menu (HomeViewModel.badgeCountFor).
    val clearedBadgeAt = viewModel.clearedBadgeAt.collectAsStateValue()
    val badgeCounts = remember(notificationPreviews, clearedBadgeAt, settings.badgeOffPackages) {
        notificationPreviews.mapValues { (packageName, list) ->
            if (packageName in settings.badgeOffPackages) {
                0
            } else {
                list.count { it.postedAt > (clearedBadgeAt[packageName] ?: 0L) }
            }
        }.filterValues { it > 0 }
    }
    val badgeBitmaps = remember(notificationPreviews) {
        notificationPreviews.mapValues { (_, list) -> list.firstNotNullOfOrNull { it.largeIcon } }
    }
    // #150 / #151 / #152 layout variant from the window's occupied
    // area (rule 70), not the device orientation.
    val configuration = LocalConfiguration.current
    val layoutVariant = AdaptiveLayout.variantForWindow(
        configuration.smallestScreenWidthDp,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    )
    val isLandscape = settings.landscapeEnabled && AdaptiveLayout.isLandscape(layoutVariant)
    val twoPane = settings.twoPaneTablet && settings.separateScreenLayouts &&
        AdaptiveLayout.isTwoPane(layoutVariant)
    val foldersById = remember(homeFolders) { homeFolders.associateBy { it.id } }
    val pairsById = remember(appPairs) { appPairs.associateBy { it.id } }

    var showDrawer by remember { mutableStateOf(false) }
    var drawerFocusSearch by remember { mutableStateOf(false) }
    var showOverview by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Audit fix: which settings area to land on when opening Settings
    // from a deep link (the At a Glance weather row). Null = the full
    // list, as before; set only by onAddWeather, cleared on dismiss.
    var settingsInitialArea by remember { mutableStateOf<SettingsArea?>(null) }
    // Phase 5: the reset & debug screen (#171), reached from
    // Settings → About.
    var showDebug by remember { mutableStateOf(false) }
    // Phase 2 overlays.
    var showSearch by remember { mutableStateOf(false) }
    var showNowBrief by remember { mutableStateOf(false) }
    // #37 the notification deep-link: open the Now Brief sheet on
    // arrival, then consume the request so it does not reopen.
    LaunchedEffect(openNowBrief) {
        if (openNowBrief) {
            showNowBrief = true
            onNowBriefOpened()
        }
    }
    var showWallpaper by remember { mutableStateOf(false) }
    var addingSticker by remember { mutableStateOf(false) }
    var editingSticker by remember { mutableStateOf<HomeItem?>(null) }
    var longPressApp by remember { mutableStateOf<Pair<AppEntry, HomeItem?>?>(null) }
    var longPressNonApp by remember { mutableStateOf<HomeItem?>(null) }
    // Phase 3 overlays.
    var showWidgetPicker by remember { mutableStateOf(false) }
    var widgetPickerPage by remember { mutableStateOf(PAGE_WIDGET_PAGE) }
    var showDesktopWidgets by remember { mutableStateOf(false) }
    var longPressWidget by remember { mutableStateOf<HomeItem?>(null) }
    // Phase 4 overlays.
    var showVault by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    // #47 backup file pickers (Storage Access Framework): export
    // creates a file the user names, import opens one.
    val exportBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }
    // Phase 4: one place that runs a mapped gesture action (#26,
    // #89, #90, #91). LOCK_DEVICE goes through the device-admin
    // route (DeviceLock); the rest are home UI state.
    fun runGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> { showDrawer = true; drawerFocusSearch = false }
            GestureAction.OPEN_NOTIFICATIONS -> showNotifications = true
            GestureAction.LOCK_DEVICE -> viewModel.lockDevice()
            GestureAction.OPEN_OVERVIEW -> showOverview = true
            GestureAction.OPEN_SEARCH -> showSearch = true
            GestureAction.OPEN_SETTINGS -> showSettings = true
            GestureAction.OPEN_VAULT -> showVault = true
        }
    }
    // #6 a bind waiting on the user's confirmation (host id,
    // provider, target page). Freed if the user declines.
    var pendingBind by remember { mutableStateOf<Triple<Int, ComponentName, Int>?>(null) }
    // Rule 43: a configure flow waiting on its result. A placed
    // widget lands only on RESULT_OK; anything else frees the id.
    // `pendingBindNeedsConfigure` chains bind → configure when the
    // system wants confirmation first; `reconfigureWidgetId` marks
    // an in-place reconfigure (nothing is re-added on its result).
    var pendingConfigurePlace by remember { mutableStateOf<Triple<Int, ComponentName, Int>?>(null) }
    var pendingBindNeedsConfigure by remember { mutableStateOf(false) }
    var reconfigureWidgetId by remember { mutableStateOf<Int?>(null) }
    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val place = pendingConfigurePlace
        pendingConfigurePlace = null
        reconfigureWidgetId = null
        if (place != null) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.addThirdPartyWidget(place.first, place.second, place.third)
            } else {
                WidgetHost.deleteId(viewModel.applicationContext(), place.first)
            }
        }
        // A reconfigure lands in place on RESULT_OK and needs
        // nothing on any other result — the widget stays as it was.
    }
    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pending = pendingBind
        pendingBind = null
        val needsConfigure = pendingBindNeedsConfigure
        pendingBindNeedsConfigure = false
        if (pending != null && result.resultCode == android.app.Activity.RESULT_OK) {
            if (needsConfigure) {
                val ctx = viewModel.applicationContext()
                val info = WidgetHost.providerInfo(ctx, pending.first)
                val intent = info?.let { WidgetHost.configureIntent(ctx, pending.first, it) }
                if (intent != null) {
                    pendingConfigurePlace = pending
                    configureLauncher.launch(intent)
                } else {
                    viewModel.addThirdPartyWidget(pending.first, pending.second, pending.third)
                }
            } else {
                viewModel.addThirdPartyWidget(pending.first, pending.second, pending.third)
            }
        } else if (pending != null) {
            WidgetHost.deleteId(viewModel.applicationContext(), pending.first)
        }
    }
    // Decision F2 (home first): the single first-run default-home
    // prompt launches the RoleManager request (or the home settings
    // screen below API 29) through here. The result needs no
    // handling — the grant is the system's to record.
    val defaultHomeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { }
    // #6 the AppWidgetHost for third-party widgets. It listens while
    // the home is composed; host views are created from it in the grid.
    val localContext = LocalContext.current
    // #18 contacts permission for universal search. Rule 67 /
    // decision F22: it is NOT asked when search first opens any
    // more — a denial-free first use just omits contacts, and the
    // grant happens from Settings → Search (the settings stream
    // owns the priming-screen pattern). The launcher stays: Settings
    // calls it on user action.
    val contactsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.rerunSearch()
    }
    // Rule 67 / decision F22: NO permission is asked at launch.
    // The calendar permission is asked in context at first use (the
    // settings stream owns the priming-screen pattern); until it is
    // granted the strip shows date + weather only. The contacts
    // permission is the same: search simply omits contacts until
    // it is granted from Settings → Search. The launchers below
    // stay — Settings calls them on user action.
    val calendarPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.refreshGlance()
    }
    val widgetHost: AppWidgetHost = remember(localContext) { WidgetHost.host(localContext) }
    DisposableEffect(widgetHost) {
        try {
            widgetHost.startListening()
        } catch (e: Exception) {
            // No AppWidget service (e.g. under Robolectric in the
            // startup gate) — third-party widgets stay unavailable
            // rather than crashing the home.
        }
        onDispose {
            try {
                widgetHost.stopListening()
            } catch (e: Exception) {
                // Already stopped or no service; nothing to do.
            }
        }
    }
    var openFolder by remember { mutableStateOf<HomeFolder?>(null) }
    var pairFirst by remember { mutableStateOf<AppEntry?>(null) }
    var folderPickApp by remember { mutableStateOf<AppEntry?>(null) }
    // #95 the pinned contact being added to a home folder, from its
    // long-press sheet.
    var folderPickContact by remember { mutableStateOf<HomeItem?>(null) }
    // #95 pinned contacts, via the system contact picker. The dock
    // path remembers the contact was picked for the dock (one page,
    // decision F5 — no page picker any more).
    var pendingContactToDock by remember { mutableStateOf(false) }

    // Rule 58: one BackHandler per overlay, in bottom-to-top
    // priority — the topmost overlay's handler is composed last, so
    // it wins when several are open. Each close action mirrors the
    // overlay's existing onDismiss/onClose exactly. The settings
    // screen's own BackHandler (composed later, inside
    // SettingsScreen) wins while settings is open.
    // Rule 58 predictive back for the drawer: the drawer scales
    // toward ~90% as the back swipe progresses; cancelling springs
    // back to 100% with no state change; completing closes the
    // drawer (and clears the query, as every close site does).
    // This replaces the plain BackHandler — one callback for the
    // drawer. The swipe-down close and the close button keep their
    // own paths; they are not back gestures.
    val drawerBackProgress = remember { Animatable(0f) }
    LaunchedEffect(showDrawer) {
        if (showDrawer) drawerBackProgress.snapTo(0f)
    }
    PredictiveBackHandler(enabled = showDrawer) { progress ->
        try {
            progress.collect { event -> drawerBackProgress.snapTo(event.progress) }
            showDrawer = false
            viewModel.setDrawerQuery("")
            viewModel.clearSearch()
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                drawerBackProgress.animateTo(0f, Motion.predictiveBackCancelSpring())
            }
        }
    }
    BackHandler(enabled = showOverview) { showOverview = false }
    BackHandler(enabled = showSearch) {
        showSearch = false
        viewModel.clearSearch()
    }
    BackHandler(enabled = showVault) {
        showVault = false
        viewModel.lockVault()
        viewModel.clearPendingHide()
    }
    BackHandler(enabled = showNotifications) { showNotifications = false }
    BackHandler(enabled = showWallpaper) { showWallpaper = false }
    BackHandler(enabled = showNowBrief) { showNowBrief = false }
    BackHandler(enabled = showWidgetPicker) { showWidgetPicker = false }
    BackHandler(enabled = longPressWidget != null) { longPressWidget = null }
    BackHandler(enabled = showDesktopWidgets) { showDesktopWidgets = false }
    BackHandler(enabled = addingSticker) { addingSticker = false }
    BackHandler(enabled = editingSticker != null) { editingSticker = null }
    BackHandler(enabled = longPressApp != null) { longPressApp = null }
    BackHandler(enabled = longPressNonApp != null) { longPressNonApp = null }
    // (No BackHandler for the open folder here: FolderDialog owns
    // its own PredictiveBackHandler — one callback per overlay,
    // rule 58.)
    BackHandler(enabled = pairFirst != null) { pairFirst = null }
    BackHandler(enabled = folderPickApp != null) { folderPickApp = null }
    BackHandler(enabled = folderPickContact != null) { folderPickContact = null }

    val pickContact = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) {
            // Decision F5: a contact picked for the dock lands on
            // the home first, so the result — or the dock-full
            // refusal snackbar — is visible, not hidden behind the
            // overview.
            if (pendingContactToDock) showOverview = false
            viewModel.addPinnedContact(uri, null, toDock = pendingContactToDock)
        }
        pendingContactToDock = false
    }
    // #53 wallpaper picker: the system picker (ACTION_GET_CONTENT),
    // local images only — no built-in sources.
    val pickWallpaper = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.applyWallpaper(uri)
    }
    // (Rule 67: the old auto-fire LaunchedEffect that launched the
    // calendar permission whenever the strip was on is gone —
    // permissions are asked in context at first use only.)

    // B1: homePages is the FULL list (its indices are HomeItem.page);
    // the pager only shows the visible ones, so every place that maps
    // between pager slots and pages goes through this list.
    val visiblePages = homePages.filter { !it.hidden }.ifEmpty { homePages }
    val visibleFullIndices = homePages.indices
        .filter { !homePages[it].hidden }
        .ifEmpty { homePages.indices.toList() }
    val pageCount = visiblePages.size
    val defaultIndex = visibleFullIndices
        .indexOf(settings.defaultPage).coerceAtLeast(0)
        .coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    // Phase 3 pager pages: the RSS feed (#38) sits swipe-right of the
    // first home page and the widget page (#122) swipe-left of the
    // last one. Infinite scroll (#66) only spans the home pages, so
    // when either extra page is on, the pager is finite and the
    // extras take their fixed positions (recorded in BUILD_LOG.md).
    val showFeedPage = settings.feedEnabled
    val showWidgetPage = settings.widgetPageEnabled
    val feedOffset = if (showFeedPage) 1 else 0
    val useInfinite = settings.infiniteScroll && !showFeedPage && !showWidgetPage
    val totalPages = feedOffset + pageCount + (if (showWidgetPage) 1 else 0)
    // #66 infinite scroll: a large virtual page count, centred on the
    // default page, mapped back onto the real pages with modulo.
    val virtualCount = if (useInfinite) pageCount * 200 else totalPages
    val initialVirtual = if (useInfinite) {
        defaultIndex + 100 * pageCount
    } else {
        feedOffset + defaultIndex
    }
    val pagerState = rememberPagerState(
        initialPage = initialVirtual,
        pageCount = { virtualCount.coerceAtLeast(1) },
    )
    val pagerScope = rememberCoroutineScope()
    val currentPage = if (pageCount == 0) 0 else {
        if (useInfinite) {
            ((pagerState.currentPage % pageCount) + pageCount) % pageCount
        } else {
            (pagerState.currentPage - feedOffset).coerceIn(0, pageCount - 1)
        }
    }
    // Rule 29: empty trailing pages auto-remove when the user moves
    // off them (the ViewModel only prunes pages BEHIND the current
    // one, so a page just added from Overview survives until it is
    // visited or left behind).
    LaunchedEffect(currentPage) {
        viewModel.pruneEmptyTrailingPages(visibleFullIndices.getOrNull(currentPage) ?: 0)
    }
    // Rule 59: the pager settling on a page fires one page-snap
    // tick. The first emission is the initial page, not a snap.
    var firstPageSeen by remember { mutableStateOf(false) }
    LaunchedEffect(currentPage) {
        if (!firstPageSeen) {
            firstPageSeen = true
            return@LaunchedEffect
        }
        haptics.pageSnap()
    }
    // Rule 56: edge auto-scroll during a drag. Throttled to one
    // page move per 900 ms; dragging past the last page creates one
    // (rule 29) and scrolls onto it.
    var lastEdgeNavMs by remember { mutableStateOf(0L) }
    // §4.5 page move: 300 ms Emphasized decelerate. Under reduced
    // motion the move is instant (scrollToPage, rule 60).
    fun scrollPagerTo(page: Int) {
        if (reducedMotion) {
            pagerScope.launch { pagerState.scrollToPage(page) }
        } else {
            pagerScope.launch {
                pagerState.animateScrollToPage(page, animationSpec = Motion.pageMoveSpec())
            }
        }
    }
    fun scrollHomeBy(delta: Int) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastEdgeNavMs < 900L) return
        lastEdgeNavMs = nowMs
        if (useInfinite) {
            scrollPagerTo(pagerState.currentPage + delta)
            return
        }
        val slot = currentPage + delta
        if (slot < 0 || slot >= pageCount) {
            if (delta > 0 && currentPage == pageCount - 1) {
                viewModel.addPage()
                scrollPagerTo(feedOffset + pageCount)
            }
            return
        }
        scrollPagerTo(feedOffset + slot)
    }
    // Rule 22: the grid-migration notice, shown once as a snackbar.
    val gridNotice = viewModel.gridNotice.collectAsStateValue()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gridNotice) {
        val notice = gridNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.consumeGridNotice()
    }
    // Rule 41: a failed widget/stack placement is shown, never
    // dropped silently.
    val widgetAddError = viewModel.widgetAddError.collectAsStateValue()
    LaunchedEffect(widgetAddError) {
        val error = widgetAddError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.consumeWidgetAddError()
    }
    // Decision F5: a refused dock add is shown, never dropped
    // silently.
    val dockAddError = viewModel.dockAddError.collectAsStateValue()
    LaunchedEffect(dockAddError) {
        val error = dockAddError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.consumeDockAddError()
    }
    // Decision F2 (home first): the single first-run default-home
    // prompt, fired once. The ViewModel decides whether it is owed;
    // the result needs no handling.
    val defaultHomePromptPending = viewModel.defaultHomePromptPending.collectAsStateValue()
    LaunchedEffect(Unit) { viewModel.maybePromptDefaultHome() }
    LaunchedEffect(defaultHomePromptPending) {
        if (!defaultHomePromptPending) return@LaunchedEffect
        try {
            viewModel.defaultHomeIntent()?.let { defaultHomeLauncher.launch(it) }
        } catch (e: Exception) {
            // No activity can take the intent (e.g. under the
            // Robolectric startup gate); the prompt is simply skipped.
        }
        viewModel.consumeDefaultHomePrompt()
    }
    // Rule 36: the feed's calendar card is permission-gated.
    val glanceNeedsCalendarPermission = viewModel.glanceNeedsCalendarPermission.collectAsStateValue()
    val calendarConnected = settings.glanceCalendar && !glanceNeedsCalendarPermission
    val recentApps = remember(visibleApps) {
        visibleApps.filter { it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(5)
    }
    // Rule 28: a spoken query from the pill mic opens search with
    // the words already in it.
    var voiceQuery by remember { mutableStateOf("") }
    // Rule 51: long-press on empty home space opens Wallpaper &
    // style / Widgets / Home settings — and nothing else.
    var showEmptySpaceMenu by remember { mutableStateOf(false) }

    // Rule 44: bump the stack reset signal when the home regains
    // focus — every overlay closed again — so Smart Stacks return
    // to their default member.
    var stackResetSignal by remember { mutableStateOf(0) }
    val anyOverlayOpen = showDrawer || showOverview || showSearch || showVault ||
        showNotifications || showWallpaper || showNowBrief || showWidgetPicker ||
        longPressWidget != null || showDesktopWidgets || addingSticker ||
        editingSticker != null || longPressApp != null || longPressNonApp != null ||
        openFolder != null || pairFirst != null || folderPickApp != null ||
        folderPickContact != null || showSettings ||
        showDebug || showEmptySpaceMenu || headphoneMusicVisible
    var wasOverlayOpen by remember { mutableStateOf(false) }
    LaunchedEffect(anyOverlayOpen) {
        if (wasOverlayOpen && !anyOverlayOpen) stackResetSignal++
        wasOverlayOpen = anyOverlayOpen
    }

    // Rule 39: focus returns to the folder icon when the folder
    // closes — (folder id, bump) so the same folder closing twice
    // still re-requests focus. The grid owns the requester.
    var folderFocusRequest by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // B13(b): double/triple-tap fires only on empty space — a tap on
    // an icon or a widget must not also run the mapped action (its
    // default is Lock device). Each grid reports its items' rects in
    // pager-page coordinates, which are the gesture Box's coordinates
    // (the pager fills the Box). (Audit fix 2026-11-26: the drawer
    // FAB is gone — swipe up opens the drawer — so there is no FAB
    // rect in this hit-test any more.)
    var itemRectsByPage by remember { mutableStateOf<Map<Int, List<Rect>>>(emptyMap()) }
    // Which surface the pager is showing: a home page (its FULL
    // index), the widget page, or the feed page (null — a scrolling
    // list, where the empty-space gestures stay off).
    val currentSlotKey: Int? = when {
        !useInfinite && showFeedPage && pagerState.currentPage == 0 -> null
        !useInfinite && showWidgetPage && pagerState.currentPage == totalPages - 1 -> PAGE_WIDGET_PAGE
        else -> visibleFullIndices.getOrNull(currentPage) ?: 0
    }
    fun isEmptySpaceTap(offset: Offset): Boolean {
        val key = currentSlotKey ?: return false
        if (itemRectsByPage[key].orEmpty().any { it.contains(offset) }) return false
        return true
    }

    val resolveDrawable: (AppEntry) -> android.graphics.drawable.Drawable? =
        { app -> viewModel.drawableFor(app) }

    // Rules 13/14: the wallpaper's luminance, from the system
    // wallpaper colours (null when they cannot be read). The label
    // text colour and the scrim strength both follow it: strong
    // over bright art, fading to nothing over dark art, an ~8%
    // veil as fallback when no pixels can be read.
    val wallpaperVersion = viewModel.wallpaperVersion.collectAsStateValue()
    val wallpaperLuminance: Float? = remember(localContext, wallpaperVersion, settings.wallpaperVisible) {
        if (!settings.wallpaperVisible) null else wallpaperLuminanceOf(localContext)
    }
    val scrimAlpha: Float = when (val lum = wallpaperLuminance) {
        null -> 0.08f
        else -> when {
            lum < 0.30f -> 0f
            lum > 0.80f -> 0.45f
            else -> (lum - 0.30f) / 0.50f * 0.45f
        }
    }

    // Rule 29: the wallpaper parallaxes CONTINUOUSLY with the
    // finger — the fractional pager offset, not just the settled
    // page. WallpaperManager is called directly here (the helper
    // only takes whole pages).
    val localView = LocalView.current
    val wallpaperManager = remember(localContext) { WallpaperManager.getInstance(localContext) }
    LaunchedEffect(settings.scrollingWallpaper, settings.wallpaperVisible, pageCount, useInfinite, reducedMotion) {
        // Rule 60: reduced motion kills the parallax — the
        // wallpaper stays put instead of tracking the pager.
        if (!settings.scrollingWallpaper || !settings.wallpaperVisible || pageCount <= 1 || reducedMotion) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { raw ->
                val homePos = if (useInfinite) {
                    ((raw % pageCount) + pageCount) % pageCount
                } else {
                    (raw - feedOffset).coerceIn(0f, (pageCount - 1).toFloat())
                }
                val offset = (homePos / (pageCount - 1).toFloat()).coerceIn(0f, 1f)
                try {
                    wallpaperManager.setWallpaperOffsets(localView.windowToken, offset, 0f)
                } catch (e: Exception) {
                    // setWallpaperOffsets is best-effort on some devices.
                }
            }
    }

    // Rule 27 split tap targets: the date/event line opens the
    // system calendar; the weather line opens the web weather for
    // the chosen city (Marlow unified search's web leg).
    fun openCalendar() {
        try {
            localContext.startActivity(
                Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No calendar app — the tap simply does nothing.
        }
    }
    fun openWeather() {
        val city = settings.weatherCityName.ifBlank { "" }
        val query = if (city.isBlank()) "weather" else "weather $city"
        viewModel.launchUrl("https://www.google.com/search?q=" + Uri.encode(query))
    }

    fun tapItem(item: HomeItem) {
        when (item.kind) {
            HomeItemKind.APP -> item.component?.let { viewModel.launchApp(it) }
            HomeItemKind.FOLDER -> openFolder = item.folderId?.let { foldersById[it] }
            HomeItemKind.SHORTCUT -> {
                if (item.shortcutId != null && item.shortcutPackage != null) {
                    viewModel.launchShortcut(
                        AppShortcut(item.shortcutId, item.shortcutPackage, item.label ?: "Shortcut", null),
                    )
                }
            }
            HomeItemKind.CONTACT -> item.contactUri?.let { viewModel.openContact(it) }
            HomeItemKind.APP_PAIR -> item.pairId?.let { pairsById[it] }?.let { viewModel.launchAppPair(it) }
            // #58 tapping a sticker edits its text.
            HomeItemKind.STICKER -> editingSticker = item
            // Phase 3: widgets handle their own taps (the host view /
            // the built-in surface); tapping one is not a launcher
            // action. Long-press opens the widget sheet instead.
            HomeItemKind.WIDGET,
            HomeItemKind.BUILTIN_WIDGET,
            HomeItemKind.WIDGET_STACK -> Unit
        }
    }

    fun longPressItem(item: HomeItem) {
        when (item.kind) {
            HomeItemKind.APP -> {
                val app = item.component?.let { appsByComponent[it] }
                if (app != null) longPressApp = Pair(app, item)
            }
            // Phase 3: long-press on a widget or stack opens the
            // widget sheet (resize / stack / remove).
            HomeItemKind.WIDGET,
            HomeItemKind.BUILTIN_WIDGET,
            HomeItemKind.WIDGET_STACK -> longPressWidget = item
            else -> longPressNonApp = item
        }
    }

    // #53/#77 when a wallpaper is active the home surface is
    // transparent so the system wallpaper shows through, with the
    // dim / gradient overlay on top of it.
    val dimAlpha = settings.wallpaperDimPercent / 100f
    // Audit fix (2026-11-26): the default home backdrop — a subtle
    // warm vertical gradient behind the grid when no wallpaper is
    // set, slightly deeper warm tone at top and bottom, cream in
    // the middle, in the stock palette. This is the theme's default
    // backdrop, NOT a wallpaper source: no asset, no collection,
    // no picker entry — plan #53 denied built-in wallpaper sources
    // and this adds none. Stock / Ambient only (both use the stock
    // warm backgrounds); Material You keeps its flat dynamic
    // background. Solid cream remains for surfaces elsewhere.
    val homeBackdropBrush: Brush? =
        if (!settings.wallpaperVisible &&
            (settings.launcherTheme == LauncherThemeChoice.STOCK ||
                settings.launcherTheme == LauncherThemeChoice.AMBIENT)
        ) {
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        } else {
            null
        }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (settings.wallpaperVisible) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val backdrop = homeBackdropBrush
            if (backdrop != null) {
                Box(modifier = Modifier.fillMaxSize().background(backdrop))
            }
            // Rule 14: luminance-aware scrim under the label/glance
            // zones — strong over bright art, fading to nothing over
            // dark art, eased multi-stop so there is no visible band
            // or edge. The user's dim slider is a floor on top of it.
            if (settings.wallpaperVisible) {
                val gradientStops = if (settings.wallpaperGradient) {
                    listOf(0.9f, 0.45f, 0.35f, 0.45f, 0.9f)
                } else {
                    listOf(0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            gradientStops.map { factor ->
                                Color.Black.copy(alpha = (dimAlpha + scrimAlpha * factor).coerceAtMost(0.7f))
                            },
                        ),
                    ),
                )
            }
        // #152 two-pane tablet / foldable-inner layout: the home
        // column plus a second pane (feed page, else widget page).
        // On the Pixel 10a this branch never runs (see
        // AdaptiveLayout.kt) — the home is a single column there.
        Row(modifier = Modifier.fillMaxSize()) {
        // Audit fix (2026-11-26): the home column clears the status
        // bar at the top (the strip used to collide with it). The
        // dock column below carries the navigation-bar inset.
        Column(modifier = Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
            // #10 At a Glance strip, above the grid. Rule 27: text
            // directly on the wallpaper, no card; an upcoming event
            // replaces the date line; split tap targets (date →
            // Calendar, weather → weather); long-press → toggles.
            // #151 landscape: the strip is hidden in landscape so
            // the grid gets the room — the date is still on the lock
            // screen and in Now Brief.
            if (!isLandscape) {
                AtAGlanceStrip(
                    items = glanceItems,
                    settings = settings,
                    todayEvents = todayEvents,
                    onDateTap = { openCalendar() },
                    onWeatherTap = { openWeather() },
                    onToggleWeather = { enabled ->
                        viewModel.updateSettings { it.copy(glanceWeather = enabled) }
                    },
                    onToggleCalendar = { enabled ->
                        viewModel.updateSettings { it.copy(glanceCalendar = enabled) }
                    },
                    overWallpaper = settings.wallpaperVisible,
                    wallpaperLuminance = wallpaperLuminance,
                )
            }
            // #5 one-handed layout: the home content is pushed into
            // the lower half (dock at the very bottom, bottom rows in
            // the bottom third) when the toggle is on. Compressing
            // the whole grid into one third would make the icons
            // unusable, so the shift — not a shrink — is the
            // interpretation.
            if (settings.oneHanded) Spacer(modifier = Modifier.weight(0.5f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Phase 4 gestures (#29, #88, #89, #90, #91):
                    // pinch in/out, two-finger swipe / double-tap,
                    // double-tap / triple-tap on empty space. Each
                    // runs the action mapped for it in settings.
                    .multiFingerGestures(
                        enabled = settings.gesturesEnabled,
                        onPinchIn = { runGestureAction(settings.pinchInAction) },
                        onPinchOut = { runGestureAction(settings.pinchOutAction) },
                        onTwoFingerSwipe = { runGestureAction(settings.twoFingerSwipeAction) },
                        onTwoFingerDoubleTap = { runGestureAction(settings.twoFingerDoubleTapAction) },
                    )
                    .doubleTripleTap(
                        enabled = settings.gesturesEnabled && currentSlotKey != null,
                        onDoubleTap = { offset ->
                            if (isEmptySpaceTap(offset)) {
                                runGestureAction(settings.doubleTapAction)
                            }
                        },
                        onTripleTap = { offset ->
                            if (isEmptySpaceTap(offset)) {
                                runGestureAction(settings.tripleTapAction)
                            }
                        },
                    )
                    // Swipe up opens the drawer; swipe down opens the
                    // notifications panel (#26); long-press on empty
                    // space opens Wallpaper & style / Widgets / Home
                    // settings (rule 51).
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (total < -120f) {
                                    showDrawer = true
                                    // Rule 33: swipe up focuses the
                                    // drawer search field (with the
                                    // keyboard) when the toggle is on.
                                    drawerFocusSearch = settings.swipeUpToSearch
                                } else if (total > 120f) {
                                    runGestureAction(settings.swipeDownAction)
                                }
                                total = 0f
                            },
                            onVerticalDrag = { _, dragAmount -> total += dragAmount },
                        )
                    }
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showEmptySpaceMenu = true },
                    ),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { virtualPage ->
                    // Phase 3 extra pages (finite pager only).
                    if (!useInfinite && showFeedPage && virtualPage == 0) {
                        FeedPageScreen(
                            sources = feedSources,
                            items = feedItems,
                            refreshing = feedRefreshing,
                            error = feedError,
                            onRefresh = { viewModel.refreshFeed() },
                            onAddSource = { viewModel.addFeedSource(it) },
                            onRemoveSource = { viewModel.removeFeedSource(it) },
                            onOpenItem = { viewModel.openFeedItem(it) },
                            modifier = Modifier.fillMaxSize(),
                            // Pager copy: the home Column above already
                            // carries the status-bar inset.
                            topInset = false,
                            // Rule 36: personal context first.
                            todayEvents = todayEvents,
                            recentApps = recentApps,
                            calendarConnected = calendarConnected,
                            onOpenApp = { viewModel.launchApp(it.component) },
                            onOpenCalendar = { openCalendar() },
                            settings = settings,
                            resolveDrawable = resolveDrawable,
                        )
                        return@HorizontalPager
                    }
                    if (!useInfinite && showWidgetPage && virtualPage == totalPages - 1) {
                        WidgetPageScreen(
                            settings = settings,
                            viewModel = viewModel,
                            host = widgetHost,
                            items = homeItems.filter { it.page == PAGE_WIDGET_PAGE },
                            appsByComponent = appsByComponent,
                            folders = foldersById,
                            resolveDrawable = resolveDrawable,
                            onTapItem = { tapItem(it) },
                            onLongPressItem = { longPressItem(it) },
                            onAddWidget = {
                                widgetPickerPage = PAGE_WIDGET_PAGE
                                showWidgetPicker = true
                            },
                            onOpenDesktopWidgets = { showDesktopWidgets = true },
                            onItemRectsChanged = { rects ->
                                itemRectsByPage = itemRectsByPage + (PAGE_WIDGET_PAGE to rects)
                            },
                            modifier = Modifier.fillMaxSize(),
                            // Pager copy: the home Column above already
                            // carries the status-bar inset.
                            topInset = false,
                            stackResetSignal = stackResetSignal,
                        )
                        return@HorizontalPager
                    }
                    // B1: the pager slot is a VISIBLE index; the
                    // full page index is what HomeItem.page holds.
                    val realPage = if (useInfinite && pageCount > 0) {
                        val slot = ((virtualPage % pageCount) + pageCount) % pageCount
                        visibleFullIndices.getOrNull(slot) ?: 0
                    } else {
                        visibleFullIndices.getOrNull(virtualPage - feedOffset) ?: 0
                    }
                    val pageItems = homeItems.filter { it.page == realPage }
                    HomePageGrid(
                        pageIndex = realPage,
                        items = pageItems,
                        appsByComponent = appsByComponent,
                        folders = foldersById,
                        settings = settings,
                        viewModel = viewModel,
                        resolveDrawable = resolveDrawable,
                        onTapItem = { tapItem(it) },
                        onLongPressItem = { longPressItem(it) },
                        host = widgetHost,
                        todayEvents = todayEvents,
                        stacks = widgetStacks,
                        allItems = homeItems,
                        editingWidgetId = editingWidgetId,
                        badgeCounts = badgeCounts,
                        badgeBitmaps = badgeBitmaps,
                        swipeUpActions = swipeUpActions,
                        onSecondaryAction = { item ->
                            viewModel.performSecondaryAction(item) { app ->
                                longPressApp = Pair(app, null)
                            }
                        },
                        onItemRectsChanged = { rects ->
                            itemRectsByPage = itemRectsByPage + (realPage to rects)
                        },
                        // Rule 56: edge auto-scroll during a drag.
                        onDragNearEdge = { scrollHomeBy(it) },
                        overWallpaper = settings.wallpaperVisible,
                        wallpaperLuminance = wallpaperLuminance,
                        stackResetSignal = stackResetSignal,
                        focusFolderRequest = folderFocusRequest,
                    )
                }
                // Audit fix (2026-11-26): the drawer FAB is gone —
                // the drawer opens by swipe up, the stock gesture,
                // already implemented above. No floating button sits
                // on the home any more.
            }
            // #120 auto-hiding media player: only while something is
            // playing. Audit fix (2026-11-26): the persistent setup
            // hint that used to show here whenever the notification
            // listener was not granted is removed — it was a nag on
            // the home. The grant path stays reachable from
            // Settings → Notifications and the notification panel.
            if (settings.mediaPlayerEnabled) {
                val playing = nowPlaying
                if (playing != null && playing.playing) {
                    MediaPlayerBar(
                        nowPlaying = playing,
                        onPlay = { viewModel.mediaPlay() },
                        onPause = { viewModel.mediaPause() },
                        onNext = { viewModel.mediaNext() },
                        onPrevious = { viewModel.mediaPrevious() },
                    )
                }
            }
            // Audit fix (2026-11-26): the indicator reflects the real
            // pager — feed page + home pages + widget page — not just
            // the home pages. Finite pager: the current slot is the
            // pager's own current page (so the feed page lights the
            // first dot and the widget page the last). Infinite
            // scroll has no extra pages, so the home slots are the
            // pager as before. Dots / accent styling and the
            // phosphorGlow on the current dot are unchanged.
            val indicatorPageCount = if (useInfinite) pageCount else totalPages
            val indicatorCurrentPage = if (useInfinite) {
                currentPage
            } else {
                pagerState.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            }
            PageIndicator(
                style = settings.indicatorStyle,
                colour = settings.indicatorColour,
                pageCount = indicatorPageCount,
                currentPage = indicatorCurrentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            DockBar(
                dockItems = homeItems.filter { it.page == -1 },
                appsByComponent = appsByComponent,
                // Audit fix (2026-11-26): the #151 landscape
                // dockHeight=56 override is gone — a 56dp dock
                // re-clips the labels (see Dock.kt), so the dock is
                // the same height in landscape as in portrait.
                settings = settings,
                resolveDrawable = resolveDrawable,
                onTapItem = { tapItem(it) },
                onLongPressItem = { longPressItem(it) },
                // Rule 69: TalkBack custom actions on dock tiles —
                // Remove / Rename / Uninstall. Rename opens the
                // long-press sheet, where the rename dialog lives.
                onRemoveItem = { viewModel.removeHomeItem(it.id) },
                onRenameApp = { app -> longPressApp = Pair(app, null) },
                onUninstallApp = { viewModel.uninstallApp(it) },
                // #112 the pill opens universal search (#18) now.
                onSearchPillClick = { showSearch = true },
                badgeCounts = badgeCounts,
                badgeBitmaps = badgeBitmaps,
                // Rule 28: a spoken query opens search with the
                // words already in it.
                onVoiceQuery = { spoken ->
                    voiceQuery = spoken
                    showSearch = true
                },
                overWallpaper = settings.wallpaperVisible,
                wallpaperLuminance = wallpaperLuminance,
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        // #152 second pane (tablet / foldable inner only): the feed
        // page when it is on, otherwise the widget page.
        if (twoPane) {
            if (settings.feedEnabled) {
                FeedPageScreen(
                    sources = feedSources,
                    items = feedItems,
                    refreshing = feedRefreshing,
                    error = feedError,
                    onRefresh = { viewModel.refreshFeed() },
                    onAddSource = { viewModel.addFeedSource(it) },
                    onRemoveSource = { viewModel.removeFeedSource(it) },
                    onOpenItem = { viewModel.openFeedItem(it) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    todayEvents = todayEvents,
                    recentApps = recentApps,
                    calendarConnected = calendarConnected,
                    onOpenApp = { viewModel.launchApp(it.component) },
                    onOpenCalendar = { openCalendar() },
                    settings = settings,
                    resolveDrawable = resolveDrawable,
                )
            } else if (settings.widgetPageEnabled) {
                WidgetPageScreen(
                    settings = settings,
                    viewModel = viewModel,
                    host = widgetHost,
                    items = homeItems.filter { it.page == PAGE_WIDGET_PAGE },
                    appsByComponent = appsByComponent,
                    folders = foldersById,
                    resolveDrawable = resolveDrawable,
                    onTapItem = { tapItem(it) },
                    onLongPressItem = { longPressItem(it) },
                    onAddWidget = {
                        widgetPickerPage = PAGE_WIDGET_PAGE
                        showWidgetPicker = true
                    },
                    onOpenDesktopWidgets = { showDesktopWidgets = true },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    stackResetSignal = stackResetSignal,
                )
            }
        }
        }
        }
    }

    // Rule 31/54: the drawer enters/exits on the shared-axis z
    // transition (400 ms in / 200 ms out, §4.5). AnimatedVisibility
    // — not a bare `if` — so the exit animation plays; the content
    // is only composed while visible or animating. The back-preview
    // scale (rule 58) rides on top via graphicsLayer.
    AnimatedVisibility(
        visible = showDrawer,
        enter = Motion.drawerEnter(reducedMotion),
        exit = Motion.drawerExit(reducedMotion),
    ) {
        // Rule 72: the drawer query is cleared when the drawer
        // hides, at every close site — never a stale query on
        // reopen. Universal-search state clears with it.
        fun closeDrawer() {
            showDrawer = false
            viewModel.setDrawerQuery("")
            viewModel.clearSearch()
        }
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val backScale = 1f - (1f - Motion.PREDICTIVE_BACK_SCALE) * drawerBackProgress.value
                scaleX = backScale
                scaleY = backScale
            },
        ) {
        DrawerScreen(
            viewModel = viewModel,
            settings = settings,
            // #34 hidden apps are excluded from the drawer.
            apps = visibleApps,
            focusSearch = drawerFocusSearch,
            onClose = { closeDrawer() },
            onOpenSettings = { closeDrawer(); showSettings = true },
            onLaunchApp = { app ->
                viewModel.launchApp(app.component)
                if (settings.drawerAutoClose) closeDrawer()
            },
            onLongPressApp = { app -> longPressApp = Pair(app, null) },
            badgeCounts = badgeCounts,
            badgeBitmaps = badgeBitmaps,
        )
        }
    }

    if (showOverview) {
        OverviewScreen(
            pages = homePages,
            // B1: the overview takes FULL page indices; the pager's
            // current page is a visible slot.
            currentPage = visibleFullIndices.getOrNull(currentPage) ?: 0,
            overviewActions = settings.overviewActions,
            defaultPage = settings.defaultPage,
            onSetDefault = { viewModel.setDefaultPage(it) },
            onSetHidden = { index, hidden -> viewModel.setPageHidden(index, hidden) },
            onMovePage = { from, to -> viewModel.movePage(from, to) },
            onAddPage = { viewModel.addPage() },
            onDeletePage = { viewModel.deletePage(it) },
            onSelectPage = { index ->
                showOverview = false
                // B1: `index` is a FULL page index; convert it to a
                // pager slot before scrolling.
                val slot = visibleFullIndices.indexOf(index).coerceAtLeast(0)
                val target = if (useInfinite && pageCount > 0) {
                    pagerState.currentPage - (pagerState.currentPage % pageCount) + slot
                } else {
                    feedOffset + slot
                }
                pagerScope.launch { pagerState.scrollToPage(target) }
            },
            onClose = { showOverview = false },
            onAddContact = { pickContact.launch(null) },
            // #95 pin a contact to the dock: no page picker any
            // more (one page, decision F5). A full dock refuses up
            // front, before the contact picker opens, so the user
            // never picks a contact that cannot be placed; the
            // overview closes so the refusal snackbar is visible.
            onAddContactToDock = {
                if (viewModel.canAddToDock()) {
                    pendingContactToDock = true
                    pickContact.launch(null)
                } else {
                    showOverview = false
                    viewModel.refuseDockAdd()
                }
            },
            homeItems = homeItems,
            appsByComponent = appsByComponent,
            settings = settings,
            resolveDrawable = resolveDrawable,
            onOpenSettings = { showOverview = false; showSettings = true },
        )
    }

    // Phase 5 (#168): the one settings surface — full-screen, with
    // search. The old bottom-sheet copy is gone.
    if (showSettings) {
        SettingsScreen(
            settings = settings,
            iconPacks = iconPacks,
            viewModel = viewModel,
            initialArea = settingsInitialArea,
            onDismiss = { showSettings = false; settingsInitialArea = null },
            onOpenWallpaper = { showSettings = false; showWallpaper = true },
            onAddSticker = { showSettings = false; addingSticker = true },
            onExportBackup = { exportBackup.launch("launcher-backup.json") },
            onImportBackup = { importBackup.launch(arrayOf("application/json")) },
            onOpenVault = { showSettings = false; showVault = true },
            onOpenDebug = { showSettings = false; showDebug = true },
            onRequestContactsPermission = {
                contactsPermission.launch(Manifest.permission.READ_CONTACTS)
            },
            onRequestCalendarPermission = {
                calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            },
        )
    }

    // Phase 5 (#171): reset & debugging tools.
    if (showDebug) {
        DebugScreen(
            settings = settings,
            viewModel = viewModel,
            onDismiss = { showDebug = false },
        )
    }

    // #34 a hide request made before any PIN exists opens the vault
    // so the user can set one; the hide then completes.
    LaunchedEffect(pendingHideComponent) {
        if (pendingHideComponent != null) showVault = true
    }

    if (showVault) {
        VaultSheet(
            hiddenApps = viewModel.hiddenAppEntries(),
            hasPin = hasPin,
            unlocked = vaultUnlocked,
            onUnlock = { pin -> viewModel.verifyPinAndUnlock(pin) },
            onSetPin = { pin -> viewModel.setPin(pin) },
            onClearPin = { viewModel.clearPin() },
            onLaunchApp = { app ->
                viewModel.launchApp(app.component)
                showVault = false
            },
            onUnhideApp = { app -> viewModel.unhideApp(app.component) },
            onDismiss = { showVault = false; viewModel.lockVault(); viewModel.clearPendingHide() },
        )
    }

    // #127 inline notification previews + direct reply (toggle-gated
    // for the previews themselves; swipe-down opens the panel either
    // way, #26 — the panel says so when previews are off).
    if (showNotifications) {
        NotificationPanelSheet(
            previews = notificationPreviews,
            appsByPackage = appsByPackage,
            listenerConnected = mediaListenerConnected,
            onReply = { key, text -> viewModel.replyToNotification(key, text) },
            onOpenListenerSettings = { viewModel.openNotificationListenerSettings() },
            onDismiss = { showNotifications = false },
            previewsEnabled = settings.inlinePreviewsEnabled,
        )
    }

    // #18 universal search, opened by the dock pill (#112).
    if (showSearch) {
        UniversalSearchScreen(
            results = searchResults,
            initialQuery = voiceQuery,
            onQueryChange = { viewModel.runSearch(it) },
            onLaunchApp = { component ->
                viewModel.launchApp(component)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onOpenContact = { uri ->
                viewModel.openContact(uri)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onOpenFile = { uri ->
                viewModel.openFileUri(uri)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onOpenUrl = { url ->
                viewModel.launchUrl(url)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onDial = { number ->
                viewModel.dial(number)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onSetTimer = { seconds ->
                viewModel.setTimer(seconds)
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
            onOpenSettings = {
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
                showSettings = true
            },
            onClose = {
                showSearch = false
                voiceQuery = ""
                viewModel.clearSearch()
            },
        )
    }

    // Rule 51: long-press on empty home space — Wallpaper & style /
    // Widgets / Home settings, and nothing else (no debug, plan, or
    // developer entries in the user-facing menu).
    if (showEmptySpaceMenu) {
        AlertDialog(
            onDismissRequest = { showEmptySpaceMenu = false },
            title = { Text("Home screen") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showEmptySpaceMenu = false; showWallpaper = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Wallpaper & style", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            showEmptySpaceMenu = false
                            widgetPickerPage = visibleFullIndices.getOrNull(currentPage) ?: 0
                            showWidgetPicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Widgets", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = { showEmptySpaceMenu = false; showSettings = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Home settings", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmptySpaceMenu = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // #37 Now Brief morning summary (toggle-gated).
    if (showNowBrief) {
        NowBriefSheet(
            events = todayEvents,
            glanceItems = glanceItems,
            remindersAvailable = false,
            onDismiss = { showNowBrief = false },
        )
    }

    // #53 / #77 wallpaper picker + scrolling / gradient / dimming.
    if (showWallpaper) {
        WallpaperSheet(
            settings = settings,
            viewModel = viewModel,
            onPickImage = { pickWallpaper.launch("image/*") },
            onDismiss = { showWallpaper = false },
        )
    }

    // #58 add a sticker (from settings) or edit one (tap on it).
    if (addingSticker) {
        StickerDialog(
            initialText = "",
            onSave = { text ->
                // B1: stickers take a FULL page index, not the
                // pager's visible slot.
                viewModel.addSticker(text, visibleFullIndices.getOrNull(currentPage) ?: 0)
                addingSticker = false
            },
            onDelete = null,
            onDismiss = { addingSticker = false },
        )
    }
    val stickerToEdit = editingSticker
    if (stickerToEdit != null) {
        StickerDialog(
            initialText = stickerToEdit.label ?: "",
            onSave = { text ->
                viewModel.updateSticker(stickerToEdit.id, text)
                editingSticker = null
            },
            onDelete = {
                viewModel.removeHomeItem(stickerToEdit.id)
                editingSticker = null
            },
            onDismiss = { editingSticker = null },
        )
    }

    // #61 music apps on headphone connect (toggle-gated).
    if (headphoneMusicVisible) {
        HeadphoneMusicSheet(
            musicApps = musicApps,
            onLaunchApp = { app ->
                viewModel.launchApp(app.component)
                viewModel.dismissHeadphoneMusic()
            },
            onDismiss = { viewModel.dismissHeadphoneMusic() },
        )
    }

    val longPress = longPressApp
    if (longPress != null) {
        LongPressSheet(
            app = longPress.first,
            homeItem = longPress.second,
            settings = settings,
            viewModel = viewModel,
            onDismiss = { longPressApp = null },
            onPickPairPartner = { pairFirst = longPress.first },
            onPickDrawerFolder = { folderPickApp = longPress.first },
            onOpenWidgets = {
                longPressApp = null
                showWidgetPicker = true
                widgetPickerPage = PAGE_WIDGET_PAGE
            },
        )
    }

    val nonApp = longPressNonApp
    if (nonApp != null) {
        NonAppLongPressSheet(
            item = nonApp,
            viewModel = viewModel,
            onDismiss = { longPressNonApp = null },
            onPickFolder = { folderPickContact = it },
        )
    }

    // #95 the folder a pinned contact is being added to, from its
    // long-press sheet.
    val contactToFolder = folderPickContact
    if (contactToFolder != null && contactToFolder.contactUri != null) {
        HomeFolderPickerDialog(
            folders = homeFolders,
            onPick = { folderId ->
                viewModel.addContactToFolder(
                    folderId,
                    contactToFolder.contactUri,
                    contactToFolder.contactName ?: contactToFolder.label ?: "Contact",
                )
                folderPickContact = null
            },
            onDismiss = { folderPickContact = null },
        )
    }

    // #95 / #14 the open folder is looked up live, so adding or
    // removing a member refreshes the dialog instead of showing the
    // copy captured when it was tapped; a dissolved folder closes it.
    val folder = openFolder?.let { foldersById[it.id] }
    if (folder != null) {
        FolderDialog(
            folder = folder,
            appsByComponent = appsByComponent,
            settings = settings,
            viewModel = viewModel,
            resolveDrawable = resolveDrawable,
            onLaunchApp = { app ->
                viewModel.launchApp(app.component)
                openFolder = null
            },
            onDismiss = {
                // Rule 39: focus returns to the folder icon.
                folderFocusRequest = folder.id to ((folderFocusRequest?.second ?: 0) + 1)
                openFolder = null
            },
            onRenameApp = { app -> longPressApp = Pair(app, null) },
        )
    }

    // #96 pick the second app for an App Pair.
    val first = pairFirst
    if (first != null) {
        AppPickerDialog(
            title = "Pair “${first.displayLabel}” with…",
            apps = apps.filter { it.component != first.component },
            onPick = { second ->
                viewModel.createAppPair(first.component, second.component, "${first.displayLabel} + ${second.displayLabel}")
                pairFirst = null
            },
            onDismiss = { pairFirst = null },
        )
    }

    // #13 pick a drawer folder to add an app to.
    val folderApp = folderPickApp
    if (folderApp != null) {
        FolderPickerDialog(
            folders = drawerFolders,
            onPick = { folderId ->
                viewModel.addToDrawerFolder(folderId, folderApp.component)
                folderPickApp = null
            },
            onDismiss = { folderPickApp = null },
        )
    }

    // Phase 3: widget picker (#6, #113, #7). A third-party pick
    // allocates a host id and binds it; when the system wants the
    // user's confirmation, the bind flow runs; a provider with a
    // configure activity is configured before it lands, and the
    // widget is placed only on RESULT_OK.
    if (showWidgetPicker) {
        WidgetPickerDialog(
            providers = viewModel.installedWidgetProviders(),
            onPickBuiltin = { type ->
                viewModel.addBuiltinWidget(type, widgetPickerPage)
                showWidgetPicker = false
            },
            onPickProvider = { provider ->
                showWidgetPicker = false
                val appWidgetId = viewModel.allocateWidgetId()
                val ctx = viewModel.applicationContext()
                val configureIntent = if (WidgetHost.hasConfigureActivity(provider)) {
                    WidgetHost.configureIntent(ctx, appWidgetId, provider)
                } else {
                    null
                }
                val alreadyBound = viewModel.bindWidget(appWidgetId, provider.provider)
                if (configureIntent != null && alreadyBound) {
                    pendingConfigurePlace = Triple(appWidgetId, provider.provider, widgetPickerPage)
                    configureLauncher.launch(configureIntent)
                } else if (alreadyBound) {
                    viewModel.addThirdPartyWidget(appWidgetId, provider.provider, widgetPickerPage)
                } else {
                    pendingBind = Triple(appWidgetId, provider.provider, widgetPickerPage)
                    pendingBindNeedsConfigure = configureIntent != null
                    bindLauncher.launch(
                        WidgetHost.bindIntent(
                            viewModel.applicationContext(),
                            appWidgetId,
                            provider.provider,
                        ),
                    )
                }
            },
            onPickStack = {
                viewModel.createStack(widgetPickerPage)
                showWidgetPicker = false
            },
            onDismiss = { showWidgetPicker = false },
            gridColumns = settings.gridSize.effectiveCols(LocalConfiguration.current.screenWidthDp),
            canPlace = { spanX, spanY ->
                viewModel.firstFreeRect(homeItems, widgetPickerPage, spanX, spanY) != null
            },
        )
    }

    // Phase 3: long-press sheet for a placed widget or stack.
    val widgetItem = longPressWidget
    if (widgetItem != null) {
        WidgetItemSheet(
            item = widgetItem,
            stacks = widgetStacks,
            viewModel = viewModel,
            onResize = { viewModel.setEditingWidget(it.id) },
            onDismiss = { longPressWidget = null },
            // Rule 43: reconfigure in place — the widget is never
            // deleted and re-added for a configure change.
            onReconfigure = { item ->
                val ctx = viewModel.applicationContext()
                val info = WidgetHost.providerInfo(ctx, item.appWidgetId)
                val intent = info?.let { WidgetHost.configureIntent(ctx, item.appWidgetId, it) }
                if (intent != null) {
                    reconfigureWidgetId = item.appWidgetId
                    configureLauncher.launch(intent)
                }
            },
            // Rule 44: the sheet filters offered stacks to this
            // widget's footprint; the ViewModel enforces it too.
            stackItems = homeItems.filter { it.kind == HomeItemKind.WIDGET_STACK },
        )
    }

    // Phase 3: the hidden Desktop widget page (#122), reached only
    // from the widget page — never in the swipe sequence.
    if (showDesktopWidgets) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DesktopWidgetPageScreen(
                settings = settings,
                viewModel = viewModel,
                host = widgetHost,
                items = homeItems.filter { it.page == PAGE_DESKTOP_WIDGETS },
                appsByComponent = appsByComponent,
                folders = foldersById,
                resolveDrawable = resolveDrawable,
                onTapItem = { tapItem(it) },
                onLongPressItem = { longPressItem(it) },
                onAddWidget = {
                    widgetPickerPage = PAGE_DESKTOP_WIDGETS
                    showWidgetPicker = true
                },
                onClose = { showDesktopWidgets = false },
                modifier = Modifier.fillMaxSize(),
                stackResetSignal = stackResetSignal,
            )
        }
    }

    // Decision F2 (home first): no first-run wizard any more — the
    // user lands straight on the home screen, and the only
    // first-run prompt is the single system default-home prompt
    // (see the defaultHomePromptPending effect above).
}

/** Small app picker dialog, reused for App Pairs. */
@Composable
private fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    onPick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        .weight(1f, fill = false),
                ) {
                    items(apps, key = { it.component }) { app ->
                        TextButton(
                            onClick = { onPick(app) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = app.displayLabel,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                // #96 honesty: split-screen is the system's call and
                // cannot be forced from a launcher — the pair never
                // promises a split. One short sentence, no fake
                // success state.
                Text(
                    text = "If the system doesn't split them, both apps just open normally.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/** Small folder picker dialog, reused for "add to drawer folder". */
@Composable
private fun FolderPickerDialog(
    folders: List<DrawerFolder>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to drawer folder") },
        text = {
            if (folders.isEmpty()) {
                Text("No drawer folders yet — create one from the drawer first.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(folders, key = { it.id }) { folder ->
                        TextButton(
                            onClick = { onPick(folder.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = folder.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/** #95 pick a home folder to add a pinned contact to. */
@Composable
private fun HomeFolderPickerDialog(
    folders: List<HomeFolder>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to folder") },
        text = {
            if (folders.isEmpty()) {
                Text("No folders yet — create one by dropping an app onto another app.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(folders, key = { it.id }) { folder ->
                        TextButton(
                            onClick = { onPick(folder.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = folder.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Rules 13/14: the system wallpaper's relative luminance (WCAG
 * formula, via the theme's `relativeLuminance`), from the
 * wallpaper's primary colour. Null when the system reports no
 * wallpaper colours (live wallpapers, older devices, any failure)
 * — the caller falls back to the ~8% veil.
 */
private fun wallpaperLuminanceOf(context: Context): Float? {
    return try {
        val colors = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
        val primary = colors.primaryColor ?: return null
        relativeLuminance(Color(primary.toArgb()))
    } catch (e: Exception) {
        null
    }
}
