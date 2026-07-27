package com.antodippo.mappics.application;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void recordsErrorOnSpanAndRethrowsWhenDelegateThrows() {
        RuntimeException failure = new RuntimeException("disk gone");
        importer = new GalleryImporterWithTracer(job -> { throw failure; }, tracer);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> importer.importGalleries(new ImportJob("job-2")));

        assertEquals(failure, thrown);
        SimpleSpan span = tracer.onlySpan();
        assertEquals("import", span.getName());
        assertEquals(failure, span.getError());
    }
}
