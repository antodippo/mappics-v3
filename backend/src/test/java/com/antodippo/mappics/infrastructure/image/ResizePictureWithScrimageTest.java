package com.antodippo.mappics.infrastructure.image;

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
        byte[] output = resizer.resize(AZORES_JPEG, 400);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        int longerSide = Math.max(img.getWidth(), img.getHeight());
        assertTrue(longerSide <= 400, "Longer side was " + longerSide);
    }

    @Test
    void thumbnailAspectRatioIsPreserved() throws IOException {
        byte[] output = resizer.resize(AZORES_JPEG, 400);

        // Original: 5984×3366 = 1.777 ratio (16:9). Thumbnail: 400×225 = same.
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        double ratio = (double) img.getWidth() / img.getHeight();
        assertEquals(5984.0 / 3366.0, ratio, 0.01);
    }

    @Test
    void fullSizeLongerSideIsAtMost1920Pixels() throws IOException {
        byte[] output = resizer.resize(AZORES_JPEG, 1920);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        int longerSide = Math.max(img.getWidth(), img.getHeight());
        assertTrue(longerSide <= 1920, "Longer side was " + longerSide);
    }

    @Test
    void doesNotUpscaleImageSmallerThanMaxDimension() throws IOException {
        byte[] smallJpeg = minimalJpeg(10, 10);

        byte[] output = resizer.resize(smallJpeg, 400);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        assertTrue(img.getWidth() <= 10 && img.getHeight() <= 10,
                "Image was upscaled to " + img.getWidth() + "×" + img.getHeight());
    }

    @Test
    void outputIsAValidJpeg() throws IOException {
        byte[] output = resizer.resize(AZORES_JPEG, 400);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(output));
        assertNotNull(img, "Output bytes could not be decoded as an image");
    }

    private static byte[] minimalJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        }
    }
}
