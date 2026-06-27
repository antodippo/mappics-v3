package com.antodippo.mappics.infrastructure.image;

import com.antodippo.mappics.domain.ImageResizer;
import com.antodippo.mappics.domain.ResizedImages;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageResizerTestDouble implements ImageResizer {

    private static final byte[] FIXED_JPEG = createFixedJpeg();

    @Override
    public ResizedImages resizeToBounds(byte[] imageData, int thumbnailMaxDim, int fullSizeMaxDim) {
        return new ResizedImages(FIXED_JPEG, FIXED_JPEG);
    }

    private static byte[] createFixedJpeg() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create fixed test JPEG", e);
        }
    }
}
