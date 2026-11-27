package app.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import app.launcher.data.NotificationPreview
import app.launcher.model.AppEntry
import java.text.DateFormat
import java.util.Date

/**
 * #127 inline notification text previews + direct reply on the
 * home screen. The sheet lists the live notifications the listener
 * currently sees, grouped by app, with the notification's own text.
 * Direct reply goes through the notification's RemoteInput action
 * (#127: only where the app supports it — rows without a reply
 * action show no reply field). The sheet is toggle-gated
 * (inlinePreviewsEnabled) and is also what swipe-down opens (#26).
 *
 * When the listener has not been granted, the sheet says so and
 * offers the system settings screen — previews are simply absent
 * until then; nothing is invented.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPanelSheet(
    previews: Map<String, List<NotificationPreview>>,
    appsByPackage: Map<String, AppEntry>,
    listenerConnected: Boolean,
    onReply: (key: String, text: String) -> Unit,
    onOpenListenerSettings: () -> Unit,
    onDismiss: () -> Unit,
    // The panel opens on swipe-down even when previews are off —
    // it says so here instead of opening nothing at all.
    previewsEnabled: Boolean = true,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Audit fix (2026-11-26): the sheet clears the navigation
        // bar at its bottom. No status-bar inset: a bottom sheet
        // never reaches the status bar, and one there would only
        // add dead space at the top of the sheet.
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Notifications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            if (!listenerConnected) {
                Text("Notification access is not granted yet — the launcher cannot see notifications until you allow it.")
                TextButton(onClick = onOpenListenerSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open notification-access settings", modifier = Modifier.fillMaxWidth())
                }
            } else if (!previewsEnabled) {
                Text("Inline notification previews are off — turn them on in Settings → Notifications to see previews here.")
            } else if (previews.isEmpty()) {
                Text("No notifications right now.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    for ((packageName, list) in previews) {
                        val label = appsByPackage[packageName]?.displayLabel ?: packageName
                        item {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(list, key = { it.key }) { preview ->
                            NotificationPreviewRow(
                                preview = preview,
                                onReply = onReply,
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun NotificationPreviewRow(
    preview: NotificationPreview,
    onReply: (key: String, text: String) -> Unit,
) {
    var replyText by remember(preview.key) { mutableStateOf("") }
    var replySent by remember(preview.key) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (preview.title != null) {
            Text(text = preview.title, style = MaterialTheme.typography.bodyMedium)
        }
        if (preview.text != null) {
            Text(text = preview.text, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(preview.postedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (preview.hasRemoteInput && !replySent) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Reply") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(preview.key, replyText.trim())
                            replySent = true
                        }
                    },
                ) { Text("Send") }
            }
        } else if (preview.hasRemoteInput) {
            Text(text = "Reply sent.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
