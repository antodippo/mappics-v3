package com.antodippo.mappics.domain;

import java.util.Optional;

public interface LocationDescriptionFetcher {

    Optional<LocationDescription> fetch(GpsCoordinates coordinates);
}
