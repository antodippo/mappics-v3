package com.antodippo.mappics.infrastructure.storage;

import com.antodippo.mappics.domain.GalleryFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

abstract class GalleryFileStorageAbstractTest {

    protected GalleryFileStorage storage;

    protected abstract GalleryFileStorage createAndSeedStorage(Map<String, Map<String, byte[]>> sourcePictures);

    @BeforeEach
    void setUp() throws IOException {
        byte[] jpeg = minimalJpeg();
        storage = createAndSeedStorage(Map.of(
                "iceland", Map.of(
                        "DSC_0001.JPG", jpeg,
                        "DSC_0002.JPG", jpeg,
                        "notes.txt", new byte[]{0x00}
                ),
                "azores", Map.of(
                        "DSC_0892.jpg", jpeg
                )
        ));
    }

    @Test
    void listGalleryIds_returnsAllGalleries() {
        List<String> ids = storage.listGalleryIds();

        assertEquals(2, ids.size());
        assertTrue(ids.contains("iceland"));
        assertTrue(ids.contains("azores"));
    }

    @Test
    void listPictureFilenames_returnsOnlyJpegFiles() {
        List<String> filenames = storage.listPictureFilenames("iceland");

        assertEquals(2, filenames.size());
        assertTrue(filenames.contains("DSC_0001.JPG"));
        assertTrue(filenames.contains("DSC_0002.JPG"));
        assertFalse(filenames.contains("notes.txt"));
    }

    @Test
    void listPictureFilenames_returnsEmptyForUnknownGallery() {
        assertTrue(storage.listPictureFilenames("unknown").isEmpty());
    }

    @Test
    void readOriginalPicture_returnsExpectedBytes() throws IOException {
        byte[] expected = minimalJpeg();

        byte[] actual = storage.readOriginalPicture("iceland", "DSC_0001.JPG");

        assertArrayEquals(expected, actual);
    }

    @Test
    void thumbnailDoesNotExistBeforeWrite() {
        assertFalse(storage.thumbnailExists("iceland", "DSC_0001.JPG"));
    }

    @Test
    void fullSizeDoesNotExistBeforeWrite() {
        assertFalse(storage.fullSizeExists("iceland", "DSC_0001.JPG"));
    }

    @Test
    void writeThumbnail_makesItDetectableViaExists() {
        storage.writeThumbnail("iceland", "DSC_0001.JPG", new byte[]{0x01});

        assertTrue(storage.thumbnailExists("iceland", "DSC_0001.JPG"));
        assertFalse(storage.fullSizeExists("iceland", "DSC_0001.JPG"));
    }

    @Test
    void writeFullSize_makesItDetectableViaExists() {
        storage.writeFullSize("iceland", "DSC_0001.JPG", new byte[]{0x01});

        assertTrue(storage.fullSizeExists("iceland", "DSC_0001.JPG"));
        assertFalse(storage.thumbnailExists("iceland", "DSC_0001.JPG"));
    }

    @Test
    void thumbnailUrl_containsGalleryAndBasename() {
        String url = storage.getThumbnailUrl("iceland", "DSC_0001.JPG");

        assertTrue(url.contains("iceland"), "URL should contain gallery id");
        assertTrue(url.contains("DSC_0001"), "URL should contain base filename");
        assertTrue(url.contains("thumb"), "URL should indicate thumbnail");
    }

    @Test
    void fullSizeUrl_containsGalleryAndBasename() {
        String url = storage.getFullSizeUrl("iceland", "DSC_0001.JPG");

        assertTrue(url.contains("iceland"), "URL should contain gallery id");
        assertTrue(url.contains("DSC_0001"), "URL should contain base filename");
        assertTrue(url.contains("full"), "URL should indicate full size");
    }

    private static byte[] minimalJpeg() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        }
    }
}
