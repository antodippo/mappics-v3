package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import com.antodippo.mappics.infrastructure.exif.ExtractExifDataWithMetadataExtractor;
import com.antodippo.mappics.infrastructure.http.HTTPClientThatAlwaysReturns;
import com.antodippo.mappics.infrastructure.image.ImageResizerTestDouble;
import com.antodippo.mappics.infrastructure.location.FetchLocationDescriptionFromOSM;
import com.antodippo.mappics.infrastructure.persistence.GalleryRepositoryInMemory;
import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import com.antodippo.mappics.infrastructure.weather.FetchWeatherDataFromOpenMeteo;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Exercises the full import flow over the untraced core chain
// (GalleryImporterCore → GalleryProcessorCore → PictureEnricherCore). Span emission is
// covered separately by the Tracing*Test classes.
class GalleryImporterCoreTest {

    private static final String OSM_RESPONSE = """
            {"display_name":"Reykjavik, Iceland","address":{"city":"Reykjavik","country":"Iceland"}}
            """;

    private static final String WEATHER_RESPONSE = """
            {"hourly":{"time":["2017-06-09T18:00"],"temperature_2m":[10.5],
             "relative_humidity_2m":[72],"weather_code":[1],"wind_speed_10m":[9.2]}}
            """;

    private GalleryFileStorageInMemory fileStorage;
    private GalleryRepositoryInMemory  repository;
    private GalleryImporter            useCase;

    @BeforeEach
    void setUp() throws IOException {
        fileStorage = new GalleryFileStorageInMemory("http://localhost/processed");
        repository  = new GalleryRepositoryInMemory();

        fileStorage.addPicture("iceland", "DSC_0114.JPG", fixture("galleries/Iceland/DSC_0114.JPG"));

        useCase = importerFor(fileStorage, repository);
    }

    @Test
    void processesNewPictureWithAllData() {
        useCase.importGalleries(job());

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
    void writesResizedImagesToStorage() {
        useCase.importGalleries(job());

        assertTrue(fileStorage.thumbnailExists("iceland", "DSC_0114.JPG"),
                "Thumbnail must actually be written to storage, not just referenced by URL");
        assertTrue(fileStorage.fullSizeExists("iceland", "DSC_0114.JPG"),
                "Full-size image must actually be written to storage, not just referenced by URL");
    }

    @Test
    void createsGalleryWithAverageGpsAndPictureIds() {
        useCase.importGalleries(job());

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
        useCase.importGalleries(job);

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

        useCase.importGalleries(job());

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
        var uc = importerFor(failingStorage, repo);

        ImportJob job = job();
        uc.importGalleries(job);

        assertEquals(ImportJobStatus.COMPLETED, job.getStatus(), "Job should complete despite one error");
        assertFalse(job.getErrors().isEmpty(), "Broken picture should produce an error entry");
        assertTrue(repo.findPictureById("iceland/DSC_0114.JPG").isPresent(), "Valid picture still processed");
    }

    @Test
    void jobProgressTracksGalleriesAndPictures() {
        ImportJob job = job();
        useCase.importGalleries(job);

        assertEquals(ImportJobStatus.COMPLETED, job.getStatus());
        assertEquals(1, job.getTotalGalleries());
        assertEquals(1, job.getProcessedGalleries());
        assertEquals(1, job.getTotalPictures());
        assertEquals(1, job.getProcessedPictures());
        assertNull(job.getCurrentGallery()); // cleared on complete
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void marksJobFailedAndRethrowsWhenImportBlowsUp() {
        var explodingStorage = new GalleryFileStorageInMemory("http://localhost/processed") {
            @Override
            public List<String> listGalleryIds() {
                throw new IllegalStateException("Simulated storage outage");
            }
        };
        var uc = importerFor(explodingStorage, new GalleryRepositoryInMemory());
        ImportJob job = job();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> uc.importGalleries(job));

        assertEquals("Simulated storage outage", ex.getMessage(), "Exception must propagate so the job exits non-zero");
        assertEquals(ImportJobStatus.FAILED, job.getStatus());
        assertFalse(job.isRunning());
        assertNotNull(job.getCompletedAt());
        assertTrue(job.getErrors().getFirst().contains("Simulated storage outage"));
    }

    @Test
    void jobIsInProgressWhileGalleriesAreProcessed() {
        List<ImportJobStatus> statusSeenByProcessor = new ArrayList<>();
        GalleryProcessor recordingProcessor = (galleryId, job) -> statusSeenByProcessor.add(job.getStatus());
        var uc = new GalleryImporterCore(fileStorage, recordingProcessor);
        ImportJob job = job();

        uc.importGalleries(job);

        assertEquals(List.of(ImportJobStatus.IN_PROGRESS), statusSeenByProcessor);
        assertNotNull(job.getStartedAt());
    }

    @Test
    void multipleGalleriesAreEachProcessed() throws IOException {
        fileStorage.addPicture("azores", "DSC_0892.JPG", fixture("galleries/Azores/DSC_0892.JPG"));

        useCase.importGalleries(job());

        assertTrue(repository.findById("iceland").isPresent());
        assertTrue(repository.findById("azores").isPresent());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private GalleryImporter importerFor(GalleryFileStorage storage, GalleryRepository repo) {
        PictureEnricher enricher = new PictureEnricherCore(
                storage,
                new ExtractExifDataWithMetadataExtractor(),
                new ImageResizerTestDouble(),
                new FetchLocationDescriptionFromOSM(new HTTPClientThatAlwaysReturns(OSM_RESPONSE), new ObjectMapper()),
                new FetchWeatherDataFromOpenMeteo(new HTTPClientThatAlwaysReturns(WEATHER_RESPONSE), new ObjectMapper()),
                0L // no OSM rate limiting in tests
        );
        GalleryProcessor processor = new GalleryProcessorCore(storage, repo, enricher);
        return new GalleryImporterCore(storage, processor);
    }

    private ImportJob job() {
        return new ImportJob("test-" + System.nanoTime());
    }

    private static byte[] fixture(String path) throws IOException {
        try (InputStream in = GalleryImporterCoreTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return in.readAllBytes();
        }
    }
}
