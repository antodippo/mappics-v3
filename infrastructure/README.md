# Mappics — Infrastructure

This directory contains Terraform code for all cloud resources Mappics needs to run in production on Google Cloud Platform.

```
infrastructure/
└── gcp/          ← GCP foundation, Cloud Run, Firebase Hosting
```

---

## Prerequisites

| Tool | Purpose | Install |
|---|---|---|
| [gcloud CLI](https://cloud.google.com/sdk/docs/install) | Authenticate to GCP | `brew install google-cloud-sdk` |
| [Terraform](https://developer.hashicorp.com/terraform/install) | Provision infrastructure | `brew install terraform` |
| A GCP project | All resources live here | [Create one](https://console.cloud.google.com/projectcreate) |

---

## Setup

### 1. Install Terraform

```bash
# macOS
brew tap hashicorp/tap
brew install hashicorp/tap/terraform

# Verify
terraform version   # should print >= 1.5.x
```

### 2. Authenticate to GCP

```bash
gcloud auth login
gcloud auth application-default login   # lets Terraform use your credentials
gcloud config set project YOUR_PROJECT_ID
```

### 3. Configure variables

```bash
cd infrastructure/gcp
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
project_id            = "your-gcp-project-id"
region                = "europe-west1"          # or us-central1, etc.
firestore_location    = "europe-west1"
source_bucket_name    = "mappics-source-abc123"    # globally unique
processed_bucket_name = "mappics-processed-abc123" # globally unique
alert_email           = "you@example.com"          # receives all alerts
```

> **Bucket names must be globally unique** across all GCP projects worldwide.  
> A safe pattern: `mappics-source-<your-project-id>`.

`alert_email` is required — every alert policy notifies this single channel. To
also get the monthly budget alert, set `billing_account_id` (see
[Monitoring & alerting](#monitoring--alerting) below).

### 4. (Optional) Set up a remote state bucket

Terraform state tracks what it has created. For solo use, local state (`terraform.tfstate`) is fine.
For team use or CI/CD, store state in GCS:

```bash
# Create the state bucket manually (only once, outside Terraform)
gcloud storage buckets create gs://mappics-tf-state-YOUR_PROJECT --location=europe-west1

# Then uncomment the backend block in gcp/main.tf and fill in the bucket name,
# and run: terraform init -migrate-state
```

### 5. Accept Firebase Terms of Service

Before `terraform apply` can link Firebase to your GCP project, you must accept the Firebase Terms of Service — the API returns 403 even for project owners if this step is skipped.

Visit [console.firebase.google.com](https://console.firebase.google.com), select your project, and accept the Terms of Service when prompted. You do not need to complete the Firebase project setup wizard; accepting the ToS is enough.

### 6. Apply

```bash
cd infrastructure/gcp

terraform init      # download providers, initialise backend
terraform plan      # preview what will be created
terraform apply     # create the resources (takes ~2 min on first run due to API enablement)
```

After a successful apply, Terraform prints the output values you need:

```
source_bucket_name             = "mappics-source-abc123"
processed_bucket_name          = "mappics-processed-abc123"
artifact_registry_url          = "europe-west1-docker.pkg.dev/your-project/mappics"
backend_service_account_email  = "mappics-backend@your-project.iam.gserviceaccount.com"
cicd_service_account_email     = "mappics-cicd@your-project.iam.gserviceaccount.com"
cloud_run_url                  = "https://mappics-backend-HASH-ew.a.run.app"
firebase_hosting_url           = "https://your-project.web.app"
```

### 7. Wire up GitHub Actions

After `terraform apply`, set the following in **GitHub → Settings → Secrets and variables → Actions**.

**Secrets** (sensitive):

| Secret | Pipeline | How to get the value |
|---|---|---|
| `WIF_PROVIDER` | all | `terraform output -raw workload_identity_provider` |
| `CICD_SERVICE_ACCOUNT` | backend + frontend | `terraform output -raw cicd_service_account_email` |
| `TERRAFORM_SERVICE_ACCOUNT` | terraform | `terraform output -raw terraform_service_account_email` |

> `TERRAFORM_SERVICE_ACCOUNT` only exists after the **first manual apply** that creates the `mappics-terraform` SA. Set it once that apply completes.

**Variables** (non-sensitive):

| Variable | Pipeline | Value |
|---|---|---|
| `GCP_PROJECT_ID` | all | your GCP project ID |
| `GCP_REGION` | all | e.g. `europe-west1` |
| `ARTIFACT_REGISTRY_URL` | backend | `terraform output -raw artifact_registry_url` |
| `TF_STATE_BUCKET` | terraform | name of the GCS bucket holding Terraform state |
| `TF_SOURCE_BUCKET` | terraform | `terraform output -raw source_bucket_name` |
| `TF_PROCESSED_BUCKET` | terraform | `terraform output -raw processed_bucket_name` |
| `ALERT_EMAIL` | terraform | email address that receives alerts — **required**, `terraform plan` fails without it |
| `BILLING_ACCOUNT_ID` | terraform | optional; billing account ID for the budget alert (`gcloud billing projects describe <project_id> --format='value(billingAccountName)'`) |

Quick reference — run these in `infrastructure/gcp/` after applying:

```bash
terraform output workload_identity_provider
terraform output cicd_service_account_email
terraform output terraform_service_account_email
terraform output artifact_registry_url
terraform output source_bucket_name
terraform output processed_bucket_name
terraform output firebase_site_id
```

### 8. Firebase Hosting first-time setup

After `terraform apply`, complete the Firebase CLI setup once:

```bash
npm install -g firebase-tools
firebase login

cd frontend
cp .firebaserc.example .firebaserc
# Edit .firebaserc and replace YOUR_GCP_PROJECT_ID with your actual project ID

# Verify deploy works manually before wiring CI/CD
npm run build
firebase deploy --only hosting
```

To point the frontend at the Cloud Run backend, set `VITE_API_BASE_URL` at build time:

```bash
VITE_API_BASE_URL=https://mappics-backend-HASH-ew.a.run.app npm run build
firebase deploy --only hosting
```

In CI/CD (step 18) this is injected automatically from the Terraform outputs.

---

## What was created

| Resource | Purpose |
|---|---|
| `mappics-source` GCS bucket | Stores original JPEGs uploaded by the user |
| `mappics-processed` GCS bucket | Stores resized thumbnails and full-size images; publicly readable |
| Firestore Native database | Persists gallery and picture metadata |
| Artifact Registry repo (`mappics`) | Stores Docker images built by CI |
| `mappics-backend` service account | Runtime identity for the Cloud Run service |
| `mappics-cicd` service account | CI/CD identity for GitHub Actions (Workload Identity set up in step 18) |
| Cloud Run service (`mappics-backend`) | Runs the Spring Boot backend; scales to zero |
| Cloud Run job (`mappics-import-job`) | Runs the photo import with full (non-throttled) CPU; billed per execution. Trigger with `gcloud run jobs execute mappics-import-job`. See `.claude/plans/import-cloud-run-job.md` |
| Firebase project linkage | Firebase enabled on the GCP project |
| Firebase Hosting site | SPA hosting with `**` → `/index.html` rewrite; Vite assets cached for 1 year |
| Uptime checks ×2 | Poll the backend `/actuator/health` and the Hosting URL every 5 min from all regions |
| Alert policies ×5 | Backend down, frontend down, 5xx responses, import job failed, monthly budget |
| Email notification channel | Single destination for every alert |
| `Mappics` dashboard | Traffic, latency, saturation, uptime, import job and error logs |

---

## Monitoring & alerting

Defined in `gcp/monitoring.tf`, all on Cloud Run's built-in metrics — no code
changes, and free at this scale.

| Alert | Fires when |
|---|---|
| Backend down | `/actuator/health` uptime check fails from more than one region |
| Frontend down | Hosting URL uptime check fails from more than one region |
| 5xx responses | Any 5xx from `mappics-backend` in a 5-minute window (traffic is low, so any 5xx is signal) |
| Import job failed | A `mappics-import-job` execution ends with `result=failed` — the job already retries 3×, so this means retries are exhausted |
| Monthly budget | Spend reaches 90% then 100% of €10 in the calendar month |

### Dashboard

`gcp/dashboard.tf` creates a single **Mappics** dashboard (Monitoring →
Dashboards). The layout lives in `gcp/dashboards/mappics.json.tftpl` — a
template rather than plain JSON because the uptime check IDs are only known
after apply.

| Row | Widgets |
|---|---|
| Top line | Backend uptime, frontend uptime, 5xx count, p95 latency (scorecards) |
| Traffic | Request rate by response class · latency p50/p95/p99 |
| Saturation | Memory p99 (85% marker) · CPU p99 · cold start p95 |
| Availability | Fraction of uptime checkers passing · import executions by result |
| Import & errors | Executions in flight · `severity>=ERROR` logs from service and job |

Memory is the widget worth watching: image resizing against a 1Gi limit makes
OOM the most likely failure mode. Cold start latency matters because the
service runs with `cpu_idle=true`.

Import job *duration* is deliberately absent — Cloud Run publishes no
execution-duration metric. The existing Cloud Trace spans (`import.gallery`,
`import.picture.*`) cover that.

To change a widget, edit the `.tftpl` and re-apply.

**Validate before committing.** `dashboard_json` is an opaque string to
Terraform, so `validate` and `plan` both pass on JSON the Monitoring API will
reject — and the failure then lands on the apply-on-merge pipeline. A `jq` syntax
check is not enough: the API also rejects valid JSON with the wrong fields (an
`xyChart` threshold accepts only `value` and `label`, while a `scorecard`
threshold also takes `color` and `direction`). Render it and POST it:

```bash
terraform console <<<'templatefile("dashboards/mappics.json.tftpl", {project_id="p", service_name="s", job_name="j", backend_check_id="b", frontend_check_id="f"})' | sed '1d;$d' | jq '.displayName="probe — delete me"' > /tmp/probe.json
```

```bash
curl -s -X POST -H "Authorization: Bearer $(gcloud auth print-access-token)" -H "Content-Type: application/json" -d @/tmp/probe.json "https://monitoring.googleapis.com/v1/projects/$(gcloud config get-value project)/dashboards" | jq -c '{name, error: .error.message}'
```

A `name` in the response means the JSON is good — then **delete the probe** with
`curl -X DELETE` on that name (it reports one error at a time, so expect to
iterate).

### Verify the email channel

GCP sends a verification email when the notification channel is first created.
**Until you click the link, alerts fire but deliver nowhere.** Check the inbox
for `alert_email` after the first apply.

### Budget alert requires billing-account permissions

Budgets live on the billing account, not the project, so `roles/editor` on the
project is not enough. The budget is skipped entirely while
`billing_account_id` is empty — the other four alerts apply normally.

To enable it, grant the Terraform service account access to the billing account
and set the variable:

```bash
gcloud billing accounts add-iam-policy-binding BILLING_ACCOUNT_ID \
  --member="serviceAccount:$(terraform output -raw terraform_service_account_email)" \
  --role="roles/billing.costsManager"

gh variable set BILLING_ACCOUNT_ID --body BILLING_ACCOUNT_ID
```

Alternatively, apply that one resource from a local run with your own
credentials and leave the CI variable unset.

`budget_currency` must match the billing account currency (`gcloud billing
accounts describe BILLING_ACCOUNT_ID`) or the API rejects the budget.

---

## Tear down

```bash
cd infrastructure/gcp
terraform destroy
```

> `terraform destroy` will fail for the Firestore database unless you first manually
> delete all documents, because `deletion_policy = "DELETE"` requires the database to be empty.
