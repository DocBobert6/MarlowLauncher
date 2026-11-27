package app.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.launcher.data.MIN_TOUCH_TARGET_DP

/**
 * Rebuilt settings controls (rules 62–64, 67).
 *
 * Row types (rule 63) are three visibly different rows:
 * - [SwitchRow] / [SliderRow] / [NavigationRow] — settings. A setting
 *   has a control or a chevron, and its subtext states its current
 *   VALUE ("5 × 5", "On", "System default") — never a description
 *   (rule 62).
 * - [StatusRow] — read-only status. No switch, no chevron, no ripple.
 *   Lives at the bottom of a screen or on About, never mid-list.
 * - [ActionRow] — a one-off verb (Verb + Object). No on/off state.
 *   Destructive actions are [destructive] (error colour) and confirmed
 *   by the caller.
 *
 * Type (rule 9): type-scale roles only — no `.sp` literals anywhere
 * in this file. Section headers ([SectionTitle], `titleMedium`) are
 * LARGER than row labels (`bodyMedium`); row values are `bodySmall`.
 * Shape (rule 15): no custom radii here — rows are full-width, and
 * dialogs/sheets keep the M3 28dp defaults.
 *
 * Controls (rule 64): switches over checkboxes (there are no
 * checkboxes); single-choice settings use radio on a sub-screen
 * ([RadioChoiceRow], rendered by SettingsScreen's choice screens) —
 * the old in-list `FilterChip` groups are gone (chips were 32dp, below
 * the 48dp target). Switch colours are the M3 tokens: selected track
 * `primary`, unselected track `surface-container-highest` + `outline`;
 * slider active track/thumb `primary`, inactive track
 * `surface-container-highest`. Toggles apply immediately; there is no
 * Save button (the grid-size-with-preview Apply exception does not
 * apply — Marlow has no grid preview, so grid size applies on tap
 * like every other choice).
 *
 * Every row is at least 48dp tall ([MIN_TOUCH_TARGET_DP], rule 68).
 * SwitchRow merges its label and switch into one TalkBack node
 * (label + on/off state read together); RadioChoiceRow likewise.
 * IconButton is 48dp by default.
 */

/** Section header — `titleMedium`, larger than row labels. */
@Composable
fun SectionTitle(text: String) {
    // Full-width opaque background: this title is also a stickyHeader
    // in SettingsScreen, so scrolled rows must not show through it.
    // The background matches the surface, so it is invisible elsewhere
    // (e.g. the Debug screen, which does not stick its headers).
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 4.dp),
    )
}

/**
 * A switch setting. [value] is the current value as text ("On" /
 * "Off") and is always shown (rule 62). When [enabled] is false the
 * switch is disabled and [disabledReason] is shown instead of the
 * value — the dependent setting stays visible with its reason rather
 * than disappearing (rule 63).
 */
@Composable
fun SwitchRow(
    label: String,
    value: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    disabledReason: String? = null,
    highlighted: Boolean = false,
) {
    // Rule 59: a toggle flip fires one crisp haptic, gated by
    // settings.hapticsEnabled via LocalMarlowHaptics. The local is
    // read here (composable context) — not inside onCheckedChange,
    // which is not one. The flip that turns haptics off still gives
    // its feedback, since the old instance is still enabled at
    // this moment.
    val haptics = LocalMarlowHaptics.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (!enabled && disabledReason != null) disabledReason else value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            // Rule 59: a toggle flip fires one crisp haptic (on =
            // click, off = tick), gated by settings.hapticsEnabled.
            onCheckedChange = { new ->
                haptics.toggleFlip(new)
                onChange(new)
            },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/**
 * A slider setting. [valueText] is the current value as text (a plain
 * number, no units — rule 62; the audit fix stripped the dp units and
 * the values with them, so values are restored here without units).
 * The row is at least 48dp tall (rule 68).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
    enabled: Boolean = true,
    disabledReason: String? = null,
    highlighted: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Text(
                text = if (!enabled && disabledReason != null) disabledReason else valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Round thumb, supplied explicitly: Material 3 1.4.0-alpha06's
        // default thumb is a tall vertical bar (the Expressive thumb),
        // and this version's Slider takes a custom `thumb` slot, so
        // the round thumb is drawn here instead of downgrading M3.
        // Code-verified only — the look on a real device is Robert's
        // check, not this change's.
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

/**
 * A row that navigates to a sub-screen (or opens a dialog/sheet).
 * [value] is the current value of the setting behind it ("5 × 5",
 * "System default") — shown as subtext (rule 62). Null for pure
 * actions, which have no value. The chevron marks it as navigation,
 * unlike [StatusRow] (no chevron, not tappable) and [ActionRow].
 */
@Composable
fun NavigationRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    disabledReason: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            val subtext = if (!enabled && disabledReason != null) disabledReason else value
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A read-only status line (rule 63): label + value, no switch, no
 * chevron, no ripple — it is not tappable. Lives at the bottom of a
 * screen or on About, never mixed into the middle of toggles.
 */
@Composable
fun StatusRow(label: String, value: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A one-off action row (rule 63): a Verb + Object label, no on/off
 * state, tappable with a ripple. [destructive] renders it in the
 * error colour; destructive actions are always confirmed by the
 * caller before they run.
 */
@Composable
fun ActionRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    highlighted: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
    }
}

/**
 * One radio option on a choice sub-screen (rule 64): single-choice
 * settings use radio on a sub-screen, never radio rows in a list.
 * The native RadioButton carries the selected state for TalkBack.
 */
@Composable
fun RadioChoiceRow(
    label: String,
    supportingText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
            .then(if (highlighted) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ReorderRow(label: String, canUp: Boolean, canDown: Boolean, onUp: () -> Unit, onDown: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET_DP.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onUp, enabled = canUp) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
        }
        IconButton(onClick = onDown, enabled = canDown) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
        }
    }
}

/**
 * The permission priming dialog (rule 67): every permission ask goes
 * through this first. One sentence stating what the user gets;
 * [onDismiss] ("Not now") cancels WITHOUT the system dialog ever
 * firing; the system dialog fires only after opt-in on [onAllow].
 * A denial degrades only the dependent feature — the caller leaves
 * the feature off and shows a one-line explanation on its row.
 */
@Composable
fun PermissionPrimingDialog(
    title: String,
    message: String,
    allowLabel: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onAllow() }) { Text(text = allowLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Not now") }
        },
    )
}

/** #173 display label for a long-press menu item id. */
fun menuLabel(id: String): String = when (id) {
    "shortcuts" -> "App shortcuts"
    "app_info" -> "App info"
    "screen_time" -> "Screen time"
    "rename" -> "Rename"
    "edit_icon" -> "Edit icon"
    "add_to_home" -> "Add to Home screen"
    "add_to_folder" -> "Add to drawer folder"
    "pair_with" -> "Pair with another app"
    "uninstall" -> "Uninstall"
    else -> id
}

/** #173 display label for an overview action id. */
fun overviewLabel(id: String): String = when (id) {
    "set_default" -> "Set as default"
    "hide_page" -> "Hide / show page"
    "move_left" -> "Move page left"
    "move_right" -> "Move page right"
    "add_page" -> "Add page"
    "delete_page" -> "Delete page"
    else -> id
}
