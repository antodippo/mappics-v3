package com.antodippo.mappics.infrastructure.exif;

import com.antodippo.mappics.domain.ExifExtractionResult;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExtractExifDataWithMetadataExtractorTest {

    private final ExtractExifDataWithMetadataExtractor extractor = new ExtractExifDataWithMetadataExtractor();

    // ── Azores fixture: Sony F5121, 2017-08-24 12:07:14, GPS 37.84°N 25.79°W ──

    @Test
    void extractsGpsCoordinatesFromAzoresJpeg() throws IOException {
        var result = extract("Azores/DSC_0892.JPG");

        assertNotNull(result.gpsCoordinates(), "GPS should be present");
        assertEquals(37.839183, result.gpsCoordinates().latitude(), 0.001);
        assertEquals(-25.793508, result.gpsCoordinates().longitude(), 0.001);
    }

    @Test
    void extractsCameraInfoFromAzoresJpeg() throws IOException {
        var result = extract("Azores/DSC_0892.JPG");

        assertEquals("Sony", result.exifData().cameraMake());
        assertEquals("F5121", result.exifData().cameraModel());
    }

    @Test
    void extractsDateTimeOriginalFromAzoresJpeg() throws IOException {
        var result = extract("Azores/DSC_0892.JPG");

        assertEquals(LocalDateTime.of(2017, 8, 24, 12, 7, 14), result.exifData().takenAt());
    }

    @Test
    void extractsLensDataFromAzoresJpeg() throws IOException {
        var result = extract("Azores/DSC_0892.JPG");

        assertEquals("4.2mm", result.exifData().focalLength());
        assertEquals("f/2.0", result.exifData().aperture());
        assertEquals(40, result.exifData().iso());
    }

    // ── Iceland fixture: Sony F5121, 2017-06-09, GPS 64.26°N 21.12°W ──

    @Test
    void extractsGpsCoordinatesFromIcelandJpeg() throws IOException {
        var result = extract("Iceland/DSC_0114.JPG");

        assertNotNull(result.gpsCoordinates());
        assertEquals(64.257839, result.gpsCoordinates().latitude(), 0.001);
        assertEquals(-21.121167, result.gpsCoordinates().longitude(), 0.001);
    }

    @Test
    void extractsDateTimeOriginalFromIcelandJpeg() throws IOException {
        var result = extract("Iceland/DSC_0114.JPG");

        assertEquals(LocalDateTime.of(2017, 6, 9, 18, 43, 32), result.exifData().takenAt());
    }

    // ── Edge cases ──

    @Test
    void returnsNullGpsForJpegWithoutGpsData() throws IOException {
        byte[] jpeg = minimalJpeg();

        var result = extractor.extract(jpeg);

        assertNull(result.gpsCoordinates());
    }

    @Test
    void returnsNonNullExifDataForJpegWithoutExif() throws IOException {
        byte[] jpeg = minimalJpeg();

        var result = extractor.extract(jpeg);

        // ExifData is always non-null to avoid repeated extraction attempts
        assertNotNull(result.exifData());
        assertNull(result.exifData().cameraMake());
        assertNull(result.exifData().takenAt());
    }

    @Test
    void handlesInvalidBytesGracefully() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};

        var result = extractor.extract(garbage);

        assertNotNull(result);
        assertNull(result.gpsCoordinates());
        assertNotNull(result.exifData());
    }

    // ── Helpers ──

    private ExifExtractionResult extract(String fixturePath) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("galleries/" + fixturePath)) {
            assertNotNull(in, "Fixture not found: " + fixturePath);
            return extractor.extract(in.readAllBytes());
        }
    }

    private static byte[] minimalJpeg() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        }
    }
}
