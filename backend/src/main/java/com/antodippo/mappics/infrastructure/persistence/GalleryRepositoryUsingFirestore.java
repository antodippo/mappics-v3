package com.antodippo.mappics.infrastructure.persistence;

import com.antodippo.mappics.domain.*;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
@Profile("prod")
public class GalleryRepositoryUsingFirestore implements GalleryRepository {

    private static final String GALLERIES = "galleries";
    private static final String PICTURES  = "pictures";

    private final Firestore firestore;

    public GalleryRepositoryUsingFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    // ── Gallery ───────────────────────────────────────────────────────────────

    @Override
    public void save(Gallery gallery) {
        await(firestore.collection(GALLERIES).document(gallery.getId()).set(galleryToMap(gallery)));
    }

    @Override
    public Optional<Gallery> findById(String id) {
        DocumentSnapshot doc = await(firestore.collection(GALLERIES).document(id).get());
        return doc.exists() ? Optional.of(toGallery(doc)) : Optional.empty();
    }

    @Override
    public List<Gallery> findAll() {
        QuerySnapshot snap = await(firestore.collection(GALLERIES).get());
        return snap.getDocuments().stream().map(doc -> toGallery(doc)).toList();
    }

    // ── Picture ───────────────────────────────────────────────────────────────

    @Override
    public void savePicture(Picture picture) {
        await(firestore.collection(GALLERIES)
                .document(picture.getGalleryId())
                .collection(PICTURES)
                .document(firestoreDocId(picture.getId()))
                .set(pictureToMap(picture)));
    }

    @Override
    public Optional<Picture> findPictureById(String pictureId) {
        int slash = pictureId.indexOf('/');
        String galleryId = pictureId.substring(0, slash);
        DocumentSnapshot doc = await(firestore.collection(GALLERIES)
                .document(galleryId)
                .collection(PICTURES)
                .document(firestoreDocId(pictureId))
                .get());
        return doc.exists() ? Optional.of(toPicture(doc)) : Optional.empty();
    }

    @Override
    public List<Picture> findPicturesByGalleryId(String galleryId) {
        QuerySnapshot snap = await(firestore.collection(GALLERIES)
                .document(galleryId)
                .collection(PICTURES)
                .get());
        return snap.getDocuments().stream().map(doc -> toPicture(doc)).toList();
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    private static Map<String, Object> galleryToMap(Gallery gallery) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", gallery.getId());
        m.put("name", gallery.getName());
        m.put("pictureIds", new ArrayList<>(gallery.getPictureIds()));
        gallery.getAverageGpsCoordinates().ifPresent(gps -> {
            m.put("averageLatitude", gps.latitude());
            m.put("averageLongitude", gps.longitude());
            if (gps.altitude() != null) m.put("averageAltitude", gps.altitude());
        });
        return m;
    }

    private static Map<String, Object> pictureToMap(Picture picture) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", picture.getId());
        m.put("galleryId", picture.getGalleryId());
        m.put("originalFilename", picture.getOriginalFilename());
        picture.getThumbnailUrl().ifPresent(v -> m.put("thumbnailUrl", v));
        picture.getFullSizeUrl().ifPresent(v -> m.put("fullSizeUrl", v));
        picture.getGpsCoordinates().ifPresent(gps -> {
            m.put("gpsLatitude", gps.latitude());
            m.put("gpsLongitude", gps.longitude());
            if (gps.altitude() != null) m.put("gpsAltitude", gps.altitude());
        });
        // Nested sub-documents: presence of the key signals the field was extracted.
        picture.getExifData().ifPresent(exif -> m.put("exif", exifToMap(exif)));
        picture.getLocationDescription().ifPresent(loc -> m.put("location", locationToMap(loc)));
        picture.getWeatherData().ifPresent(weather -> m.put("weather", weatherToMap(weather)));
        return m;
    }

    private static Map<String, Object> exifToMap(ExifData exif) {
        Map<String, Object> m = new HashMap<>();
        m.put("cameraMake", exif.cameraMake());
        m.put("cameraModel", exif.cameraModel());
        m.put("takenAt", exif.takenAt() != null ? exif.takenAt().toString() : null);
        m.put("focalLength", exif.focalLength());
        m.put("aperture", exif.aperture());
        m.put("iso", exif.iso());
        return m;
    }

    private static Map<String, Object> locationToMap(LocationDescription loc) {
        return Map.of("name", loc.name(), "shortDescription", loc.shortDescription());
    }

    private static Map<String, Object> weatherToMap(WeatherData weather) {
        return Map.of(
                "temperatureCelsius", weather.temperatureCelsius(),
                "humidity", weather.humidity(),
                "windSpeedKmh", weather.windSpeedKmh(),
                "weatherCode", weather.weatherCode(),
                "description", weather.description()
        );
    }

    // ── Deserialisation ───────────────────────────────────────────────────────

    private static Gallery toGallery(DocumentSnapshot doc) {
        String id = doc.getString("id");
        String name = doc.getString("name");
        @SuppressWarnings("unchecked")
        List<String> pictureIds = (List<String>) doc.get("pictureIds");
        Double lat = doc.getDouble("averageLatitude");
        Double lon = doc.getDouble("averageLongitude");
        Double avgAlt = doc.getDouble("averageAltitude");
        GpsCoordinates avgGps = lat != null ? new GpsCoordinates(lat, lon, avgAlt) : null;
        return new Gallery(id, name, pictureIds != null ? pictureIds : List.of(), avgGps);
    }

    private static Picture toPicture(DocumentSnapshot doc) {
        @SuppressWarnings("unchecked")
        Map<String, Object> exifMap     = (Map<String, Object>) doc.get("exif");
        @SuppressWarnings("unchecked")
        Map<String, Object> locationMap = (Map<String, Object>) doc.get("location");
        @SuppressWarnings("unchecked")
        Map<String, Object> weatherMap  = (Map<String, Object>) doc.get("weather");

        Double lat = doc.getDouble("gpsLatitude");
        Double lon = doc.getDouble("gpsLongitude");
        Double alt = doc.getDouble("gpsAltitude");

        return new Picture(
                doc.getString("id"),
                doc.getString("galleryId"),
                doc.getString("originalFilename"),
                doc.getString("thumbnailUrl"),
                doc.getString("fullSizeUrl"),
                lat != null ? new GpsCoordinates(lat, lon, alt) : null,
                exifMap != null ? exifFromMap(exifMap) : null,
                locationMap != null ? locationFromMap(locationMap) : null,
                weatherMap != null ? weatherFromMap(weatherMap) : null
        );
    }

    private static ExifData exifFromMap(Map<String, Object> m) {
        String takenAtStr = (String) m.get("takenAt");
        Number iso = (Number) m.get("iso");
        return new ExifData(
                (String) m.get("cameraMake"),
                (String) m.get("cameraModel"),
                takenAtStr != null ? LocalDateTime.parse(takenAtStr) : null,
                (String) m.get("focalLength"),
                (String) m.get("aperture"),
                iso != null ? iso.intValue() : null
        );
    }

    private static LocationDescription locationFromMap(Map<String, Object> m) {
        return new LocationDescription((String) m.get("name"), (String) m.get("shortDescription"));
    }

    private static WeatherData weatherFromMap(Map<String, Object> m) {
        Number temp      = (Number) m.get("temperatureCelsius");
        Number humidity  = (Number) m.get("humidity");
        Number windSpeed = (Number) m.get("windSpeedKmh");
        Number code      = (Number) m.get("weatherCode");
        return new WeatherData(
                temp      != null ? temp.doubleValue()     : 0,
                humidity  != null ? humidity.intValue()    : 0,
                windSpeed != null ? windSpeed.doubleValue(): 0,
                code      != null ? code.intValue()        : 0,
                (String) m.get("description")
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Firestore document IDs cannot contain '/'; strip the gallery prefix.
    private static String firestoreDocId(String pictureId) {
        int slash = pictureId.indexOf('/');
        return slash >= 0 ? pictureId.substring(slash + 1) : pictureId;
    }

    private static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for Firestore", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore operation failed: " + e.getMessage(), e.getCause());
        }
    }
}
