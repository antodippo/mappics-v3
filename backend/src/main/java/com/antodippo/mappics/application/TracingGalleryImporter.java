package com.antodippo.mappics.application;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TracingGalleryImporter implements GalleryImporter {

    private final GalleryImporter delegate;
    private final Tracer          tracer;

    public TracingGalleryImporter(@Qualifier("galleryImporterCore") GalleryImporter delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer   = tracer;
    }

    @Override
    public void importGalleries(ImportJob job) {
        MDC.put("importJobId", job.getId());
        Span span = tracer.nextSpan().name("import").tag("jobId", job.getId()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            delegate.importGalleries(job);
            span.tag("galleries.processed", String.valueOf(job.getProcessedGalleries()));
            span.tag("pictures.processed", String.valueOf(job.getProcessedPictures()));
            span.tag("errors", String.valueOf(job.getErrors().size()));
        } catch (Exception e) {
            span.error(e);
        } finally {
            span.end();
            MDC.clear();
        }
    }
}
