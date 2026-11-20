package app.evcockpit.weather

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import org.junit.Assert.*
import org.junit.Test

class OpenMeteoWeatherTest {
    @Test fun samplesRouteStartMiddleEndOnly() {
        val geometry = (0..10).map { GeoPoint(38.0 + it / 100.0, 35.0) }
        val route = NavigationRoute(geometry, 1000.0, 200.0, emptyList())
        val samples = OpenMeteoWeatherLogic.sample(route)
        assertEquals(3, samples.size)
        assertEquals(geometry[0], samples.first())
        assertEquals(geometry[5], samples[1])
        assertEquals(geometry[10], samples.last())
    }
    @Test fun parsesMultiLocationCurrentWeather() {
        val json = """[{"current":{"temperature_2m":17.4,"weather_code":2}},
                    {"current":{"temperature_2m":22,"weather_code":3}}]"""
        val points = OpenMeteoWeatherLogic.parse(json)
        assertEquals(2, points.size)
        assertTrue(OpenMeteoWeatherLogic.summary(points).contains("Başlangıç 17°C"))
        assertTrue(OpenMeteoWeatherLogic.summary(points).contains("Hedef 22°C"))
    }
    @Test fun unknownWeatherIsNotFabricated() {
        assertTrue(OpenMeteoWeatherLogic.parse("""{"current":{}}""").isEmpty())
        assertTrue(OpenMeteoWeatherLogic.summary(emptyList()).contains("DOĞRULANMADI"))
        assertTrue(OpenMeteoWeatherLogic.sample(
            NavigationRoute(emptyList(), 0.0, 0.0, emptyList())).isEmpty())
    }
}
