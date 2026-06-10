package com.antodippo.mappics.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportJob {

    private final String id;
    private final Instant startedAt;
    private volatile ImportJobStatus status = ImportJobStatus.PENDING;
    private volatile Instant completedAt;
    private volatile int totalGalleries;
    private final AtomicInteger processedGalleries = new AtomicInteger(0);
    private final AtomicInteger totalPictures = new AtomicInteger(0);
    private final AtomicInteger processedPictures = new AtomicInteger(0);
    private volatile String currentGallery;
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());

    public ImportJob(String id) {
        this.id = id;
        this.startedAt = Instant.now();
    }

    public void start() {
        this.status = ImportJobStatus.IN_PROGRESS;
    }

    public void setTotalGalleries(int total) {
        this.totalGalleries = total;
    }

    public void startGallery(String galleryId, int pictureCount) {
        this.currentGallery = galleryId;
        this.totalPictures.addAndGet(pictureCount);
    }

    public void pictureCompleted() {
        processedPictures.incrementAndGet();
    }

    public void galleryCompleted() {
        processedGalleries.incrementAndGet();
    }

    public void addError(String message) {
        errors.add(message);
    }

    public void complete() {
        this.status = ImportJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.currentGallery = null;
    }

    public void fail(String reason) {
        this.status = ImportJobStatus.FAILED;
        this.completedAt = Instant.now();
        errors.add(reason);
    }

    public boolean isRunning() {
        return status == ImportJobStatus.PENDING || status == ImportJobStatus.IN_PROGRESS;
    }

    public String getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public ImportJobStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public int getTotalGalleries() { return totalGalleries; }
    public int getProcessedGalleries() { return processedGalleries.get(); }
    public int getTotalPictures() { return totalPictures.get(); }
    public int getProcessedPictures() { return processedPictures.get(); }
    public String getCurrentGallery() { return currentGallery; }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
}
