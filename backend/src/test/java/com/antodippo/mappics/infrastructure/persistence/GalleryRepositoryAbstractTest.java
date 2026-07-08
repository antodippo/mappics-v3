package com.antodippo.mappics.infrastructure.persistence;

import com.antodippo.mappics.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

abstract class GalleryRepositoryAbstractTest {

    protected GalleryRepository repository;

    protected abstract GalleryRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    // ── Gallery ───────────────────────────────────────────────────────────────

    @Test
    void savesAndRetrievesGallery() {
        Gallery gallery = Gallery.create("iceland");

        repository.save(gallery);

        Gallery found = repository.findById("iceland").orElseThrow();
        assertEquals("iceland", found.getId());
        assertEquals("Iceland", found.getName());
        assertTrue(found.getPictureIds().isEmpty());
        assertTrue(found.getAverageGpsCoordinates().isEmpty());
    }

    @Test
    void savesGalleryWithAverageGpsAndPictureIds() {
        Gallery gallery = Gallery.create("azores")
                .withPictureIds(List.of("azores/p1.jpg", "azores/p2.jpg"))
                .withAverageGpsCoordinates(new GpsCoordinates(37.84, -25.79, null));

        repository.save(gallery);

        Gallery found = repository.findById("azores").orElseThrow();
        assertEquals(2, found.getPictureIds().size());
        GpsCoordinates gps = found.getAverageGpsCoordinates().orElseThrow();
        assertEquals(37.84, gps.latitude(), 0.001);
        assertEquals(-25.79, gps.longitude(), 0.001);
    }

    @Test
    void overwritingGallerySavesLatestVersion() {
        repository.save(Gallery.create("iceland").withPictureIds(List.of("iceland/p1.jpg")));
        repository.save(Gallery.create("iceland").withPictureIds(List.of("iceland/p1.jpg", "iceland/p2.jpg")));

        assertEquals(2, repository.findById("iceland").orElseThrow().getPictureIds().size());
    }

    @Test
    void findByIdReturnsEmptyForUnknownGallery() {
        assertTrue(repository.findById("nonexistent").isEmpty());
    }

    @Test
    void findAllReturnsAllSavedGalleries() {
        repository.save(Gallery.create("iceland"));
        repository.save(Gallery.create("azores"));

        List<Gallery> all = repository.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(g -> g.getId().equals("iceland")));
        assertTrue(all.stream().anyMatch(g -> g.getId().equals("azores")));
    }

    // ── Picture ───────────────────────────────────────────────────────────────

    @Test
    void savesAndRetrievesUnprocessedPicture() {
        Picture picture = Picture.create("iceland/DSC_0001.JPG", "iceland", "DSC_0001.JPG");

        repository.savePicture(picture);

        Picture found = repository.findPictureById("iceland/DSC_0001.JPG").orElseThrow();
        assertEquals("iceland/DSC_0001.JPG", found.getId());
        assertEquals("iceland", found.getGalleryId());
        assertEquals("DSC_0001.JPG", found.getOriginalFilename());
        assertTrue(found.getThumbnailUrl().isEmpty());
        assertTrue(found.getGpsCoordinates().isEmpty());
        assertNull(found.getExifData().orElse(null));
    }

    @Test
    void savesAndRetrievesFullyProcessedPicture() {
        Picture picture = new com.antodippo.mappics.domain.PictureBuilder()
                .withId("iceland/DSC_0001.JPG")
                .withGalleryId("iceland")
                .build();

        repository.savePicture(picture);

        Picture found = repository.findPictureById("iceland/DSC_0001.JPG").orElseThrow();
        assertTrue(found.hasAllData());
        assertEquals("Canon", found.getExifData().orElseThrow().cameraMake());
        assertEquals("London", found.getLocationDescription().orElseThrow().name());
        assertEquals(18.5, found.getWeatherData().orElseThrow().temperatureCelsius(), 0.01);
        assertEquals(65, found.getWeatherData().orElseThrow().humidity());
    }

    @Test
    void overwritingPictureSavesLatestVersion() {
        Picture original = Picture.create("iceland/DSC_0001.JPG", "iceland", "DSC_0001.JPG");
        repository.savePicture(original);

        Picture enriched = original.withGpsCoordinates(new GpsCoordinates(64.26, -21.12, null));
        repository.savePicture(enriched);

        Picture found = repository.findPictureById("iceland/DSC_0001.JPG").orElseThrow();
        assertTrue(found.getGpsCoordinates().isPresent());
    }

    @Test
    void findPictureByIdReturnsEmptyForUnknownPicture() {
        assertTrue(repository.findPictureById("iceland/missing.jpg").isEmpty());
    }

    @Test
    void findPicturesByGalleryIdReturnsOnlyPicturesForThatGallery() {
        repository.savePicture(Picture.create("iceland/p1.jpg", "iceland", "p1.jpg"));
        repository.savePicture(Picture.create("iceland/p2.jpg", "iceland", "p2.jpg"));
        repository.savePicture(Picture.create("azores/p3.jpg", "azores",  "p3.jpg"));

        List<Picture> icelandPictures = repository.findPicturesByGalleryId("iceland");

        assertEquals(2, icelandPictures.size());
        assertTrue(icelandPictures.stream().allMatch(p -> p.getGalleryId().equals("iceland")));
    }

    @Test
    void findAllPicturesReturnsPicturesAcrossEveryGallery() {
        repository.savePicture(Picture.create("iceland/p1.jpg", "iceland", "p1.jpg"));
        repository.savePicture(Picture.create("iceland/p2.jpg", "iceland", "p2.jpg"));
        repository.savePicture(Picture.create("azores/p3.jpg", "azores", "p3.jpg"));

        List<Picture> all = repository.findAllPictures();

        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(p -> p.getId().equals("iceland/p1.jpg")));
        assertTrue(all.stream().anyMatch(p -> p.getId().equals("azores/p3.jpg")));
    }

    @Test
    void findAllPicturesReturnsEmptyWhenNoPictures() {
        assertTrue(repository.findAllPictures().isEmpty());
    }

    @Test
    void nullExifDataRoundtripsAsAbsent() {
        Picture picture = Picture.create("iceland/p1.jpg", "iceland", "p1.jpg");
        repository.savePicture(picture);

        Picture found = repository.findPictureById("iceland/p1.jpg").orElseThrow();

        // exifData == null means "not yet extracted"; must survive a save/load cycle
        assertTrue(found.getExifData().isEmpty());
    }
}
