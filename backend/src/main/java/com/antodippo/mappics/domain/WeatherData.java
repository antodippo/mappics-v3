package com.antodippo.mappics.domain;

public record WeatherData(double temperatureCelsius, int humidity, int weatherCode, String description) {}
