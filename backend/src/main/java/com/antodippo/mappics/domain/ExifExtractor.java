package com.antodippo.mappics.domain;

public interface ExifExtractor {

    ExifExtractionResult extract(byte[] imageData);
}
