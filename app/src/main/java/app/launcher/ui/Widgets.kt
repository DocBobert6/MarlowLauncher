package app.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.launcher.data.WidgetHost
import app.launcher.data.isSystemReducedMotion
import app.launcher.model.BuiltinWidgetType
import app.launcher.model.CalendarEvent
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.WidgetStack
import app.launcher.ui.theme.phosphorGlow
import kotlinx.coroutines.delay
import kotlin.math.floor

/**
 * Third-party widgets + stacking rendering.
 *
 * A placed widget is a HomeItem of kind WIDGET (AppWidgetHostView,
 * created from the launcher's AppWidgetHost) or BUILTIN_WIDGET (a
 * first-party Compose surface — see BuiltinWidgets.kt). A stack is
 * a HomeItem of kind WIDGET_STACK whose members are HomeItems with
 * page == PAGE_IN_STACK; the stack surface swipes between them with
 * a HorizontalPager.
 *
 * Resize (rule 42): in edit mode (long-press a widget → Resize) a
 * frame with exactly four handles (left/top/right/bottom) appears;
 * dragging one changes the item's half-cell span, which is
 * persisted and reported to the provider via
 * AppWidgetManager.updateAppWidgetOptions.
 */

/**
 * Renders one widget member (third-party or built-in). Third-party
 * widgets are rounded and padded by the host (rule 41) on every OS
 * version, even where the provider supplied no frame of its own.
 */
@Composable
fun WidgetMemberContent(
    item: HomeItem,
    host: AppWidgetHost,
    viewModel: HomeViewModel,
    todayEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier,
) {
    when (item.kind) {
        HomeItemKind.WIDGET -> Box(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(12.dp),
                )
                .padding(8.dp),
        ) {
            ThirdPartyWidgetView(item = item, host = host, modifier = Modifier.fillMaxSize())
        }
        HomeItemKind.BUILTIN_WIDGET -> {
            val type = BuiltinWidgetType.values().firstOrNull { it.id == item.builtinWidgetId }
            if (type != null) {
                BuiltinWidgetSurface(type = type, viewModel = viewModel, todayEvents = todayEvents, modifier = modifier)
            } else {
                WidgetUnavailable(text = "Unknown widget", modifier = modifier)
            }
        }
        else -> WidgetUnavailable(text = "Not a widget", modifier = modifier)
    }
}

/** One third-party widget, hosted by the AppWidgetHost. */
@Composable
private fun ThirdPartyWidgetView(
    item: HomeItem,
    host: AppWidgetHost,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val info = remember(item.appWidgetId) { WidgetHost.providerInfo(context, item.appWidgetId) }
    if (info == null) {
        WidgetUnavailable(
            text = "Widget unavailable — its app may have been removed.",
            modifier = modifier,
        )
        return
    }
    AndroidView(
        factory = { ctx ->
            try {
                host.createView(ctx, item.appWidgetId, info).apply {
                    setAppWidget(item.appWidgetId, info)
                }
            } catch (e: Exception) {
                // No AppWidget service (e.g. under Robolectric in the
                // startup gate): a plain placeholder, never a crash.
                android.widget.TextView(ctx).apply {
                    text = "Widget unavailable"
                }
            }
        },
        update = { view ->
            try {
                if (view is android.appwidget.AppWidgetHostView) {
                    view.setAppWidget(item.appWidgetId, info)
                }
            } catch (e: Exception) {
                // Provider gone between compositions; the placeholder
                // shows on the next recomposition.
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun WidgetUnavailable(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Rule 42 resize overlay shown in edit mode: exactly four handles
 * (left / top / right / bottom), honouring the provider's
 * resizeMode — handles for disallowed axes are gone, and a
 * non-resizable widget shows no resize frame at all (only Done).
 * halfCellW/H are the grid's half-cell sizes in pixels.
 *
 * Snap threshold: a handle must cross 66% of a cell before the
 * span changes; below that, dragging changes nothing. (The 150 ms
 * snap animation is a later motion pass — the threshold logic here
 * is the behaviour the rule pins down.)
 *
 * [onResizeAt] is the anchored variant for the left/top handles,
 * which move the item's origin as well as its span; it is null
 * until HomeViewModel gains the matching call (see the patch
 * request in rebuild-notes-drawer.md). Until then the left/top
 * handles resize span-only, anchored at the item's top-left.
 */
@Composable
fun WidgetResizeOverlay(
    item: HomeItem,
    halfCellW: Float,
    halfCellH: Float,
    onResize: (spanX: Int, spanY: Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onResizeAt: ((spanX: Int, spanY: Int, subX: Int, subY: Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    // The provider's resizeMode, for third-party widgets; built-in
    // widgets and stacks are ours and resize on both axes.
    val resizeMode = remember(item.id, item.appWidgetId, item.kind) {
        if (item.kind == HomeItemKind.WIDGET) {
            WidgetHost.providerInfo(context, item.appWidgetId)?.resizeMode
                ?: AppWidgetProviderInfo.RESIZE_BOTH
        } else {
            AppWidgetProviderInfo.RESIZE_BOTH
        }
    }
    val canResizeX = resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 ||
        resizeMode == AppWidgetProviderInfo.RESIZE_BOTH
    val canResizeY = resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0 ||
        resizeMode == AppWidgetProviderInfo.RESIZE_BOTH

    /** Cells crossed at the 66% threshold, per axis. */
    fun thresholdCells(dragPx: Float, cellPx: Float): Int {
        if (cellPx <= 0f) return 0
        val magnitude = floor(kotlin.math.abs(dragPx) / cellPx + (1f - 0.66f)).toInt()
        return if (dragPx < 0f) -magnitude else magnitude
    }

    fun applyHorizontal(dCells: Int, fromLeft: Boolean) {
        if (dCells == 0) return
        val newSpanX = (item.spanX + dCells).coerceAtLeast(2)
        if (fromLeft && onResizeAt != null) {
            // Growing from the left moves the origin left by the
            // same cells the span grows by (and vice versa).
            val newSubX = (item.subX - dCells).coerceAtLeast(0)
            onResizeAt(newSpanX, item.spanY, newSubX, item.subY)
        } else {
            onResize(newSpanX, item.spanY)
        }
    }

    fun applyVertical(dCells: Int, fromTop: Boolean) {
        if (dCells == 0) return
        val newSpanY = (item.spanY + dCells).coerceAtLeast(2)
        if (fromTop && onResizeAt != null) {
            val newSubY = (item.subY - dCells).coerceAtLeast(0)
            onResizeAt(item.spanX, newSpanY, item.subX, newSubY)
        } else {
            onResize(item.spanX, newSpanY)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("Done")
        }
        if (resizeMode == AppWidgetProviderInfo.RESIZE_NONE) return@Box
        // Right handle: grows/shrinks the width from the right edge.
        if (canResizeX) {
            ResizeHandle(
                description = "Resize widget wider or narrower",
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontal = false,
            ) { total ->
                applyHorizontal(thresholdCells(total.x, halfCellW), fromLeft = false)
            }
            // Left handle: grows/shrinks the width from the left edge.
            ResizeHandle(
                description = "Resize widget wider or narrower from the left",
                modifier = Modifier.align(Alignment.CenterStart),
                horizontal = false,
            ) { total ->
                applyHorizontal(thresholdCells(-total.x, halfCellW), fromLeft = true)
            }
        }
        if (canResizeY) {
            ResizeHandle(
                description = "Resize widget taller or shorter",
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontal = true,
            ) { total ->
                applyVertical(thresholdCells(total.y, halfCellH), fromTop = false)
            }
            ResizeHandle(
                description = "Resize widget taller or shorter from the top",
                modifier = Modifier.align(Alignment.TopCenter),
                horizontal = true,
            ) { total ->
                applyVertical(thresholdCells(-total.y, halfCellH), fromTop = true)
            }
        }
    }
}

/**
 * One resize handle: a 48dp hit area with a 32dp visual pill.
 * The halo lives only on the handle (a locked Marlow decision).
 * [onDragEnd] receives the total drag offset in pixels.
 */
@Composable
private fun ResizeHandle(
    description: String,
    modifier: Modifier,
    horizontal: Boolean,
    onDragEnd: (Offset) -> Unit,
) {
    var dragTotal by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragTotal = Offset.Zero },
                    onDragEnd = {
                        onDragEnd(dragTotal)
                        dragTotal = Offset.Zero
                    },
                    onDragCancel = { dragTotal = Offset.Zero },
                    onDrag = { _, dragAmount -> dragTotal += dragAmount },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (horizontal) Modifier.size(width = 32.dp, height = 4.dp)
                    else Modifier.size(width = 4.dp, height = 32.dp),
                )
                .phosphorGlow(MaterialTheme.colorScheme.primary)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

/**
 * Smart Stack surface: swipes through the stack's members with a
 * pager + dots. Auto-rotate (toggleable, OFF by default) advances
 * one widget every 10 seconds while the stack is on screen.
 *
 * Stacking is a Marlow extension, not stock behaviour (rule 44):
 * the surface says so to TalkBack, and the picker and long-press
 * sheet label it in visible text. Same-footprint membership is
 * enforced where stacks are built (see the patch request in
 * rebuild-notes-drawer.md). The stack resets to its default
 * member on returning Home: on first composition, and whenever
 * [resetSignal] changes (the home wires a counter it bumps when
 * the home regains focus).
 */
@Composable
fun WidgetStackSurface(
    stack: WidgetStack,
    members: List<HomeItem>,
    host: AppWidgetHost,
    viewModel: HomeViewModel,
    todayEvents: List<CalendarEvent>,
    autoRotate: Boolean,
    modifier: Modifier = Modifier,
    resetSignal: Int = 0,
) {
    if (members.isEmpty()) {
        WidgetUnavailable(text = "Empty stack", modifier = modifier)
        return
    }
    val safeIndex = stack.currentIndex.coerceIn(0, members.size - 1)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { members.size })
    LaunchedEffect(safeIndex) {
        if (pagerState.currentPage != safeIndex) pagerState.scrollToPage(safeIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != stack.currentIndex) {
            viewModel.setStackIndex(stack.id, pagerState.currentPage)
        }
    }
    // Rule 44: reset to the default member on returning Home.
    LaunchedEffect(Unit) {
        if (stack.currentIndex != 0) viewModel.setStackIndex(stack.id, 0)
    }
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && stack.currentIndex != 0) {
            viewModel.setStackIndex(stack.id, 0)
        }
    }
    // Reduced motion: when the system has animations off,
    // auto-rotate jumps instantly instead of animating.
    val localContext = LocalContext.current
    val reduceMotion = remember(localContext) { isSystemReducedMotion(localContext) }
    LaunchedEffect(autoRotate, members.size, reduceMotion) {
        if (!autoRotate || members.size <= 1) return@LaunchedEffect
        while (true) {
            delay(10_000)
            val next = (pagerState.currentPage + 1) % members.size
            if (reduceMotion) pagerState.scrollToPage(next)
            else pagerState.animateScrollToPage(next)
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Smart Stack — a Marlow extra, not standard Android behaviour" },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val member = members.getOrNull(page) ?: return@HorizontalPager
            WidgetMemberContent(
                item = member,
                host = host,
                viewModel = viewModel,
                todayEvents = todayEvents,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (members.size > 1) {
            StackDots(
                count = members.size,
                current = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun StackDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier = modifier) {
        for (i in 0 until count) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == current) 8.dp else 6.dp)
                    .background(
                        color = if (i == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}
