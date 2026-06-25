package com.antodippo.mappics.application;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Thin async entry point: the @Async boundary must sit OUTSIDE the tracing span so the
// `import` span (created in TracingGalleryImporter) lives on the async worker thread.
@Component
public class ProcessUploadedGalleries {

    private final GalleryImporter importer;

    public ProcessUploadedGalleries(GalleryImporter importer) {
        this.importer = importer;
    }

    // Called through the Spring proxy → runs on the async thread pool.
    @Async
    public void processAsync(ImportJob job) {
        importer.importGalleries(job);
    }
}
