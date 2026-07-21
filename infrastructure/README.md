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
```

> **Bucket names must be globally unique** across all GCP projects worldwide.  
> A safe pattern: `mappics-source-<your-project-id>`.

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

---

## Tear down

```bash
cd infrastructure/gcp
terraform destroy
```

> `terraform destroy` will fail for the Firestore database unless you first manually
> delete all documents, because `deletion_policy = "DELETE"` requires the database to be empty.
