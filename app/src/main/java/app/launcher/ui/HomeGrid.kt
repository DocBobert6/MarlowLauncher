package app.launcher.ui

import android.appwidget.AppWidgetHost
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.launcher.data.appIconDescription
import app.launcher.data.decorative
import app.launcher.data.folderDescription
import app.launcher.model.AppEntry
import app.launcher.model.CalendarEvent
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.IndicatorColour
import app.launcher.model.LauncherSettings
import app.launcher.model.MAX_HORIZONTAL_MARGIN_PERCENT
import app.launcher.model.PAGE_IN_STACK
import app.launcher.model.PageIndicatorStyle
import app.launcher.model.ShortcutSecondaryAction
import app.launcher.model.WidgetStack
import app.launcher.ui.theme.phosphorGlow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * One home page: items placed on the grid with sub-grid half-cell
 * positions (#62), grid margin/spacing (#64), long-press-drag to move,
 * drop an app onto an app to make a folder (#14). Desktop lock (#65)
 * disables moving. Phase 3: also renders placed widgets (#6), the
 * built-in suite (#113) and Smart Stacks (#7); [allItems] carries
 * the full home list so stack members (page == PAGE_IN_STACK) can
 * be found — [items] is only this page's slice.
 *
 * Rule 56 drag system: long-press at the system timeout (Compose
 * `detectDragGesturesAfterLongPress`); a 650 ms reorder hover before
 * neighbours slide aside (the swap is committed live, so the grid
 * re-flows under the finger); the dragged tile scales up on a
 * spring while its source cell shows a shrunken hole (never a
 * duplicate); edge auto-scroll via [onDragNearEdge] (the home
 * scrolls the pager, and can create a page); drag-to-top Remove /
 * Uninstall (left half removes, right half uninstalls).
 *
 * Rules 37/40 folders: dropping an icon onto a folder ADDS it;
 * hovering a drag over a closed folder opens it after 800 ms;
 * the hovered target shows a ring while dragging.
 */
@Composable
fun HomePageGrid(
    pageIndex: Int,
    items: List<HomeItem>,
    appsByComponent: Map<String, AppEntry>,
    folders: Map<String, HomeFolder>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    resolveDrawable: (AppEntry) -> Drawable?,
    onTapItem: (HomeItem) -> Unit,
    onLongPressItem: (HomeItem) -> Unit,
    host: AppWidgetHost,
    todayEvents: List<CalendarEvent> = emptyList(),
    stacks: List<WidgetStack> = emptyList(),
    allItems: List<HomeItem> = items,
    editingWidgetId: String? = null,
    // Phase 4: notification badges (#31, keyed by package name) +
    // dynamic-badge artwork (#128), and swipe-up secondary actions
    // (#28, keyed by home-item id).
    badgeCounts: Map<String, Int> = emptyMap(),
    badgeBitmaps: Map<String, Bitmap?> = emptyMap(),
    swipeUpActions: Map<String, ShortcutSecondaryAction> = emptyMap(),
    onSecondaryAction: (HomeItem) -> Unit = {},
    // The home reports each item's rect (in grid coordinates, pixels)
    // so the double/triple-tap handler can tell a tap on an icon
    // from a tap on empty space (Gestures.kt). Computed from the
    // placement math itself — no layout callbacks needed.
    onItemRectsChanged: (List<Rect>) -> Unit = {},
    // Rule 56: the dragged item nears a horizontal edge; the home
    // scrolls the pager (and may create a page). -1 = left, +1 = right.
    onDragNearEdge: (Int) -> Unit = {},
    // Rules 13/14: labels over wallpaper use the luminance-aware
    // text colour + scrim (see HomeScreen).
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
    // Rule 44: the home bumps this when it regains focus; stacks
    // reset to their default member.
    stackResetSignal: Int = 0,
    // Rule 39: (folder id, bump) — focus returns to the folder icon
    // when the folder closes.
    focusFolderRequest: Pair<String, Int>? = null,
    modifier: Modifier = Modifier,
) {
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragAnchor by remember { mutableStateOf(0 to 0) }
    var hoverTargetId by remember { mutableStateOf<String?>(null) }
    var zoneHover by remember { mutableStateOf<Int?>(null) }
    val locked = settings.desktopLocked
    // Motion wave: the drag scale runs on the Motion spring, and
    // the drag haptics (rule 59) fire from this file's callbacks.
    // Both read from composition — no signature change.
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalMarlowHaptics.current
    // Rule 56: the dragged tile scales up on a spring; the source
    // cell shows a shrunken hole instead of a duplicate.
    val dragScale by animateFloatAsState(
        targetValue = if (draggingId != null) 1.08f else 1f,
        animationSpec = Motion.dragLiftSpring(reducedMotion),
        label = "dragScale",
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Rule 23: horizontal margin is capped at 14% of screen width.
        val maxMarginPx = (constraints.maxWidth * MAX_HORIZONTAL_MARGIN_PERCENT).roundToInt()
        val marginPx = minOf(with(density) { settings.gridMarginDp.dp.roundToPx() }, maxMarginPx)
        val spacingPx = with(density) { settings.gridSpacingDp.dp.roundToPx() }
        val grid = settings.gridSize
        // #1 FLUID derives its columns from the screen width (see
        // GridSize.effectiveCols); the ViewModel placement math uses
        // the same formula, so the two always agree.
        val cols = grid.effectiveCols(LocalConfiguration.current.screenWidthDp)
        val halfCellW = (constraints.maxWidth - marginPx * 2) / (cols * 2).toFloat()
        val halfCellH = (constraints.maxHeight - marginPx * 2) / (grid.rows * 2).toFloat()
        val zoneHeightPx = with(density) { 64.dp.roundToPx() }
        val edgePx = with(density) { 28.dp.roundToPx() }
        // The drag callbacks below run in a PointerInputScope, which
        // cannot see BoxWithConstraints' implicit receiver — capture
        // the width as a plain value first.
        val gridWidthPx = constraints.maxWidth
        // #5 one-handed: pack the rows to the BOTTOM of the pager
        // area — shift every item down by the empty rows below the
        // lowest occupied row, so the bottom row sits on the pager's
        // bottom edge (just above the dock) instead of the rows
        // laying out from the top. The shift is display-only: drag
        // math works from model cells plus the drag delta, so it
        // needs no adjustment. Off when one-handed is off.
        val bottomShiftHalfCells = if (settings.oneHanded) {
            val lowestBottom = items.maxOfOrNull { it.subY + it.spanY } ?: 0
            (grid.rows * 2 - lowestBottom).coerceAtLeast(0)
        } else {
            0
        }
        val bottomShiftPx = bottomShiftHalfCells * halfCellH
        // Item rects, from the same numbers the layout below uses.
        val rects = remember(items, halfCellW, halfCellH, marginPx, spacingPx, bottomShiftPx) {
            items.map { item ->
                Rect(
                    Offset(
                        marginPx + item.subX * halfCellW + spacingPx / 2f,
                        marginPx + item.subY * halfCellH + spacingPx / 2f + bottomShiftPx,
                    ),
                    Size(
                        (item.spanX * halfCellW - spacingPx).coerceAtLeast(1f),
                        (item.spanY * halfCellH - spacingPx).coerceAtLeast(1f),
                    ),
                )
            }
        }
        LaunchedEffect(rects) { onItemRectsChanged(rects) }
        // Rules 40/56: hover timers. A folder opens after 800 ms of
        // hover (rule 40); an app target swaps live after 650 ms of
        // hover (rule 56) — the grid re-flows under the finger.
        // The checks re-read the live ids after the delay, so a
        // drag that moved on does nothing. The item list is read
        // through rememberUpdatedState so a live swap during the
        // wait does not leave the timer working on stale cells.
        val latestItems by rememberUpdatedState(items)
        LaunchedEffect(hoverTargetId, draggingId) {
            val targetId = hoverTargetId
            val dragId = draggingId
            if (targetId == null || dragId == null) return@LaunchedEffect
            val target = latestItems.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
            val dragged = latestItems.firstOrNull { it.id == dragId } ?: return@LaunchedEffect
            if (target.kind == HomeItemKind.FOLDER && dragged.kind == HomeItemKind.APP) {
                delay(800)
                if (hoverTargetId == targetId && draggingId == dragId) {
                    // Rule 59: crossing the folder threshold fires
                    // one threshold haptic with the open.
                    haptics.threshold()
                    onTapItem(target)
                }
            } else if (target.kind == HomeItemKind.APP && dragged.kind == HomeItemKind.APP) {
                delay(650)
                if (hoverTargetId == targetId && draggingId == dragId) {
                    viewModel.moveHomeItem(dragId, pageIndex, target.subX, target.subY)
                    // Rule 59: neighbours sliding aside is a drag
                    // snap — one quiet tick.
                    haptics.dragSnap()
                    dragAnchor = target.subX to target.subY
                    dragOffset = Offset.Zero
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            for (item in items) {
                val app = item.component?.let { appsByComponent[it] }
                val folder = if (item.kind == HomeItemKind.FOLDER) {
                    item.folderId?.let { folders[it] }
                } else {
                    null
                }
                val isDragging = draggingId == item.id
                // The dragged tile follows the anchor + drag delta;
                // everything else sits on its model cell.
                val baseSubX = if (isDragging) dragAnchor.first else item.subX
                val baseSubY = if (isDragging) dragAnchor.second else item.subY
                val extra = if (isDragging) dragOffset else Offset.Zero
                // #64 the grid-spacing slider insets every tile by
                // half the spacing on each side.
                val xPx = marginPx + baseSubX * halfCellW + spacingPx / 2f + extra.x
                val yPx = marginPx + baseSubY * halfCellH + spacingPx / 2f + bottomShiftPx + extra.y
                val widthPx = (item.spanX * halfCellW - spacingPx).roundToInt().coerceAtLeast(1)
                val heightPx = (item.spanY * halfCellH - spacingPx).roundToInt().coerceAtLeast(1)
                // Rule 56 source-cell hole: while dragging, the
                // original cell shows a shrunken placeholder — the
                // grid keeps its shape and there is no duplicate.
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (marginPx + dragAnchor.first * halfCellW + spacingPx / 2f).roundToInt(),
                                    (marginPx + dragAnchor.second * halfCellH + spacingPx / 2f + bottomShiftPx).roundToInt(),
                                )
                            }
                            .size(
                                width = with(density) { widthPx.toDp() },
                                height = with(density) { heightPx.toDp() },
                            )
                            .graphicsLayer(scaleX = 0.92f, scaleY = 0.92f)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium,
                            ),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .size(
                            width = with(density) { widthPx.toDp() },
                            height = with(density) { heightPx.toDp() },
                        )
                        // Rule 42: a widget span change snaps over
                        // 150 ms (Motion.widgetResizeSnapSpec). App
                        // icons never change size, so only widget
                        // kinds get the animated size.
                        .then(
                            if (item.kind == HomeItemKind.WIDGET ||
                                item.kind == HomeItemKind.BUILTIN_WIDGET ||
                                item.kind == HomeItemKind.WIDGET_STACK
                            ) {
                                Modifier.animateContentSize(
                                    Motion.widgetResizeSnapSpec(reducedMotion),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .graphicsLayer(
                            scaleX = if (isDragging) dragScale else 1f,
                            scaleY = if (isDragging) dragScale else 1f,
                        )
                        // Rule 37/40: hover ring on the drag target.
                        .then(
                            if (draggingId != null && hoverTargetId == item.id && !isDragging) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shapeFor(settings.iconShape, settings.iconSizeDp.dp))
                            } else {
                                Modifier
                            },
                        )
                        // Rule 69: TalkBack announces the tile on its
                        // actionable node — "Chrome, page 1,
                        // position 3" for apps, "Social, folder,
                        // 6 apps" for folders. The description lives
                        // here (not on the tile's inner Column) so
                        // the tap action and the announcement are
                        // one node.
                        .then(
                            when {
                                item.kind == HomeItemKind.APP && app != null -> Modifier.semantics {
                                    contentDescription = appIconDescription(
                                        app.displayLabel,
                                        pageIndex + 1,
                                        items.filter { it.kind == HomeItemKind.APP }.indexOf(item) + 1,
                                    )
                                }
                                item.kind == HomeItemKind.FOLDER && folder != null -> Modifier.semantics {
                                    contentDescription = folderDescription(
                                        folder.name,
                                        folder.apps.size,
                                    )
                                }
                                else -> Modifier
                            },
                        )
                        .pointerInput(item.id, locked) {
                            if (locked) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    // Rule 59: the haptic fires AT the
                                    // long-press moment — drag starts
                                    // only after it.
                                    haptics.longPressPickup()
                                    draggingId = item.id
                                    dragOffset = Offset.Zero
                                    dragAnchor = item.subX to item.subY
                                    hoverTargetId = null
                                    zoneHover = null
                                },
                                onDragEnd = {
                                    val dragged = item
                                    val total = dragOffset
                                    val anchor = dragAnchor
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                    hoverTargetId = null
                                    zoneHover = null
                                    if (total.getDistance() < 24f) {
                                        onLongPressItem(dragged)
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    val newSubX = ((marginPx + anchor.first * halfCellW + total.x - marginPx) / halfCellW).roundToInt()
                                    val newSubY = ((marginPx + anchor.second * halfCellH + total.y - marginPx) / halfCellH).roundToInt()
                                    // Rule 56 drag-to-top: dropping in
                                    // the top zone removes (left half)
                                    // or uninstalls (right half).
                                    val dropTopY = marginPx + anchor.second * halfCellH + total.y + bottomShiftPx
                                    val dropCentreX = marginPx + (anchor.first + dragged.spanX / 2f) * halfCellW + total.x
                                    if (dropTopY < zoneHeightPx) {
                                        if (dropCentreX < gridWidthPx / 2f) {
                                            viewModel.removeHomeItem(dragged.id)
                                        } else if (dragged.kind == HomeItemKind.APP) {
                                            val pkg = appsByComponent[dragged.component]?.packageName
                                            if (pkg != null) viewModel.uninstallApp(pkg)
                                            else viewModel.removeHomeItem(dragged.id)
                                        } else {
                                            viewModel.removeHomeItem(dragged.id)
                                        }
                                        // Rule 59: the drop lands.
                                        haptics.drop()
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    // Drop onto an app makes a folder
                                    // (#14); onto a folder ADDS to it
                                    // (rule 37 — the old code only
                                    // matched APP targets).
                                    if (dragged.kind == HomeItemKind.APP && dragged.component != null) {
                                        val target = items.firstOrNull { other ->
                                            other.id != dragged.id &&
                                                newSubX in other.subX until other.subX + other.spanX &&
                                                newSubY in other.subY until other.subY + other.spanY
                                        }
                                        if (target != null && target.kind == HomeItemKind.FOLDER && target.folderId != null) {
                                            viewModel.addAppToFolder(dragged.component, target.folderId)
                                            haptics.drop()
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        if (target != null && target.kind == HomeItemKind.APP) {
                                            viewModel.createFolderFromDrop(dragged.component, target)
                                            haptics.drop()
                                            return@detectDragGesturesAfterLongPress
                                        }
                                    }
                                    viewModel.moveHomeItem(dragged.id, pageIndex, newSubX, newSubY)
                                    haptics.drop()
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                    hoverTargetId = null
                                    zoneHover = null
                                },
                                onDrag = { _, dragAmount ->
                                    dragOffset += dragAmount
                                    // Hover target under the dragged
                                    // cell's centre, for the ring and
                                    // the 650/800 ms timers.
                                    val curSubX = (dragAnchor.first + (dragOffset.x / halfCellW).roundToInt())
                                        .coerceIn(0, cols * 2 - 1)
                                    val curSubY = (dragAnchor.second + (dragOffset.y / halfCellH).roundToInt())
                                        .coerceIn(0, grid.rows * 2 - 1)
                                    hoverTargetId = items.firstOrNull { other ->
                                        other.id != item.id &&
                                            curSubX in other.subX until other.subX + other.spanX &&
                                            curSubY in other.subY until other.subY + other.spanY
                                    }?.id
                                    // Remove/uninstall zone hover.
                                    val dropTopY = marginPx + dragAnchor.second * halfCellH + dragOffset.y + bottomShiftPx
                                    val dropCentreX = marginPx + (dragAnchor.first + item.spanX / 2f) * halfCellW + dragOffset.x
                                    zoneHover = if (dropTopY < zoneHeightPx) {
                                        if (dropCentreX < gridWidthPx / 2f) -1 else 1
                                    } else {
                                        null
                                    }
                                    // Edge auto-scroll (rule 56): the
                                    // home throttles and scrolls the
                                    // pager, creating a page at the end.
                                    val centreX = marginPx + (dragAnchor.first + item.spanX / 2f) * halfCellW + dragOffset.x
                                    if (centreX < edgePx) {
                                        onDragNearEdge(-1)
                                    } else if (centreX > gridWidthPx - edgePx) {
                                        onDragNearEdge(1)
                                    }
                                },
                            )
                        }
                        .clickable { onTapItem(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.kind) {
                        // Plain (non-clickable) icon column here: the outer
                        // Box owns tap (clickable) and long-press-drag, so
                        // an inner clickable would fire every tap twice.
                        HomeItemKind.APP -> if (app != null) {
                            // #28 swipe-up secondary action: only when
                            // one is mapped for this item — otherwise
                            // the gesture passes through to the home's
                            // swipe-up-for-drawer handler untouched.
                            val secondary = swipeUpActions[item.id]
                                ?: ShortcutSecondaryAction.NONE
                            val swipeModifier = if (secondary == ShortcutSecondaryAction.NONE) {
                                Modifier
                            } else {
                                Modifier.pointerInput(item.id, secondary) {
                                    var total = 0f
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (total < -80f) onSecondaryAction(item)
                                            total = 0f
                                        },
                                        onVerticalDrag = { _, dragAmount -> total += dragAmount },
                                    )
                                }
                            }
                            Column(
                                modifier = swipeModifier,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppIconImage(
                                    app = app, settings = settings,
                                    resolveDrawable = resolveDrawable,
                                    badgeCount = badgeCounts[app.packageName] ?: 0,
                                    badgeBitmap = badgeBitmaps[app.packageName],
                                    overWallpaper = overWallpaper,
                                    wallpaperLuminance = wallpaperLuminance,
                                )
                                if (settings.showLabels) {
                                    val labelColor = if (overWallpaper) {
                                        val wallColor = labelColorForWallpaper(wallpaperLuminance)
                                        if (wallColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else wallColor
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        color = labelColor,
                                    )
                                }
                            }
                        }
                        HomeItemKind.FOLDER -> {
                            if (folder != null) {
                                val folderHasBadge = folder.apps.any { component ->
                                    val pkg = appsByComponent[component]?.packageName
                                    pkg != null && (badgeCounts[pkg] ?: 0) > 0
                                }
                                FolderTile(
                                    folder = folder,
                                    appsByComponent = appsByComponent,
                                    settings = settings,
                                    resolveDrawable = resolveDrawable,
                                    hasBadge = folderHasBadge,
                                    overWallpaper = overWallpaper,
                                    wallpaperLuminance = wallpaperLuminance,
                                    focusRequest = focusFolderRequest,
                                )
                            }
                        }
                        HomeItemKind.SHORTCUT -> ShortcutTile(label = item.label ?: "Shortcut", settings = settings)
                        HomeItemKind.CONTACT -> ContactTile(
                            name = item.contactName ?: item.label ?: "Contact",
                            settings = settings,
                        )
                        HomeItemKind.APP_PAIR -> PairTile(
                            label = item.label ?: "App Pair",
                            settings = settings,
                        )
                        HomeItemKind.STICKER -> StickerTile(text = item.label ?: "")
                        // Phase 3 widgets (#6, #113): the host view or
                        // the built-in surface. Taps go to the widget
                        // itself (the outer clickable is a no-op for
                        // widgets — see HomeScreen.tapItem); long-press
                        // opens the widget sheet. In edit mode the
                        // resize overlay sits on top.
                        HomeItemKind.WIDGET, HomeItemKind.BUILTIN_WIDGET -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                WidgetMemberContent(
                                    item = item,
                                    host = host,
                                    viewModel = viewModel,
                                    todayEvents = todayEvents,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (editingWidgetId == item.id) {
                                    WidgetResizeOverlay(
                                        item = item,
                                        halfCellW = halfCellW,
                                        halfCellH = halfCellH,
                                        onResize = { spanX, spanY ->
                                            viewModel.resizeWidget(item.id, spanX, spanY)
                                        },
                                        onResizeAt = { spanX, spanY, subX, subY ->
                                            viewModel.resizeWidgetAt(item.id, spanX, spanY, subX, subY)
                                        },
                                        onDone = { viewModel.setEditingWidget(null) },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                        // #7 Smart Stack: swipe through its members.
                        HomeItemKind.WIDGET_STACK -> {
                            val stack = item.stackId?.let { id ->
                                stacks.firstOrNull { it.id == id }
                            }
                            val members = if (stack != null) {
                                allItems.filter {
                                    it.page == PAGE_IN_STACK && it.id in stack.memberIds
                                }.sortedBy { stack.memberIds.indexOf(it.id) }
                            } else {
                                emptyList()
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (stack != null) {
                                    WidgetStackSurface(
                                        stack = stack,
                                        members = members,
                                        host = host,
                                        viewModel = viewModel,
                                        todayEvents = todayEvents,
                                        autoRotate = settings.stackAutoRotate,
                                        modifier = Modifier.fillMaxSize(),
                                        resetSignal = stackResetSignal,
                                    )
                                }
                                if (editingWidgetId == item.id) {
                                    WidgetResizeOverlay(
                                        item = item,
                                        halfCellW = halfCellW,
                                        halfCellH = halfCellH,
                                        onResize = { spanX, spanY ->
                                            viewModel.resizeWidget(item.id, spanX, spanY)
                                        },
                                        onResizeAt = { spanX, spanY, subX, subY ->
                                            viewModel.resizeWidgetAt(item.id, spanX, spanY, subX, subY)
                                        },
                                        onDone = { viewModel.setEditingWidget(null) },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Rule 56 drag-to-top Remove / Uninstall: appears at the
            // top of the page only while dragging. Left half removes
            // the item from the home; right half uninstalls the app.
            if (draggingId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DragDropZone(
                        label = "Remove",
                        icon = { Icon(Icons.Filled.Close, contentDescription = null) },
                        highlighted = zoneHover == -1,
                        modifier = Modifier.weight(1f),
                    )
                    DragDropZone(
                        label = "Uninstall",
                        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        highlighted = zoneHover == 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DragDropZone(
    label: String,
    icon: @Composable () -> Unit,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/**
 * #14 folder tile, rule 38: CIRCULAR, a 2×2 preview of the first
 * four apps (at most 4 minis, in stable order), one cell, same
 * size/mask/label treatment as an app icon. The preview shows the
 * CONTENTS, not a generic folder glyph. A folder with a badged
 * child shows the same 6dp dot as an app icon (rules 49/50).
 */
@Composable
fun FolderTile(
    folder: HomeFolder,
    appsByComponent: Map<String, AppEntry>,
    settings: LauncherSettings,
    resolveDrawable: (AppEntry) -> Drawable?,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
    // Rule 39: (folder id, bump) — focus returns to this tile when
    // the folder closes. Null means no return is pending.
    focusRequest: Pair<String, Int>? = null,
) {
    // Rule 39: the tile owns its FocusRequester; the home bumps
    // focusRequest on close and this effect moves focus back here.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequest) {
        if (focusRequest != null && focusRequest.first == folder.id) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    Column(
        modifier = modifier.focusRequester(focusRequester).focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(settings.iconSizeDp.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                // Rule 69: the 2×2 preview minis are decorative —
                // the tile's own description (on the grid cell) is
                // the announcement, never the minis one by one.
                .decorative(),
            contentAlignment = Alignment.Center,
        ) {
            val firstApps = folder.apps.mapNotNull { appsByComponent[it] }.take(4)
            if (firstApps.isEmpty()) {
                Icon(Icons.Filled.Folder, contentDescription = folder.name)
            } else {
                // Rule 16: the minis sit inside the circular tile
                // with padding, so their apparent shape stays
                // proportional to the tile.
                val miniDp = (settings.iconSizeDp.dp - 12.dp) / 2
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        firstApps.getOrNull(0)?.let {
                            AppIconImage(app = it, settings = settings, resolveDrawable = resolveDrawable, iconSize = miniDp)
                        }
                        firstApps.getOrNull(1)?.let {
                            AppIconImage(app = it, settings = settings, resolveDrawable = resolveDrawable, iconSize = miniDp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        firstApps.getOrNull(2)?.let {
                            AppIconImage(app = it, settings = settings, resolveDrawable = resolveDrawable, iconSize = miniDp)
                        }
                        firstApps.getOrNull(3)?.let {
                            AppIconImage(app = it, settings = settings, resolveDrawable = resolveDrawable, iconSize = miniDp)
                        }
                    }
                }
            }
            NotificationBadge(
                count = if (hasBadge) 1 else 0,
                largeIcon = null,
                settings = settings,
            )
        }
        if (settings.showLabels) {
            val labelColor = if (overWallpaper) {
                val wallColor = labelColorForWallpaper(wallpaperLuminance)
                if (wallColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else wallColor
            } else {
                MaterialTheme.colorScheme.onBackground
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = labelColor,
            )
        }
    }
}

/** #94 pinned shortcut tile. Rule 52: launcher chrome is neutral —
 *  the tile uses a surface container, not a saturated container. */
@Composable
fun ShortcutTile(label: String, modifier: Modifier = Modifier, settings: LauncherSettings? = null, showLabel: Boolean = true) {
    val iconSize = settings?.iconSizeDp?.dp ?: 56.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(iconSize).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = label)
        }
        if (showLabel) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** #95 pinned contact tile. */
@Composable
fun ContactTile(name: String, modifier: Modifier = Modifier, settings: LauncherSettings? = null, showLabel: Boolean = true) {
    val iconSize = settings?.iconSizeDp?.dp ?: 56.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(iconSize).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = name)
        }
        if (showLabel) {
            Text(text = name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** #96 app-pair tile. */
@Composable
fun PairTile(label: String, modifier: Modifier = Modifier, settings: LauncherSettings? = null, showLabel: Boolean = true) {
    val iconSize = settings?.iconSizeDp?.dp ?: 56.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(iconSize).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = label)
        }
        if (showLabel) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** #58 sticker / free-text tile: the text itself, no icon. */
@Composable
fun StickerTile(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxSize().padding(4.dp),
    )
}

/** #67 page indicators: dots / numbers / lines / none, in the chosen colour. */
@Composable
fun PageIndicator(
    style: PageIndicatorStyle,
    colour: IndicatorColour,
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    if (style == PageIndicatorStyle.NONE || pageCount <= 1) return
    val color = when (colour) {
        IndicatorColour.ACCENT -> MaterialTheme.colorScheme.primary
        IndicatorColour.WHITE -> Color.White
        IndicatorColour.BLACK -> Color.Black
    }
    Row(modifier = modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until pageCount) {
            when (style) {
                PageIndicatorStyle.DOTS -> Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == currentPage) 10.dp else 7.dp)
                        .then(if (i == currentPage) Modifier.phosphorGlow(color) else Modifier)
                        .background(
                            color = if (i == currentPage) color else color.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
                PageIndicatorStyle.NUMBERS -> Text(
                    text = (i + 1).toString(),
                    style = if (i == currentPage) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    color = if (i == currentPage) color else color.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                PageIndicatorStyle.LINES -> Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = 18.dp, height = 3.dp)
                        .background(
                            color = if (i == currentPage) color else color.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
                PageIndicatorStyle.NONE -> Unit
            }
        }
    }
}
