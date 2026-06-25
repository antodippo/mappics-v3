package com.antodippo.mappics.application;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalleryImporterWithTracerTest {

    private SimpleTracer             tracer;
    private GalleryImporterWithTracer importer;

    @BeforeEach
    void setUp() {
        tracer = new SimpleTracer();
    }

    @Test
    void emitsImportSpanWithJobIdAndSummaryTags() {
        importer = new GalleryImporterWithTracer(job -> {
            job.setTotalGalleries(2);
            job.galleryCompleted();
            job.galleryCompleted();
            job.startGallery("g", 3);
            job.pictureCompleted();
            job.pictureCompleted();
            job.pictureCompleted();
            job.addError("boom");
        }, tracer);

        importer.importGalleries(new ImportJob("job-1"));

        SimpleSpan span = tracer.onlySpan();
        assertEquals("import", span.getName());
        assertEquals("job-1", span.getTags().get("jobId"));
        assertEquals("2", span.getTags().get("galleries.processed"));
        assertEquals("3", span.getTags().get("pictures.processed"));
        assertEquals("1", span.getTags().get("errors"));
    }

    @Test
    void recordsErrorOnSpanWhenDelegateThrows() {
        RuntimeException failure = new RuntimeException("disk gone");
        importer = new GalleryImporterWithTracer(job -> { throw failure; }, tracer);

        // The decorator records the error on the span and swallows it (fire-and-forget import).
        importer.importGalleries(new ImportJob("job-2"));

        SimpleSpan span = tracer.onlySpan();
        assertEquals("import", span.getName());
        assertEquals(failure, span.getError());
    }
}
