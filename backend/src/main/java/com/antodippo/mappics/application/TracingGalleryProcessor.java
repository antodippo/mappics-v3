package com.antodippo.mappics.application;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TracingGalleryProcessor implements GalleryProcessor {

    private final GalleryProcessor delegate;
    private final Tracer           tracer;

    public TracingGalleryProcessor(@Qualifier("galleryProcessorCore") GalleryProcessor delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer   = tracer;
    }

    @Override
    public void processGallery(String galleryId, ImportJob job) {
        MDC.put("galleryId", galleryId);
        Span span = tracer.nextSpan().name("import.gallery").tag("galleryId", galleryId).start();
        int picturesBefore = job.getProcessedPictures();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            delegate.processGallery(galleryId, job);
            // Every picture (enriched or skipped) increments the job counter exactly once.
            span.tag("pictures.total", String.valueOf(job.getProcessedPictures() - picturesBefore));
        } finally {
            span.end();
            MDC.remove("galleryId");
        }
    }
}
