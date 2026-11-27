package app.launcher.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import app.launcher.model.AppEntry
import app.launcher.model.SearchProvider
import app.launcher.model.SearchResult
import app.launcher.model.SearchResultKind
import app.launcher.model.SettingSearchEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.pow

/**
 * Universal search (#18, #104, #106): apps, contacts, settings,
 * files, web, plus the in-search calculator, timer and direct-dial.
 * Voice search (#107) is the recogniser intent launched from the
 * search UI — there is NO visual / camera search (explicitly not
 * included in the #107 decision).
 *
 * Everything here runs on Dispatchers.IO (content-provider queries
 * and the suggestion fetch are blocking). Nothing here invents data:
 * a source that cannot be read returns no rows.
 */
object SearchEngine {

    /** #104 built-in providers. DuckDuckGo is the default (it is the
     *  search Robert already chose for AutoSpace's browser); the
     *  default is a choice for review, not a plan decision. */
    fun defaultProviders(): List<SearchProvider> = listOf(
        SearchProvider(
            id = "duckduckgo",
            name = "DuckDuckGo",
            queryUrl = "https://duckduckgo.com/?q=%s",
            suggestionUrl = "https://ac.duckduckgo.com/ac/?q=%s&type=list",
        ),
        SearchProvider(
            id = "google",
            name = "Google",
            queryUrl = "https://www.google.com/search?q=%s",
            suggestionUrl = "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
        ),
        SearchProvider(
            id = "bing",
            name = "Bing",
            queryUrl = "https://www.bing.com/search?q=%s",
            suggestionUrl = "https://www.bing.com/AS/Suggestions?q=%s&cvid=1",
        ),
        SearchProvider(
            id = "brave",
            name = "Brave Search",
            queryUrl = "https://search.brave.com/search?q=%s",
            suggestionUrl = "https://search.brave.com/api/suggest?q=%s",
        ),
    )

    /** #18 the launcher settings searchable from universal search.
     *  Tapping one opens the settings sheet. */
    fun settingEntries(): List<SettingSearchEntry> = listOf(
        SettingSearchEntry("Grid size", "Home grid"),
        SettingSearchEntry("Infinite scroll", "Home grid"),
        SettingSearchEntry("Page indicators", "Home grid"),
        SettingSearchEntry("Dock", "Show, height, background, shadow"),
        SettingSearchEntry("Search pill in dock", "Dock"),
        SettingSearchEntry("Drawer style", "App drawer"),
        SettingSearchEntry("Sort apps by", "App drawer"),
        SettingSearchEntry("Frequently used row", "App drawer"),
        SettingSearchEntry("Icon shape", "Icons"),
        SettingSearchEntry("Icon size", "Icons"),
        SettingSearchEntry("Show app labels", "Icons"),
        SettingSearchEntry("Themed icons", "Icons"),
        SettingSearchEntry("Live clock icon", "Icons"),
        SettingSearchEntry("Icon pack", "Icons"),
        SettingSearchEntry("One-handed layout", "Behaviour"),
        SettingSearchEntry("Lock home-screen layout", "Behaviour"),
        SettingSearchEntry("Add new apps automatically", "Behaviour"),
        SettingSearchEntry("Theme mode", "Theme — Auto, Light, Dark"),
        SettingSearchEntry("Theme", "Material You or Stock"),
        SettingSearchEntry("Accent colour", "Theme"),
        SettingSearchEntry("Font", "Theme"),
        SettingSearchEntry("Surface style", "Opaque or blur glass"),
        SettingSearchEntry("AMOLED black", "Theme"),
        SettingSearchEntry("At a Glance", "Home strip"),
        SettingSearchEntry("Smartspacer", "At a Glance"),
        SettingSearchEntry("Now Brief", "Daily summary"),
        SettingSearchEntry("Search provider", "Search"),
        SettingSearchEntry("Search suggestions", "Search"),
        SettingSearchEntry("Hide status bar", "Wallpaper & display"),
        SettingSearchEntry("Wallpaper", "Wallpaper & display"),
        SettingSearchEntry("Scrolling wallpaper", "Wallpaper & display"),
        SettingSearchEntry("Headphone music", "Behaviour"),
        SettingSearchEntry("Stickers", "Home screen free text"),
    )

    /**
     * Runs one query across every source and returns grouped rows in
     * display order: calculator, timer, direct-dial, apps, contacts,
     * settings, files, suggestions, web. An empty query returns no
     * rows. Suggestions are only fetched when the toggle is on.
     */
    suspend fun search(
        context: Context,
        rawQuery: String,
        apps: List<AppEntry>,
        providers: List<SearchProvider>,
        defaultProviderId: String,
        suggestionsEnabled: Boolean,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return@withContext emptyList()
        val out = ArrayList<SearchResult>()
        val provider = providers.firstOrNull { it.id == defaultProviderId }
            ?: providers.firstOrNull()
            ?: defaultProviders().first()

        // #106 in-search calculator: a bare arithmetic expression.
        evaluateExpression(query)?.let { value ->
            out.add(
                SearchResult(
                    kind = SearchResultKind.CALCULATOR,
                    title = formatNumber(value),
                    subtitle = "$query =",
                ),
            )
        }

        // #106 timer: "timer 5 min", "timer 90s", "timer 1h 30m".
        parseTimerSeconds(query)?.let { seconds ->
            out.add(
                SearchResult(
                    kind = SearchResultKind.TIMER,
                    title = "Set a ${describeDuration(seconds)} timer",
                    subtitle = "Timer",
                    payload = seconds.toString(),
                ),
            )
        }

        // #106 direct-dial: a phone-number-shaped query.
        if (isPhoneNumber(query)) {
            out.add(
                SearchResult(
                    kind = SearchResultKind.DIAL,
                    title = "Call $query",
                    subtitle = "Direct dial",
                    payload = query,
                ),
            )
        }

        // #18 apps (label or package match).
        val lower = query.lowercase()
        apps.filter { it.displayLabel.lowercase().contains(lower) || it.packageName.lowercase().contains(lower) }
            .take(8)
            .forEach { app ->
                out.add(
                    SearchResult(
                        kind = SearchResultKind.APP,
                        title = app.displayLabel,
                        subtitle = app.packageName,
                        payload = app.component,
                    ),
                )
            }

        // #18 contacts (system contacts; needs READ_CONTACTS — a
        // denied permission yields no rows, never a crash).
        searchContacts(context, query).forEach { (name, uri) ->
            out.add(
                SearchResult(
                    kind = SearchResultKind.CONTACT,
                    title = name,
                    subtitle = "Contact",
                    payload = uri,
                ),
            )
        }

        // #18 settings.
        settingEntries().filter {
            it.label.lowercase().contains(lower) || it.hint.lowercase().contains(lower)
        }.take(5).forEach { entry ->
            out.add(
                SearchResult(
                    kind = SearchResultKind.SETTING,
                    title = entry.label,
                    subtitle = "Launcher setting — ${entry.hint}",
                ),
            )
        }

        // #18 files (MediaStore display-name match, capped at 5).
        searchFiles(context, query).forEach { (name, uri) ->
            out.add(
                SearchResult(
                    kind = SearchResultKind.FILE,
                    title = name,
                    subtitle = "File",
                    payload = uri,
                ),
            )
        }

        // #104 suggestions from the active provider (toggle-gated).
        if (suggestionsEnabled && query.length >= 2) {
            fetchSuggestions(provider, query).forEach { suggestion ->
                out.add(
                    SearchResult(
                        kind = SearchResultKind.SUGGESTION,
                        title = suggestion,
                        subtitle = "Search ${provider.name} for “$suggestion”",
                        payload = provider.queryUrl.replace("%s", URLEncoder.encode(suggestion, "UTF-8")),
                    ),
                )
            }
        }

        // #18 web: the provider search for the typed query.
        out.add(
            SearchResult(
                kind = SearchResultKind.WEB,
                title = "Search the web for “$query”",
                subtitle = provider.name,
                payload = provider.queryUrl.replace("%s", URLEncoder.encode(query, "UTF-8")),
            ),
        )
        out
    }

    // ---- contacts / files ----

    private fun searchContacts(context: Context, query: String): List<Pair<String, String>> {
        return try {
            val found = ArrayList<Pair<String, String>>()
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.LOOKUP_KEY,
                ),
                ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                arrayOf("%$query%"),
                ContactsContract.Contacts.DISPLAY_NAME + " ASC",
            )?.use { cursor ->
                while (cursor.moveToNext() && found.size < 5) {
                    val name = cursor.getString(0) ?: continue
                    val lookup = cursor.getString(1) ?: continue
                    val uri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup,
                    ).toString()
                    found.add(Pair(name, uri))
                }
            }
            found
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun searchFiles(context: Context, query: String): List<Pair<String, String>> {
        return try {
            val found = ArrayList<Pair<String, String>>()
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns._ID),
                MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?",
                arrayOf("%$query%"),
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC",
            )?.use { cursor ->
                while (cursor.moveToNext() && found.size < 5) {
                    val name = cursor.getString(0) ?: continue
                    val id = cursor.getLong(1)
                    found.add(Pair(name, Uri.withAppendedPath(uri, id.toString()).toString()))
                }
            }
            found
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---- suggestions (#104) ----

    /**
     * Fetches up to 5 query completions from the provider's suggestion
     * endpoint. Two shapes are handled: JSON arrays (DuckDuckGo /
     * Brave return [query, [suggestions]], Google's firefox client
     * the same) and Bing's object shape ({ suggestionGroups: [{
     * searchSuggestions: [{ query, displayText }] }] }). Any failure
     * — no network, unexpected shape — returns no rows.
     */
    private fun fetchSuggestions(provider: SearchProvider, query: String): List<String> {
        if (provider.suggestionUrl.isBlank() || "%s" !in provider.suggestionUrl) return emptyList()
        return try {
            val url = URL(provider.suggestionUrl.replace("%s", URLEncoder.encode(query, "UTF-8")))
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "Launcher/1.0")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseSuggestions(body)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSuggestions(body: String): List<String> {
        val out = ArrayList<String>()
        try {
            val root = JSONArray(body)
            if (root.length() < 2) return emptyList()
            val suggestions = root.getJSONArray(1)
            for (i in 0 until suggestions.length()) {
                if (out.size >= 5) break
                val item = suggestions.opt(i) ?: continue
                val text = when (item) {
                    is String -> item
                    is JSONArray -> item.optString(0, "")
                    else -> ""
                }
                if (text.isNotBlank() && text !in out) out.add(text)
            }
            return out
        } catch (e: JSONException) {
            // Not an array — try Bing's object shape.
        }
        val root = JSONObject(body)
        val groups = root.optJSONArray("suggestionGroups") ?: return emptyList()
        for (g in 0 until groups.length()) {
            val suggestions = groups.optJSONObject(g)?.optJSONArray("searchSuggestions")
                ?: continue
            for (i in 0 until suggestions.length()) {
                if (out.size >= 5) break
                val entry = suggestions.optJSONObject(i) ?: continue
                val text = entry.optString("query").ifBlank { entry.optString("displayText") }
                if (text.isNotBlank() && text !in out) out.add(text)
            }
            if (out.size >= 5) break
        }
        return out
    }

    // ---- #106 calculator ----

    /**
     * Evaluates a bare arithmetic expression (+ - * / % ^, parentheses,
     * decimals, unary minus). Returns null when the text is not a
     * pure expression, so ordinary words never produce a calculator
     * row. Division by zero and malformed input return null.
     */
    fun evaluateExpression(input: String): Double? {
        val text = input.replace(" ", "").replace("×", "*").replace("÷", "/")
        if (text.isEmpty() || !text.all { it.isDigit() || it in "+-*/%^()." }) return null
        if (!text.any { it.isDigit() }) return null
        return try {
            val parser = ExpressionParser(text)
            val value = parser.parseExpression()
            if (parser.atEnd() && value.isFinite()) value else null
        } catch (e: Exception) {
            null
        }
    }

    private class ExpressionParser(private val text: String) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= text.length

        fun parseExpression(): Double {
            var value = parseTerm()
            while (pos < text.length) {
                when (text[pos]) {
                    '+' -> { pos++; value += parseTerm() }
                    '-' -> { pos++; value -= parseTerm() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (pos < text.length) {
                when (text[pos]) {
                    '*' -> { pos++; value *= parseFactor() }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("divide by zero")
                        value /= divisor
                    }
                    '%' -> { pos++; value %= parseFactor() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            var value = parsePrimary()
            while (pos < text.length && text[pos] == '^') {
                pos++
                value = value.pow(parsePrimary())
            }
            return value
        }

        private fun parsePrimary(): Double {
            if (pos < text.length && text[pos] == '-') {
                pos++
                return -parsePrimary()
            }
            if (pos < text.length && text[pos] == '(') {
                pos++
                val value = parseExpression()
                if (pos >= text.length || text[pos] != ')') throw IllegalArgumentException("unbalanced")
                pos++
                return value
            }
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("expected number")
            return text.substring(start, pos).toDouble()
        }
    }

    private fun formatNumber(value: Double): String {
        if (value == kotlin.math.floor(value) && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return "%.6f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }

    // ---- #106 timer / direct-dial ----

    private val timerPattern = Regex(
        """(?i)^timer\s+(?:(\d+)\s*h(?:ours?|rs?)?\s*)?(?:(\d+)\s*m(?:in(?:utes?)?)?\s*)?(?:(\d+)\s*s(?:ec(?:onds?)?)?\s*)?$""",
    )
    private val timerShortPattern = Regex("""(?i)^timer\s+(\d+)\s*([hms])$""")

    /** "timer 5 min" / "timer 1h 30m" / "timer 90s" -> seconds, else null. */
    fun parseTimerSeconds(input: String): Int? {
        val trimmed = input.trim()
        timerShortPattern.matchEntire(trimmed)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            return when (match.groupValues[2].lowercase()) {
                "h" -> amount * 3600
                "m" -> amount * 60
                else -> amount
            }
        }
        val match = timerPattern.matchEntire(trimmed) ?: return null
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        val total = hours * 3600 + minutes * 60 + seconds
        return if (total > 0) total else null
    }

    private fun describeDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val parts = ArrayList<String>()
        if (h > 0) parts.add("$h hour" + if (h > 1) "s" else "")
        if (m > 0) parts.add("$m minute" + if (m > 1) "s" else "")
        if (s > 0 || parts.isEmpty()) parts.add("$s second" + if (s != 1) "s" else "")
        return parts.joinToString(" ")
    }

    /** A phone-number-shaped query: optional +, at least 7 digits. */
    fun isPhoneNumber(input: String): Boolean {
        val digits = input.count { it.isDigit() }
        if (digits < 7) return false
        return input.all { it.isDigit() || it in "+()-. " } &&
            input.trim().firstOrNull()?.let { it.isDigit() || it == '+' } == true
    }
}
