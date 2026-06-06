package com.antodippo.mappics.domain;

import java.time.LocalDateTime;

// takenAt uses LocalDateTime intentionally: EXIF stores local time with no timezone offset.
public record ExifData(
        String cameraMake,
        String cameraModel,
        LocalDateTime takenAt,
        String focalLength,
        String aperture,
        Integer iso
) {}
