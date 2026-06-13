# Mappics — Infrastructure

This directory contains Terraform code for all cloud resources Mappics needs to run in production on Google Cloud Platform.

```
infrastructure/
└── gcp/          ← Step 15-17: GCP foundation, Cloud Run, Firebase Hosting
```

---

## Prerequisites

| Tool | Purpose | Install |
|---|---|---|
| [gcloud CLI](https://cloud.google.com/sdk/docs/install) | Authenticate to GCP | `brew install google-cloud-sdk` |
| [Terraform](https://developer.hashicorp.com/terraform/install) | Provision infrastructure | `brew install terraform` |
| A GCP project | All resources live here | [Create one](https://console.cloud.google.com/projectcreate) |

---

## Option A — Terraform (recommended)

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
gsutil mb -l europe-west1 gs://mappics-tf-state-YOUR_PROJECT

# Then uncomment the backend block in gcp/main.tf and fill in the bucket name,
# and run: terraform init -migrate-state
```

### 5. Apply

```bash
cd infrastructure/gcp

terraform init      # download providers, initialise backend
terraform plan      # preview what will be created
terraform apply     # create the resources (takes ~2 min on first run due to API enablement)
```

After a successful apply, Terraform prints the output values you need for the next steps:

```
source_bucket_name             = "mappics-source-abc123"
processed_bucket_name          = "mappics-processed-abc123"
artifact_registry_url          = "europe-west1-docker.pkg.dev/your-project/mappics"
backend_service_account_email  = "mappics-backend@your-project.iam.gserviceaccount.com"
cicd_service_account_email     = "mappics-cicd@your-project.iam.gserviceaccount.com"
```

---

## Option B — Manual setup via gcloud

If you prefer not to use Terraform, run these commands once. Replace the placeholder values.

```bash
PROJECT_ID="your-gcp-project-id"
REGION="europe-west1"
SOURCE_BUCKET="mappics-source-${PROJECT_ID}"
PROCESSED_BUCKET="mappics-processed-${PROJECT_ID}"

gcloud config set project $PROJECT_ID
```

### Enable APIs

```bash
gcloud services enable \
  run.googleapis.com \
  firestore.googleapis.com \
  artifactregistry.googleapis.com \
  storage.googleapis.com \
  iam.googleapis.com \
  secretmanager.googleapis.com \
  iamcredentials.googleapis.com
```

### Create GCS buckets

```bash
# Source bucket (private — stores original JPEGs)
gsutil mb -l $REGION gs://$SOURCE_BUCKET
gsutil uniformbucketlevelaccess set on gs://$SOURCE_BUCKET

# Processed bucket (public read — serves thumbnails and full-size images)
gsutil mb -l $REGION gs://$PROCESSED_BUCKET
gsutil uniformbucketlevelaccess set on gs://$PROCESSED_BUCKET
gsutil iam ch allUsers:objectViewer gs://$PROCESSED_BUCKET

# CORS on the processed bucket (allows browser image loading)
cat > /tmp/cors.json <<'EOF'
[{"origin":["*"],"method":["GET","HEAD"],"responseHeader":["Content-Type"],"maxAgeSeconds":3600}]
EOF
gsutil cors set /tmp/cors.json gs://$PROCESSED_BUCKET
```

### Create Firestore database

```bash
gcloud firestore databases create \
  --location=$REGION \
  --type=firestore-native
```

> If you see "already exists" the project already has a Firestore database — that's fine.

### Create Artifact Registry repository

```bash
gcloud artifacts repositories create mappics \
  --repository-format=docker \
  --location=$REGION \
  --description="Docker images for Mappics backend"
```

### Create service accounts

```bash
# Backend runtime identity
gcloud iam service-accounts create mappics-backend \
  --display-name="Mappics Backend" \
  --description="Runtime identity for the Mappics Cloud Run service"

BACKEND_SA="mappics-backend@${PROJECT_ID}.iam.gserviceaccount.com"

# Grant bucket access
gsutil iam ch serviceAccount:${BACKEND_SA}:objectViewer gs://$SOURCE_BUCKET
gsutil iam ch serviceAccount:${BACKEND_SA}:objectAdmin  gs://$PROCESSED_BUCKET

# Grant Firestore access
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${BACKEND_SA}" \
  --role="roles/datastore.user"

# CI/CD identity (used by GitHub Actions)
gcloud iam service-accounts create mappics-cicd \
  --display-name="Mappics CI/CD" \
  --description="Used by GitHub Actions to build and deploy Mappics"

CICD_SA="mappics-cicd@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CICD_SA}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CICD_SA}" \
  --role="roles/artifactregistry.writer"

# Allow CI/CD to attach the backend SA to the Cloud Run service
gcloud iam service-accounts add-iam-policy-binding $BACKEND_SA \
  --member="serviceAccount:${CICD_SA}" \
  --role="roles/iam.serviceAccountUser"
```

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

---

## Next steps

- **Step 16** — `infrastructure/gcp/cloud_run.tf`: Cloud Run service definition
- **Step 17** — Firebase Hosting configuration for the React frontend
- **Step 18** — GitHub Actions pipeline: build → push → deploy
- **Step 20** — Workload Identity Federation binding for `mappics-cicd`

---

## Tear down

```bash
# Terraform
cd infrastructure/gcp
terraform destroy

# Manual — delete buckets (this deletes all photos!)
gsutil -m rm -r gs://mappics-source-YOUR_PROJECT
gsutil -m rm -r gs://mappics-processed-YOUR_PROJECT
```

> `terraform destroy` will fail for the Firestore database unless you first manually
> delete all documents, because `deletion_policy = "DELETE"` requires the database to be empty.
