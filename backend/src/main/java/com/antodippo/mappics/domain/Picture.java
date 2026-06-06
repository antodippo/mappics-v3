package com.antodippo.mappics.domain;

import java.util.Optional;

public final class Picture {

    private final String id;
    private final String galleryId;
    private final String originalFilename;
    private final String thumbnailUrl;
    private final String fullSizeUrl;
    private final GpsCoordinates gpsCoordinates;
    private final ExifData exifData;
    private final LocationDescription locationDescription;
    private final WeatherData weatherData;

    public Picture(
            String id,
            String galleryId,
            String originalFilename,
            String thumbnailUrl,
            String fullSizeUrl,
            GpsCoordinates gpsCoordinates,
            ExifData exifData,
            LocationDescription locationDescription,
            WeatherData weatherData
    ) {
        this.id = id;
        this.galleryId = galleryId;
        this.originalFilename = originalFilename;
        this.thumbnailUrl = thumbnailUrl;
        this.fullSizeUrl = fullSizeUrl;
        this.gpsCoordinates = gpsCoordinates;
        this.exifData = exifData;
        this.locationDescription = locationDescription;
        this.weatherData = weatherData;
    }

    public static Picture create(String id, String galleryId, String originalFilename) {
        return new Picture(id, galleryId, originalFilename, null, null, null, null, null, null);
    }

    public Picture withProcessedImages(String thumbnailUrl, String fullSizeUrl) {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public Picture withGpsCoordinates(GpsCoordinates gpsCoordinates) {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public Picture withExifData(ExifData exifData) {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public Picture withLocationDescription(LocationDescription locationDescription) {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public Picture withWeatherData(WeatherData weatherData) {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public boolean hasAllData() {
        return thumbnailUrl != null
                && fullSizeUrl != null
                && gpsCoordinates != null
                && exifData != null
                && locationDescription != null
                && weatherData != null;
    }

    public String getId() { return id; }
    public String getGalleryId() { return galleryId; }
    public String getOriginalFilename() { return originalFilename; }

    public Optional<String> getThumbnailUrl() { return Optional.ofNullable(thumbnailUrl); }
    public Optional<String> getFullSizeUrl() { return Optional.ofNullable(fullSizeUrl); }
    public Optional<GpsCoordinates> getGpsCoordinates() { return Optional.ofNullable(gpsCoordinates); }
    public Optional<ExifData> getExifData() { return Optional.ofNullable(exifData); }
    public Optional<LocationDescription> getLocationDescription() { return Optional.ofNullable(locationDescription); }
    public Optional<WeatherData> getWeatherData() { return Optional.ofNullable(weatherData); }
}
