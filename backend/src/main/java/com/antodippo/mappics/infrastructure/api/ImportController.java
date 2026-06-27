package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import com.antodippo.mappics.application.ProcessUploadedGalleries;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Local-dev only: triggers an in-process import and exposes live polling.
// In prod the import runs as the `mappics-import-job` Cloud Run Job (the web
// service throttles CPU while idle), so this controller is not registered and
// POST/GET /import return 404.
@RestController
@RequestMapping("/import")
@Profile("local")
public class ImportController {

    private final ProcessUploadedGalleries processUploadedGalleries;
    private final ImportJobStore importJobStore;
    private final String importSecret;

    public ImportController(ProcessUploadedGalleries processUploadedGalleries,
                            ImportJobStore importJobStore,
                            @Value("${mappics.import.secret:}") String importSecret) {
        this.processUploadedGalleries = processUploadedGalleries;
        this.importJobStore = importJobStore;
        this.importSecret = importSecret;
    }

    @PostMapping
    public ResponseEntity<?> startImport(
            @RequestHeader(value = "X-Import-Secret", required = false) String providedSecret) {
        if (!importSecret.isBlank() && !importSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized"));
        }
        if (importJobStore.hasRunningJob()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("An import is already in progress"));
        }
        ImportJob job = importJobStore.create();
        processUploadedGalleries.processAsync(job);
        return ResponseEntity.accepted()
                .body(new ImportStartedResponse(job.getId(), "/import/" + job.getId()));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ImportStatusResponse> getStatus(@PathVariable String jobId) {
        return importJobStore.findById(jobId)
                .map(job -> ResponseEntity.ok(ImportStatusResponse.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    record ErrorResponse(String error) {}

    record ImportStartedResponse(String jobId, String statusUrl) {}

    record ImportStatusResponse(
            String id,
            String status,
            String startedAt,
            String completedAt,
            Galleries galleries,
            Pictures pictures,
            List<String> errors
    ) {
        record Galleries(int total, int processed, String current) {}
        record Pictures(int total, int processed) {}

        static ImportStatusResponse from(ImportJob job) {
            return new ImportStatusResponse(
                    job.getId(),
                    job.getStatus().name(),
                    job.getStartedAt().toString(),
                    job.getCompletedAt() != null ? job.getCompletedAt().toString() : null,
                    new Galleries(job.getTotalGalleries(), job.getProcessedGalleries(), job.getCurrentGallery()),
                    new Pictures(job.getTotalPictures(), job.getProcessedPictures()),
                    job.getErrors()
            );
        }
    }
}
