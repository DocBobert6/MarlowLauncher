package app.launcher.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry
import app.launcher.model.DOCK_MAX_ITEMS
import app.launcher.model.HomeItem
import app.launcher.model.HomeItemKind
import app.launcher.model.LauncherSettings
import app.launcher.model.SurfaceStyle

/**
 * Dock / favourites row (#4 hide/resize/add-remove, #71
 * background/shadow styling, #112 fixed bottom search pill,
 * toggleable).
 *
 * Decision F5 (rules 25–26): EXACTLY five icons, labels OFF, one
 * page, seeded by kind — Phone / Messages / browser / mail /
 * Camera, in that order (see HomeViewModel.dockSeedApps). There are
 * no dock pages and no page indicators any more; the cap is enforced
 * at every mutation in the ViewModel (a sixth icon is refused, never
 * silently dropped) and the render takes at most five as a guard.
 * The search pill is NOT a dock icon and does not count toward the
 * five: it stays a separate element below the dock.
 *
 * Rules 6/17 (colours): the dock uses `surface-container` and the
 * search pill uses `surface-container-high` — more than one step
 * apart, so the two read as separate levels with no shadow at all.
 * Depth is tonal, not shadow: the old `shadow(8.dp)` is gone, and
 * the 1dp outline is gone too — the container steps separate the
 * pills on their own. Blur glass keeps its translucency as an alpha
 * over the same roles.
 */
@Composable
fun DockBar(
    dockItems: List<HomeItem>,
    appsByComponent: Map<String, AppEntry>,
    settings: LauncherSettings,
    resolveDrawable: (AppEntry) -> Drawable?,
    onTapItem: (HomeItem) -> Unit,
    onLongPressItem: (HomeItem) -> Unit,
    onSearchPillClick: () -> Unit,
    // Rule 69: TalkBack custom actions on dock tiles (Remove /
    // Rename / Uninstall), wired by the home to the ViewModel
    // actions and the long-press sheet's rename dialog.
    onRemoveItem: (HomeItem) -> Unit = {},
    onRenameApp: (AppEntry) -> Unit = {},
    onUninstallApp: (String) -> Unit = {},
    // Phase 4: notification badges (#31, keyed by package name).
    badgeCounts: Map<String, Int> = emptyMap(),
    badgeBitmaps: Map<String, Bitmap?> = emptyMap(),
    // Rule 28: a spoken query from the pill mic opens search with
    // the words already in it.
    onVoiceQuery: (String) -> Unit = {},
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
    modifier: Modifier = Modifier,
) {
    // #112 the search pill lives outside the dockVisible guard: hiding
    // the dock must not also hide the pill.
    // Decision F5: exactly five icons, labels off, one page. The
    // ViewModel enforces the cap at every mutation; take(5) here is
    // a render guard only.
    val items = dockItems.take(DOCK_MAX_ITEMS)
    // The dock row must fit a whole tile — the icon
    // (settings.iconSizeDp) PLUS 8dp of breathing room. Labels are
    // off in the dock, so the old +18dp label allowance is gone. A
    // taller dockHeightDp setting still wins; the floor is what
    // stops the clip. The navigation-bar inset sits at the bottom
    // of this column, so the dock and pill clear the gesture bar.
    val dockRowHeightDp = maxOf(settings.dockHeightDp, settings.iconSizeDp + 8)
    // Rules 6/17: dock = surface-container. Blur glass applies its
    // translucency over the same role.
    val dockColor = MaterialTheme.colorScheme.surfaceContainer.let { base ->
        if (settings.surfaceStyle == SurfaceStyle.BLUR_GLASS) base.copy(alpha = settings.surfaceAlpha) else base
    }
    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (settings.dockVisible) {
        val dockContent: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockRowHeightDp.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (item in items) {
                    val app = item.component?.let { appsByComponent[it] }
                    when (item.kind) {
                        HomeItemKind.APP -> if (app != null) {
                            AppIconTile(
                                app = app, settings = settings,
                                resolveDrawable = resolveDrawable,
                                onClick = { onTapItem(item) },
                                onLongClick = { onLongPressItem(item) },
                                badgeCount = badgeCounts[app.packageName] ?: 0,
                                badgeBitmap = badgeBitmaps[app.packageName],
                                overWallpaper = overWallpaper,
                                wallpaperLuminance = wallpaperLuminance,
                                onRemove = { onRemoveItem(item) },
                                onRename = { onRenameApp(app) },
                                onUninstall = { onUninstallApp(app.packageName) },
                                // Decision F5: labels are always off
                                // in the dock, whatever the global
                                // label setting is.
                                showLabel = false,
                            )
                        }
                        HomeItemKind.CONTACT -> Box(Modifier.clickable { onTapItem(item) }) {
                            ContactTile(name = item.contactName ?: item.label ?: "Contact", showLabel = false)
                        }
                        HomeItemKind.SHORTCUT -> Box(Modifier.clickable { onTapItem(item) }) {
                            ShortcutTile(label = item.label ?: "Shortcut", showLabel = false)
                        }
                        HomeItemKind.APP_PAIR -> Box(Modifier.clickable { onTapItem(item) }) {
                            PairTile(label = item.label ?: "App Pair", showLabel = false)
                        }
                        // #58 stickers are home-only; a sticker
                        // dragged to the dock just shows its text.
                        HomeItemKind.STICKER -> Text(
                            text = item.label ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onTapItem(item) },
                        )
                        HomeItemKind.FOLDER -> Box(
                            modifier = Modifier
                                .size(settings.iconSizeDp.dp)
                                .clickable { onTapItem(item) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = "Folder")
                        }
                        // Phase 3: widgets are never placed in
                        // the dock (#69 removed) — nothing
                        // renders if one ever lands here.
                        HomeItemKind.WIDGET,
                        HomeItemKind.BUILTIN_WIDGET,
                        HomeItemKind.WIDGET_STACK -> Unit
                    }
                }
            }
        }
        if (settings.dockBackground) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = MaterialTheme.shapes.large,
                color = dockColor,
                content = dockContent,
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) { dockContent() }
        }
        }
        if (settings.searchPillVisible) {
            SearchPill(onClick = onSearchPillClick, settings = settings, onVoiceQuery = onVoiceQuery)
        }
    }
}

/**
 * #112 fixed bottom search pill. Phase 2: it opens universal search
 * (#18) — apps, contacts, settings, files, web, calculator, timer,
 * direct-dial — instead of the Phase 1 drawer-search entry point.
 *
 * Rule 28: one entry point, bottom, below the dock, on every page;
 * the pill is Marlow unified search — device-first, web via the
 * chosen search provider. The mic is a WORKING trailing icon (the
 * system speech recogniser; the spoken words open search already
 * typed). Lens is OMITTED: no Lens API exists for third-party
 * launchers, so there is nothing truthful to wire it to.
 */
@Composable
fun SearchPill(
    onClick: () -> Unit,
    settings: LauncherSettings,
    onVoiceQuery: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Rule 28 / #107: the system speech recogniser, launched from
    // the pill itself. The spoken result opens unified search with
    // the query already in it; if no recogniser exists, the mic
    // just opens search.
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onVoiceQuery(spoken)
            } else {
                onClick()
            }
        }
    }
    // Rule 6: the pill is surface-container-high — more than one
    // step above the dock's surface-container.
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh.let { base ->
        if (settings.surfaceStyle == SurfaceStyle.BLUR_GLASS) base.copy(alpha = settings.surfaceAlpha) else base
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .semantics { contentDescription = "Search" },
        shape = CircleShape,
        color = pillColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Text(
                text = "Search apps",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        onClick()
                    }
                },
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice search")
            }
        }
    }
}
