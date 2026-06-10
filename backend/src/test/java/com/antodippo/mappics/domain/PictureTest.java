package com.antodippo.mappics.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PictureTest {

    @Test
    void newPictureHasNoData() {
        Picture picture = Picture.create("gallery/photo.jpg", "gallery", "photo.jpg");

        assertFalse(picture.hasAllData());
        assertTrue(picture.getThumbnailUrl().isEmpty());
        assertTrue(picture.getFullSizeUrl().isEmpty());
        assertTrue(picture.getGpsCoordinates().isEmpty());
        assertTrue(picture.getExifData().isEmpty());
        assertTrue(picture.getLocationDescription().isEmpty());
        assertTrue(picture.getWeatherData().isEmpty());
    }

    @Test
    void hasAllDataReturnsTrueWhenAllFieldsAreSet() {
        Picture picture = PictureBuilder.aFullyProcessedPicture();

        assertTrue(picture.hasAllData());
    }

    @Test
    void hasAllDataReturnsFalseWhenAnyFieldIsMissing() {
        assertFalse(new PictureBuilder().withNoProcessedImages().build().hasAllData());
        assertFalse(new PictureBuilder().withNoGps().build().hasAllData());
        assertFalse(new PictureBuilder().withNoExif().build().hasAllData());
        assertFalse(new PictureBuilder().withNoLocation().build().hasAllData());
        assertFalse(new PictureBuilder().withNoWeather().build().hasAllData());
    }

    @Test
    void withMethodsReturnNewInstanceLeavingOriginalUnchanged() {
        Picture original = Picture.create("gallery/photo.jpg", "gallery", "photo.jpg");
        GpsCoordinates gps = new GpsCoordinates(51.5, -0.1, null);

        Picture enriched = original.withGpsCoordinates(gps);

        assertNotSame(original, enriched);
        assertTrue(original.getGpsCoordinates().isEmpty());
        assertEquals(gps, enriched.getGpsCoordinates().orElseThrow());
    }

    @Test
    void enrichmentChainBuildsAFullPicture() {
        GpsCoordinates gps = new GpsCoordinates(51.5, -0.1, null);
        ExifData exif = new ExifData("Nikon", "D750", null, "35mm", "f/4", 200);
        LocationDescription location = new LocationDescription("London", "Capital of UK");
        WeatherData weather = new WeatherData(14.0, 72, 0.0, 2, "Partly cloudy");

        Picture picture = Picture.create("g/p.jpg", "g", "p.jpg")
                .withProcessedImages("https://example.com/thumb.jpg", "https://example.com/full.jpg")
                .withGpsCoordinates(gps)
                .withExifData(exif)
                .withLocationDescription(location)
                .withWeatherData(weather);

        assertTrue(picture.hasAllData());
        assertEquals(gps, picture.getGpsCoordinates().orElseThrow());
        assertEquals("Nikon", picture.getExifData().orElseThrow().cameraMake());
        assertEquals("London", picture.getLocationDescription().orElseThrow().name());
        assertEquals(14.0, picture.getWeatherData().orElseThrow().temperatureCelsius());
    }
}
