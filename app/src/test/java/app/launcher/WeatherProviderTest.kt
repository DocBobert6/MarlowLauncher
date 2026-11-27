package app.launcher

import app.launcher.data.describeWeatherCode
import app.launcher.data.parseCurrent
import app.launcher.data.parseGeocode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Weather provider (Open-Meteo) parsing — Robolectric, because org.json
// is an Android stub in plain JUnit and these functions parse with it.
// No network is touched here: every test feeds canned JSON to the pure
// parsers, so the tests run offline and deterministically.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherProviderTest {

    private val geocodeBody = """
        {
          "results": [
            {"name": "Crittenden", "country": "United States",
             "latitude": 38.78, "longitude": -84.60},
            {"name": "Crittenden", "country": "United States",
             "latitude": 36.90, "longitude": -89.50},
            {"name": "Nowhere", "country": "United States",
             "longitude": -84.60}
          ],
          "generationtime_ms": 0.5
        }
    """.trimIndent()

    private val forecastBody = """
        {
          "latitude": 38.78, "longitude": -84.60,
          "current": {"time": "2026-11-26T12:00",
                      "temperature_2m": 12.3, "weather_code": 3}
        }
    """.trimIndent()

    @Test
    fun geocodeParsesCandidatesAndSkipsMalformedEntries() {
        val cities = parseGeocode(geocodeBody)
        // Two complete entries; the third has no latitude and is skipped.
        assertEquals(2, cities.size)
        assertEquals("Crittenden", cities[0].name)
        assertEquals("United States", cities[0].country)
        assertEquals(38.78, cities[0].latitude, 0.001)
        assertEquals(-84.60, cities[0].longitude, 0.001)
        assertEquals("Crittenden, United States", cities[0].displayName)
    }

    @Test
    fun geocodeEmptyResultsIsEmpty() {
        assertTrue(parseGeocode("""{"results": []}""").isEmpty())
        assertTrue(parseGeocode("""{}""").isEmpty())
    }

    @Test
    fun currentParsesTemperatureAndCondition() {
        val now = parseCurrent(forecastBody, "Crittenden, United States")
        assertEquals(12.3, now?.temperatureC ?: 0.0, 0.001)
        assertEquals("Overcast", now?.condition)
        assertEquals("Crittenden, United States", now?.cityName)
        assertEquals("12° · Overcast", now?.summary)
    }

    @Test
    fun currentMissingBlockIsNull() {
        assertNull(parseCurrent("""{"latitude": 38.78}""", "Crittenden"))
        assertNull(
            parseCurrent(
                """{"current": {"temperature_2m": 12.3}}""",
                "Crittenden",
            ),
        )
    }

    @Test
    fun weatherCodesDescribeConditions() {
        assertEquals("Clear sky", describeWeatherCode(0))
        assertEquals("Partly cloudy", describeWeatherCode(2))
        assertEquals("Light rain", describeWeatherCode(61))
        assertEquals("Thunderstorm", describeWeatherCode(95))
        assertEquals("Unknown conditions", describeWeatherCode(999))
    }
}
