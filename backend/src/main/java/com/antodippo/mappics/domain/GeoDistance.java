package com.antodippo.mappics.domain;

// Great-circle (haversine) distance between two GPS coordinates. Altitude is ignored.
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoDistance() {}

    public static double kmBetween(GpsCoordinates a, GpsCoordinates b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
