package com.antodippo.mappics.infrastructure.image;

import com.antodippo.mappics.domain.ImageResizer;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ResizePictureWithScrimage implements ImageResizer {

    private static final JpegWriter WRITER = new JpegWriter().withCompression(85).withProgressive(false);

    @Override
    public byte[] resize(byte[] imageData, int maxDimension) {
        try {
            return ImmutableImage.loader()
                    .fromBytes(imageData)
                    .bound(maxDimension, maxDimension)
                    .bytes(WRITER);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not resize image: " + e.getMessage(), e);
        }
    }
}
