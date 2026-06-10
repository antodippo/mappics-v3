package com.antodippo.mappics.infrastructure.http;

import java.util.Map;

public class HTTPClientForLocalDev implements HTTPClient {

    private static final String OSM_RESPONSE = """
            {"display_name":"Local Area","address":{"city":"Local City","country":"Local Country"}}
            """;

    private static final String WEATHER_RESPONSE = """
            {"hourly":{"time":["2020-01-01T12:00"],
             "temperature_2m":[18.0],"relative_humidity_2m":[65],"weather_code":[1]}}
            """;

    @Override
    public String get(String url, Map<String, String> headers) {
        if (url.contains("nominatim"))  return OSM_RESPONSE;
        if (url.contains("open-meteo")) return WEATHER_RESPONSE;
        throw new UnsupportedOperationException("No stub configured for URL: " + url);
    }
}
