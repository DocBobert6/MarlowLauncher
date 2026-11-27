package app.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.HomePage
import app.launcher.model.LauncherSettings
import kotlin.math.roundToInt

/**
 * #76 home-screen preview / edit mode (overview): page thumbnails with
 * the overview actions (#173 — only the actions listed in
 * settings.overviewActions are shown): set default (#75), hide/show,
 * move left/right, add page, delete page (#3 reorder/hide pages).
 *
 * Rule 10 / §B.18: the thumbnails are REAL scaled mini-previews of
 * the pages — every item drawn at its true grid position, scaled
 * down — not icons-only rows. Action labels use the one 12sp role
 * (body-small), not 11sp.
 */
@Composable
fun OverviewScreen(
    pages: List<HomePage>,
    currentPage: Int,
    overviewActions: List<String>,
    defaultPage: Int,
    onSetDefault: (Int) -> Unit,
    onSetHidden: (Int, Boolean) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onSelectPage: (Int) -> Unit,
    onClose: () -> Unit,
    onAddContact: () -> Unit,
    onOpenSettings: () -> Unit,
    homeItems: List<HomeItem> = emptyList(),
    appsByComponent: Map<String, AppEntry> = emptyMap(),
    settings: LauncherSettings? = null,
    resolveDrawable: (AppEntry) -> android.graphics.drawable.Drawable? = { null },
    // #95 pin a contact to a dock page instead of the home.
    onAddContactToDock: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Home pages", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close overview")
                }
            }
            LazyVerticalGrid(
                // One page spans the full width — a single page must
                // not be a tiny card lost in a 2-column grid.
                columns = GridCells.Fixed(if (pages.size == 1) 1 else 2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(pages.size) { index ->
                    val page = pages[index]
                    Column(
                        modifier = Modifier.padding(8.dp).clickable { onSelectPage(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .background(
                                    color = if (index == currentPage) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // #76 real scaled mini-preview of the
                                // page: items at their true positions.
                                if (settings != null) {
                                    PageMiniPreview(
                                        pageIndex = index,
                                        homeItems = homeItems,
                                        appsByComponent = appsByComponent,
                                        settings = settings,
                                        resolveDrawable = resolveDrawable,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(0.55f),
                                    )
                                }
                                Text(
                                    text = "Page ${index + 1}" +
                                        (if (index == defaultPage) " • Home" else "") +
                                        (if (page.hidden) " • Hidden" else ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        OverviewActionsRow(
                            index = index,
                            pageCount = pages.size,
                            page = page,
                            actions = overviewActions,
                            onSetDefault = onSetDefault,
                            onSetHidden = onSetHidden,
                            onMovePage = onMovePage,
                            onDeletePage = onDeletePage,
                        )
                    }
                }
                // "+ Add page" is a card in the grid itself, not a
                // lone button underneath it.
                if ("add_page" in overviewActions) {
                    item {
                        Column(
                            modifier = Modifier.padding(8.dp).clickable { onAddPage() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(text = "Add page", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                TextButton(onClick = onAddContact) { Text("Add contact") }
                TextButton(onClick = onAddContactToDock) { Text("Contact to dock") }
                TextButton(onClick = onOpenSettings) { Text("Launcher settings") }
            }
        }
    }
}

/**
 * Rule 10 / §B.18: a page drawn small — each item at its true
 * sub-grid position, scaled to the preview. Apps show their icon,
 * folders a circle, widgets a rounded rect, so the preview reads
 * as the page, not as a list of its first icons.
 */
@Composable
private fun PageMiniPreview(
    pageIndex: Int,
    homeItems: List<HomeItem>,
    appsByComponent: Map<String, AppEntry>,
    settings: LauncherSettings,
    resolveDrawable: (AppEntry) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
) {
    val cols = settings.gridSize.effectiveCols(LocalConfiguration.current.screenWidthDp)
    val rows = settings.gridSize.rows
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val cellW = constraints.maxWidth / cols.toFloat()
        val cellH = constraints.maxHeight / rows.toFloat()
        val iconPx = minOf(cellW, cellH) * 0.55f
        val iconDp = with(density) { iconPx.toDp() }
        Box(modifier = Modifier.fillMaxSize()) {
            for (item in homeItems.filter { it.page == pageIndex }) {
                val xPx = item.subX / 2f * cellW
                val yPx = item.subY / 2f * cellH
                val wPx = item.spanX / 2f * cellW
                val hPx = item.spanY / 2f * cellH
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .size(
                            width = with(density) { wPx.toDp() },
                            height = with(density) { hPx.toDp() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.kind) {
                        HomeItemKind.APP -> {
                            val app = item.component?.let { appsByComponent[it] }
                            if (app != null) {
                                AppIconImage(
                                    app = app,
                                    settings = settings,
                                    resolveDrawable = resolveDrawable,
                                    iconSize = iconDp,
                                )
                            }
                        }
                        HomeItemKind.FOLDER -> Box(
                            modifier = Modifier
                                .size(iconDp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                        )
                        HomeItemKind.WIDGET,
                        HomeItemKind.BUILTIN_WIDGET,
                        HomeItemKind.WIDGET_STACK -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.small,
                                ),
                        )
                        HomeItemKind.SHORTCUT,
                        HomeItemKind.CONTACT,
                        HomeItemKind.APP_PAIR -> Box(
                            modifier = Modifier
                                .size(iconDp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                        )
                        HomeItemKind.STICKER -> Unit
                    }
                }
            }
        }
    }
}

/** One page's action row, in the user's overview-action order. */
@Composable
private fun OverviewActionsRow(
    index: Int,
    pageCount: Int,
    page: HomePage,
    actions: List<String>,
    onSetDefault: (Int) -> Unit,
    onSetHidden: (Int, Boolean) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onDeletePage: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (action in actions) {
            when (action) {
                "set_default" -> OverviewActionButton(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    enabled = true,
                    onClick = { onSetDefault(index) },
                    contentDescription = "Set as default home page",
                )
                "hide_page" -> OverviewActionButton(
                    icon = if (page.hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    label = if (page.hidden) "Show" else "Hide",
                    enabled = true,
                    onClick = { onSetHidden(index, !page.hidden) },
                    contentDescription = if (page.hidden) "Show page" else "Hide page",
                )
                "move_left" -> OverviewActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Move left",
                    enabled = index > 0,
                    onClick = { onMovePage(index, index - 1) },
                    contentDescription = "Move page left",
                )
                "move_right" -> OverviewActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = "Move right",
                    enabled = index < pageCount - 1,
                    onClick = { onMovePage(index, index + 1) },
                    contentDescription = "Move page right",
                )
                "delete_page" -> OverviewActionButton(
                    icon = Icons.Filled.Close,
                    label = "Delete",
                    enabled = pageCount > 1,
                    onClick = { onDeletePage(index) },
                    contentDescription = "Delete page",
                )
            }
        }
    }
}

/** One page action: icon with a small visible label under it. A
 *  disabled action stays visible but clearly disabled — dimmed icon
 *  (IconButton handles that) and dimmed label — so the user can see
 *  what the row offers instead of guessing at bare icons. The label
 *  uses the one 12sp role (rule 10), not 11sp. */
@Composable
private fun OverviewActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = contentDescription)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}
