package com.antodippo.mappics.application;

import com.antodippo.mappics.domain.GalleryFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GalleryImporterCore implements GalleryImporter {

    private static final Logger log = LoggerFactory.getLogger(GalleryImporterCore.class);

    private final GalleryFileStorage fileStorage;
    private final GalleryProcessor   galleryProcessor;

    public GalleryImporterCore(GalleryFileStorage fileStorage, GalleryProcessor galleryProcessor) {
        this.fileStorage      = fileStorage;
        this.galleryProcessor = galleryProcessor;
    }

    @Override
    public void importGalleries(ImportJob job) {
        long startMs = System.currentTimeMillis();
        try {
            job.start();
            List<String> galleryIds = fileStorage.listGalleryIds();
            job.setTotalGalleries(galleryIds.size());
            log.info("Import started: {} galleries to process", galleryIds.size());

            for (String galleryId : galleryIds) {
                galleryProcessor.processGallery(galleryId, job);
                job.galleryCompleted();
            }

            job.complete();
            log.info("Import completed in {}ms: {} galleries, {} pictures, {} errors",
                    System.currentTimeMillis() - startMs,
                    job.getProcessedGalleries(), job.getProcessedPictures(), job.getErrors().size());
        } catch (Exception e) {
            log.error("Import failed unexpectedly", e);
            job.fail("Unexpected error: " + e.getMessage());
            throw e; // let the tracing decorator record the failure on the span
        }
    }
}
