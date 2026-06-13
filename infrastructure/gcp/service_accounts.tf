# ── Backend service account ───────────────────────────────────────────────────
# Used by the Cloud Run service at runtime.

resource "google_service_account" "backend" {
  account_id   = "mappics-backend"
  display_name = "Mappics Backend"
  description  = "Runtime identity for the Mappics Cloud Run service"
}

# Read original photos from the source bucket
resource "google_storage_bucket_iam_member" "backend_source_viewer" {
  bucket = google_storage_bucket.source.name
  role   = "roles/storage.objectViewer"
  member = "serviceAccount:${google_service_account.backend.email}"
}

# Write + manage resized photos in the processed bucket
resource "google_storage_bucket_iam_member" "backend_processed_admin" {
  bucket = google_storage_bucket.processed.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.backend.email}"
}

# Read and write Firestore documents
resource "google_project_iam_member" "backend_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.backend.email}"
}

# ── CI/CD service account ─────────────────────────────────────────────────────
# Used by GitHub Actions to push Docker images and deploy to Cloud Run.
# Workload Identity Federation binds are added in step 18 (GitHub Actions).

resource "google_service_account" "cicd" {
  account_id   = "mappics-cicd"
  display_name = "Mappics CI/CD"
  description  = "Used by GitHub Actions to build and deploy Mappics"
}

resource "google_project_iam_member" "cicd_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.cicd.email}"
}

resource "google_project_iam_member" "cicd_artifact_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.cicd.email}"
}

# Allows CI/CD to set the runtime identity on the Cloud Run service
resource "google_service_account_iam_member" "cicd_act_as_backend" {
  service_account_id = google_service_account.backend.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.cicd.email}"
}
