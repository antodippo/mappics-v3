package com.antodippo.mappics.infrastructure.image;

import com.antodippo.mappics.domain.ImageResizer;
import com.antodippo.mappics.domain.ResizedImages;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ResizePictureWithScrimage implements ImageResizer {

    private static final JpegWriter WRITER = new JpegWriter().withCompression(85).withProgressive(false);

    @Override
    public ResizedImages resizeToBounds(byte[] imageData, int thumbnailMaxDim, int fullSizeMaxDim) {
        try {
            // ImmutableImage is immutable, so bound() returns a new image and the
            // decoded source is reused for both outputs (decoded once, not twice).
            ImmutableImage source = ImmutableImage.loader().fromBytes(imageData);
            byte[] thumbnail = source.bound(thumbnailMaxDim, thumbnailMaxDim).bytes(WRITER);
            byte[] fullSize  = source.bound(fullSizeMaxDim, fullSizeMaxDim).bytes(WRITER);
            return new ResizedImages(thumbnail, fullSize);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not resize image: " + e.getMessage(), e);
        }
    }
}
