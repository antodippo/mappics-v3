package com.antodippo.mappics.application;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalleryProcessorWithTracerTest {

    private SimpleTracer               tracer;
    private GalleryProcessorWithTracer processor;

    @BeforeEach
    void setUp() {
        tracer = new SimpleTracer();
    }

    @Test
    void emitsGallerySpanWithGalleryIdAndPictureTotal() {
        // Delegate processes three pictures (each bumps the job counter once).
        processor = new GalleryProcessorWithTracer((galleryId, job) -> {
            job.pictureCompleted();
            job.pictureCompleted();
            job.pictureCompleted();
        }, tracer);

        processor.processGallery("iceland", new ImportJob("job-1"));

        SimpleSpan span = tracer.onlySpan();
        assertEquals("import.gallery", span.getName());
        assertEquals("iceland", span.getTags().get("galleryId"));
        assertEquals("3", span.getTags().get("pictures.total"));
    }
}
