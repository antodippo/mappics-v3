resource "google_firestore_database" "default" {
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  # Prevent accidental deletion of production data
  deletion_policy = "DELETE"

  depends_on = [google_project_service.firestore]
}
