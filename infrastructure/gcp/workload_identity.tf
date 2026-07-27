# Workload Identity Federation lets GitHub Actions authenticate to GCP without
# storing long-lived service account keys as secrets.
#
# How it works:
#   GitHub Actions → OIDC token → Google STS → short-lived GCP credentials
#                                              → impersonate mappics-cicd SA

resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-actions"
  display_name              = "GitHub Actions"
  description               = "Identity pool for GitHub Actions CI/CD"

  depends_on = [google_project_service.iam_credentials]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-provider"
  display_name                       = "GitHub OIDC"
  description                        = "Trusts OIDC tokens issued by GitHub Actions"

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.actor"      = "assertion.actor"
    "attribute.repository" = "assertion.repository"
    # job_workflow_ref is "<owner>/<repo>/.github/workflows/<file>@<ref>". The ref
    # changes per run (refs/heads/main on push, refs/pull/N/merge on a PR), so strip
    # it: the workflow FILE is the only stable way to tell one workflow from another,
    # and repository alone lets ANY workflow impersonate ANY of these accounts.
    "attribute.workflow_file" = "assertion.job_workflow_ref.split('@')[0]"
  }

  # Only tokens from YOUR repository can use this provider
  attribute_condition = "assertion.repository == '${var.github_repository}'"
}

locals {
  workflow_principal_prefix = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.workflow_file/${var.github_repository}/.github/workflows"
}

# Each service account is impersonable only by the workflow files that legitimately
# need it — not by every workflow in the repo. depends_on so a failed provider update
# can never leave these bindings pointing at an attribute the provider doesn't map
# (which would lock CI out of GCP entirely).
resource "google_service_account_iam_member" "cicd_workload_identity" {
  for_each = toset(["backend.yml", "frontend.yml"])

  service_account_id = google_service_account.cicd.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "${local.workflow_principal_prefix}/${each.value}"

  # Changing `member` replaces the binding: create the new one first so a rejected
  # principalSet leaves the old binding in place instead of locking CI out of GCP.
  lifecycle {
    create_before_destroy = true
  }

  depends_on = [google_iam_workload_identity_pool_provider.github]
}

# Only the Terraform pipeline may impersonate the Terraform SA. This matters more
# than for cicd: with editor + securityAdmin it can grant itself any role, so any
# other workflow holding `id-token: write` — claude.yml runs an AI agent on
# issue_comment — could otherwise mint credentials that own the whole project.
resource "google_service_account_iam_member" "terraform_workload_identity" {
  service_account_id = google_service_account.terraform.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "${local.workflow_principal_prefix}/terraform.yml"

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [google_iam_workload_identity_pool_provider.github]
}
