package com.antodippo.mappics.domain;

import java.util.Optional;

public record ExifExtractionResult(GpsCoordinates gpsCoordinates, ExifData exifData) {

    public Optional<GpsCoordinates> optGpsCoordinates() {
        return Optional.ofNullable(gpsCoordinates);
    }

    public Optional<ExifData> optExifData() {
        return Optional.ofNullable(exifData);
    }
}
