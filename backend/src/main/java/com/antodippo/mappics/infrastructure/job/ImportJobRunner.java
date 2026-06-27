package com.antodippo.mappics.infrastructure.job;

import com.antodippo.mappics.application.GalleryImporter;
import com.antodippo.mappics.application.ImportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Entrypoint for the `mappics-import-job` Cloud Run Job. Activated by setting
// `mappics.run-mode=import` (env MAPPICS_RUN_MODE=import). Runs the import to
// completion on the always-allocated Job CPU, then exits — unlike the web
// service, whose CPU is throttled while idle (the cause of the slow import).
//
// The Job also sets SPRING_MAIN_WEB_APPLICATION_TYPE=none so no server starts
// and the JVM terminates when this runner returns.
@Component
@ConditionalOnProperty(name = "mappics.run-mode", havingValue = "import")
public class ImportJobRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportJobRunner.class);

    private final GalleryImporter importer;
    private final ConfigurableApplicationContext context;

    public ImportJobRunner(GalleryImporter importer, ConfigurableApplicationContext context) {
        this.importer = importer;
        this.context  = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        ImportJob job = new ImportJob(UUID.randomUUID().toString());
        int exitCode;
        try {
            log.info("Import job '{}' starting", job.getId());
            importer.importGalleries(job);
            exitCode = 0;
        } catch (Exception e) {
            log.error("Import job '{}' failed", job.getId(), e);
            exitCode = 1;
        }
        // SpringApplication.exit closes the context cleanly; System.exit propagates
        // the status to Cloud Run so a failed import surfaces as a failed execution.
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
