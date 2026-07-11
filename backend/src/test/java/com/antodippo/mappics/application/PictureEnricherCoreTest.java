package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;
import com.antodippo.mappics.infrastructure.exif.ExtractExifDataWithMetadataExtractor;
import com.antodippo.mappics.infrastructure.http.HTTPClientThatAlwaysReturns;
import com.antodippo.mappics.infrastructure.image.ImageResizerTestDouble;
import com.antodippo.mappics.infrastructure.location.FetchLocationDescriptionFromOSM;
import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import com.antodippo.mappics.infrastructure.weather.FetchWeatherDataFromOpenMeteo;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The OSM rate limit protects the Nominatim 1 req/s ToS in prod; the rest of the
// enricher is exercised through GalleryImporterCoreTest with the limit at 0.
class PictureEnricherCoreTest {

    private static final String OSM_RESPONSE = """
            {"display_name":"Reykjavik, Iceland","address":{"city":"Reykjavik","country":"Iceland"}}
            """;

    private static final GpsCoordinates GPS = new GpsCoordinates(64.13, -21.90, null);

    private final Picture picture = Picture.create("iceland/DSC_0114.JPG", "iceland", "DSC_0114.JPG");

    @Test
    void fetchLocationSleepsForTheConfiguredOsmRateLimit() {
        PictureEnricher enricher = enricherWithRateLimit(50);

        long start = System.nanoTime();
        enricher.fetchLocation(picture, GPS);
        enricher.fetchLocation(picture, GPS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs >= 100, "Two calls at 50ms rate limit should take >= 100ms, took " + elapsedMs);
    }

    @Test
    void interruptDuringRateLimitRethrowsAndRestoresInterruptFlag() {
        PictureEnricher enricher = enricherWithRateLimit(50);

        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> enricher.fetchLocation(picture, GPS));

        // Thread.interrupted() also clears the flag so it can't leak into other tests
        assertTrue(Thread.interrupted(), "Interrupt flag must be restored for callers up the stack");
    }

    private PictureEnricher enricherWithRateLimit(long osmRateLimitMs) {
        return new PictureEnricherCore(
                new GalleryFileStorageInMemory("http://localhost/processed"),
                new ExtractExifDataWithMetadataExtractor(),
                new ImageResizerTestDouble(),
                new FetchLocationDescriptionFromOSM(new HTTPClientThatAlwaysReturns(OSM_RESPONSE), new ObjectMapper()),
                new FetchWeatherDataFromOpenMeteo(new HTTPClientThatAlwaysReturns("{}"), new ObjectMapper()),
                osmRateLimitMs);
    }
}
