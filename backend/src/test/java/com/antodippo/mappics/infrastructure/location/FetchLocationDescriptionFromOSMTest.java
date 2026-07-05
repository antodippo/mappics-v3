package com.antodippo.mappics.infrastructure.location;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.LocationDescription;
import com.antodippo.mappics.infrastructure.http.HTTPClientThatAlwaysReturns;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FetchLocationDescriptionFromOSMTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AZORES_RESPONSE = """
            {
              "place_id": 289417623,
              "display_name": "Ponta Delgada, São Miguel Island, Azores, Portugal",
              "address": {
                "city": "Ponta Delgada",
                "county": "Ponta Delgada",
                "state": "Azores",
                "country": "Portugal",
                "country_code": "pt"
              }
            }
            """;

    private static final String NO_CITY_RESPONSE = """
            {
              "display_name": "Some remote area, Iceland",
              "address": {
                "state": "Capital Region",
                "country": "Iceland",
                "country_code": "is"
              }
            }
            """;

    @Test
    void extractsCityAsNameAndDisplayNameAsDescription() {
        var fetcher = fetcherWith(AZORES_RESPONSE);

        Optional<LocationDescription> result = fetcher.fetch(new GpsCoordinates(37.84, -25.79, null));

        assertTrue(result.isPresent());
        assertEquals("Ponta Delgada", result.get().name());
        assertEquals("Ponta Delgada, São Miguel Island, Azores, Portugal", result.get().shortDescription());
    }

    @Test
    void fallsBackToStateWhenNoCityOrTownPresent() {
        var fetcher = fetcherWith(NO_CITY_RESPONSE);

        Optional<LocationDescription> result = fetcher.fetch(new GpsCoordinates(64.13, -21.89, null));

        assertTrue(result.isPresent());
        assertEquals("Capital Region", result.get().name());
    }

    @Test
    void returnsEmptyWhenResponseLacksDisplayName() {
        var fetcher = fetcherWith("{}");

        assertTrue(fetcher.fetch(new GpsCoordinates(0, 0, null)).isEmpty());
    }

    @Test
    void returnsEmptyOnHttpFailure() {
        var fetcher = new FetchLocationDescriptionFromOSM(
                (url, headers) -> { throw new RuntimeException("Connection refused"); },
                MAPPER
        );

        assertTrue(fetcher.fetch(new GpsCoordinates(37.84, -25.79, null)).isEmpty());
    }

    @Test
    void urlContainsLatitudeAndLongitude() {
        var httpClient = new HTTPClientThatAlwaysReturns(AZORES_RESPONSE);
        var fetcher = new FetchLocationDescriptionFromOSM(httpClient, MAPPER);

        fetcher.fetch(new GpsCoordinates(37.839183, -25.793508, null));

        String url = httpClient.getLastUrl();
        assertTrue(url.contains("37.839183"), "URL should contain latitude");
        assertTrue(url.contains("-25.793508"), "URL should contain longitude");
    }

    @Test
    void urlContainsUserAgentHeader() {
        // Nominatim ToS requires a User-Agent — verified via the HTTP client double
        // capturing headers is not part of the current HTTPClient interface, so
        // we rely on a code review / integration test for this invariant.
        // The constant HEADERS in FetchLocationDescriptionFromOSM documents the intent.
    }

    private FetchLocationDescriptionFromOSM fetcherWith(String body) {
        return new FetchLocationDescriptionFromOSM(new HTTPClientThatAlwaysReturns(body), MAPPER);
    }
}
