package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GalleryProcessorCore implements GalleryProcessor {

    private static final Logger log = LoggerFactory.getLogger(GalleryProcessorCore.class);

    private final GalleryFileStorage fileStorage;
    private final GalleryRepository  repository;
    private final PictureEnricher    enricher;

    public GalleryProcessorCore(
            GalleryFileStorage fileStorage,
            GalleryRepository repository,
            PictureEnricher enricher) {
        this.fileStorage = fileStorage;
        this.repository  = repository;
        this.enricher    = enricher;
    }

    @Override
    public void processGallery(String galleryId, ImportJob job) {
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
        log.info("Gallery '{}' done: {} enriched, {} skipped", galleryId, filenames.size() - skipped, skipped);
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
