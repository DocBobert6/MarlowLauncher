package app.launcher.ui

import android.graphics.drawable.Drawable
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry
import app.launcher.model.HomeFolder
import app.launcher.model.LauncherSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Folder contents (rules 38-39).
 *
 * Opening a folder dims the home screen and shows the folder as a
 * FLOATING OVERLAY on the drag layer — not a new page and not an
 * AlertDialog. The home stays visible, dimmed, behind it. Back or
 * tap-outside closes it; focus returns to the folder icon (the
 * caller clears the open folder, which restores the home — see the
 * patch request in rebuild-notes-drawer.md for the focus-return
 * half, which lives in HomeScreen).
 *
 * The folder grid comes from the grid record
 * (`settings.gridSize.folderCols` × `folderRows`, rule 39): 4
 * columns x 3 rows per page, paginating beyond one page. Renaming
 * is: open the folder, tap the name, type, confirm — the name does
 * not truncate at the folder's width, it spans the overlay. There
 * is no "Remove" button under every app: long-press an app (or a
 * contact) to take it out of the folder.
 *
 * Entrance is a basic fade/scale; the circular-reveal polish is a
 * later motion pass.
 *
 * The function keeps its old name and signature so the HomeScreen
 * call site is untouched; only the presentation changed.
 *
 * #95 folder contacts sit in the same grid: tap one to open it,
 * long-press takes it out of the folder (the home tile is untouched).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderDialog(
    folder: HomeFolder,
    appsByComponent: Map<String, AppEntry>,
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    resolveDrawable: (AppEntry) -> Drawable?,
    onLaunchApp: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
    // Rule 69: TalkBack "Rename" on a folder app opens the
    // long-press sheet, where the rename dialog lives.
    onRenameApp: (AppEntry) -> Unit = {},
) {
    // Motion wave: entrance/exit run on the §4.5 table (folder
    // drop-in 400 ms in / 200 ms out, contents 350 ms starting
    // 45 ms after the container — rule 54 choreography). The exit
    // runs as an animation before onDismiss removes the dialog:
    // requestDismiss() starts the exit, and the caller clears the
    // open folder only once it has played.
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalMarlowHaptics.current
    var exiting by remember { mutableStateOf(false) }
    fun requestDismiss() {
        if (!exiting) exiting = true
    }
    LaunchedEffect(exiting) {
        if (exiting) {
            delay(Motion.FOLDER_FINAL_ITEM_MS.toLong())
            onDismiss()
        }
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val targetAlpha = if (visible && !exiting) 1f else 0f
    val targetScale = if (visible && !exiting) 1f else 0.92f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (targetAlpha == 1f) Motion.folderDropInSpec(reducedMotion)
        else Motion.folderExitSpec(reducedMotion),
        label = "folderOverlayAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (targetScale == 1f) Motion.folderDropInSpec(reducedMotion)
        else Motion.folderExitSpec(reducedMotion),
        label = "folderOverlayScale",
    )
    // Rule 54: the container drops in first, the contents fade in
    // 45 ms after (350 ms). The exit takes both together (200 ms).
    val contentAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (targetAlpha == 1f) Motion.folderContentsSpec(reducedMotion)
        else Motion.folderExitSpec(reducedMotion),
        label = "folderContentsAlpha",
    )
    // Rule 58 predictive back: the folder scales toward ~90% as the
    // back swipe progresses; cancelling springs back to 100% with
    // no state change; completing runs the exit animation. The
    // HomeScreen-level BackHandler for the open folder is gone —
    // this is the one callback for the folder (rule 58).
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = !exiting) { progress ->
        try {
            progress.collect { event -> backProgress.snapTo(event.progress) }
            requestDismiss()
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                backProgress.animateTo(0f, Motion.predictiveBackCancelSpring())
            }
        }
    }

    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember(folder.id, folder.name) { mutableStateOf(folder.name) }
    // Rule 59: a successful rename confirm fires one haptic.
    fun confirmRename() {
        if (nameDraft.isNotBlank() && nameDraft != folder.name) {
            viewModel.renameFolder(folder.id, nameDraft.trim())
            haptics.confirm()
        }
        editingName = false
    }
    val folderApps = folder.apps.mapNotNull { appsByComponent[it] }
    val folderContacts = folder.contacts
    val totalItems = folderApps.size + folderContacts.size
    // Rule 39: the folder grid comes from the grid record.
    val perPage = settings.gridSize.folderCols * settings.gridSize.folderRows
    val pageCount = maxOf(1, (totalItems + perPage - 1) / perPage)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed home behind the overlay (32% scrim, rule 18);
        // tapping outside the folder closes it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable { requestDismiss() },
        )
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        // Predictive-back preview (rule 58): the
                        // surface scales toward 90% with the swipe.
                        val backScale = 1f - (1f - Motion.PREDICTIVE_BACK_SCALE) * backProgress.value
                        scaleX = scale * backScale
                        scaleY = scale * backScale
                    },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                        .graphicsLayer { this.alpha = contentAlpha },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (editingName) {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { confirmRename() },
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { confirmRename() }) {
                                Icon(Icons.Filled.Check, contentDescription = "Confirm folder name")
                            }
                            IconButton(onClick = {
                                nameDraft = folder.name
                                editingName = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel renaming")
                            }
                        } else {
                            // The name spans the overlay, not the
                            // folder's width — tap it to rename.
                            Text(
                                folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        nameDraft = folder.name
                                        editingName = true
                                    },
                            )
                            IconButton(onClick = { requestDismiss() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close folder")
                            }
                        }
                    }
                    if (totalItems == 0) {
                        Text(
                            "Empty folder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            // Fixed height: three grid rows. The
                            // pager needs a bounded height for the
                            // grid inside it; the overlay itself
                            // wraps its content up to 560dp.
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        ) { page ->
                            val pageApps = folderApps.drop(page * perPage).take(perPage)
                            // Contacts follow the apps in the same
                            // paginated grid; a page holds 12 items
                            // of either kind.
                            val appsOnPage = pageApps.size
                            val contactsOnPage = if (appsOnPage < perPage) {
                                folderContacts.drop(
                                    (page * perPage - folderApps.size).coerceAtLeast(0),
                                ).take(perPage - appsOnPage)
                            } else {
                                emptyList()
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(settings.gridSize.folderCols),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(
                                    pageApps, key = { it.component },
                                ) { app ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(4.dp),
                                    ) {
                                        AppIconTile(
                                            app = app, settings = settings,
                                            resolveDrawable = resolveDrawable,
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = {
                                                viewModel.removeAppFromFolder(folder.id, app.component)
                                            },
                                            onRemove = {
                                                viewModel.removeAppFromFolder(folder.id, app.component)
                                            },
                                            onRename = { onRenameApp(app) },
                                            onUninstall = { viewModel.uninstallApp(app.packageName) },
                                        )
                                    }
                                }
                                items(
                                    contactsOnPage, key = { it.uri },
                                ) { contact ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .combinedClickable(
                                                onClick = { viewModel.openContact(contact.uri) },
                                                onLongClick = {
                                                    viewModel.removeContactFromFolder(folder.id, contact.uri)
                                                },
                                            ),
                                    ) {
                                        ContactTile(name = contact.name)
                                    }
                                }
                            }
                        }
                        if (pageCount > 1) {
                            PageIndicator(
                                style = settings.indicatorStyle, colour = settings.indicatorColour,
                                pageCount = pageCount, currentPage = pagerState.currentPage,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                    Text(
                        "Tap the name to rename. Long-press an app to remove it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
