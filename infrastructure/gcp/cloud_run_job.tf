# ── Cloud Run Job: photo import ───────────────────────────────────────────────
# The import is CPU-bound (image resize). Running it on the web service is slow
# because that service has cpu_idle=true, so CPU is throttled while no request is
# in flight (the import runs on a background thread after POST /import returns).
#
# A Cloud Run Job always has CPU allocated *while it runs* and is billed only for
# those minutes — so the resize runs at full speed and there is no 24/7 CPU cost.
# Trigger it with:  gcloud run jobs execute mappics-import-job --region <region> --wait

resource "google_cloud_run_v2_job" "import" {
  name     = "mappics-import-job"
  location = var.region

  template {
    template {
      service_account = google_service_account.backend.email

      # Old runs took ~2h; with full CPU this should be minutes. Generous headroom.
      timeout = "3600s"
      # The import is idempotent (already-processed pictures are skipped), so
      # retrying a failed execution is safe.
      max_retries = 3

      containers {
        # Placeholder on first apply; CI/CD (backend.yml) updates the image on
        # every deploy. Terraform ignores the image field afterwards.
        image = var.backend_image

        resources {
          limits = {
            cpu    = var.import_job_cpu
            memory = var.import_job_memory
          }
        }

        # Activate the ImportJobRunner ApplicationRunner and skip the web server,
        # so the JVM runs the import to completion and exits.
        env {
          name  = "MAPPICS_RUN_MODE"
          value = "import"
        }
        env {
          name  = "SPRING_MAIN_WEB_APPLICATION_TYPE"
          value = "none"
        }
        env {
          name  = "GCP_PROJECT_ID"
          value = var.project_id
        }
        env {
          name  = "MAPPICS_SOURCE_BUCKET"
          value = google_storage_bucket.source.name
        }
        env {
          name  = "MAPPICS_PROCESSED_BUCKET"
          value = google_storage_bucket.processed.name
        }
      }
    }
  }

  # CI/CD updates the image tag on every deploy; Terraform must not revert it.
  lifecycle {
    ignore_changes = [
      template[0].template[0].containers[0].image,
    ]
  }

  depends_on = [
    google_project_service.run,
    google_artifact_registry_repository.docker,
    google_service_account.backend,
  ]
}
