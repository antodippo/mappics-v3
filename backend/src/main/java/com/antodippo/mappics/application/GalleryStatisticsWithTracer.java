package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.Statistics;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class GalleryStatisticsWithTracer implements GalleryStatistics {

    private final GalleryStatistics delegate;
    private final Tracer           tracer;

    public GalleryStatisticsWithTracer(@Qualifier("galleryStatisticsCore") GalleryStatistics delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer   = tracer;
    }

    @Override
    public Statistics compute() {
        Span span = tracer.nextSpan().name("statistics.compute").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            Statistics statistics = delegate.compute();
            span.tag("pictures.total", String.valueOf(statistics.totalPictures()));
            return statistics;
        } finally {
            span.end();
        }
    }
}
