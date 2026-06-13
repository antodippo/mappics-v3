terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }

  # Remote state in GCS — uncomment once you have created the state bucket.
  # See infrastructure/README.md for instructions.
  #
  # backend "gcs" {
  #   bucket = "YOUR_STATE_BUCKET"        # e.g. "mappics-tf-state"
  #   prefix = "mappics/gcp"
  # }
}

provider "google" {
  project = var.project_id
  region  = var.region
}
