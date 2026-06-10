package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImportJobStore {

    private final ConcurrentHashMap<String, ImportJob> jobs = new ConcurrentHashMap<>();

    public ImportJob create() {
        ImportJob job = new ImportJob(UUID.randomUUID().toString());
        jobs.put(job.getId(), job);
        return job;
    }

    public Optional<ImportJob> findById(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public boolean hasRunningJob() {
        return jobs.values().stream().anyMatch(ImportJob::isRunning);
    }
}
