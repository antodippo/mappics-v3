terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 6.0"
    }
  }

  # GCS backend — bucket is supplied at init time so it stays out of source control.
  #
  # Local dev (local state):
  #   terraform init -backend=false
  #
  # Remote state (recommended):
  #   terraform init -backend-config="bucket=YOUR_STATE_BUCKET"
  #   Create the bucket first: gcloud storage buckets create gs://YOUR_STATE_BUCKET --location=REGION
  backend "gcs" {
    prefix = "mappics/gcp"
    # bucket is passed via -backend-config at init time (CI and local remote runs)
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}
