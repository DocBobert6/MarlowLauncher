package app.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.launcher.data.WidgetHost
import app.launcher.model.AppEntry
import app.launcher.model.BuiltinWidgetType
import app.launcher.model.HomeFolder
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.LauncherSettings
import app.launcher.model.PAGE_DESKTOP_WIDGETS
import app.launcher.model.PAGE_WIDGET_PAGE
import app.launcher.model.WidgetStack
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dedicated widget page: the swipe-left page that keeps
 * widgets off the main home. It is the same grid as a home page
 * (same placement, drag and resize), filtered to page
 * PAGE_WIDGET_PAGE, with an "Add widget" entry point.
 *
 * The hidden Desktop widget page is a second widgets-only
 * surface, reached from here — never in the swipe sequence. Its
 * items live on page PAGE_DESKTOP_WIDGETS.
 */
@Composable
fun WidgetPageScreen(
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    host: AppWidgetHost,
    items: List<HomeItem>,
    appsByComponent: Map<String, AppEntry>,
    folders: Map<String, HomeFolder>,
    resolveDrawable: (AppEntry) -> Drawable?,
    onTapItem: (HomeItem) -> Unit,
    onLongPressItem: (HomeItem) -> Unit,
    onAddWidget: () -> Unit,
    onOpenDesktopWidgets: () -> Unit,
    // The home passes this only for the pager page, so a double-tap
    // on a widget can be told from a tap on empty space; the
    // two-pane copy is outside the gesture Box and stays silent.
    onItemRectsChanged: (List<androidx.compose.ui.geometry.Rect>) -> Unit = {},
    modifier: Modifier = Modifier,
    // Same topInset rule as FeedPageScreen — the pager copy sits
    // inside the already-padded home Column, the two-pane copy is a
    // standalone pane.
    topInset: Boolean = true,
    // Rule 44: the home bumps this when it regains focus; stacks
    // reset to their default member.
    stackResetSignal: Int = 0,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .then(if (topInset) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp),
            )
            TextButton(onClick = onAddWidget) {
                Text("Add widget")
            }
            if (settings.desktopWidgetPageEnabled) {
                TextButton(onClick = onOpenDesktopWidgets) {
                    Text("Desktop widgets")
                }
            }
        }
        if (items.isEmpty()) {
            Text(
                text = "No widgets here yet — tap “Add widget” to pick one.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        HomePageGrid(
            pageIndex = PAGE_WIDGET_PAGE,
            items = items,
            appsByComponent = appsByComponent,
            folders = folders,
            settings = settings,
            viewModel = viewModel,
            resolveDrawable = resolveDrawable,
            onTapItem = onTapItem,
            onLongPressItem = onLongPressItem,
            host = host,
            modifier = Modifier.weight(1f),
            onItemRectsChanged = onItemRectsChanged,
            stackResetSignal = stackResetSignal,
        )
    }
}

/**
 * The hidden Desktop widget page: the same grid, filtered to
 * page PAGE_DESKTOP_WIDGETS, shown as a full-screen surface (not a
 * pager page). Reached only from the widget page.
 */
@Composable
fun DesktopWidgetPageScreen(
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    host: AppWidgetHost,
    items: List<HomeItem>,
    appsByComponent: Map<String, AppEntry>,
    folders: Map<String, HomeFolder>,
    resolveDrawable: (AppEntry) -> Drawable?,
    onTapItem: (HomeItem) -> Unit,
    onLongPressItem: (HomeItem) -> Unit,
    onAddWidget: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // Rule 44: see WidgetPageScreen.
    stackResetSignal: Int = 0,
) {
    // A full-screen surface, so it clears both system bars itself.
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = "Desktop widgets",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp),
            )
            TextButton(onClick = onAddWidget) {
                Text("Add widget")
            }
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
        if (items.isEmpty()) {
            Text(
                text = "No widgets here yet — tap “Add widget” to pick one.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        HomePageGrid(
            pageIndex = PAGE_DESKTOP_WIDGETS,
            items = items,
            appsByComponent = appsByComponent,
            folders = folders,
            settings = settings,
            viewModel = viewModel,
            resolveDrawable = resolveDrawable,
            onTapItem = onTapItem,
            onLongPressItem = onLongPressItem,
            host = host,
            modifier = Modifier.weight(1f),
            stackResetSignal = stackResetSignal,
        )
    }
}

/**
 * Widget picker (rule 43): a full-screen surface, not a text-only
 * dialog. Previews are truthful — the provider's own preview
 * image where supplied (AppWidgetProviderInfo.loadPreviewImage),
 * never a stale screenshot or a generic placeholder; a provider
 * with no preview shows its label, description and size, and no
 * image at all. Descriptions come from the provider. The list is
 * searchable and fast, grouped alphabetically per app, with no
 * "recommended" row — Marlow does no recommendations.
 *
 * A pick that does not fit the free space is refused VISIBLY
 * BEFORE release: [canPlace] is asked with the pick's half-cell
 * span, and when it reports no room the picker shows the refusal
 * in place and picks nothing. The home wires [canPlace] to the
 * placement math (see the patch request in
 * rebuild-notes-drawer.md); until it does, the default always
 * allows the pick and the placement layer reports the refusal.
 *
 * Configuration (rule 43): a provider with a configure activity is
 * never placed bare — the caller runs the configuration flow
 * (WidgetHost.configureIntent) and places only on its result, so a
 * configurable widget lands exactly once.
 */
@Composable
fun WidgetPickerDialog(
    providers: List<AppWidgetProviderInfo>,
    onPickBuiltin: (BuiltinWidgetType) -> Unit,
    onPickProvider: (AppWidgetProviderInfo) -> Unit,
    onPickStack: () -> Unit,
    onDismiss: () -> Unit,
    gridColumns: Int = 5,
    canPlace: (spanXHalfCells: Int, spanYHalfCells: Int) -> Boolean = { _, _ -> true },
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val densityDpi = (LocalDensity.current.density * 160).roundToInt()
    val collator = remember { Collator.getInstance(Locale.getDefault()) }
    var query by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<String?>(null) }

    fun tryPickBuiltin(type: BuiltinWidgetType) {
        val spanX = if (type == BuiltinWidgetType.CLOCK) gridColumns * 2 else 4
        val spanY = if (type == BuiltinWidgetType.CLOCK) 2 else 4
        if (!canPlace(spanX, spanY)) {
            refusal = "No free space for this widget on this page — remove something first."
        } else {
            onPickBuiltin(type)
        }
    }

    fun tryPickProvider(provider: AppWidgetProviderInfo) {
        val (cellsX, cellsY) = WidgetHost.spanForMinSizeDp(provider.minWidth, provider.minHeight)
        if (!canPlace(cellsX * 2, cellsY * 2)) {
            refusal = "No free space for this widget on this page — remove something first."
        } else {
            onPickProvider(provider)
        }
    }

    fun tryPickStack() {
        if (!canPlace(4, 4)) {
            refusal = "No free space for a stack on this page — remove something first."
        } else {
            onPickStack()
        }
    }

    // Alphabetical per-app groups; the query filters widget and app labels.
    val groups = remember(providers, query, collator) {
        val filtered = if (query.isBlank()) providers else providers.filter { provider ->
            val label = provider.loadLabel(context.packageManager).toString()
            val appLabel = appLabelFor(context, provider.provider.packageName)
            label.contains(query, ignoreCase = true) ||
                appLabel.contains(query, ignoreCase = true)
        }
        filtered.groupBy { it.provider.packageName }
            .map { (packageName, list) ->
                appLabelFor(context, packageName) to
                    list.sortedWith(
                        Comparator { a, b ->
                            collator.compare(
                                a.loadLabel(context.packageManager).toString(),
                                b.loadLabel(context.packageManager).toString(),
                            )
                        },
                    )
            }
            .sortedWith(Comparator { a, b -> collator.compare(a.first, b.first) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Add widget",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close widget picker")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search widgets") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (refusal != null) {
            Text(
                text = refusal!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "Built-in widgets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            items(BuiltinWidgetType.values().toList(), key = { it.id }) { type ->
                val spanText = if (type == BuiltinWidgetType.CLOCK) "Full width × 1 row"
                else "2 × 2 cells"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tryPickBuiltin(type) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            widgetTypeHint(type) + " · $spanText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    "Smart Stack",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tryPickStack() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("Smart Stack — swipe through widgets", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A Marlow extra, not standard Android behaviour · 2 × 2 cells",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            for ((appLabel, appProviders) in groups) {
                item {
                    Text(
                        appLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(appProviders, key = { it.provider.flattenToString() }) { provider ->
                    WidgetProviderRow(
                        provider = provider,
                        densityDpi = densityDpi,
                        onPick = { tryPickProvider(provider) },
                    )
                }
            }
            if (groups.isEmpty() && query.isNotBlank()) {
                item {
                    Text(
                        "No widgets match “$query”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun appLabelFor(context: android.content.Context, packageName: String): String =
    try {
        context.packageManager
            .getApplicationInfo(packageName, 0)
            .let { context.packageManager.getApplicationLabel(it).toString() }
    } catch (e: Exception) {
        packageName
    }

/** One provider row: truthful preview, description, size. */
@Composable
private fun WidgetProviderRow(
    provider: AppWidgetProviderInfo,
    densityDpi: Int,
    onPick: () -> Unit,
) {
    val context = LocalContext.current
    val label = remember(provider) {
        provider.loadLabel(context.packageManager).toString()
    }
    val description = remember(provider) {
        try {
            provider.loadDescription(context)?.toString()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    val previewBitmap = remember(provider, densityDpi) {
        try {
            val drawable = provider.loadPreviewImage(context, densityDpi) ?: return@remember null
            val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: 320
            val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: 200
            val targetW = 320
            val targetH = (320f * intrinsicH / intrinsicW).roundToInt().coerceIn(80, 320)
            drawable.toBitmap(targetW, targetH).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    val (cellsX, cellsY) = WidgetHost.spanForMinSizeDp(provider.minWidth, provider.minHeight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap,
                contentDescription = null,
                modifier = Modifier.size(80.dp).padding(end = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "$cellsX × $cellsY cells",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun widgetTypeHint(type: BuiltinWidgetType): String = when (type) {
    BuiltinWidgetType.NOTES -> "Notes"
    BuiltinWidgetType.TASKS -> "Tasks"
    BuiltinWidgetType.TIMERS -> "Timers"
    BuiltinWidgetType.HABITS -> "Habit tracking"
    BuiltinWidgetType.CALENDAR -> "Today's events"
    BuiltinWidgetType.WEATHER -> "Forecast"
    BuiltinWidgetType.SYSTEM -> "Battery, memory, storage"
    BuiltinWidgetType.CLOCK -> "Time and date"
}

/**
 * Long-press sheet for a placed widget or stack: resize, add to
 * a stack, remove; for a stack, step through / remove the current
 * member, dissolve, or remove the whole stack. A placed widget
 * with a configure activity can be reconfigured in place, without
 * deleting it, through [onReconfigure] (the entry shows only when
 * the home wires it — see the patch request in
 * rebuild-notes-drawer.md).
 *
 * Stacking is a Marlow extension, not stock behaviour — the sheet
 * says so where stacking is offered. Only stacks with the same
 * footprint as this widget are offered ([stackItems] carries the
 * stack HomeItems so footprints can be compared; when it is empty
 * the sheet cannot compare and offers every stack, and the
 * placement layer enforces the rule instead).
 */
@Composable
fun WidgetItemSheet(
    item: HomeItem,
    stacks: List<WidgetStack>,
    viewModel: HomeViewModel,
    onResize: (HomeItem) -> Unit,
    onDismiss: () -> Unit,
    onReconfigure: ((HomeItem) -> Unit)? = null,
    stackItems: List<HomeItem> = emptyList(),
) {
    val isStack = item.kind == HomeItemKind.WIDGET_STACK
    val context = LocalContext.current
    val hasConfigure = remember(item) {
        if (item.kind != HomeItemKind.WIDGET || item.appWidgetId < 0) false
        else WidgetHost.hasConfigureActivity(WidgetHost.providerInfo(context, item.appWidgetId))
    }
    val sameFootprintStacks = remember(stacks, stackItems, item) {
        if (stackItems.isEmpty()) stacks
        else stacks.filter { stack ->
            val stackItem = stackItems.firstOrNull {
                it.kind == HomeItemKind.WIDGET_STACK && it.stackId == stack.id
            }
            stackItem == null || (stackItem.spanX == item.spanX && stackItem.spanY == item.spanY)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isStack) "Smart Stack" else "Widget") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (hasConfigure && onReconfigure != null) {
                    item {
                        TextButton(
                            onClick = { onReconfigure(item); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reconfigure", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { onResize(item); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Resize…", modifier = Modifier.fillMaxWidth())
                    }
                }
                // Move between the widget page and home page 0.
                if (item.page == PAGE_WIDGET_PAGE) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.moveWidgetToPage(item.id, 0)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Move to home page", modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else if (item.page >= 0) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.moveWidgetToPage(item.id, PAGE_WIDGET_PAGE)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Move to widget page", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (!isStack) {
                    item {
                        Text(
                            "Smart Stack is a Marlow extra, not standard Android behaviour. " +
                                "Stacks hold widgets of the same size only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    item {
                        TextButton(
                            onClick = {
                                viewModel.addWidgetToNewStack(item.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Add to a new Smart Stack", modifier = Modifier.fillMaxWidth())
                        }
                    }
                    items(sameFootprintStacks, key = { it.id }) { stack ->
                        TextButton(
                            onClick = {
                                viewModel.addWidgetToStack(item.id, stack.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Add to Smart Stack (${stack.memberIds.size} widgets)",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    val stack = stacks.firstOrNull { it.id == item.stackId }
                    if (stack != null && stack.memberIds.size > 1) {
                        item {
                            TextButton(
                                onClick = {
                                    viewModel.setStackIndex(
                                        stack.id,
                                        (stack.currentIndex + 1) % stack.memberIds.size,
                                    )
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Show next widget", modifier = Modifier.fillMaxWidth())
                            }
                        }
                        item {
                            TextButton(
                                onClick = {
                                    viewModel.removeCurrentWidgetFromStack(stack.id)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Remove current widget from stack", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = {
                                viewModel.dissolveStack(stack?.id ?: item.stackId ?: "")
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Dissolve stack (widgets stay)", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            viewModel.removeWidget(item.id)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isStack) "Remove stack" else "Remove widget", modifier = Modifier.fillMaxWidth())
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
