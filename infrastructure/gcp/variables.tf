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
