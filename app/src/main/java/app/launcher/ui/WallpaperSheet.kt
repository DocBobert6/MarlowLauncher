package app.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import app.launcher.model.LauncherSettings

/**
 * Wallpaper (#53 note, #77) and stickers (#58).
 *
 * The wallpaper picker is the system picker (ACTION_GET_CONTENT,
 * image mime type) — only photos / wallpapers / other apps already on the
 * phone. NO built-in sources. Picking applies the image as the
 * system wallpaper via WallpaperManager and turns the home's
 * wallpaper layer on.
 *
 * #77 scrolling wallpaper (the wallpaper pans with the home pages),
 * gradient and dimming are toggles/sliders here, all OFF / 0 by
 * default. The dim/gradient overlays are drawn by HomeScreen over
 * the transparent home surface.
 */

/** #53 / #77 wallpaper controls, opened from settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSheet(
    settings: LauncherSettings,
    viewModel: HomeViewModel,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text("Wallpaper", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            item {
                Text(
                    text = "Pick from photos, wallpapers or other apps already on your phone. " +
                        "There are no built-in wallpapers.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                TextButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a wallpaper…", modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                SwitchRowLocal(
                    label = "Show wallpaper behind the home screen",
                    checked = settings.wallpaperVisible,
                    onChange = { checked -> viewModel.updateSettings { it.copy(wallpaperVisible = checked) } },
                )
            }
            item {
                SwitchRowLocal(
                    label = "Scrolling wallpaper (pans with pages)",
                    checked = settings.scrollingWallpaper,
                    onChange = { checked -> viewModel.updateSettings { it.copy(scrollingWallpaper = checked) } },
                )
            }
            item {
                SwitchRowLocal(
                    label = "Gradient (darken top and bottom)",
                    checked = settings.wallpaperGradient,
                    onChange = { checked -> viewModel.updateSettings { it.copy(wallpaperGradient = checked) } },
                )
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = "Dimming (${settings.wallpaperDimPercent}%)")
                    Slider(
                        value = settings.wallpaperDimPercent.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateSettings { s -> s.copy(wallpaperDimPercent = value.toInt()) }
                        },
                        valueRange = 0f..70f,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRowLocal(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** #58 add / edit a home-screen sticker (free text). */
@Composable
fun StickerDialog(
    initialText: String,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText.isEmpty()) "Add sticker" else "Edit sticker") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write anything…") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
