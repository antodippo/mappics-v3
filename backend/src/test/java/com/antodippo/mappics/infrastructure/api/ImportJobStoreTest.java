package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImportJobStoreTest {

    private final ImportJobStore store = new ImportJobStore();

    @Test
    void createdJobCanBeFoundById() {
        ImportJob job = store.create();
        assertSame(job, store.findById(job.getId()).orElseThrow());
    }

    @Test
    void createdJobsGetDistinctIds() {
        assertNotEquals(store.create().getId(), store.create().getId());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(store.findById("no-such-job").isEmpty());
    }

    @Test
    void hasRunningJobIsFalseWhenStoreIsEmpty() {
        assertFalse(store.hasRunningJob());
    }

    // hasRunningJob is what makes POST /import return 409 instead of starting a
    // second concurrent import (which would break the OSM 1 req/s rate limit).
    @Test
    void hasRunningJobIsTrueWhilePendingOrInProgress() {
        ImportJob job = store.create();
        assertTrue(store.hasRunningJob(), "just-created (PENDING) job counts as running");

        job.start();
        assertTrue(store.hasRunningJob(), "IN_PROGRESS job counts as running");
    }

    @Test
    void hasRunningJobIsFalseOnceAllJobsFinish() {
        store.create().complete();
        store.create().fail("boom");

        assertFalse(store.hasRunningJob());
    }
}
