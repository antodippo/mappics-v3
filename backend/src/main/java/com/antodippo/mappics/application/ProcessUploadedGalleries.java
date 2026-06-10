package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ProcessUploadedGalleries {

    private static final Logger log = LoggerFactory.getLogger(ProcessUploadedGalleries.class);
    private static final int THUMBNAIL_MAX_DIM = 400;
    private static final int FULL_SIZE_MAX_DIM  = 1920;

    private final GalleryFileStorage    fileStorage;
    private final GalleryRepository     repository;
    private final ExifExtractor         exifExtractor;
    private final ImageResizer          imageResizer;
    private final LocationDescriptionFetcher locationFetcher;
    private final WeatherFetcher        weatherFetcher;
    private final long                  osmRateLimitMs;

    public ProcessUploadedGalleries(
            GalleryFileStorage fileStorage,
            GalleryRepository repository,
            ExifExtractor exifExtractor,
            ImageResizer imageResizer,
            LocationDescriptionFetcher locationFetcher,
            WeatherFetcher weatherFetcher,
            @Value("${mappics.import.osm-rate-limit-ms:1000}") long osmRateLimitMs) {
        this.fileStorage     = fileStorage;
        this.repository      = repository;
        this.exifExtractor   = exifExtractor;
        this.imageResizer    = imageResizer;
        this.locationFetcher = locationFetcher;
        this.weatherFetcher  = weatherFetcher;
        this.osmRateLimitMs  = osmRateLimitMs;
    }

    // Called through Spring proxy → runs on the async thread pool.
    // Called directly in tests (no proxy) → runs synchronously, no @Async effect.
    @Async
    public void processAsync(ImportJob job) {
        process(job);
    }

    public void process(ImportJob job) {
        job.start();
        try {
            List<String> galleryIds = fileStorage.listGalleryIds();
            job.setTotalGalleries(galleryIds.size());
            log.info("Import started: {} galleries to process", galleryIds.size());

            for (String galleryId : galleryIds) {
                processGallery(galleryId, job);
                job.galleryCompleted();
            }
            job.complete();
            log.info("Import completed: {} galleries, {} pictures processed",
                    job.getProcessedGalleries(), job.getProcessedPictures());
        } catch (Exception e) {
            log.error("Import failed unexpectedly", e);
            job.fail("Unexpected error: " + e.getMessage());
        }
    }

    private void processGallery(String galleryId, ImportJob job) {
        List<String> filenames = fileStorage.listPictureFilenames(galleryId);
        job.startGallery(galleryId, filenames.size());
        log.info("Processing gallery '{}': {} pictures", galleryId, filenames.size());

        Gallery gallery = repository.findById(galleryId)
                .orElseGet(() -> Gallery.create(galleryId));

        List<String> pictureIds = new ArrayList<>();
        List<GpsCoordinates> gpsCoords = new ArrayList<>();

        for (String filename : filenames) {
            String pictureId = galleryId + "/" + filename;
            pictureIds.add(pictureId);

            Optional<Picture> existing = repository.findPictureById(pictureId);

            if (existing.isPresent() && existing.get().hasAllData()) {
                existing.get().getGpsCoordinates().ifPresent(gpsCoords::add);
                job.pictureCompleted();
                continue;
            }

            Picture picture = existing.orElseGet(() -> Picture.create(pictureId, galleryId, filename));

            try {
                picture = enrich(picture);
                picture.getGpsCoordinates().ifPresent(gpsCoords::add);
                repository.savePicture(picture);
            } catch (Exception e) {
                log.warn("Failed to process {}: {}", pictureId, e.getMessage());
                job.addError("Failed to process " + pictureId + ": " + e.getMessage());
            }
            job.pictureCompleted();
        }

        Gallery updated = gallery.withPictureIds(pictureIds);
        Optional<GpsCoordinates> avgGps = Gallery.calculateAverageGps(gpsCoords);
        if (avgGps.isPresent()) {
            updated = updated.withAverageGpsCoordinates(avgGps.get());
        }
        repository.save(updated);
    }

    private Picture enrich(Picture picture) {
        byte[] imageData = fileStorage.readOriginalPicture(
                picture.getGalleryId(), picture.getOriginalFilename());

        // ── EXIF + GPS ────────────────────────────────────────────────────────
        if (picture.getExifData().isEmpty()) {
            ExifExtractionResult result = exifExtractor.extract(imageData);
            if (result.gpsCoordinates() != null) {
                picture = picture.withGpsCoordinates(result.gpsCoordinates());
            }
            if (result.exifData() != null) {
                picture = picture.withExifData(result.exifData());
            }
        }

        // ── Image resizing ────────────────────────────────────────────────────
        if (picture.getThumbnailUrl().isEmpty() || picture.getFullSizeUrl().isEmpty()) {
            byte[] thumbnail = imageResizer.resize(imageData, THUMBNAIL_MAX_DIM);
            byte[] fullSize  = imageResizer.resize(imageData, FULL_SIZE_MAX_DIM);
            fileStorage.writeThumbnail(picture.getGalleryId(), picture.getOriginalFilename(), thumbnail);
            fileStorage.writeFullSize(picture.getGalleryId(), picture.getOriginalFilename(), fullSize);
            picture = picture.withProcessedImages(
                    fileStorage.getThumbnailUrl(picture.getGalleryId(), picture.getOriginalFilename()),
                    fileStorage.getFullSizeUrl(picture.getGalleryId(), picture.getOriginalFilename()));
        }

        // ── Location + weather (require GPS) ──────────────────────────────────
        if (picture.getGpsCoordinates().isEmpty()) {
            log.warn("No GPS in {}, skipping location and weather", picture.getId());
            return picture;
        }

        GpsCoordinates gps = picture.getGpsCoordinates().get();

        if (picture.getLocationDescription().isEmpty()) {
            Optional<LocationDescription> location = locationFetcher.fetch(gps);
            if (location.isPresent()) {
                picture = picture.withLocationDescription(location.get());
            }
            rateLimitOsm();
        }

        if (picture.getWeatherData().isEmpty()) {
            LocalDateTime takenAt = picture.getExifData()
                    .map(ExifData::takenAt)
                    .orElse(null);
            if (takenAt != null) {
                Optional<WeatherData> weather = weatherFetcher.fetch(gps, takenAt);
                if (weather.isPresent()) {
                    picture = picture.withWeatherData(weather.get());
                }
            }
        }

        return picture;
    }

    private void rateLimitOsm() {
        if (osmRateLimitMs <= 0) return;
        try {
            Thread.sleep(osmRateLimitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Import interrupted during OSM rate limiting", e);
        }
    }
}
