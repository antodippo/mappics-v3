package com.antodippo.mappics.infrastructure.weather;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.WeatherData;
import com.antodippo.mappics.domain.WeatherFetcher;
import com.antodippo.mappics.infrastructure.http.HTTPClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Component
public class FetchWeatherDataFromOpenMeteo implements WeatherFetcher {

    private static final Logger log = LoggerFactory.getLogger(FetchWeatherDataFromOpenMeteo.class);
    private static final String API_URL =
            "https://archive-api.open-meteo.com/v1/archive" +
            "?latitude=%s&longitude=%s&start_date=%s&end_date=%s" +
            "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto";
    // Open-Meteo returns times as "yyyy-MM-ddTHH:mm" (no seconds, no offset).
    private static final DateTimeFormatter HOURLY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final HTTPClient httpClient;
    private final ObjectMapper objectMapper;

    public FetchWeatherDataFromOpenMeteo(HTTPClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WeatherData> fetch(GpsCoordinates coordinates, LocalDateTime takenAt) {
        try {
            String date = takenAt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = String.format(API_URL, coordinates.latitude(), coordinates.longitude(), date, date);
            String json = httpClient.get(url, Map.of());
            return parse(json, takenAt);
        } catch (Exception e) {
            log.warn("Failed to fetch weather for {} at {}: {}", coordinates, takenAt, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WeatherData> parse(String json, LocalDateTime takenAt) throws Exception {
        JsonNode hourly = objectMapper.readTree(json).get("hourly");
        if (hourly == null) return Optional.empty();

        JsonNode times        = hourly.get("time");
        JsonNode temperatures = hourly.get("temperature_2m");
        JsonNode humidities   = hourly.get("relative_humidity_2m");
        JsonNode weatherCodes = hourly.get("weather_code");
        JsonNode windSpeeds   = hourly.get("wind_speed_10m");

        if (times == null || times.isEmpty()) return Optional.empty();

        int i = closestHourIndex(times, takenAt);
        int code = weatherCodes.get(i).asInt();
        double windSpeed = windSpeeds != null ? windSpeeds.get(i).asDouble() : 0.0;

        return Optional.of(new WeatherData(
                temperatures.get(i).asDouble(),
                humidities.get(i).asInt(),
                windSpeed,
                code,
                WmoWeatherCode.describe(code)
        ));
    }

    private int closestHourIndex(JsonNode times, LocalDateTime target) {
        int best = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime t = LocalDateTime.parse(times.get(i).asText(), HOURLY_TIME_FORMAT);
            long diff = Math.abs(Duration.between(t, target).toMinutes());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }
}
