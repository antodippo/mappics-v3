package com.antodippo.mappics.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoDistanceTest {

    @Test
    void zeroForIdenticalPoints() {
        GpsCoordinates london = new GpsCoordinates(51.5074, -0.1278, null);
        assertEquals(0.0, GeoDistance.kmBetween(london, london), 0.0001);
    }

    @Test
    void knownDistanceLondonToParis() {
        GpsCoordinates london = new GpsCoordinates(51.5074, -0.1278, null);
        GpsCoordinates paris = new GpsCoordinates(48.8566, 2.3522, null);
        // ~343 km great-circle; allow a few km of tolerance.
        assertEquals(343.0, GeoDistance.kmBetween(london, paris), 5.0);
    }

    @Test
    void ignoresAltitude() {
        GpsCoordinates lowSeaLevel = new GpsCoordinates(46.0, 7.0, 0.0);
        GpsCoordinates highAltitude = new GpsCoordinates(46.0, 7.0, 4000.0);
        assertEquals(0.0, GeoDistance.kmBetween(lowSeaLevel, highAltitude), 0.0001);
    }
}
