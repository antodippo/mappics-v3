# Mappics v3

A map-based photo gallery. Point it at a Google Cloud Storage bucket full of geotagged JPEGs and it produces an interactive world map where each gallery is a pin, with per-picture maps, full-size overlays, and EXIF / weather / location metadata — all fetched automatically at import time.

---

## How it works

1. Upload JPEGs to a GCS bucket, organised in folders by location (`Iceland/`, `Azores/`, …).
2. Hit `POST /import` — the backend extracts GPS, EXIF, weather (Open-Meteo) and location names (OpenStreetMap Nominatim), resizes images, and writes everything to Firestore.
3. The React frontend reads the REST API and renders the gallery.

---

## Architecture

| Layer | Technology |
|---|---|
| Backend | Java 21 + Spring Boot 3.x (DDD / hexagonal) |
| Database | Cloud Firestore |
| File storage | Google Cloud Storage |
| EXIF extraction | metadata-extractor |
| Image resizing | Scrimage |
| Location names | OSM Nominatim (free, no key) |
| Weather | Open-Meteo historical archive (free, no key) |
| Frontend | React + Vite + Leaflet |
| Hosting | Firebase Hosting |
| Compute | Cloud Run (scales to zero) |
| IaC | Terraform |
| CI/CD | GitHub Actions + Workload Identity Federation |

---

## Running locally

No GCP account or credentials needed. The `local` Spring profile wires in-memory adapters and stubs, and seeds 3 real galleries (Azores, Iceland, Italy) from test fixtures at startup.

### Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven wrapper | included (`./mvnw`) |
| Node.js | 20+ |
| npm | included with Node |

### Start the backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on **http://localhost:8081** in a few seconds. You should see log lines like:

```
Seeding local dev data from .../src/test/resources/galleries…
Seeded Azores/DSC_0892.JPG
Seeded Iceland/DSC_0114.JPG
…
Local dev seed complete — 3 galleries, 4 pictures
```

### Start the frontend

In a second terminal:

```bash
cd frontend
npm install     # first time only
npm run dev
```

Open **http://localhost:5173** — the Vite dev server proxies `/api/*` to the backend automatically.

### What you'll see

- **World map** with markers for Azores, Iceland and Italy (real GPS from the fixture JPEGs).
- Click a marker → gallery page with per-picture thumbnail markers on a local map.
- Click a thumbnail or map pin → full-size overlay with EXIF, GPS coordinates, altitude, and weather panel.

---

## API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/galleries` | List all galleries with average GPS and picture count |
| `GET` | `/api/galleries/{id}` | Gallery detail with all picture metadata |
| `POST` | `/import` | Start an async import job; returns `202` with a `jobId` |
| `GET` | `/import/{jobId}` | Live import progress (galleries/pictures processed, errors) |

Example:

```bash
# Start import
curl -X POST http://localhost:8081/import

# Check progress
curl http://localhost:8081/import/{jobId}
```

---

## Running tests

```bash
cd backend

# Unit tests only (fast, no Docker needed)
./mvnw test

# Unit + integration tests (Firestore emulator via Testcontainers — needs Docker)
./mvnw verify
```

The integration test (`GalleryRepositoryUsingFirestoreIT`) pulls `gcr.io/google.com/cloudsdktool/cloud-sdk:emulators` on first run; subsequent runs use the cached image.

> **macOS + Docker Desktop**: before running `./mvnw verify`, go to  
> Docker Desktop → Settings → Advanced → enable **"Allow the default Docker socket to be used"**.

---

## Project structure

```
mappics-v3/
├── backend/                   ← Spring Boot backend
│   ├── src/
│   │   ├── main/java/com/antodippo/mappics/
│   │   │   ├── domain/        ← aggregates, value objects, port interfaces
│   │   │   ├── application/   ← import use case, job tracking
│   │   │   └── infrastructure/← adapters (GCS, Firestore, OSM, Open-Meteo…)
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                  ← React + Vite frontend
│   ├── src/
│   │   ├── api/client.js      ← fetchGalleries(), fetchGallery()
│   │   ├── components/        ← PictureOverlay
│   │   └── pages/             ← MapPage, GalleryPage
│   ├── firebase.json
│   └── vite.config.js         ← /api/* proxy → localhost:8081
│
├── infrastructure/
│   ├── gcp/                   ← Terraform: APIs, GCS, Firestore, Cloud Run,
│   │   │                         Artifact Registry, Firebase Hosting, WIF
│   │   └── *.tf
│   └── README.md              ← full infra setup guide (Terraform + manual)
│
└── .github/
    └── workflows/
        ├── backend.yml        ← test → build → push image → deploy Cloud Run
        ├── frontend.yml       ← build (with Cloud Run URL) → Firebase deploy
        └── terraform.yml      ← plan on PRs, apply on merge to main
```

---

## Production deployment

See **[infrastructure/README.md](infrastructure/README.md)** for the full guide, including:

- Terraform setup and first apply
- GCS buckets, Firestore, Artifact Registry, Cloud Run, Firebase Hosting
- Workload Identity Federation (no long-lived service account keys in CI)
- GitHub Secrets and Variables needed for each pipeline
- Firebase CLI first-time deploy

### Quick reference — GitHub configuration

After `terraform apply`, the values you need are printed as outputs:

```bash
cd infrastructure/gcp
terraform output   # prints all values
```

| GitHub Secret | Source |
|---|---|
| `WIF_PROVIDER` | `terraform output -raw workload_identity_provider` |
| `CICD_SERVICE_ACCOUNT` | `terraform output -raw cicd_service_account_email` |
| `TERRAFORM_SERVICE_ACCOUNT` | `terraform output -raw terraform_service_account_email` *(after first apply)* |

| GitHub Variable | Source |
|---|---|
| `GCP_PROJECT_ID` | your GCP project ID |
| `GCP_REGION` | e.g. `europe-west1` |
| `ARTIFACT_REGISTRY_URL` | `terraform output -raw artifact_registry_url` |
| `TF_STATE_BUCKET` | name of the GCS bucket for Terraform state |
| `TF_SOURCE_BUCKET` | `terraform output -raw source_bucket_name` |
| `TF_PROCESSED_BUCKET` | `terraform output -raw processed_bucket_name` |
