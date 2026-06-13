output "source_bucket_name" {
  description = "Name of the GCS bucket for original JPEGs."
  value       = google_storage_bucket.source.name
}

output "processed_bucket_name" {
  description = "Name of the GCS bucket for resized images."
  value       = google_storage_bucket.processed.name
}

output "processed_bucket_public_url" {
  description = "Base URL for publicly readable processed images."
  value       = "https://storage.googleapis.com/${google_storage_bucket.processed.name}"
}

output "artifact_registry_url" {
  description = "Docker registry URL — use as the image prefix when pushing."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_repository}"
}

output "backend_service_account_email" {
  description = "Service account email to attach to the Cloud Run service."
  value       = google_service_account.backend.email
}

output "cicd_service_account_email" {
  description = "Service account email for GitHub Actions Workload Identity binding."
  value       = google_service_account.cicd.email
}

output "cloud_run_url" {
  description = "Public HTTPS URL of the deployed Cloud Run service."
  value       = google_cloud_run_v2_service.backend.uri
}

output "docker_image_base" {
  description = "Base image path (without tag) to use when pushing from CI/CD."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_repository}/mappics-backend"
}

output "firebase_hosting_url" {
  description = "Default Firebase Hosting URL for the frontend."
  value       = "https://${local.firebase_site_id}.web.app"
}

output "firebase_site_id" {
  description = "Firebase Hosting site ID — needed in .firebaserc and GitHub Actions."
  value       = local.firebase_site_id
}
