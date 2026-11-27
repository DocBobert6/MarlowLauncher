package app.launcher.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.launcher.data.CalendarGlanceProvider
import app.launcher.model.AppEntry
import app.launcher.model.CalendarEvent
import app.launcher.model.FeedItem
import app.launcher.model.FeedSource
import app.launcher.model.LauncherSettings
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Feed page (#38) — rule 36, decision F16: PERSONAL CONTEXT FIRST.
 * Calendar (next event, permission-gated), people/recents leading;
 * RSS news secondary, below. Every card is optional, reorderable
 * and removable. Disabling the feed leaves no dead page and no
 * placeholder (the home simply has no feed page). There is no
 * Google feed here and no negative notice about it — the page says
 * plainly what the feed is: your day first, news below.
 *
 * This page is a feed, not a home replacement (#114 denied): the
 * icon grid home stays the home.
 */
@Composable
fun FeedPageScreen(
    sources: List<FeedSource>,
    items: List<FeedItem>,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onAddSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onOpenItem: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
    // Audit fix (2026-11-26): the pager copy sits inside the home
    // Column, which already carries statusBarsPadding — so the home
    // passes topInset = false there and the inset is not doubled.
    // The two-pane copy is a standalone pane and keeps it. The
    // navigation-bar inset always applies (the page reaches the
    // screen bottom in two-pane mode).
    topInset: Boolean = true,
    // Rule 36: personal context first — today's events (only when
    // the calendar permission is granted) and recently used apps.
    todayEvents: List<CalendarEvent> = emptyList(),
    recentApps: List<AppEntry> = emptyList(),
    calendarConnected: Boolean = false,
    onOpenApp: (AppEntry) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    settings: LauncherSettings? = null,
    resolveDrawable: (AppEntry) -> Drawable? = { null },
) {
    var draftUrl by remember { mutableStateOf("") }
    // Rule 36: every card optional, reorderable, removable — the
    // order and the dismissals are the page's own state.
    var cardOrder by remember { mutableStateOf(listOf("calendar", "recents", "news")) }
    var dismissed by remember { mutableStateOf(setOf<String>()) }
    val now = System.currentTimeMillis()
    val nextEvent = todayEvents.firstOrNull { it.startMillis + 60L * 60 * 1000 > now }

    Column(
        modifier = modifier.fillMaxSize()
            .then(if (topInset) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your feed",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh feed")
            }
        }
        Text(
            text = "Your day first — calendar, people and recents — with news below. " +
                "Cards can be moved or removed; turning the feed off removes this page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            for (cardId in cardOrder) {
                if (cardId in dismissed) continue
                when (cardId) {
                    "calendar" -> item(key = "card-calendar") {
                        FeedCard(
                            title = "Up next",
                            onMoveUp = { cardOrder = moveCard(cardOrder, "calendar", -1) },
                            onMoveDown = { cardOrder = moveCard(cardOrder, "calendar", 1) },
                            onDismiss = { dismissed = dismissed + "calendar" },
                        ) {
                            if (!calendarConnected) {
                                Text(
                                    text = "Calendar isn't connected — grant calendar access in Settings to see your next event here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (nextEvent != null) {
                                Text(
                                    text = nextEvent.title + " · " + CalendarGlanceProvider.formatEventTime(nextEvent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onOpenCalendar() },
                                )
                                val following = todayEvents.filter { it != nextEvent }.take(2)
                                for (event in following) {
                                    Text(
                                        text = event.title + " · " + CalendarGlanceProvider.formatEventTime(event),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp).clickable { onOpenCalendar() },
                                    )
                                }
                            } else {
                                Text(
                                    text = "Nothing scheduled for the rest of today.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    "recents" -> item(key = "card-recents") {
                        FeedCard(
                            title = "Recent apps",
                            onMoveUp = { cardOrder = moveCard(cardOrder, "recents", -1) },
                            onMoveDown = { cardOrder = moveCard(cardOrder, "recents", 1) },
                            onDismiss = { dismissed = dismissed + "recents" },
                        ) {
                            if (recentApps.isEmpty()) {
                                Text(
                                    text = "Apps you open will show up here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    for (app in recentApps.take(5)) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { onOpenApp(app) },
                                        ) {
                                            if (settings != null) {
                                                AppIconImage(
                                                    app = app,
                                                    settings = settings,
                                                    resolveDrawable = resolveDrawable,
                                                    iconSize = 48.dp,
                                                )
                                            }
                                            Text(
                                                text = app.displayLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "news" -> item(key = "card-news") {
                        FeedCard(
                            title = "News",
                            onMoveUp = { cardOrder = moveCard(cardOrder, "news", -1) },
                            onMoveDown = { cardOrder = moveCard(cardOrder, "news", 1) },
                            onDismiss = { dismissed = dismissed + "news" },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = draftUrl,
                                    onValueChange = { draftUrl = it },
                                    label = { Text("Add RSS feed URL") },
                                    placeholder = { Text("https://example.com/feed.xml") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    if (draftUrl.isNotBlank()) {
                                        onAddSource(draftUrl.trim())
                                        draftUrl = ""
                                    }
                                }) {
                                    Text("Add")
                                }
                            }
                            for (source in sources) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = source.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { onRemoveSource(source.id) }, modifier = Modifier.padding(start = 4.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove feed")
                                    }
                                }
                            }
                            if (sources.isEmpty()) {
                                Text(
                                    text = "No news sources yet — add an RSS URL above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            if (items.isEmpty() && sources.isNotEmpty() && !refreshing) {
                                Text(
                                    text = "No entries loaded yet — tap Refresh.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            for (item in items) {
                                FeedItemRow(item = item, onClick = { onOpenItem(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun moveCard(order: List<String>, id: String, delta: Int): List<String> {
    val index = order.indexOf(id)
    val target = (index + delta).coerceIn(0, order.size - 1)
    if (index < 0 || target == index) return order
    val mutable = order.toMutableList()
    val moved = mutable.removeAt(index)
    mutable.add(target, moved)
    return mutable
}

/** One feed card: title row with reorder + remove, then content. */
@Composable
private fun FeedCard(
    title: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move card up")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move card down")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove card")
                }
            }
            content()
        }
    }
}

@Composable
private fun FeedItemRow(item: FeedItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                text = item.sourceTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (item.publishedMillis > 0) {
                Text(
                    text = dateFormat.format(item.publishedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!item.summary.isNullOrBlank()) {
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
