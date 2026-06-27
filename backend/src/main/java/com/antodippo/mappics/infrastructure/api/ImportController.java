package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import com.antodippo.mappics.application.ProcessUploadedGalleries;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/import")
public class ImportController {

    private final ProcessUploadedGalleries processUploadedGalleries;
    private final ImportJobStore importJobStore;
    private final String importSecret;
    private final boolean inProcessEnabled;
    private final Environment environment;

    public ImportController(ProcessUploadedGalleries processUploadedGalleries,
                            ImportJobStore importJobStore,
                            @Value("${mappics.import.secret:}") String importSecret,
                            @Value("${mappics.import.in-process:true}") boolean inProcessEnabled,
                            Environment environment) {
        this.processUploadedGalleries = processUploadedGalleries;
        this.importJobStore = importJobStore;
        this.importSecret = importSecret;
        this.inProcessEnabled = inProcessEnabled;
        this.environment = environment;
    }

    @PostMapping
    public ResponseEntity<?> startImport(
            @RequestHeader(value = "X-Import-Secret", required = false) String providedSecret) {
        if (!inProcessEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(new ErrorResponse("In-process import is disabled. Run the Cloud Run Job instead: "
                            + "gcloud run jobs execute mappics-import-job --region <region> --wait"));
        }
        if (importSecret.isBlank() && !environment.matchesProfiles("local")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Import endpoint not configured"));
        }
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
