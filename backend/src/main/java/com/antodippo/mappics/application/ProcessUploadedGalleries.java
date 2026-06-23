package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ProcessUploadedGalleries {

    private static final Logger log = LoggerFactory.getLogger(ProcessUploadedGalleries.class);

    private final GalleryFileStorage fileStorage;
    private final GalleryRepository  repository;
    private final PictureEnricher    enricher;
    private final Tracer             tracer;

    public ProcessUploadedGalleries(
            GalleryFileStorage fileStorage,
            GalleryRepository repository,
            PictureEnricher enricher,
            Tracer tracer) {
        this.fileStorage = fileStorage;
        this.repository  = repository;
        this.enricher    = enricher;
        this.tracer      = tracer;
    }

    // Called through Spring proxy → runs on the async thread pool.
    // Called directly in tests (no proxy) → runs synchronously, no @Async effect.
    @Async
    public void processAsync(ImportJob job) {
        process(job);
    }

    public void process(ImportJob job) {
        MDC.put("importJobId", job.getId());
        Span importSpan = tracer.nextSpan().name("import").tag("jobId", job.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(importSpan)) {
            long startMs = System.currentTimeMillis();
            job.start();
            List<String> galleryIds = fileStorage.listGalleryIds();
            job.setTotalGalleries(galleryIds.size());
            log.info("Import started: {} galleries to process", galleryIds.size());

            for (String galleryId : galleryIds) {
                processGallery(galleryId, job);
                job.galleryCompleted();
            }

            job.complete();
            importSpan.tag("galleries.processed", String.valueOf(job.getProcessedGalleries()));
            importSpan.tag("pictures.processed", String.valueOf(job.getProcessedPictures()));
            importSpan.tag("errors", String.valueOf(job.getErrors().size()));
            log.info("Import completed in {}ms: {} galleries, {} pictures, {} errors",
                    System.currentTimeMillis() - startMs,
                    job.getProcessedGalleries(), job.getProcessedPictures(), job.getErrors().size());
        } catch (Exception e) {
            importSpan.error(e);
            log.error("Import failed unexpectedly", e);
            job.fail("Unexpected error: " + e.getMessage());
        } finally {
            importSpan.end();
            MDC.clear();
        }
    }

    private void processGallery(String galleryId, ImportJob job) {
        MDC.put("galleryId", galleryId);
        Span gallerySpan = tracer.nextSpan().name("import.gallery").tag("galleryId", galleryId).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(gallerySpan)) {
            List<String> filenames = fileStorage.listPictureFilenames(galleryId);
            job.startGallery(galleryId, filenames.size());
            log.info("Gallery '{}' started: {} pictures", galleryId, filenames.size());

            Gallery gallery = repository.findById(galleryId)
                    .orElseGet(() -> Gallery.create(galleryId));

            List<String> pictureIds = new ArrayList<>();
            List<GpsCoordinates> gpsCoords = new ArrayList<>();
            int skipped = 0;

            for (String filename : filenames) {
                String pictureId = galleryId + "/" + filename;
                pictureIds.add(pictureId);

                Optional<Picture> existing = repository.findPictureById(pictureId);

                if (existing.isPresent() && existing.get().hasAllData()) {
                    log.debug("Picture '{}' skipped (already complete)", pictureId);
                    existing.get().getGpsCoordinates().ifPresent(gpsCoords::add);
                    job.pictureCompleted();
                    skipped++;
                    continue;
                }

                Picture picture = existing.orElseGet(() -> Picture.create(pictureId, galleryId, filename));
                log.info("Enriching picture '{}'", pictureId);

                try {
                    picture = enrich(picture);
                    picture.getGpsCoordinates().ifPresent(gpsCoords::add);
                    repository.savePicture(picture);
                } catch (Exception e) {
                    log.warn("Failed to process '{}': {}", pictureId, e.getMessage());
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
            gallerySpan.tag("pictures.total", String.valueOf(filenames.size()));
            gallerySpan.tag("pictures.skipped", String.valueOf(skipped));
            log.info("Gallery '{}' done: {} enriched, {} skipped", galleryId, filenames.size() - skipped, skipped);
        } finally {
            gallerySpan.end();
            MDC.remove("galleryId");
        }
    }

    private Picture enrich(Picture picture) {
        byte[] imageData = fileStorage.readOriginalPicture(
                picture.getGalleryId(), picture.getOriginalFilename());

        if (picture.getExifData().isEmpty())
            picture = enricher.extractExif(picture, imageData);

        if (picture.getThumbnailUrl().isEmpty() || picture.getFullSizeUrl().isEmpty())
            picture = enricher.resizeImages(picture, imageData);

        if (picture.getGpsCoordinates().isEmpty()) {
            log.warn("No GPS coordinates in '{}', skipping location and weather", picture.getId());
            return picture;
        }

        GpsCoordinates gps = picture.getGpsCoordinates().get();

        if (picture.getLocationDescription().isEmpty())
            picture = enricher.fetchLocation(picture, gps);

        if (picture.getWeatherData().isEmpty()) {
            LocalDateTime takenAt = picture.getExifData().map(ExifData::takenAt).orElse(null);
            if (takenAt != null)
                picture = enricher.fetchWeather(picture, gps, takenAt);
            else
                log.debug("No takenAt in EXIF for '{}', skipping weather fetch", picture.getId());
        }

        return picture;
    }
}
