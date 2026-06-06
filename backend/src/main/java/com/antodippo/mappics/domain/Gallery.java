package com.antodippo.mappics.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Gallery {

    private final String id;
    private final String name;
    private final List<String> pictureIds;
    private final GpsCoordinates averageGpsCoordinates;

    public Gallery(String id, String name, List<String> pictureIds, GpsCoordinates averageGpsCoordinates) {
        this.id = id;
        this.name = name;
        this.pictureIds = List.copyOf(pictureIds);
        this.averageGpsCoordinates = averageGpsCoordinates;
    }

    public static Gallery create(String id) {
        return new Gallery(id, formatName(id), List.of(), null);
    }

    public Gallery withPictureIds(List<String> pictureIds) {
        return new Gallery(id, name, pictureIds, averageGpsCoordinates);
    }

    public Gallery withAverageGpsCoordinates(GpsCoordinates averageGpsCoordinates) {
        return new Gallery(id, name, pictureIds, averageGpsCoordinates);
    }

    public static Optional<GpsCoordinates> calculateAverageGps(List<GpsCoordinates> coordinates) {
        if (coordinates.isEmpty()) {
            return Optional.empty();
        }
        double avgLat = coordinates.stream().mapToDouble(GpsCoordinates::latitude).average().orElseThrow();
        double avgLon = coordinates.stream().mapToDouble(GpsCoordinates::longitude).average().orElseThrow();
        return Optional.of(new GpsCoordinates(avgLat, avgLon));
    }

    private static String formatName(String id) {
        return Arrays.stream(id.split("[-_]"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getPictureIds() { return pictureIds; }
    public Optional<GpsCoordinates> getAverageGpsCoordinates() { return Optional.ofNullable(averageGpsCoordinates); }
}
