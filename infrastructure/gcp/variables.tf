variable "project_id" {
  description = "GCP project ID where all resources will be created."
  type        = string
}

variable "region" {
  description = "Default GCP region for regional resources (Cloud Run, Artifact Registry, GCS)."
  type        = string
  default     = "europe-west1"
}

variable "firestore_location" {
  description = "Firestore database location. Must be a supported Firestore region (may differ from var.region). See https://cloud.google.com/firestore/docs/locations"
  type        = string
  default     = "europe-west1"
}

variable "source_bucket_name" {
  description = "Globally unique name for the GCS bucket that holds original uploaded JPEGs."
  type        = string
}

variable "processed_bucket_name" {
  description = "Globally unique name for the GCS bucket that holds resized/processed images. Objects are publicly readable."
  type        = string
}

variable "artifact_registry_repository" {
  description = "Name of the Artifact Registry repository for Docker images."
  type        = string
  default     = "mappics"
}

# ── Cloud Run ─────────────────────────────────────────────────────────────────

variable "backend_image" {
  description = "Full Docker image reference for the Cloud Run service. Use the placeholder on first apply; CI/CD will update the running image automatically without Terraform re-applying."
  type        = string
  default     = "gcr.io/cloudrun/placeholder:latest"
}

variable "cloud_run_memory" {
  description = "Memory limit for each Cloud Run instance."
  type        = string
  default     = "1Gi"
}

variable "cloud_run_cpu" {
  description = "CPU limit for each Cloud Run instance."
  type        = string
  default     = "1"
}

variable "cloud_run_min_instances" {
  description = "Minimum number of Cloud Run instances."
  type        = number
  default     = 1
}

variable "cloud_run_max_instances" {
  description = "Maximum number of Cloud Run instances."
  type        = number
  default     = 1
}

variable "import_job_cpu" {
  description = "CPU limit for the mappics-import-job Cloud Run Job. The Job always allocates CPU while running and is billed per execution, so this can be generous."
  type        = string
  default     = "2"
}

variable "import_job_memory" {
  description = "Memory limit for the mappics-import-job Cloud Run Job."
  type        = string
  default     = "2Gi"
}

variable "import_secret" {
  description = "Shared secret required in the X-Import-Secret header to call POST /import. Use a random string (e.g. openssl rand -hex 32)."
  type        = string
  sensitive   = true
}

# ── Firebase Hosting ──────────────────────────────────────────────────────────

variable "github_repository" {
  description = "GitHub repository in 'owner/repo' format. Only Actions tokens from this repo can impersonate the CI/CD service account via Workload Identity Federation."
  type        = string
}

# ── Firebase Hosting ──────────────────────────────────────────────────────────

variable "firebase_site_id" {
  description = "Firebase Hosting site ID (must be globally unique, 1-39 chars, lowercase). Defaults to the GCP project ID."
  type        = string
  default     = ""
}
