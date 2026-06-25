package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;

import java.time.LocalDateTime;

public interface PictureEnricher {

    Picture extractExif(Picture picture, byte[] imageData);

    Picture resizeImages(Picture picture, byte[] imageData);

    Picture fetchLocation(Picture picture, GpsCoordinates gps);

    Picture fetchWeather(Picture picture, GpsCoordinates gps, LocalDateTime takenAt);
}
