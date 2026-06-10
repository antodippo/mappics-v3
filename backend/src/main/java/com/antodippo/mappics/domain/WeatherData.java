package com.antodippo.mappics.domain;

public record WeatherData(double temperatureCelsius, int humidity, double windSpeedKmh, int weatherCode, String description) {}
