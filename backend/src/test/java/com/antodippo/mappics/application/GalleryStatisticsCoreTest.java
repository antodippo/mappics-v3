package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import com.antodippo.mappics.infrastructure.persistence.GalleryRepositoryInMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GalleryStatisticsCoreTest {

    private GalleryRepositoryInMemory repository;
    private GalleryStatistics useCase;

    @BeforeEach
    void setUp() {
        repository = new GalleryRepositoryInMemory();
        useCase = new GalleryStatisticsCore(repository);
    }

    @Test
    void emptyRepositoryYieldsZeroTotalsAndNullExtremums() {
        Statistics stats = useCase.compute();

        assertEquals(0, stats.totalPictures());
        assertEquals(0, stats.galleryCount());
        assertEquals(0.0, stats.totalTraveledKm());
        assertNull(stats.northernmost());
        assertNull(stats.southernmost());
        assertNull(stats.oldest());
        assertNull(stats.mostUsedCamera());
        assertNull(stats.dateSpanDays());
        assertNull(stats.biggestGallery());
        assertNull(stats.averageTemperatureCelsius());
    }

    @Test
    void resolvesEachExtremumToTheRightPicture() {
        savePicture("iceland/p1.jpg", "iceland", new GpsCoordinates(64.13, -21.90, 100.0),
                LocalDateTime.of(2020, 1, 1, 12, 0), 2.0, "Nikon", "D850");
        savePicture("azores/p2.jpg", "azores", new GpsCoordinates(37.70, -25.60, 500.0),
                LocalDateTime.of(2021, 6, 1, 12, 0), 25.0, "Nikon", "D850");
        savePicture("chile/p3.jpg", "chile", new GpsCoordinates(-53.10, -70.90, 10.0),
                LocalDateTime.of(2019, 3, 1, 12, 0), -5.0, "Canon", "EOS R5");
        savePicture("kenya/p4.jpg", "kenya", new GpsCoordinates(-1.20, 36.80, 1700.0),
                LocalDateTime.of(2022, 12, 1, 12, 0), 30.0, "Nikon", "D850");

        Statistics stats = useCase.compute();

        assertEquals(4, stats.totalPictures());
        assertEquals("iceland/p1.jpg", stats.northernmost().pictureId());
        assertEquals("chile/p3.jpg", stats.southernmost().pictureId());
        assertEquals("kenya/p4.jpg", stats.easternmost().pictureId());
        assertEquals("chile/p3.jpg", stats.westernmost().pictureId());
        assertEquals("kenya/p4.jpg", stats.highestAltitude().pictureId());
        assertEquals(1700.0, stats.highestAltitude().value());
        assertEquals("chile/p3.jpg", stats.coldest().pictureId());
        assertEquals("kenya/p4.jpg", stats.hottest().pictureId());
        assertEquals("chile/p3.jpg", stats.oldest().pictureId());
        assertEquals("kenya/p4.jpg", stats.newest().pictureId());
        assertEquals("Nikon D850", stats.mostUsedCamera());
        assertEquals(13.0, stats.averageTemperatureCelsius(), 0.0001);
        assertTrue(stats.totalTraveledKm() > 0);
        assertTrue(stats.dateSpanDays() > 0);
    }

    @Test
    void extremumCarriesThePicturesGalleryIdNameThumbnailAndValue() {
        repository.save(Gallery.create("iceland").withPictureIds(List.of("iceland/p1.jpg")));
        Picture picture = new PictureBuilder()
                .withId("iceland/p1.jpg")
                .withGalleryId("iceland")
                .build()
                .withGpsCoordinates(new GpsCoordinates(64.13, -21.90, 100.0));
        repository.savePicture(picture);

        Statistics.PictureStat northernmost = useCase.compute().northernmost();

        assertEquals("iceland", northernmost.galleryId());
        assertEquals("Iceland", northernmost.galleryName());
        assertEquals("https://example.com/thumb.jpg", northernmost.thumbnailUrl());
        assertEquals(64.13, northernmost.value());
    }

    @Test
    void galleryNameFallsBackToTheIdWhenTheGalleryIsUnknown() {
        Picture picture = new PictureBuilder()
                .withId("orphan/p1.jpg")
                .withGalleryId("orphan")
                .build()
                .withGpsCoordinates(new GpsCoordinates(64.13, -21.90, 100.0));
        repository.savePicture(picture);

        assertEquals("orphan", useCase.compute().northernmost().galleryName());
    }

    @Test
    void skipsPicturesMissingTheRelevantData() {
        repository.savePicture(new PictureBuilder().withId("g/withData.jpg").withGalleryId("g")
                .build().withGpsCoordinates(new GpsCoordinates(10.0, 10.0, null)));
        repository.savePicture(Picture.create("g/noGps.jpg", "g", "noGps.jpg"));

        Statistics stats = useCase.compute();

        assertEquals(2, stats.totalPictures());
        assertEquals("g/withData.jpg", stats.northernmost().pictureId());
    }

    @Test
    void highestAltitudeIgnoresPicturesWithoutAltitude() {
        repository.savePicture(picture("g/noAlt.jpg", "g", new GpsCoordinates(10.0, 10.0, null)));
        repository.savePicture(picture("g/withAlt.jpg", "g", new GpsCoordinates(20.0, 20.0, 850.0)));

        assertEquals("g/withAlt.jpg", useCase.compute().highestAltitude().pictureId());
    }

    @Test
    void biggestGalleryHasTheMostPictureIds() {
        repository.save(Gallery.create("iceland").withPictureIds(List.of("iceland/a.jpg", "iceland/b.jpg", "iceland/c.jpg")));
        repository.save(Gallery.create("azores").withPictureIds(List.of("azores/a.jpg")));

        Statistics stats = useCase.compute();

        assertEquals(2, stats.galleryCount());
        assertEquals("iceland", stats.biggestGallery().galleryId());
        assertEquals("Iceland", stats.biggestGallery().name());
        assertEquals(3, stats.biggestGallery().pictureCount());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void savePicture(String id, String galleryId, GpsCoordinates gps,
                             LocalDateTime takenAt, double temperature, String make, String model) {
        Picture picture = Picture.create(id, galleryId, id.substring(id.indexOf('/') + 1))
                .withProcessedImages("https://example.com/" + id + "-thumb.jpg", "https://example.com/" + id + "-full.jpg")
                .withGpsCoordinates(gps)
                .withExifData(new ExifData(make, model, takenAt, "50mm", "f/2.8", 100))
                .withWeatherData(new WeatherData(temperature, 60, 5.0, 1, "Clear"));
        repository.savePicture(picture);
    }

    private Picture picture(String id, String galleryId, GpsCoordinates gps) {
        return Picture.create(id, galleryId, id.substring(id.indexOf('/') + 1)).withGpsCoordinates(gps);
    }
}
