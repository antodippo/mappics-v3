package com.antodippo.mappics.application;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Thin async entry point for the local-dev import endpoint (ImportController):
// the @Async boundary must sit OUTSIDE the tracing span so the `import` span
// (created in GalleryImporterWithTracer) lives on the async worker thread.
// Prod runs the import synchronously to completion via ImportJobRunner instead.
@Component
@Profile("local")
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
