resource "google_artifact_registry_repository" "docker" {
  location      = var.region
  repository_id = var.artifact_registry_repository
  description   = "Docker images for Mappics backend"
  format        = "DOCKER"

  depends_on = [google_project_service.artifact_registry]
}
