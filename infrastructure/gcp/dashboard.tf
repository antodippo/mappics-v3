# ── Monitoring dashboard ──────────────────────────────────────────────────────
# One dashboard covering the web service and the import job. The layout lives in
# dashboards/mappics.json.tftpl; the uptime check IDs are only known after apply,
# hence the template rather than a static JSON file.
#
# Note: legendTemplate placeholders in the template use $${...} so Terraform
# leaves them for Cloud Monitoring to interpolate.

resource "google_monitoring_dashboard" "mappics" {
  dashboard_json = templatefile("${path.module}/dashboards/mappics.json.tftpl", {
    project_id        = var.project_id
    service_name      = google_cloud_run_v2_service.backend.name
    job_name          = google_cloud_run_v2_job.import.name
    backend_check_id  = google_monitoring_uptime_check_config.backend.uptime_check_id
    frontend_check_id = google_monitoring_uptime_check_config.frontend.uptime_check_id
  })

  depends_on = [google_project_service.monitoring]
}
