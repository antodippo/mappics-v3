package com.antodippo.mappics.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GalleryTest {

    @Test
    void averageGpsIsCalculatedFromAllCoordinates() {
        List<GpsCoordinates> coords = List.of(
                new GpsCoordinates(10.0, 20.0, null),
                new GpsCoordinates(20.0, 40.0, null),
                new GpsCoordinates(30.0, 60.0, null)
        );

        GpsCoordinates avg = Gallery.calculateAverageGps(coords).orElseThrow();

        assertEquals(20.0, avg.latitude(), 0.001);
        assertEquals(40.0, avg.longitude(), 0.001);
    }

    @Test
    void averageGpsIsEmptyWhenNoCoordinatesProvided() {
        assertTrue(Gallery.calculateAverageGps(List.of()).isEmpty());
    }

    @Test
    void averageGpsOfSingleCoordinateIsItself() {
        GpsCoordinates single = new GpsCoordinates(48.8566, 2.3522, null);

        GpsCoordinates avg = Gallery.calculateAverageGps(List.of(single)).orElseThrow();

        assertEquals(single.latitude(), avg.latitude(), 0.0001);
        assertEquals(single.longitude(), avg.longitude(), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({
            "iceland-2023, Iceland 2023",
            "new_york,     New York",
            "azores,       Azores",
            "PARIS,        Paris",
    })
    void galleryNameFormatsIdAsHumanReadableTitle(String id, String expectedName) {
        Gallery gallery = Gallery.create(id);

        assertEquals(expectedName.trim(), gallery.getName());
    }

    @Test
    void newGalleryHasNoPicturesAndNoAverageGps() {
        Gallery gallery = Gallery.create("iceland");

        assertTrue(gallery.getPictureIds().isEmpty());
        assertTrue(gallery.getAverageGpsCoordinates().isEmpty());
    }

    @Test
    void withMethodsReturnNewInstanceLeavingOriginalUnchanged() {
        Gallery original = Gallery.create("iceland");
        List<String> pictureIds = List.of("iceland/photo1.jpg", "iceland/photo2.jpg");

        Gallery updated = original.withPictureIds(pictureIds);

        assertNotSame(original, updated);
        assertTrue(original.getPictureIds().isEmpty());
        assertEquals(2, updated.getPictureIds().size());
    }
}
