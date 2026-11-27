package app.launcher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry

/**
 * #61 contextual music apps on headphone connect. When headphones
 * are connected (and the toggle is on), this sheet offers the
 * installed music apps — the user picks one, nothing auto-plays.
 * "Music apps" here means installed apps in the MEDIA category
 * whose label or package suggests music/audio playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadphoneMusicSheet(
    musicApps: List<AppEntry>,
    onLaunchApp: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text("Headphones connected", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
            }
            item {
                Text(
                    text = "Pick a music app to open:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (musicApps.isEmpty()) {
                item { Text("No music apps found on this phone.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(musicApps, key = { it.component }) { app ->
                    TextButton(
                        onClick = { onLaunchApp(app) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = app.displayLabel, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
