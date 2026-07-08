package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Component
public class GalleryStatisticsCore implements GalleryStatistics {

    private final GalleryRepository repository;

    public GalleryStatisticsCore(GalleryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Statistics compute() {
        List<Picture> pictures = repository.findAllPictures();
        List<Gallery> galleries = repository.findAll();
        Map<String, String> galleryNames = galleries.stream()
                .collect(Collectors.toMap(Gallery::getId, Gallery::getName));

        return new Statistics(
                pictures.size(),
                galleries.size(),
                totalTraveledKm(pictures),
                gpsExtremum(pictures, GpsCoordinates::latitude, true, galleryNames),
                gpsExtremum(pictures, GpsCoordinates::latitude, false, galleryNames),
                gpsExtremum(pictures, GpsCoordinates::longitude, true, galleryNames),
                gpsExtremum(pictures, GpsCoordinates::longitude, false, galleryNames),
                highestAltitude(pictures, galleryNames),
                weatherExtremum(pictures, false, galleryNames),
                weatherExtremum(pictures, true, galleryNames),
                oldestOrNewest(pictures, false, galleryNames),
                oldestOrNewest(pictures, true, galleryNames),
                mostUsedCamera(pictures),
                dateSpanDays(pictures),
                biggestGallery(galleries),
                averageTemperature(pictures)
        );
    }

    private Statistics.PictureStat gpsExtremum(List<Picture> pictures, ToDoubleFunction<GpsCoordinates> value, boolean max, Map<String, String> galleryNames) {
        Comparator<Picture> byValue = Comparator.comparingDouble(p -> value.applyAsDouble(p.getGpsCoordinates().orElseThrow()));
        return pictures.stream()
                .filter(p -> p.getGpsCoordinates().isPresent())
                .max(max ? byValue : byValue.reversed())
                .map(p -> stat(p, value.applyAsDouble(p.getGpsCoordinates().orElseThrow()), galleryNames))
                .orElse(null);
    }

    private Statistics.PictureStat highestAltitude(List<Picture> pictures, Map<String, String> galleryNames) {
        return pictures.stream()
                .filter(p -> p.getGpsCoordinates().map(GpsCoordinates::altitude).isPresent())
                .max(Comparator.comparingDouble(p -> p.getGpsCoordinates().orElseThrow().altitude()))
                .map(p -> stat(p, p.getGpsCoordinates().orElseThrow().altitude(), galleryNames))
                .orElse(null);
    }

    private Statistics.PictureStat weatherExtremum(List<Picture> pictures, boolean hottest, Map<String, String> galleryNames) {
        Comparator<Picture> byTemp = Comparator.comparingDouble(p -> p.getWeatherData().orElseThrow().temperatureCelsius());
        return pictures.stream()
                .filter(p -> p.getWeatherData().isPresent())
                .max(hottest ? byTemp : byTemp.reversed())
                .map(p -> stat(p, p.getWeatherData().orElseThrow().temperatureCelsius(), galleryNames))
                .orElse(null);
    }

    private Statistics.DatedPictureStat oldestOrNewest(List<Picture> pictures, boolean newest, Map<String, String> galleryNames) {
        Comparator<Picture> byTakenAt = Comparator.comparing(p -> takenAt(p).orElseThrow());
        return pictures.stream()
                .filter(p -> takenAt(p).isPresent())
                .max(newest ? byTakenAt : byTakenAt.reversed())
                .map(p -> new Statistics.DatedPictureStat(
                        p.getId(), p.getGalleryId(), galleryName(p, galleryNames),
                        p.getThumbnailUrl().orElse(null), takenAt(p).orElseThrow()))
                .orElse(null);
    }

    private double totalTraveledKm(List<Picture> pictures) {
        List<Picture> route = pictures.stream()
                .filter(p -> p.getGpsCoordinates().isPresent() && takenAt(p).isPresent())
                .sorted(Comparator.comparing(p -> takenAt(p).orElseThrow()))
                .toList();
        double km = 0;
        for (int i = 1; i < route.size(); i++) {
            km += GeoDistance.kmBetween(
                    route.get(i - 1).getGpsCoordinates().orElseThrow(),
                    route.get(i).getGpsCoordinates().orElseThrow());
        }
        return km;
    }

    private String mostUsedCamera(List<Picture> pictures) {
        return pictures.stream()
                .map(Picture::getExifData)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(GalleryStatisticsCore::cameraLabel)
                .filter(label -> !label.isBlank())
                .collect(Collectors.groupingBy(label -> label, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Integer dateSpanDays(List<Picture> pictures) {
        List<LocalDateTime> takenAts = pictures.stream()
                .map(GalleryStatisticsCore::takenAt)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
        if (takenAts.isEmpty()) {
            return null;
        }
        LocalDateTime oldest = takenAts.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime newest = takenAts.stream().max(Comparator.naturalOrder()).orElseThrow();
        return (int) Duration.between(oldest, newest).toDays();
    }

    private Statistics.BiggestGallery biggestGallery(List<Gallery> galleries) {
        return galleries.stream()
                .filter(g -> !g.getPictureIds().isEmpty())
                .max(Comparator.comparingInt(g -> g.getPictureIds().size()))
                .map(g -> new Statistics.BiggestGallery(g.getId(), g.getName(), g.getPictureIds().size()))
                .orElse(null);
    }

    private Double averageTemperature(List<Picture> pictures) {
        OptionalDouble avg = pictures.stream()
                .filter(p -> p.getWeatherData().isPresent())
                .mapToDouble(p -> p.getWeatherData().orElseThrow().temperatureCelsius())
                .average();
        return avg.isPresent() ? avg.getAsDouble() : null;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Optional<LocalDateTime> takenAt(Picture picture) {
        return picture.getExifData().map(ExifData::takenAt);
    }

    private static String cameraLabel(ExifData exif) {
        String make = exif.cameraMake() == null ? "" : exif.cameraMake().trim();
        String model = exif.cameraModel() == null ? "" : exif.cameraModel().trim();
        return (make + " " + model).trim();
    }

    private static Statistics.PictureStat stat(Picture picture, double value, Map<String, String> galleryNames) {
        return new Statistics.PictureStat(
                picture.getId(), picture.getGalleryId(), galleryName(picture, galleryNames),
                picture.getThumbnailUrl().orElse(null), value);
    }

    private static String galleryName(Picture picture, Map<String, String> galleryNames) {
        return galleryNames.getOrDefault(picture.getGalleryId(), picture.getGalleryId());
    }
}
