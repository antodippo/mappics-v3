# ── Cloud Run service ─────────────────────────────────────────────────────────

resource "google_cloud_run_v2_service" "backend" {
  name     = "mappics-backend"
  location = var.region

  # Prevent unauthenticated access at the Cloud Run level is set via IAM below.
  # ingress = "INGRESS_TRAFFIC_ALL" allows traffic from the internet.
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.backend.email

    scaling {
      min_instance_count = var.cloud_run_min_instances
      max_instance_count = var.cloud_run_max_instances
    }

    containers {
      # On first apply this is a placeholder. CI/CD (step 18) will push the
      # real image and update the service. Terraform ignores the image field
      # after creation so it does not revert CI/CD deployments.
      image = var.backend_image

      resources {
        limits = {
          memory = var.cloud_run_memory
          cpu    = var.cloud_run_cpu
        }
        # CPU is only allocated during request processing (not idle).
        cpu_idle = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
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
      env {
        name  = "MAPPICS_IMPORT_SECRET"
        value = var.import_secret
      }

      startup_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 10
      }

      liveness_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        period_seconds    = 30
        failure_threshold = 3
      }
    }
  }

  # CI/CD updates the image tag on every deploy; Terraform must not revert it.
  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
    ]
  }

  depends_on = [
    google_project_service.run,
    google_artifact_registry_repository.docker,
    google_service_account.backend,
  ]
}

# ── Public access ─────────────────────────────────────────────────────────────

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
