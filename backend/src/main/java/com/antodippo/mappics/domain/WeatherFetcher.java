package com.antodippo.mappics.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WeatherFetcher {

    Optional<WeatherData> fetch(GpsCoordinates coordinates, LocalDateTime takenAt);
}
