# ── Source bucket (private) ───────────────────────────────────────────────────
# Holds original full-resolution JPEGs uploaded by the user.

resource "google_storage_bucket" "source" {
  name          = var.source_bucket_name
  location      = var.region
  force_destroy = false

  uniform_bucket_level_access = true

  depends_on = [google_project_service.storage]
}

# ── Processed bucket (public read) ───────────────────────────────────────────
# Holds thumbnails and full-size resized images served directly to the frontend.

resource "google_storage_bucket" "processed" {
  name          = var.processed_bucket_name
  location      = var.region
  force_destroy = false

  uniform_bucket_level_access = true

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD"]
    response_header = ["Content-Type", "Content-Length"]
    max_age_seconds = 3600
  }

  depends_on = [google_project_service.storage]
}

# Allow anyone to read objects in the processed bucket so the frontend can load
# images directly from https://storage.googleapis.com/{bucket}/{object}.
resource "google_storage_bucket_iam_member" "processed_public_read" {
  bucket = google_storage_bucket.processed.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}
