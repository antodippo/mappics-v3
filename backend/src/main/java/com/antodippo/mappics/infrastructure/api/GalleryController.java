package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/galleries")
@CrossOrigin(origins = "*")
public class GalleryController {

    private final GalleryRepository repository;

    public GalleryController(GalleryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<GallerySummaryResponse> listGalleries() {
        return repository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalleryDetailResponse> getGallery(@PathVariable String id) {
        return repository.findById(id)
                .map(gallery -> {
                    List<PictureResponse> pictures = repository.findPicturesByGalleryId(id).stream()
                            .map(this::toPicture)
                            .toList();
                    return ResponseEntity.ok(toDetail(gallery, pictures));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private GallerySummaryResponse toSummary(Gallery gallery) {
        return new GallerySummaryResponse(
                gallery.getId(),
                gallery.getName(),
                gallery.getAverageGpsCoordinates().map(GpsResponse::from).orElse(null),
                gallery.getPictureIds().size()
        );
    }

    private GalleryDetailResponse toDetail(Gallery gallery, List<PictureResponse> pictures) {
        return new GalleryDetailResponse(
                gallery.getId(),
                gallery.getName(),
                gallery.getAverageGpsCoordinates().map(GpsResponse::from).orElse(null),
                pictures
        );
    }

    private PictureResponse toPicture(Picture picture) {
        return new PictureResponse(
                picture.getId(),
                picture.getThumbnailUrl().orElse(null),
                picture.getFullSizeUrl().orElse(null),
                picture.getGpsCoordinates().map(GpsResponse::from).orElse(null),
                picture.getExifData().map(this::toExif).orElse(null),
                picture.getLocationDescription().map(loc ->
                        new LocationResponse(loc.name(), loc.shortDescription())).orElse(null),
                picture.getWeatherData().map(w ->
                        new WeatherResponse(w.temperatureCelsius(), w.humidity(), w.windSpeedKmh(), w.weatherCode(), w.description())).orElse(null)
        );
    }

    private ExifResponse toExif(ExifData exif) {
        return new ExifResponse(
                exif.cameraMake(),
                exif.cameraModel(),
                exif.takenAt() != null ? exif.takenAt().toString() : null,
                exif.focalLength(),
                exif.aperture(),
                exif.iso()
        );
    }

    // ── Response records ──────────────────────────────────────────────────────

    record GpsResponse(double latitude, double longitude, Double altitude) {
        static GpsResponse from(GpsCoordinates gps) {
            return new GpsResponse(gps.latitude(), gps.longitude(), gps.altitude());
        }
    }

    record GallerySummaryResponse(
            String id,
            String name,
            GpsResponse averageGps,
            int pictureCount
    ) {}

    record GalleryDetailResponse(
            String id,
            String name,
            GpsResponse averageGps,
            List<PictureResponse> pictures
    ) {}

    record PictureResponse(
            String id,
            String thumbnailUrl,
            String fullSizeUrl,
            GpsResponse gps,
            ExifResponse exif,
            LocationResponse location,
            WeatherResponse weather
    ) {}

    record ExifResponse(
            String cameraMake,
            String cameraModel,
            String takenAt,
            String focalLength,
            String aperture,
            Integer iso
    ) {}

    record LocationResponse(String name, String shortDescription) {}

    record WeatherResponse(
            double temperatureCelsius,
            int humidity,
            double windSpeedKmh,
            int weatherCode,
            String description
    ) {}
}
