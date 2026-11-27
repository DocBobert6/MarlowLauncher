package app.launcher.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.launcher.model.SearchResult
import app.launcher.model.SearchResultKind

/**
 * Universal search (#18, #104, #106, #107 voice only).
 *
 * Full-screen search opened by the dock pill (#112). Rows, in order:
 * calculator answer, timer, direct-dial, apps, contacts, settings,
 * files, provider suggestions (toggle-gated, #104), web. The search
 * bar also carries voice search (#107): the system recogniser
 * (RecognizerIntent) — there is NO visual / camera search, which
 * the #107 decision explicitly excluded.
 *
 * The search itself lives in SearchEngine (data/); this file is
 * only the surface. Tapping a row hands off to the ViewModel, which
 * launches the app / dialler / timer / browser / contact / file, or
 * opens the settings sheet for a setting row.
 */
@Composable
fun UniversalSearchScreen(
    results: List<SearchResult>,
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenContact: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDial: (String) -> Unit,
    onSetTimer: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    // #107 voice search: the system speech recogniser fills the query.
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                query = spoken
                onQueryChange(spoken)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (initialQuery.isNotEmpty()) onQueryChange(initialQuery)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Audit fix (2026-11-26): a full-screen surface, so it
        // clears both system bars itself.
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { value ->
                        query = value
                        onQueryChange(value)
                    },
                    placeholder = { Text("Search apps, contacts, files, web…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                        )
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                                    }
                                    voiceLauncher.launch(intent)
                                },
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice search")
                            }
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; onQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                }
            }
            if (query.isBlank()) {
                Text(
                    text = "Apps, contacts, settings, files and the web — plus " +
                        "calculator, timer and direct-dial. Tap the microphone to speak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(results, key = { it.kind.name + "|" + it.title + "|" + it.payload }) { result ->
                    SearchResultRow(
                        result = result,
                        onClick = {
                            when (result.kind) {
                                SearchResultKind.APP -> onLaunchApp(result.payload)
                                SearchResultKind.CONTACT -> onOpenContact(result.payload)
                                SearchResultKind.FILE -> onOpenFile(result.payload)
                                SearchResultKind.WEB, SearchResultKind.SUGGESTION -> onOpenUrl(result.payload)
                                SearchResultKind.DIAL -> onDial(result.payload)
                                SearchResultKind.TIMER -> result.payload.toIntOrNull()?.let { onSetTimer(it) }
                                SearchResultKind.SETTING -> onOpenSettings()
                                SearchResultKind.CALCULATOR -> Unit // answer row: display only.
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    val icon = when (result.kind) {
        SearchResultKind.CALCULATOR -> Icons.Filled.Calculate
        SearchResultKind.TIMER -> Icons.Filled.Timer
        SearchResultKind.DIAL -> Icons.Filled.Phone
        SearchResultKind.APP -> Icons.Filled.Apps
        SearchResultKind.CONTACT -> Icons.Filled.Contacts
        SearchResultKind.SETTING -> Icons.Filled.Settings
        SearchResultKind.FILE -> Icons.Filled.Description
        SearchResultKind.WEB, SearchResultKind.SUGGESTION -> Icons.Filled.Search
    }
    val clickable = result.kind != SearchResultKind.CALCULATOR
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(text = result.title, style = MaterialTheme.typography.bodyLarge)
            result.subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
