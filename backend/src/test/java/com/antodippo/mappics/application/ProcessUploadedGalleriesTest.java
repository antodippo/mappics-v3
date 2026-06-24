package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import com.antodippo.mappics.infrastructure.exif.ExtractExifDataWithMetadataExtractor;
import com.antodippo.mappics.infrastructure.http.HTTPClientThatAlwaysReturns;
import com.antodippo.mappics.infrastructure.image.ImageResizerTestDouble;
import com.antodippo.mappics.infrastructure.location.FetchLocationDescriptionFromOSM;
import com.antodippo.mappics.infrastructure.persistence.GalleryRepositoryInMemory;
import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import com.antodippo.mappics.infrastructure.weather.FetchWeatherDataFromOpenMeteo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class ProcessUploadedGalleriesTest {

    private static final String OSM_RESPONSE = """
            {"display_name":"Reykjavik, Iceland","address":{"city":"Reykjavik","country":"Iceland"}}
            """;

    private static final String WEATHER_RESPONSE = """
            {"hourly":{"time":["2017-06-09T18:00"],"temperature_2m":[10.5],
             "relative_humidity_2m":[72],"weather_code":[1],"wind_speed_10m":[9.2]}}
            """;

    private GalleryFileStorageInMemory fileStorage;
    private GalleryRepositoryInMemory  repository;
    private ProcessUploadedGalleries   useCase;

    @BeforeEach
    void setUp() throws IOException {
        fileStorage = new GalleryFileStorageInMemory("http://localhost/processed");
        repository  = new GalleryRepositoryInMemory();

        fileStorage.addPicture("iceland", "DSC_0114.JPG", fixture("galleries/Iceland/DSC_0114.JPG"));

        PictureEnricher enricher = new PictureEnricher(
                fileStorage,
                new ExtractExifDataWithMetadataExtractor(),
                new ImageResizerTestDouble(),
                new FetchLocationDescriptionFromOSM(new HTTPClientThatAlwaysReturns(OSM_RESPONSE), new ObjectMapper()),
                new FetchWeatherDataFromOpenMeteo(new HTTPClientThatAlwaysReturns(WEATHER_RESPONSE), new ObjectMapper()),
                0L // no OSM rate limiting in tests
        );
        useCase = new ProcessUploadedGalleries(
                fileStorage,
                repository,
                enricher,
                mock(Tracer.class, RETURNS_DEEP_STUBS)
        );
    }

    @Test
    void processesNewPictureWithAllData() {
        useCase.process(job());

        Picture picture = repository.findPictureById("iceland/DSC_0114.JPG").orElseThrow();
        assertTrue(picture.hasAllData());
        assertTrue(picture.getThumbnailUrl().isPresent());
        assertTrue(picture.getFullSizeUrl().isPresent());
        assertTrue(picture.getGpsCoordinates().isPresent());
        assertTrue(picture.getExifData().isPresent());
        assertTrue(picture.getLocationDescription().isPresent());
        assertTrue(picture.getWeatherData().isPresent());
    }

    @Test
    void createsGalleryWithAverageGpsAndPictureIds() {
        useCase.process(job());

        Gallery gallery = repository.findById("iceland").orElseThrow();
        assertEquals(List.of("iceland/DSC_0114.JPG"), gallery.getPictureIds());
        assertTrue(gallery.getAverageGpsCoordinates().isPresent());
    }

    @Test
    void skipsFullyProcessedPictures() {
        // Pre-load a fully processed picture into the repository
        Picture processed = new PictureBuilder()
                .withId("iceland/DSC_0114.JPG")
                .withGalleryId("iceland")
                .withOriginalFilename("DSC_0114.JPG")
                .build();
        repository.savePicture(processed);

        ImportJob job = job();
        useCase.process(job);

        // processedPictures increments for skipped ones too, so gallery is still updated
        assertEquals(1, job.getProcessedPictures());
        // But the picture in the repo should be the one we pre-loaded (unchanged)
        Picture found = repository.findPictureById("iceland/DSC_0114.JPG").orElseThrow();
        assertEquals("Canon", found.getExifData().orElseThrow().cameraMake()); // from PictureBuilder
    }

    @Test
    void fillsMissingFieldsOnPartiallyProcessedPicture() {
        // Pre-load a picture that has EXIF+images but no location or weather.
        // takenAt must be non-null for weather fetching to proceed.
        Picture partial = Picture.create("iceland/DSC_0114.JPG", "iceland", "DSC_0114.JPG")
                .withExifData(new ExifData("Sony", "F5121",
                        java.time.LocalDateTime.of(2017, 6, 9, 18, 43, 32), null, null, null))
                .withGpsCoordinates(new GpsCoordinates(64.26, -21.12, null))
                .withProcessedImages("http://localhost/thumb.jpg", "http://localhost/full.jpg");
        repository.savePicture(partial);

        useCase.process(job());

        Picture enriched = repository.findPictureById("iceland/DSC_0114.JPG").orElseThrow();
        assertTrue(enriched.getLocationDescription().isPresent(), "Location should be filled in");
        assertTrue(enriched.getWeatherData().isPresent(), "Weather should be filled in");
        // Images should not be overwritten (already present)
        assertEquals("http://localhost/thumb.jpg", enriched.getThumbnailUrl().orElseThrow());
    }

    @Test
    void recordsErrorAndContinuesWhenOnePictureFails() throws IOException {
        // Simulate a storage I/O failure on one picture. Corrupt bytes alone are handled
        // gracefully (EXIF returns empty, test-double resizer never throws), so we need a
        // real exception at the read level to exercise the error-recording path.
        var failingStorage = new GalleryFileStorageInMemory("http://localhost/processed") {
            @Override
            public byte[] readOriginalPicture(String galleryId, String filename) {
                if ("broken.JPG".equals(filename)) throw new RuntimeException("Simulated disk read error");
                return super.readOriginalPicture(galleryId, filename);
            }
        };
        failingStorage.addPicture("iceland", "DSC_0114.JPG", fixture("galleries/Iceland/DSC_0114.JPG"));
        failingStorage.addPicture("iceland", "broken.JPG", new byte[]{0x00});

        var repo = new GalleryRepositoryInMemory();
        var uc = new ProcessUploadedGalleries(
                failingStorage, repo,
                new PictureEnricher(
                        failingStorage,
                        new ExtractExifDataWithMetadataExtractor(),
                        new ImageResizerTestDouble(),
                        new FetchLocationDescriptionFromOSM(new HTTPClientThatAlwaysReturns(OSM_RESPONSE), new ObjectMapper()),
                        new FetchWeatherDataFromOpenMeteo(new HTTPClientThatAlwaysReturns(WEATHER_RESPONSE), new ObjectMapper()),
                        0L),
                mock(Tracer.class, RETURNS_DEEP_STUBS)
        );

        ImportJob job = job();
        uc.process(job);

        assertEquals(ImportJobStatus.COMPLETED, job.getStatus(), "Job should complete despite one error");
        assertFalse(job.getErrors().isEmpty(), "Broken picture should produce an error entry");
        assertTrue(repo.findPictureById("iceland/DSC_0114.JPG").isPresent(), "Valid picture still processed");
    }

    @Test
    void jobProgressTracksGalleriesAndPictures() {
        ImportJob job = job();
        useCase.process(job);

        assertEquals(ImportJobStatus.COMPLETED, job.getStatus());
        assertEquals(1, job.getTotalGalleries());
        assertEquals(1, job.getProcessedGalleries());
        assertEquals(1, job.getTotalPictures());
        assertEquals(1, job.getProcessedPictures());
        assertNull(job.getCurrentGallery()); // cleared on complete
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void multipleGalleriesAreEachProcessed() throws IOException {
        fileStorage.addPicture("azores", "DSC_0892.JPG", fixture("galleries/Azores/DSC_0892.JPG"));

        useCase.process(job());

        assertTrue(repository.findById("iceland").isPresent());
        assertTrue(repository.findById("azores").isPresent());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ImportJob job() {
        return new ImportJob("test-" + System.nanoTime());
    }

    private static byte[] fixture(String path) throws IOException {
        try (InputStream in = ProcessUploadedGalleriesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return in.readAllBytes();
        }
    }
}
