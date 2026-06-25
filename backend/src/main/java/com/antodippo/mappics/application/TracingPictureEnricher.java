package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Primary
public class TracingPictureEnricher implements PictureEnricher {

    private final PictureEnricher delegate;
    private final Tracer          tracer;

    public TracingPictureEnricher(@Qualifier("pictureEnricherCore") PictureEnricher delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer   = tracer;
    }

    @Override
    public Picture extractExif(Picture picture, byte[] imageData) {
        Span span = tracer.nextSpan().name("import.picture.exif").tag("pictureId", picture.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return delegate.extractExif(picture, imageData);
        } finally {
            span.end();
        }
    }

    @Override
    public Picture resizeImages(Picture picture, byte[] imageData) {
        Span span = tracer.nextSpan().name("import.picture.resize").tag("pictureId", picture.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return delegate.resizeImages(picture, imageData);
        } finally {
            span.end();
        }
    }

    @Override
    public Picture fetchLocation(Picture picture, GpsCoordinates gps) {
        Span span = tracer.nextSpan().name("import.picture.location").tag("pictureId", picture.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return delegate.fetchLocation(picture, gps);
        } finally {
            span.end();
        }
    }

    @Override
    public Picture fetchWeather(Picture picture, GpsCoordinates gps, LocalDateTime takenAt) {
        Span span = tracer.nextSpan().name("import.picture.weather").tag("pictureId", picture.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return delegate.fetchWeather(picture, gps, takenAt);
        } finally {
            span.end();
        }
    }
}
