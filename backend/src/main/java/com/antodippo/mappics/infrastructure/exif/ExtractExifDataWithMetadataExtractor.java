package com.antodippo.mappics.infrastructure.exif;

import com.antodippo.mappics.domain.*;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ExtractExifDataWithMetadataExtractor implements ExifExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExtractExifDataWithMetadataExtractor.class);
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    @Override
    public ExifExtractionResult extract(byte[] imageData) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageData));
            return new ExifExtractionResult(extractGps(metadata), extractExifData(metadata));
        } catch (Exception e) {
            log.warn("Failed to read image metadata: {}", e.getMessage());
            return new ExifExtractionResult(null, new ExifData(null, null, null, null, null, null));
        }
    }

    private GpsCoordinates extractGps(Metadata metadata) {
        GpsDirectory dir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (dir == null) return null;
        GeoLocation location = dir.getGeoLocation();
        if (location == null || location.isZero()) return null;
        return new GpsCoordinates(location.getLatitude(), location.getLongitude(), extractAltitude(dir));
    }

    private Double extractAltitude(GpsDirectory dir) {
        Rational r = dir.getRational(GpsDirectory.TAG_ALTITUDE);
        if (r == null) return null;
        double alt = r.doubleValue();
        try {
            int ref = dir.getInt(GpsDirectory.TAG_ALTITUDE_REF);
            return ref == 1 ? -alt : alt;
        } catch (Exception e) {
            return alt;
        }
    }

    private ExifData extractExifData(Metadata metadata) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        String cameraMake = ifd0 != null ? ifd0.getString(ExifIFD0Directory.TAG_MAKE) : null;
        String cameraModel = ifd0 != null ? ifd0.getString(ExifIFD0Directory.TAG_MODEL) : null;
        LocalDateTime takenAt = parseTakenAt(subIFD);
        String focalLength = formatFocalLength(subIFD);
        String aperture = formatAperture(subIFD);
        Integer iso = subIFD != null ? subIFD.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : null;

        return new ExifData(cameraMake, cameraModel, takenAt, focalLength, aperture, iso);
    }

    private LocalDateTime parseTakenAt(ExifSubIFDDirectory dir) {
        if (dir == null) return null;
        String raw = dir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        if (raw == null) return null;
        try {
            return LocalDateTime.parse(raw.trim(), EXIF_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Could not parse EXIF datetime '{}': {}", raw, e.getMessage());
            return null;
        }
    }

    private String formatFocalLength(ExifSubIFDDirectory dir) {
        if (dir == null) return null;
        Rational r = dir.getRational(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
        if (r == null) return null;
        double mm = r.doubleValue();
        return mm == Math.floor(mm) ? (int) mm + "mm" : String.format(Locale.ROOT, "%.1fmm", mm);
    }

    private String formatAperture(ExifSubIFDDirectory dir) {
        if (dir == null) return null;
        Rational r = dir.getRational(ExifSubIFDDirectory.TAG_FNUMBER);
        if (r == null) return null;
        return String.format(Locale.ROOT, "f/%.1f", r.doubleValue());
    }
}
