package com.antodippo.mappics.infrastructure.location;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.LocationDescription;
import com.antodippo.mappics.domain.LocationDescriptionFetcher;
import com.antodippo.mappics.infrastructure.http.HTTPClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FetchLocationDescriptionFromOSM implements LocationDescriptionFetcher {

    private static final Logger log = LoggerFactory.getLogger(FetchLocationDescriptionFromOSM.class);
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";
    // OSM Nominatim ToS requires a User-Agent identifying the application.
    private static final Map<String, String> HEADERS =
            Map.of("User-Agent", "mappics-v3/1.0 (https://github.com/antodippo/mappics-v3)");
    private static final List<String> NAME_FIELDS =
            List.of("city", "town", "village", "municipality", "county", "state", "country");

    private final HTTPClient httpClient;
    private final ObjectMapper objectMapper;

    public FetchLocationDescriptionFromOSM(HTTPClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<LocationDescription> fetch(GpsCoordinates coordinates) {
        try {
            String url = String.format(NOMINATIM_URL, coordinates.latitude(), coordinates.longitude());
            String json = httpClient.get(url, HEADERS);
            return parse(json, coordinates);
        } catch (Exception e) {
            log.warn("Failed to fetch location description for {}: {}", coordinates, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LocationDescription> parse(String json, GpsCoordinates coordinates) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!root.has("display_name")) {
            log.warn("OSM response missing display_name for {}", coordinates);
            return Optional.empty();
        }
        String displayName = root.get("display_name").asText();
        String name = extractName(root.get("address"), displayName);
        return Optional.of(new LocationDescription(name, displayName));
    }

    private String extractName(JsonNode address, String fallback) {
        if (address == null || address.isNull()) return fallback;
        return NAME_FIELDS.stream()
                .filter(field -> address.has(field) && !address.get(field).asText().isBlank())
                .map(field -> address.get(field).asText())
                .findFirst()
                .orElse(fallback);
    }
}
