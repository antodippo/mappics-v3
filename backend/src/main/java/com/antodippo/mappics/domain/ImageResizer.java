package com.antodippo.mappics.domain;

public interface ImageResizer {

    // Decodes the source image once and produces both bounded outputs, so a large
    // original is not decoded twice.
    ResizedImages resizeToBounds(byte[] imageData, int thumbnailMaxDim, int fullSizeMaxDim);
}
