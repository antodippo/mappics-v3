package com.antodippo.mappics.domain;

public interface ImageResizer {

    byte[] resize(byte[] imageData, int maxDimension);
}
