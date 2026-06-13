# Firebase Hosting requires the google-beta provider for project linkage.

# ── Enable Firebase APIs ──────────────────────────────────────────────────────

resource "google_project_service" "firebase" {
  provider           = google-beta
  service            = "firebase.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "firebase_hosting" {
  provider           = google-beta
  service            = "firebasehosting.googleapis.com"
  disable_on_destroy = false
}

# ── Link the GCP project to Firebase ─────────────────────────────────────────
# This is a one-way operation — linking cannot be undone via Terraform.

resource "google_firebase_project" "default" {
  provider = google-beta
  project  = var.project_id

  depends_on = [google_project_service.firebase]
}

# ── Create the Hosting site ───────────────────────────────────────────────────
# The site_id determines the default URL: https://{site_id}.web.app
# It defaults to the GCP project ID when left empty.

locals {
  firebase_site_id = var.firebase_site_id != "" ? var.firebase_site_id : var.project_id
}

resource "google_firebase_hosting_site" "frontend" {
  provider = google-beta
  project  = var.project_id
  site_id  = local.firebase_site_id

  depends_on = [google_firebase_project.default]
}

# ── Grant CI/CD permission to deploy Hosting ─────────────────────────────────

resource "google_project_iam_member" "cicd_firebase_hosting" {
  project = var.project_id
  role    = "roles/firebasehosting.admin"
  member  = "serviceAccount:${google_service_account.cicd.email}"
}
