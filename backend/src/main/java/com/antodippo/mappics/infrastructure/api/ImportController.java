package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import com.antodippo.mappics.application.ProcessUploadedGalleries;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/import")
public class ImportController {

    private final ProcessUploadedGalleries processUploadedGalleries;
    private final ImportJobStore importJobStore;

    public ImportController(ProcessUploadedGalleries processUploadedGalleries,
                            ImportJobStore importJobStore) {
        this.processUploadedGalleries = processUploadedGalleries;
        this.importJobStore = importJobStore;
    }

    @PostMapping
    public ResponseEntity<?> startImport() {
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
