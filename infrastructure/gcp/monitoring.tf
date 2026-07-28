# ── Monitoring, uptime checks and alerting ────────────────────────────────────
# Scope: uptime checks (backend + frontend), 5xx alert, import-job-failure alert
# and a monthly budget alert. SLOs, burn-rate alerts, log-based metrics and the
# dashboard are deliberately left out — they are separate steps of the
# observability plan (see the "Observability: monitoring, alerts" project item).
#
# Everything here runs on Cloud Run's built-in metrics, so no code changes and
# no cost at this scale (uptime checks and system metrics are free tier).

data "google_project" "current" {
  project_id = var.project_id
}

# ── Notification channel ──────────────────────────────────────────────────────
# One email channel, shared by every alert. GCP sends a verification email on
# creation: until it is confirmed, alerts fire but deliver nowhere.

resource "google_monitoring_notification_channel" "email" {
  display_name = "Mappics alerts"
  type         = "email"

  labels = {
    email_address = var.alert_email
  }

  depends_on = [google_project_service.monitoring]
}

# ── Uptime checks ─────────────────────────────────────────────────────────────
# Checked from all available regions, so a single flaky checker cannot page us
# (the alert policies below require more than one region to report a failure).

resource "google_monitoring_uptime_check_config" "backend" {
  display_name = "Mappics backend"
  timeout      = "10s"
  period       = "300s"

  http_check {
    request_method = "GET"
    path           = "/actuator/health"
    port           = 443
    use_ssl        = true
    validate_ssl   = true
  }

  monitored_resource {
    type = "uptime_url"

    labels = {
      project_id = var.project_id
      host       = trimprefix(google_cloud_run_v2_service.backend.uri, "https://")
    }
  }

  depends_on = [google_project_service.monitoring]
}

resource "google_monitoring_uptime_check_config" "frontend" {
  display_name = "Mappics frontend"
  timeout      = "10s"
  period       = "300s"

  http_check {
    request_method = "GET"
    path           = "/"
    port           = 443
    use_ssl        = true
    validate_ssl   = true
  }

  monitored_resource {
    type = "uptime_url"

    labels = {
      project_id = var.project_id
      host       = "${google_firebase_hosting_site.frontend.site_id}.web.app"
    }
  }

  depends_on = [google_project_service.monitoring]
}

# ── Alert: service down ───────────────────────────────────────────────────────
# COUNT_FALSE over a 20-minute window: "more than one checker region failed".

resource "google_monitoring_alert_policy" "backend_down" {
  display_name = "Mappics backend is down"
  combiner     = "OR"
  severity     = "CRITICAL"

  conditions {
    display_name = "Uptime check failing from more than one region"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"monitoring.googleapis.com/uptime_check/check_passed\"",
        "resource.type=\"uptime_url\"",
        "metric.label.check_id=\"${google_monitoring_uptime_check_config.backend.uptime_check_id}\"",
      ])

      comparison      = "COMPARISON_GT"
      threshold_value = 1
      duration        = "60s"

      aggregations {
        alignment_period     = "1200s"
        per_series_aligner   = "ALIGN_NEXT_OLDER"
        cross_series_reducer = "REDUCE_COUNT_FALSE"
        group_by_fields      = ["resource.label.host"]
      }

      trigger {
        count = 1
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.id]

  alert_strategy {
    auto_close = "1800s"
  }
}

resource "google_monitoring_alert_policy" "frontend_down" {
  display_name = "Mappics frontend is down"
  combiner     = "OR"
  severity     = "CRITICAL"

  conditions {
    display_name = "Uptime check failing from more than one region"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"monitoring.googleapis.com/uptime_check/check_passed\"",
        "resource.type=\"uptime_url\"",
        "metric.label.check_id=\"${google_monitoring_uptime_check_config.frontend.uptime_check_id}\"",
      ])

      comparison      = "COMPARISON_GT"
      threshold_value = 1
      duration        = "60s"

      aggregations {
        alignment_period     = "1200s"
        per_series_aligner   = "ALIGN_NEXT_OLDER"
        cross_series_reducer = "REDUCE_COUNT_FALSE"
        group_by_fields      = ["resource.label.host"]
      }

      trigger {
        count = 1
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.id]

  alert_strategy {
    auto_close = "1800s"
  }
}

# ── Alert: 5xx responses ──────────────────────────────────────────────────────
# Traffic is low enough that any 5xx is worth a look, hence threshold 0.
# request_count is a DELTA metric: ALIGN_DELTA sums per series, REDUCE_SUM
# collapses the response-code series into one number per 5-minute window.

resource "google_monitoring_alert_policy" "backend_5xx" {
  display_name = "Mappics backend returned 5xx"
  combiner     = "OR"
  severity     = "ERROR"

  conditions {
    display_name = "Any 5xx response in a 5-minute window"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"run.googleapis.com/request_count\"",
        "resource.type=\"cloud_run_revision\"",
        "resource.label.service_name=\"${google_cloud_run_v2_service.backend.name}\"",
        "metric.label.response_code_class=\"5xx\"",
      ])

      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"

      aggregations {
        alignment_period     = "300s"
        per_series_aligner   = "ALIGN_DELTA"
        cross_series_reducer = "REDUCE_SUM"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.id]

  # The series stops reporting once the errors stop, so the incident would
  # otherwise stay open forever.
  alert_strategy {
    auto_close = "1800s"
  }
}

# ── Alert: import job failure ─────────────────────────────────────────────────
# The job retries 3× on its own (cloud_run_job.tf), so result="failed" means the
# retries are exhausted and a human needs to look at it.

resource "google_monitoring_alert_policy" "import_job_failed" {
  display_name = "Mappics import job failed"
  combiner     = "OR"
  severity     = "ERROR"

  conditions {
    display_name = "Execution finished with result=failed"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"run.googleapis.com/job/completed_execution_count\"",
        "resource.type=\"cloud_run_job\"",
        "resource.label.job_name=\"${google_cloud_run_v2_job.import.name}\"",
        "metric.label.result=\"failed\"",
      ])

      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"

      aggregations {
        alignment_period     = "600s"
        per_series_aligner   = "ALIGN_DELTA"
        cross_series_reducer = "REDUCE_SUM"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.id]

  alert_strategy {
    auto_close = "1800s"
  }
}

# ── Alert: monthly budget ─────────────────────────────────────────────────────
# Created only when billing_account_id is set: budgets live on the billing
# account, and roles/editor on the project does not grant access to it. See
# infrastructure/README.md for the role the Terraform SA needs.

resource "google_billing_budget" "monthly" {
  count = var.billing_account_id != "" ? 1 : 0

  billing_account = var.billing_account_id
  display_name    = "Mappics monthly spend"

  budget_filter {
    projects        = ["projects/${data.google_project.current.number}"]
    calendar_period = "MONTH"
  }

  amount {
    specified_amount {
      # Must match the billing account currency or the API rejects the budget.
      currency_code = var.budget_currency
      units         = tostring(var.monthly_budget_amount)
    }
  }

  # Early warning, then the actual limit.
  threshold_rules {
    threshold_percent = 0.9
  }

  threshold_rules {
    threshold_percent = 1.0
  }

  all_updates_rule {
    schema_version                   = "1.0"
    monitoring_notification_channels = [google_monitoring_notification_channel.email.id]
    disable_default_iam_recipients   = true
  }

  depends_on = [google_project_service.billing_budgets]
}
