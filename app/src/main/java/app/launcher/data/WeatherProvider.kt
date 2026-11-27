package app.launcher.data

import app.launcher.model.GlanceItem
import app.launcher.model.GlanceSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Weather for At a Glance (#10) and the weather widget (#126).
 *
 * Provider DECIDED 2026-11-26 (finish job): Open-Meteo — free, no API
 * key, no account, no location permission. The user types a city; the
 * free geocoding API resolves it to coordinates, and the forecast API
 * returns the current temperature + WMO weather code. Commute /
 * finance / sports stay placeholder slots — no free no-key equivalent
 * exists for them, and they are recorded as awaiting provider picks
 * in BUILD_LOG.md.
 *
 * Network runs on Dispatchers.IO with 10s timeouts; any failure
 * yields null / emptyList, never a crash and never fake weather.
 * The parsing functions are pure (org.json only) so they are unit
 * tested directly (WeatherProviderTest, Robolectric — org.json is an
 * Android stub in plain JUnit).
 */

data class WeatherCity(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Crittenden, United States" — the label shown in the picker. */
    val displayName: String get() = if (country.isBlank()) name else "$name, $country"
}

data class WeatherNow(
    val temperatureC: Double,
    val condition: String,
    val cityName: String,
    val fetchedAtMillis: Long,
) {
    /** "12° · Partly cloudy" — the glance-strip line. */
    val summary: String get() = "${temperatureC.toInt()}° · $condition"
}

/** WMO weather-code table (Open-Meteo `weather_code`). */
fun describeWeatherCode(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51 -> "Light drizzle"
    53 -> "Drizzle"
    55 -> "Dense drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Light rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71 -> "Light snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80 -> "Light rain showers"
    81 -> "Rain showers"
    82 -> "Violent rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown conditions"
}

/** Parses an Open-Meteo geocoding response into candidate cities. */
fun parseGeocode(body: String): List<WeatherCity> {
    val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
    val out = ArrayList<WeatherCity>()
    for (i in 0 until results.length()) {
        val item = results.optJSONObject(i) ?: continue
        val name = item.optString("name", "").trim()
        if (name.isEmpty()) continue
        if (!item.has("latitude") || !item.has("longitude")) continue
        out.add(
            WeatherCity(
                name = name,
                country = item.optString("country", ""),
                latitude = item.optDouble("latitude"),
                longitude = item.optDouble("longitude"),
            ),
        )
    }
    return out
}

/** Parses an Open-Meteo forecast response (`current` block) into now. */
fun parseCurrent(body: String, cityName: String): WeatherNow? {
    val current = JSONObject(body).optJSONObject("current") ?: return null
    if (!current.has("temperature_2m") || !current.has("weather_code")) return null
    return WeatherNow(
        temperatureC = current.optDouble("temperature_2m"),
        condition = describeWeatherCode(current.optInt("weather_code")),
        cityName = cityName,
        fetchedAtMillis = System.currentTimeMillis(),
    )
}

private fun httpGet(url: String): String? {
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

/** Resolves a typed city name to up to 5 candidates (Open-Meteo). */
suspend fun geocodeCity(query: String): List<WeatherCity> = withContext(Dispatchers.IO) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return@withContext emptyList()
    val encoded = URLEncoder.encode(trimmed, "UTF-8")
    val body = httpGet(
        "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$encoded&count=5&language=en&format=json",
    ) ?: return@withContext emptyList()
    try {
        parseGeocode(body)
    } catch (e: Exception) {
        emptyList()
    }
}

/** Fetches the current weather for a resolved city. */
suspend fun fetchCurrent(city: WeatherCity): WeatherNow? = withContext(Dispatchers.IO) {
    val body = httpGet(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,weather_code" +
            "&temperature_unit=celsius&timezone=auto",
    ) ?: return@withContext null
    try {
        parseCurrent(body, city.displayName)
    } catch (e: Exception) {
        null
    }
}

/** #10 weather slot, fed by the ViewModel's cached [WeatherNow]. */
class WeatherGlanceProvider(
    private val enabled: () -> Boolean,
    private val citySet: () -> Boolean,
    private val now: () -> WeatherNow?,
) : GlanceSlotProvider {
    override val slot = GlanceSlot.WEATHER
    override fun isEnabled(): Boolean = enabled()
    override fun load(context: android.content.Context): List<GlanceItem> {
        val current = now() ?: return emptyList()
        return listOf(
            GlanceItem(
                slot = GlanceSlot.WEATHER,
                title = current.summary,
                subtitle = current.cityName,
            ),
        )
    }

    override fun unavailableReason(): String? =
        if (!citySet()) {
            "No location set yet — set one in Settings → At a Glance"
        } else {
            "Weather not fetched yet"
        }
}
