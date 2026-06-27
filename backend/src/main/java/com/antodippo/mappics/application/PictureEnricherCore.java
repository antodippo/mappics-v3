package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PictureEnricherCore implements PictureEnricher {

    private static final Logger log = LoggerFactory.getLogger(PictureEnricherCore.class);
    private static final int THUMBNAIL_MAX_DIM = 400;
    private static final int FULL_SIZE_MAX_DIM  = 1920;

    private final GalleryFileStorage         fileStorage;
    private final ExifExtractor              exifExtractor;
    private final ImageResizer               imageResizer;
    private final LocationDescriptionFetcher locationFetcher;
    private final WeatherFetcher             weatherFetcher;
    private final long                       osmRateLimitMs;

    public PictureEnricherCore(
            GalleryFileStorage fileStorage,
            ExifExtractor exifExtractor,
            ImageResizer imageResizer,
            LocationDescriptionFetcher locationFetcher,
            WeatherFetcher weatherFetcher,
            @Value("${mappics.import.osm-rate-limit-ms:1000}") long osmRateLimitMs) {
        this.fileStorage     = fileStorage;
        this.exifExtractor   = exifExtractor;
        this.imageResizer    = imageResizer;
        this.locationFetcher = locationFetcher;
        this.weatherFetcher  = weatherFetcher;
        this.osmRateLimitMs  = osmRateLimitMs;
    }

    @Override
    public Picture extractExif(Picture picture, byte[] imageData) {
        ExifExtractionResult result = exifExtractor.extract(imageData);
        if (result.gpsCoordinates() != null) {
            picture = picture.withGpsCoordinates(result.gpsCoordinates());
            log.debug("GPS extracted: lat={}, lon={}", result.gpsCoordinates().latitude(), result.gpsCoordinates().longitude());
        } else {
            log.debug("No GPS coordinates in EXIF");
        }
        if (result.exifData() != null) {
            picture = picture.withExifData(result.exifData());
            log.debug("EXIF extracted: {} {}, taken at {}", result.exifData().cameraMake(), result.exifData().cameraModel(), result.exifData().takenAt());
        } else {
            log.debug("No EXIF data found in image");
        }
        return picture;
    }

    @Override
    public Picture resizeImages(Picture picture, byte[] imageData) {
        ResizedImages resized = imageResizer.resizeToBounds(imageData, THUMBNAIL_MAX_DIM, FULL_SIZE_MAX_DIM);
        fileStorage.writeThumbnail(picture.getGalleryId(), picture.getOriginalFilename(), resized.thumbnail());
        fileStorage.writeFullSize(picture.getGalleryId(), picture.getOriginalFilename(), resized.fullSize());
        picture = picture.withProcessedImages(
                fileStorage.getThumbnailUrl(picture.getGalleryId(), picture.getOriginalFilename()),
                fileStorage.getFullSizeUrl(picture.getGalleryId(), picture.getOriginalFilename()));
        log.debug("Images resized and stored (thumbnail: {}px, full-size: {}px)", THUMBNAIL_MAX_DIM, FULL_SIZE_MAX_DIM);
        return picture;
    }

    @Override
    public Picture fetchLocation(Picture picture, GpsCoordinates gps) {
        Optional<LocationDescription> location = locationFetcher.fetch(gps);
        if (location.isPresent()) {
            picture = picture.withLocationDescription(location.get());
            log.debug("Location fetched: {}", location.get().name());
        } else {
            log.warn("Location fetch returned empty for '{}' at lat={}, lon={}", picture.getId(), gps.latitude(), gps.longitude());
        }
        rateLimitOsm();
        return picture;
    }

    @Override
    public Picture fetchWeather(Picture picture, GpsCoordinates gps, LocalDateTime takenAt) {
        Optional<WeatherData> weather = weatherFetcher.fetch(gps, takenAt);
        if (weather.isPresent()) {
            picture = picture.withWeatherData(weather.get());
            log.debug("Weather fetched: {}°C, {}", weather.get().temperatureCelsius(), weather.get().description());
        } else {
            log.warn("Weather fetch returned empty for '{}' at lat={}, lon={}, takenAt={}", picture.getId(), gps.latitude(), gps.longitude(), takenAt);
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
