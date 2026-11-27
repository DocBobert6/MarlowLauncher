package app.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.launcher.model.AppCategory
import app.launcher.model.AppEntry
import app.launcher.model.DrawerFolder
import app.launcher.model.DrawerSort
import app.launcher.model.DrawerStyle
import app.launcher.model.LauncherSettings
import app.launcher.model.SearchBarPlacement
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * App drawer (rules 31-34, 9-10, 24, 50).
 *
 * Default: a vertical scrolling grid, alphabetical by default
 * (locale-aware collation), every installed app exactly once, home
 * order independent and never re-sorted. Letter sections use ragged
 * section-merge, so no lonely single-icon row sits under a letter
 * header. There are no package names in rows, no category tabs and
 * no "+ Folder" button in the default view, and no work tab
 * (work/personal separation was removed from the plan).
 *
 * Categories, custom order and drawer folders are kept as opt-in
 * non-default extensions, not stock behaviour: their code stays in
 * this file (DrawerFolderView, the CUSTOM sort branch) and they are
 * surfaced only when the drawer settings opt into them — see
 * rebuild-notes-drawer.md for the settings fields that gate them.
 * List mode and pages mode stay available as explicit non-default
 * options through the drawer style setting.
 *
 * The frequent row is the launcher's own launch history, labelled
 * plainly as local history — never as predictions. There is no
 * prediction service and no prediction row.
 *
 * Badges are NOT threaded through the drawer (rule 50): the
 * badgeCounts / badgeBitmaps parameters below are accepted only so
 * the existing call site keeps compiling, and are ignored.
 *
 * Swipe-down on the top handle closes the drawer (rule 31); Back
 * handling lives in HomeScreen (see the patch request in
 * rebuild-notes-drawer.md).
 */
@Composable
fun DrawerScreen(
    viewModel: HomeViewModel,
    settings: LauncherSettings,
    apps: List<AppEntry>,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    // Rule 50: accepted for call-site compatibility only — badges
    // are never rendered in the drawer, so these are ignored.
    badgeCounts: Map<String, Int> = emptyMap(),
    badgeBitmaps: Map<String, android.graphics.Bitmap?> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val query = viewModel.drawerQuery.collectAsStateValue()
    var openFolder by remember { mutableStateOf<DrawerFolder?>(null) }
    var uninstallMode by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(settings.searchBarPlacement == SearchBarPlacement.TOP) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Motion wave: the search field animates on the §4.5 drawer
    // search transition (300 ms); reduced motion collapses it.
    val reducedMotion = rememberReducedMotion()
    // Drawer folders are an opt-in extension (see the file header):
    // the folder chips below set openFolder when the drawer-folders
    // toggle is on; the default view has no folder affordance.
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val drawerFolders = viewModel.drawerFolders.collectAsStateValue()
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchVisible = true
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Rule 32: alphabetical by default, locale-aware collation.
    // Other sort orders are non-default extensions; the default
    // branch never re-sorts by anything but the name.
    val collator = remember { Collator.getInstance(Locale.getDefault()) }
    val customOrder = viewModel.drawerCustomOrder.collectAsStateValue()
    val filtered = remember(apps, query, settings.drawerSort, customOrder, collator) {
        val byQuery = if (query.isBlank()) apps else apps.filter {
            it.displayLabel.contains(query, ignoreCase = true)
        }
        when (settings.drawerSort) {
            DrawerSort.NAME -> byQuery.sortedWith(
                Comparator { a, b -> collator.compare(a.displayLabel, b.displayLabel) },
            )
            DrawerSort.INSTALL_DATE -> byQuery.sortedByDescending { it.installTime }
            DrawerSort.MOST_USED -> byQuery.sortedByDescending { it.launchCount }
            DrawerSort.SIZE -> byQuery.sortedByDescending { it.sizeBytes }
            DrawerSort.LAST_USED -> byQuery.sortedByDescending { it.lastUsed }
            DrawerSort.CUSTOM -> byQuery.sortedWith(
                compareBy<AppEntry> { app ->
                    val idx = customOrder.indexOf(app.component)
                    if (idx < 0) Int.MAX_VALUE else idx
                }.thenComparator { a, b -> collator.compare(a.displayLabel, b.displayLabel) },
            )
        }
    }
    // Rule 32 extension (opt-in, off by default): category tabs
    // filter the list. The tabs only render when the toggle is on;
    // the selection stays ALL otherwise.
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    val categoryFiltered = remember(filtered, selectedCategory) {
        if (selectedCategory == AppCategory.ALL) filtered
        else filtered.filter { viewModel.categorize(it) == selectedCategory }
    }
    val presentCategories = remember(filtered) {
        listOf(AppCategory.ALL) + AppCategory.values().filter { cat ->
            cat != AppCategory.ALL && filtered.any { viewModel.categorize(it) == cat }
        }
    }
    // Rule 34: the launcher's own launch history, off by default
    // (frequentRowVisible), labelled plainly as local history.
    val frequent = remember(apps) {
        apps.filter { it.launchCount > 0 }
            .sortedByDescending { it.launchCount }.take(6)
    }
    val recent = remember(apps) {
        apps.filter { it.lastUsed > 0 }
            .sortedByDescending { it.lastUsed }.take(6)
    }

    val searchField: @Composable () -> Unit = {
        // §4.5 drawer search transition (300 ms, T5 §6.1).
        AnimatedVisibility(
            visible = searchVisible,
            enter = Motion.drawerSearchEnter(reducedMotion),
            exit = Motion.drawerSearchExit(reducedMotion),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setDrawerQuery(it) },
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setDrawerQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
            )
        }
    }

    // Audit fix (2026-11-26): a full-screen surface, so it clears
    // both system bars itself.
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        // Rule 31: swipe down on the handle / header closes the
        // drawer. The drag detector lives on the header only, so it
        // never fights the grid's own scrolling.
        var headerDragTotal by remember { mutableStateOf(0f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { headerDragTotal = 0f },
                        onDragEnd = {
                            if (headerDragTotal > 120f) onClose()
                            headerDragTotal = 0f
                        },
                        onDragCancel = { headerDragTotal = 0f },
                        onVerticalDrag = { _, dragAmount -> headerDragTotal += dragAmount },
                    )
                },
        ) {
            // Pull-to-search: drag down on the handle to reveal search.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Apps",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Row {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Launcher settings")
                    }
                    IconButton(onClick = { uninstallMode = !uninstallMode }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Uninstall mode",
                            tint = if (uninstallMode) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close drawer")
                    }
                }
            }
        }
        if (settings.searchBarPlacement == SearchBarPlacement.TOP) searchField()
        // Rule 34: zero-state (empty query) shows the local launch
        // history, plainly labelled — never predictions.
        if (settings.frequentRowVisible && frequent.isNotEmpty() && query.isBlank()) {
            Text(
                "Frequently used",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
            Text(
                "Based on apps opened on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                items(frequent) { app ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                onLaunchApp(app)
                                if (settings.drawerAutoClose) onClose()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIconImage(
                            app = app, settings = settings,
                            resolveDrawable = { entry -> viewModel.drawableFor(entry) },
                        )
                        Text(
                            app.displayLabel, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.width(72.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        if (settings.frequentRowVisible && recent.isNotEmpty() && query.isBlank()) {
            Text(
                "Recently used",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
            Text(
                "Based on apps opened on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                items(recent) { app ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                onLaunchApp(app)
                                if (settings.drawerAutoClose) onClose()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIconImage(
                            app = app, settings = settings,
                            resolveDrawable = { entry -> viewModel.drawableFor(entry) },
                        )
                        Text(
                            app.displayLabel, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.width(72.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        if (settings.searchBarPlacement == SearchBarPlacement.BOTTOM) searchField()
        // Rule 32 extensions (opt-in, off by default): category tabs
        // above the list, drawer-folder chips below them. Neither
        // renders in the default view.
        if (settings.drawerShowCategories) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(presentCategories, key = { it.name }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                    )
                }
            }
        }
        if (settings.drawerShowDrawerFolders) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(drawerFolders, key = { it.id }) { folder ->
                    FilterChip(
                        selected = openFolder?.id == folder.id,
                        onClick = { openFolder = folder },
                        label = { Text(folder.name) },
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { newFolderName = ""; showNewFolderDialog = true },
                        label = { Text("+ Folder") },
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (openFolder != null) {
                DrawerFolderView(
                    folder = openFolder!!,
                    appsByComponent = apps.associateBy { it.component },
                    settings = settings,
                    viewModel = viewModel,
                    onLaunchApp = {
                        onLaunchApp(it)
                        if (settings.drawerAutoClose) onClose()
                    },
                    onBack = { openFolder = null },
                )
            } else when (settings.drawerStyle) {
                DrawerStyle.PAGES -> DrawerPages(
                    apps = categoryFiltered, settings = settings, viewModel = viewModel,
                    uninstallMode = uninstallMode,
                    onLaunchApp = {
                        onLaunchApp(it)
                        if (settings.drawerAutoClose) onClose()
                    },
                    onLongPressApp = onLongPressApp,
                )
                DrawerStyle.LIST -> DrawerList(
                    apps = categoryFiltered, settings = settings, viewModel = viewModel,
                    uninstallMode = uninstallMode,
                    onLaunchApp = {
                        onLaunchApp(it)
                        if (settings.drawerAutoClose) onClose()
                    },
                    onLongPressApp = onLongPressApp,
                )
                // Rule 32 default: the vertical scrolling grid.
                DrawerStyle.GRID -> DrawerGrid(
                    apps = categoryFiltered, settings = settings, viewModel = viewModel,
                    uninstallMode = uninstallMode,
                    onLaunchApp = {
                        onLaunchApp(it)
                        if (settings.drawerAutoClose) onClose()
                    },
                    onLongPressApp = onLongPressApp,
                )
            }
        }
    }

    // Rule 32 extension: create a drawer folder by name. The new
    // folder appears as a chip above; tapping it opens it.
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New drawer folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    placeholder = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotBlank()) {
                            viewModel.createDrawerFolder(name, emptyList())
                        }
                        showNewFolderDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            },
        )
    }
}

/** One entry in the ragged-merged drawer grid. */
private sealed interface DrawerGridEntry {
    data class Header(val letter: Char) : DrawerGridEntry
    data class App(val app: AppEntry) : DrawerGridEntry
}

/**
 * Rule 32 ragged section-merge: apps are grouped by first letter
 * (already collation-sorted). A section header is emitted only when
 * its section starts a row; a short section that starts mid-row, or
 * whose items share a row with the next section, gets no header of
 * its own — so no lonely single-icon row ever sits under a letter
 * header, except the final row of the whole list.
 */
private fun buildDrawerGridEntries(apps: List<AppEntry>, columns: Int): List<DrawerGridEntry> {
    val entries = mutableListOf<DrawerGridEntry>()
    val sections = apps.groupBy { app ->
        app.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
    }
    var rowFill = 0
    for ((letter, sectionApps) in sections) {
        var remaining = sectionApps
        var headerEmitted = false
        while (remaining.isNotEmpty()) {
            if (rowFill == 0 && !headerEmitted) {
                entries += DrawerGridEntry.Header(letter)
                headerEmitted = true
            }
            val space = columns - rowFill
            val take = minOf(space, remaining.size)
            for (app in remaining.take(take)) entries += DrawerGridEntry.App(app)
            remaining = remaining.drop(take)
            rowFill += take
            if (rowFill == columns) rowFill = 0
        }
    }
    return entries
}

/**
 * Rule 32 default drawer: a vertical scrolling grid with the
 * workspace column count, alphabetical, with ragged-merged letter
 * sections and one continuous alphabet fast scroller.
 */
@Composable
private fun DrawerGrid(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    uninstallMode: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val baseColumns = settings.gridSize.effectiveCols(screenWidthDp)
    val wide = settings.twoColumnDrawer && screenWidthDp > 600
    val columns = if (wide) (baseColumns * 2).coerceAtMost(8) else baseColumns
    val entries = remember(apps, columns) { buildDrawerGridEntries(apps, columns) }
    val letterIndex = remember(entries) {
        val map = LinkedHashMap<Char, Int>()
        entries.forEachIndexed { index, entry ->
            if (entry is DrawerGridEntry.Header) map.putIfAbsent(entry.letter, index)
        }
        map
    }
    val letters = remember(letterIndex) { letterIndex.keys.toList() }
    val initialIndex = if (settings.rememberDrawerPosition) viewModel.drawerScrollIndex.collectAsStateValue() else 0
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collectLatest { viewModel.setDrawerScrollIndex(it) }
    }
    var scrubLetter by remember { mutableStateOf<Char?>(null) }
    val scrollToLetter: (Char) -> Unit = { letter ->
        val index = letterIndex[letter]
        if (index != null) scope.launch { gridState.scrollToItem(index) }
    }
    val scrollToFraction: (Float) -> Unit = { fraction ->
        if (entries.isNotEmpty()) {
            val index = (fraction * entries.size).toInt().coerceIn(0, entries.size - 1)
            val entry = entries[index]
            val letter = when (entry) {
                is DrawerGridEntry.Header -> entry.letter
                is DrawerGridEntry.App ->
                    entry.app.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
            }
            scrubLetter = letter
            scrollToLetter(letter)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(end = 48.dp),
        ) {
            items(
                entries,
                key = { entry ->
                    when (entry) {
                        is DrawerGridEntry.Header -> "header:${entry.letter}"
                        is DrawerGridEntry.App -> entry.app.component
                    }
                },
                span = { entry ->
                    if (entry is DrawerGridEntry.Header) GridItemSpan(columns)
                    else GridItemSpan(1)
                },
            ) { entry ->
                when (entry) {
                    is DrawerGridEntry.Header -> Text(
                        entry.letter.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                    is DrawerGridEntry.App -> {
                        val app = entry.app
                        DrawerGridCell(
                            app = app, settings = settings, viewModel = viewModel,
                            uninstallMode = uninstallMode,
                            onLaunchApp = onLaunchApp, onLongPressApp = onLongPressApp,
                        )
                    }
                }
            }
        }
        // Rule 32: one continuous 48dp drag strip, not 26 tiny
        // targets. Dragging it scrubs through the letters; the
        // letter under the finger shows in a bubble.
        if (letters.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AlphabetScroller(
                    letters = letters,
                    firstVisibleIndex = gridState.firstVisibleItemIndex,
                    itemCount = entries.size,
                    onScrub = scrollToFraction,
                    onScrubEnd = { scrubLetter = null },
                    onPickLetter = scrollToLetter,
                    currentLetter = scrubLetter ?: run {
                        val first = entries.getOrNull(gridState.firstVisibleItemIndex)
                        when (first) {
                            is DrawerGridEntry.Header -> first.letter
                            is DrawerGridEntry.App ->
                                first.app.displayLabel.firstOrNull()?.uppercaseChar()
                            null -> null
                        }
                    },
                )
            }
        }
        if (scrubLetter != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        scrubLetter.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/** One cell of the drawer grid: home-sized icon, one 12sp label. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerGridCell(
    app: AppEntry,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    uninstallMode: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            // Rule 24: the same icon size as home (the settings
            // default, not a drawer-only size), no badge (rule 50).
            AppIconImage(
                app = app, settings = settings,
                resolveDrawable = { entry -> viewModel.drawableFor(entry) },
            )
            if (uninstallMode) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            viewModel.uninstallIntent(app.packageName),
                        )
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Uninstall ${app.displayLabel}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (settings.showLabels) {
            Text(
                app.displayLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Rule 32 alphabet fast scroller: a single continuous 48dp-wide
 * touch strip. One pointerInput handles finger-down and drag, so
 * there are no 26 separate tiny targets. TalkBack reaches it as
 * one element with previous/next-letter actions.
 */
@Composable
private fun AlphabetScroller(
    letters: List<Char>,
    firstVisibleIndex: Int,
    itemCount: Int,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onPickLetter: (Char) -> Unit,
    currentLetter: Char?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .width(48.dp)
            .semantics {
                contentDescription = "Alphabet scroller. Drag to jump to a letter."
                customActions = listOf(
                    CustomAccessibilityAction("Previous letter") {
                        val idx = letters.indexOf(currentLetter)
                        val prev = if (idx > 0) letters[idx - 1] else letters.firstOrNull()
                        if (prev != null) onPickLetter(prev)
                        prev != null
                    },
                    CustomAccessibilityAction("Next letter") {
                        val idx = letters.indexOf(currentLetter)
                        val next = if (idx >= 0 && idx < letters.lastIndex) letters[idx + 1]
                        else letters.firstOrNull()
                        if (next != null) onPickLetter(next)
                        next != null
                    },
                )
            }
            .pointerInput(letters) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            val fraction = (change.position.y / size.height)
                                .coerceIn(0f, 1f)
                            onScrub(fraction)
                            change.consume()
                        } else {
                            onScrubEnd()
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        if (itemCount > 0) {
            val thumbHeight = 48.dp
            val fraction = firstVisibleIndex.toFloat() / itemCount.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .padding(top = ((maxHeight - thumbHeight) * fraction).coerceAtLeast(0.dp))
                    .width(4.dp)
                    .height(thumbHeight)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

/**
 * List mode — kept as an explicit non-default option. No package
 * names in rows, one 12sp label role, no badges (rule 50).
 */
@Composable
private fun DrawerList(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    uninstallMode: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initialIndex = if (settings.rememberDrawerPosition) viewModel.drawerScrollIndex.collectAsStateValue() else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    val wide = settings.twoColumnDrawer &&
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp > 600
    LaunchedEffect(listState, gridState, wide) {
        if (wide) {
            snapshotFlow { gridState.firstVisibleItemIndex }
                .collectLatest { viewModel.setDrawerScrollIndex(it) }
        } else {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collectLatest { viewModel.setDrawerScrollIndex(it) }
        }
    }
    val letters = remember(apps) {
        apps.mapNotNull { it.displayLabel.firstOrNull()?.uppercaseChar() }
            .distinct().sorted()
    }
    var scrubLetter by remember { mutableStateOf<Char?>(null) }
    val scrollToLetter: (Char) -> Unit = { letter ->
        val index = apps.indexOfFirst {
            it.displayLabel.firstOrNull()?.uppercaseChar() == letter
        }
        if (index >= 0) {
            scope.launch {
                if (wide) gridState.scrollToItem(index) else listState.scrollToItem(index)
            }
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (wide) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(end = 48.dp),
            ) {
                items(apps, key = { it.component }) { app ->
                    DrawerRow(
                        app = app, settings = settings, viewModel = viewModel,
                        uninstallMode = uninstallMode,
                        onLaunchApp = onLaunchApp, onLongPressApp = onLongPressApp,
                    )
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 48.dp)) {
                items(apps, key = { it.component }) { app ->
                    DrawerRow(
                        app = app, settings = settings, viewModel = viewModel,
                        uninstallMode = uninstallMode,
                        onLaunchApp = onLaunchApp, onLongPressApp = onLongPressApp,
                    )
                }
            }
        }
        if (letters.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AlphabetScroller(
                    letters = letters,
                    firstVisibleIndex = if (wide) gridState.firstVisibleItemIndex
                    else listState.firstVisibleItemIndex,
                    itemCount = apps.size,
                    onScrub = { fraction ->
                        val index = (fraction * apps.size).toInt()
                            .coerceIn(0, (apps.size - 1).coerceAtLeast(0))
                        val letter = apps.getOrNull(index)
                            ?.displayLabel?.firstOrNull()?.uppercaseChar()
                        if (letter != null) {
                            scrubLetter = letter
                            scrollToLetter(letter)
                        }
                    },
                    onScrubEnd = { scrubLetter = null },
                    onPickLetter = scrollToLetter,
                    currentLetter = scrubLetter,
                )
            }
        }
        if (scrubLetter != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        scrubLetter.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/** Horizontal-pages drawer mode — kept as an explicit non-default option. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerPages(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    uninstallMode: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val perPage = 20
    val pageCount = maxOf(1, (apps.size + perPage - 1) / perPage)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val pageApps = apps.drop(page * perPage).take(perPage)
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()) {
                items(pageApps, key = { it.component }) { app ->
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            AppIconImage(
                                app = app, settings = settings,
                                resolveDrawable = { entry -> viewModel.drawableFor(entry) },
                            )
                            if (uninstallMode) {
                                IconButton(
                                    onClick = {
                                        context.startActivity(
                                            viewModel.uninstallIntent(app.packageName),
                                        )
                                    },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Uninstall ${app.displayLabel}",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        if (settings.showLabels) {
                            Text(
                                app.displayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        PageIndicator(
            style = settings.indicatorStyle, colour = settings.indicatorColour,
            pageCount = pageCount, currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(
    app: AppEntry,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    uninstallMode: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rule 24: home-sized icon, no badge (rule 50).
        AppIconImage(
            app = app, settings = settings,
            resolveDrawable = { entry -> viewModel.drawableFor(entry) },
        )
        Text(
            app.displayLabel,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        if (uninstallMode) {
            IconButton(onClick = {
                context.startActivity(viewModel.uninstallIntent(app.packageName))
            }) {
                Icon(Icons.Filled.Close, contentDescription = "Uninstall ${app.displayLabel}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Drawer folder contents, with in-folder reorder. This is the
 * drawer-folders extension (opt-in, not the default view): it is
 * reached only when the drawer settings opt into drawer folders —
 * see rebuild-notes-drawer.md. No badges here either (rule 50).
 */
@Composable
private fun DrawerFolderView(
    folder: DrawerFolder,
    appsByComponent: Map<String, AppEntry>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    onLaunchApp: (AppEntry) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Back")
            }
            Text(
                folder.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.deleteDrawerFolder(folder.id); onBack() }) {
                Text("Delete folder")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(folder.apps.size) { index ->
                val component = folder.apps[index]
                val app = appsByComponent[component] ?: return@items
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(
                        app = app, settings = settings,
                        resolveDrawable = { entry -> viewModel.drawableFor(entry) },
                    )
                    Text(
                        app.displayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(start = 12.dp).clickable { onLaunchApp(app) },
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { viewModel.reorderInDrawerFolder(folder.id, index, index - 1) },
                        enabled = index > 0,
                    ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { viewModel.reorderInDrawerFolder(folder.id, index, index + 1) },
                        enabled = index < folder.apps.size - 1,
                    ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down") }
                    IconButton(onClick = { viewModel.removeFromDrawerFolder(folder.id, component) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove from folder")
                    }
                }
            }
        }
    }
}

/** Small helper: collect a StateFlow as state in a composable. */
@Composable
fun <T> StateFlow<T>.collectAsStateValue(): T = collectAsState().value
