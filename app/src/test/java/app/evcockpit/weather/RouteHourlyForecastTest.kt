package app.evcockpit.weather

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import org.junit.Assert.*
import org.junit.Test

class RouteHourlyForecastTest {
    private val route = NavigationRoute(
        geometry = (0..4).map { GeoPoint(38.0 - it*0.2, 35.0 - it*0.2) },
        distanceMeters = 220_000.0, durationSeconds = 14_400.0,
        maneuvers = emptyList(),
    )
    private val departure = 1_800_000_000L

    @Test fun samplesExpectedPassageTimeNotCurrentObservation() {
        val samples = RouteHourlyForecastPolicy.sample(route, departure)
        assertEquals(3, samples.size)
        assertEquals("Başlangıç", samples[0].label)
        assertEquals("Yol ortası", samples[1].label)
        assertEquals("Hedef", samples[2].label)
        assertEquals(departure, samples[0].expectedEpochSec)
        assertEquals(departure + 7_200, samples[1].expectedEpochSec)
        assertEquals(departure + 14_400, samples[2].expectedEpochSec)
        assertEquals(route.geometry[2], samples[1].location)
    }

    @Test fun usesNearestHourlyForecastForEachExpectedTime() {
        val samples = RouteHourlyForecastPolicy.sample(route, departure)
        val json = """[
          {"hourly":{"time":[1799996400,1800000000,1800003600],"temperature_2m":[8,12,15],
            "wind_speed_10m":[3,14,20],"precipitation_probability":[5,80,99],"weather_code":[0,61,3]}},
          {"hourly":{"time":[1800003600,1800007200,1800010800],"temperature_2m":[8,12,15],
            "wind_speed_10m":[3,14,20],"precipitation_probability":[5,80,99],"weather_code":[0,61,3]}},
          {"hourly":{"time":[1800010800,1800014400,1800018000],"temperature_2m":[8,12,15],
            "wind_speed_10m":[3,14,20],"precipitation_probability":[5,80,99],"weather_code":[0,61,3]}}
        ]"""
        val forecast = RouteHourlyForecastPolicy.parse(json, samples)
        assertEquals(3, forecast.size)
        assertEquals(12.0, forecast[0].temperatureC, 0.0)
        assertEquals(14.0, forecast[1].windKmh!!, 0.0)
        assertEquals(80, forecast[2].rainProbabilityPct)
        val summary = RouteHourlyForecastPolicy.summary(forecast)
        assertTrue(summary.contains("Open-Meteo"))
        assertTrue(summary.contains("TAHMİN"))
        assertTrue(summary.contains("yağış"))
        assertTrue(summary.contains("rüzgâr"))
        assertTrue(summary.contains("Trafik dahil değil"))
    }

    @Test fun rejectsOldForecastInsteadOfFabricatingWeather() {
        val samples = RouteHourlyForecastPolicy.sample(route, departure)
        val old = """{"hourly":{"time":[1700000000],
            "temperature_2m":[20],"wind_speed_10m":[10]}}"""
        assertTrue(RouteHourlyForecastPolicy.parse(old, samples).isEmpty())
        assertTrue(RouteHourlyForecastPolicy.summary(emptyList()).contains("DOĞRULANMADI"))
        assertTrue(RouteHourlyForecastPolicy.parse("invalid", samples).isEmpty())
        assertTrue(RouteHourlyForecastPolicy.sample(
            NavigationRoute(emptyList(), 0.0,0.0, emptyList()),departure).isEmpty())
    }
}
