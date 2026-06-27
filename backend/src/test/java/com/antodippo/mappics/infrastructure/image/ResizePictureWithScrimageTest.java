package com.antodippo.mappics.infrastructure.image;

import com.antodippo.mappics.domain.ResizedImages;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResizePictureWithScrimageTest {

    // Fixture: 5984×3366 Sony Xperia JPEG
    private static byte[] AZORES_JPEG;

    private final ResizePictureWithScrimage resizer = new ResizePictureWithScrimage();

    @BeforeAll
    static void loadFixture() throws IOException {
        try (InputStream in = ResizePictureWithScrimageTest.class
                .getClassLoader().getResourceAsStream("galleries/Azores/DSC_0892.JPG")) {
            assertNotNull(in, "Azores fixture not found");
            AZORES_JPEG = in.readAllBytes();
        }
    }

    @Test
    void thumbnailLongerSideIsAtMost400Pixels() throws IOException {
        byte[] output = resizer.resizeToBounds(AZORES_JPEG, 400, 1920).thumbnail();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        int longerSide = Math.max(img.getWidth(), img.getHeight());
        assertTrue(longerSide <= 400, "Longer side was " + longerSide);
    }

    @Test
    void thumbnailAspectRatioIsPreserved() throws IOException {
        byte[] output = resizer.resizeToBounds(AZORES_JPEG, 400, 1920).thumbnail();

        // Original: 5984×3366 = 1.777 ratio (16:9). Thumbnail: 400×225 = same.
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        double ratio = (double) img.getWidth() / img.getHeight();
        assertEquals(5984.0 / 3366.0, ratio, 0.01);
    }

    @Test
    void fullSizeLongerSideIsAtMost1920Pixels() throws IOException {
        byte[] output = resizer.resizeToBounds(AZORES_JPEG, 400, 1920).fullSize();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        int longerSide = Math.max(img.getWidth(), img.getHeight());
        assertTrue(longerSide <= 1920, "Longer side was " + longerSide);
    }

    @Test
    void doesNotUpscaleImageSmallerThanMaxDimension() throws IOException {
        byte[] smallJpeg = minimalJpeg(10, 10);

        byte[] output = resizer.resizeToBounds(smallJpeg, 400, 1920).thumbnail();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        assertTrue(img.getWidth() <= 10 && img.getHeight() <= 10,
                "Image was upscaled to " + img.getWidth() + "×" + img.getHeight());
    }

    @Test
    void producesBothThumbnailAndFullSizeFromOneCall() throws IOException {
        ResizedImages output = resizer.resizeToBounds(AZORES_JPEG, 400, 1920);

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(output.thumbnail()));
        BufferedImage full = ImageIO.read(new ByteArrayInputStream(output.fullSize()));
        assertNotNull(thumb, "Thumbnail bytes could not be decoded as an image");
        assertNotNull(full, "Full-size bytes could not be decoded as an image");
        assertTrue(Math.max(thumb.getWidth(), thumb.getHeight()) <= 400);
        assertTrue(Math.max(full.getWidth(), full.getHeight()) <= 1920);
    }

    private static byte[] minimalJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        }
    }
}
