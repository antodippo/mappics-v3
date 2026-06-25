package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TracingPictureEnricherTest {

    private static final String PICTURE_ID = "iceland/DSC_0114.JPG";
    private static final GpsCoordinates GPS = new GpsCoordinates(64.13, -21.90, null);

    private SimpleTracer            tracer;
    private TracingPictureEnricher  enricher;
    private Picture                 picture;

    @BeforeEach
    void setUp() {
        tracer   = new SimpleTracer();
        enricher = new TracingPictureEnricher(new PassThroughEnricher(), tracer);
        picture  = Picture.create(PICTURE_ID, "iceland", "DSC_0114.JPG");
    }

    @Test
    void extractExif_emitsSpanWithPictureIdTag() {
        enricher.extractExif(picture, new byte[0]);
        assertSpan("import.picture.exif");
    }

    @Test
    void resizeImages_emitsSpanWithPictureIdTag() {
        enricher.resizeImages(picture, new byte[0]);
        assertSpan("import.picture.resize");
    }

    @Test
    void fetchLocation_emitsSpanWithPictureIdTag() {
        enricher.fetchLocation(picture, GPS);
        assertSpan("import.picture.location");
    }

    @Test
    void fetchWeather_emitsSpanWithPictureIdTag() {
        enricher.fetchWeather(picture, GPS, LocalDateTime.now());
        assertSpan("import.picture.weather");
    }

    private void assertSpan(String expectedName) {
        SimpleSpan span = tracer.onlySpan();
        assertEquals(expectedName, span.getName());
        assertEquals(PICTURE_ID, span.getTags().get("pictureId"));
    }

    // Returns the picture unchanged — the decorator under test owns only span lifecycle.
    private static class PassThroughEnricher implements PictureEnricher {
        @Override public Picture extractExif(Picture picture, byte[] imageData) { return picture; }
        @Override public Picture resizeImages(Picture picture, byte[] imageData) { return picture; }
        @Override public Picture fetchLocation(Picture picture, GpsCoordinates gps) { return picture; }
        @Override public Picture fetchWeather(Picture picture, GpsCoordinates gps, LocalDateTime takenAt) { return picture; }
    }
}
