package com.antodippo.mappics.domain;

import java.time.LocalDateTime;

public class PictureBuilder {

    private String id = "gallery/photo.jpg";
    private String galleryId = "gallery";
    private String originalFilename = "photo.jpg";
    private String thumbnailUrl = "https://example.com/thumb.jpg";
    private String fullSizeUrl = "https://example.com/full.jpg";
    private GpsCoordinates gpsCoordinates = new GpsCoordinates(51.5, -0.1);
    private ExifData exifData = new ExifData("Canon", "EOS 5D", LocalDateTime.of(2023, 6, 15, 14, 30), "50mm", "f/2.8", 100);
    private LocationDescription locationDescription = new LocationDescription("London", "Capital of the United Kingdom");
    private WeatherData weatherData = new WeatherData(18.5, 65, 1, "Mainly clear");

    public PictureBuilder withId(String id) { this.id = id; return this; }
    public PictureBuilder withGalleryId(String galleryId) { this.galleryId = galleryId; return this; }
    public PictureBuilder withOriginalFilename(String filename) { this.originalFilename = filename; return this; }
    public PictureBuilder withNoProcessedImages() { this.thumbnailUrl = null; this.fullSizeUrl = null; return this; }
    public PictureBuilder withNoGps() { this.gpsCoordinates = null; return this; }
    public PictureBuilder withNoExif() { this.exifData = null; return this; }
    public PictureBuilder withNoLocation() { this.locationDescription = null; return this; }
    public PictureBuilder withNoWeather() { this.weatherData = null; return this; }

    public Picture build() {
        return new Picture(id, galleryId, originalFilename, thumbnailUrl, fullSizeUrl,
                gpsCoordinates, exifData, locationDescription, weatherData);
    }

    public static Picture aFullyProcessedPicture() {
        return new PictureBuilder().build();
    }

    public static Picture anUnprocessedPicture() {
        return Picture.create("gallery/photo.jpg", "gallery", "photo.jpg");
    }
}
