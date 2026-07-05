package com.antodippo.mappics.infrastructure.weather;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.WeatherData;
import com.antodippo.mappics.infrastructure.http.HTTPClientThatAlwaysReturns;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class FetchWeatherDataFromOpenMeteoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Four hourly entries around noon. takenAt 12:07 should resolve to index 2 (12:00).
    private static final String RESPONSE = """
            {
              "latitude": 37.84,
              "longitude": -25.79,
              "hourly": {
                "time":                 ["2017-08-24T10:00","2017-08-24T11:00","2017-08-24T12:00","2017-08-24T13:00"],
                "temperature_2m":       [21.5, 22.0, 23.1, 22.8],
                "relative_humidity_2m": [68,   65,   62,   64],
                "weather_code":         [1,    1,    0,    1],
                "wind_speed_10m":       [14.2, 12.8,  8.5, 10.1]
              }
            }
            """;

    private final GpsCoordinates azores = new GpsCoordinates(37.84, -25.79, null);

    @Test
    void picksHourClosestToTakenAt() {
        // 12:07 is 7 min from 12:00 and 53 min from 11:00 — should resolve to index 2
        WeatherData weather = fetch(LocalDateTime.of(2017, 8, 24, 12, 7));

        assertEquals(23.1, weather.temperatureCelsius(), 0.01);
        assertEquals(62,   weather.humidity());
        assertEquals(8.5,  weather.windSpeedKmh(), 0.01);
        assertEquals(0,    weather.weatherCode());
        assertEquals("Clear sky", weather.description());
    }

    @Test
    void picksEarlierHourWhenEquidistant() {
        // 11:30 is exactly 30 min from both 11:00 and 12:00; tie breaks to first found (11:00, index 1)
        WeatherData weather = fetch(LocalDateTime.of(2017, 8, 24, 11, 30));

        assertEquals(22.0, weather.temperatureCelsius(), 0.01);
        assertEquals(1,    weather.weatherCode());
    }

    @Test
    void mapsWmoCodeToHumanReadableDescription() {
        WeatherData weather = fetch(LocalDateTime.of(2017, 8, 24, 10, 0));

        // index 0 → code 1 → "Mainly clear"
        assertEquals(1,              weather.weatherCode());
        assertEquals("Mainly clear", weather.description());
    }

    @Test
    void returnsEmptyWhenHourlyDataAbsent() {
        var fetcher = fetcherWith("{}");

        assertTrue(fetcher.fetch(azores, LocalDateTime.of(2017, 8, 24, 12, 0)).isEmpty());
    }

    @Test
    void returnsEmptyOnHttpFailure() {
        var fetcher = new FetchWeatherDataFromOpenMeteo(
                (url, headers) -> { throw new RuntimeException("Timeout"); },
                MAPPER
        );

        assertTrue(fetcher.fetch(azores, LocalDateTime.of(2017, 8, 24, 12, 0)).isEmpty());
    }

    @Test
    void urlContainsCoordinatesAndDate() {
        var httpClient = new HTTPClientThatAlwaysReturns(RESPONSE);
        var fetcher = new FetchWeatherDataFromOpenMeteo(httpClient, MAPPER);

        fetcher.fetch(new GpsCoordinates(37.839183, -25.793508, null), LocalDateTime.of(2017, 8, 24, 12, 7));

        String url = httpClient.getLastUrl();
        assertTrue(url.contains("37.839183"),  "URL should contain latitude");
        assertTrue(url.contains("-25.793508"), "URL should contain longitude");
        assertTrue(url.contains("2017-08-24"), "URL should contain the date");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private WeatherData fetch(LocalDateTime takenAt) {
        return fetcherWith(RESPONSE)
                .fetch(azores, takenAt)
                .orElseThrow(() -> new AssertionError("Expected weather data but got empty"));
    }

    private FetchWeatherDataFromOpenMeteo fetcherWith(String body) {
        return new FetchWeatherDataFromOpenMeteo(new HTTPClientThatAlwaysReturns(body), MAPPER);
    }
}
