package app.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import app.launcher.model.AppEntry
import app.launcher.model.AppLanguageOverride

/**
 * #34 hide apps + PIN vault. Hidden apps are excluded from the
 * drawer, search and home (see HomeViewModel.visibleApps); the
 * vault lists them only after the PIN is entered. The PIN is
 * 4–8 digits and is stored only as a salted hash (PinCrypto).
 *
 * #155 per-app language: the dialog below records a BCP 47 tag for
 * an app and opens the system per-app language screen for it — the
 * system applies the locale; the launcher only stores the choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    hiddenApps: List<AppEntry>,
    hasPin: Boolean,
    unlocked: Boolean,
    onUnlock: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onUnhideApp: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var settingPin by remember { mutableStateOf(false) }
    var pinConfirm by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Audit fix (2026-11-26): the sheet clears the navigation
        // bar at its bottom; no status-bar inset (see
        // NotificationPanelSheet — a bottom sheet never reaches
        // the status bar).
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Hidden apps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            if (!hasPin || settingPin) {
                Text(
                    if (hasPin) "Change the vault PIN (4–8 digits)." else "Set a vault PIN (4–8 digits) to hide apps.",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pinConfirm,
                    onValueChange = { pinConfirm = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("Repeat PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (pinError != null) {
                    Text(text = pinError!!, modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    onClick = {
                        when {
                            pin.length < 4 -> pinError = "The PIN needs at least 4 digits."
                            pin != pinConfirm -> pinError = "The two PINs do not match."
                            else -> {
                                onSetPin(pin)
                                pin = ""
                                pinConfirm = ""
                                pinError = null
                                settingPin = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text(if (hasPin) "Change PIN" else "Set PIN") }
            } else if (!unlocked) {
                Text("Enter the vault PIN to see hidden apps.", modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pinError != null) {
                    Text(text = pinError!!, modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    onClick = {
                        if (onUnlock(pin)) {
                            pin = ""
                            pinError = null
                        } else {
                            pinError = "Wrong PIN."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Unlock") }
            } else {
                if (hiddenApps.isEmpty()) {
                    Text("No apps are hidden. Hide one from its long-press sheet.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(hiddenApps, key = { it.component }) { app ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(
                                    text = app.displayLabel,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                )
                                TextButton(onClick = { onLaunchApp(app) }) { Text("Open") }
                                TextButton(onClick = { onUnhideApp(app) }) { Text("Unhide") }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { settingPin = true; pin = ""; pinConfirm = "" },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Change PIN", modifier = Modifier.fillMaxWidth()) }
                TextButton(
                    onClick = {
                        onClearPin()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Remove PIN (unhides nothing — apps stay hidden)", modifier = Modifier.fillMaxWidth()) }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

/** #155 per-app language dialog: pick a tag, store it, and open
 *  the system per-app language screen for that app. Common tags are
 *  offered as buttons; any other BCP 47 tag can be typed in. */
@Composable
fun AppLanguageDialog(
    app: AppEntry,
    current: AppLanguageOverride?,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tag by remember { mutableStateOf(current?.languageTag ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Language — ${app.displayLabel}") },
        text = {
            Column {
                Text("The system applies the language; the launcher stores your choice and opens the system per-app language screen.")
                if (current != null) {
                    Text("Stored choice: ${current.languageTag}", modifier = Modifier.padding(top = 8.dp))
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.trim() },
                    label = { Text("Language tag (e.g. es, fr, de)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    for (common in listOf("en", "es", "fr", "de", "zh", "hi", "ru")) {
                        TextButton(onClick = { tag = common }) { Text(common) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (tag.isNotBlank()) onSave(tag)
                onOpenSystemSettings()
                onDismiss()
            }) { Text("Save & open system settings") }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
