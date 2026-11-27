package app.launcher.data

import android.util.Xml
import app.launcher.model.FeedItem
import app.launcher.model.FeedSource
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * RSS / Discover feed (#38): fetches user-added feeds over plain
 * HTTP(S) and parses RSS 2.0 (item) and Atom (entry) with the
 * platform pull parser. No feed library is a declared dependency.
 *
 * The fetch runs on the caller's IO dispatcher. A feed that fails to
 * load (bad URL, no network, malformed XML) yields no items and the
 * error text, never invented entries — the feed page shows the gap.
 */
object FeedRepository {

    data class FetchResult(
        val items: List<FeedItem>,
        val error: String?,
    )

    private const val MAX_ITEMS_PER_SOURCE = 20

    /** Fetches every source and merges the entries, newest first. */
    fun fetchAll(sources: List<FeedSource>): FetchResult {
        val items = ArrayList<FeedItem>()
        var firstError: String? = null
        for (source in sources) {
            val result = fetchOne(source)
            items.addAll(result.items)
            if (result.error != null && firstError == null) firstError = result.error
        }
        items.sortByDescending { it.publishedMillis }
        return FetchResult(items, firstError)
    }

    /** Fetches one source. Never throws. */
    fun fetchOne(source: FeedSource): FetchResult {
        return try {
            val url = URL(source.url)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Launcher RSS reader")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return FetchResult(emptyList(), "${source.title}: HTTP ${connection.responseCode}")
            }
            val items = connection.inputStream.use { stream ->
                parse(stream, source)
            }
            FetchResult(items, null)
        } catch (e: Exception) {
            FetchResult(emptyList(), "${source.title}: ${e.message ?: "could not load feed"}")
        }
    }

    /**
     * Parses an RSS 2.0 or Atom document. The channel/feed title is
     * ignored (the source's own title is used); each item/entry
     * contributes title, link and an optional date + summary.
     */
    fun parse(stream: java.io.InputStream, source: FeedSource): List<FeedItem> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)
        val items = ArrayList<FeedItem>()
        var title: String? = null
        var link: String? = null
        var summary: String? = null
        var published = 0L
        var inItem = false
        var index = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < MAX_ITEMS_PER_SOURCE) {
            val name = parser.name ?: ""
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (name == "item" || name == "entry") {
                        inItem = true
                        title = null
                        link = null
                        summary = null
                        published = 0L
                    } else if (inItem) {
                        when (name) {
                            "title" -> title = parser.nextText().trim()
                            "link" -> {
                                // Atom links carry the URL in href.
                                val href = parser.getAttributeValue(null, "href")
                                link = href?.takeIf { it.isNotBlank() } ?: parser.nextText().trim()
                            }
                            "description", "summary", "content" -> {
                                if (summary == null) summary = parser.nextText().trim()
                            }
                            "pubDate", "published", "updated" -> {
                                published = parseDate(parser.nextText().trim())
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((name == "item" || name == "entry") && inItem) {
                        inItem = false
                        val itemTitle = title?.takeIf { it.isNotBlank() } ?: continue
                        val itemLink = link?.takeIf { it.isNotBlank() } ?: ""
                        items.add(
                            FeedItem(
                                id = source.id + "-" + (index++),
                                sourceId = source.id,
                                sourceTitle = source.title,
                                title = itemTitle,
                                link = itemLink,
                                publishedMillis = published,
                                summary = summary?.takeIf { it.isNotBlank() }?.take(300),
                            ),
                        )
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    /** Parses an RFC 822 (RSS) or ISO 8601 (Atom) date; 0 when unparsed. */
    fun parseDate(text: String): Long {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
        )
        for (pattern in formats) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(text)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // Try the next format.
            }
        }
        return 0L
    }
}
