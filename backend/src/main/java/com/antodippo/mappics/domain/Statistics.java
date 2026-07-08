package com.antodippo.mappics.domain;

import java.time.LocalDateTime;

// Aggregate statistics over every picture. Extremum fields resolve to the picture
// that holds the extreme value (with a thumbnail + gallery link); any field is null
// when no picture qualifies (e.g. an empty repository or missing weather/exif data).
public record Statistics(
        int totalPictures,
        int galleryCount,
        double totalTraveledKm,
        PictureStat northernmost,
        PictureStat southernmost,
        PictureStat easternmost,
        PictureStat westernmost,
        PictureStat highestAltitude,
        PictureStat coldest,
        PictureStat hottest,
        DatedPictureStat oldest,
        DatedPictureStat newest,
        String mostUsedCamera,
        Integer dateSpanDays,
        BiggestGallery biggestGallery,
        Double averageTemperatureCelsius
) {

    public record PictureStat(String pictureId, String galleryId, String thumbnailUrl, double value) {}

    public record DatedPictureStat(String pictureId, String galleryId, String thumbnailUrl, LocalDateTime takenAt) {}

    public record BiggestGallery(String galleryId, String name, int pictureCount) {}
}
