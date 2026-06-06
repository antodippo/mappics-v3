# Mappics V3 — Implementation Plan

## Technology Decisions

| Concern | Choice | Rationale |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.x | LTS, strong GCP library support |
| Database | Cloud Firestore | Serverless, schemaless, same as v2 |
| File storage | Google Cloud Storage | Required by spec |
| Image resizing | Scrimage (Java port) | Used in v2, well-tested |
| EXIF extraction | metadata-extractor | Used in v2, comprehensive |
| Location names | OpenStreetMap Nominatim | Free, no API key, used in v2 |
| Weather (historical) | Open-Meteo | Completely free, no API key, covers history since 1940 |
| Compute | Cloud Run | Scales to zero, simple deploy |
| Frontend | React + Vite + Leaflet | Component-based, lightweight map |
| Frontend hosting | Firebase Hosting | Free CDN + HTTPS, GCP-native |
| IaC | Terraform | Required by spec |
| CI/CD | GitHub Actions | Required by spec |
| Auth | None (public gallery) | — |

## Architecture Overview

Hexagonal (Ports & Adapters) + DDD:

```
domain/          ← Gallery, Picture aggregates; value objects; port interfaces
application/     ← Use cases (import pipeline, gallery queries)
infrastructure/  ← Adapters: Firestore, GCS, OSM, Open-Meteo, Scrimage, REST controllers
```

Two GCS buckets:
- `mappics-source` — original JPEGs uploaded by the user, organised in folders by location
- `mappics-processed` — thumbnails + full-size resized copies written during import

---

## Steps

### Step 1 — Project scaffolding
Create the Maven project with Spring Boot 3.x, Java 21.
Set up the hexagonal package structure (`domain`, `application`, `infrastructure`).
Add base dependencies: `spring-boot-starter-web`, `spring-cloud-gcp-starter`, `spring-cloud-gcp-starter-storage`, `google-cloud-firestore`, `slf4j`/`logback`, `spring-boot-starter-test`.
Add `application.properties` with profiles: `local` (stubs) and `prod` (real GCP adapters).

### Step 2 — Core domain model
Define the domain aggregates and value objects (no framework dependencies):

- `Gallery` aggregate: id (= folder name), name, list of `Picture` ids, average GPS position, processed flag
- `Picture` aggregate: id, gallery id, original filename, thumbnail URL, full-size URL, `GpsCoordinates`, `ExifData`, `LocationDescription`, `WeatherData`
- Value objects: `GpsCoordinates(lat, lon)`, `ExifData(cameraMake, cameraModel, takenAt, focalLength, aperture, iso)`, `LocationDescription(name, shortDescription)`, `WeatherData(temperatureCelsius, weatherCode, description)`

Define all outbound port interfaces in `domain`:
- `GalleryRepository` (save, findById, findAll)
- `GalleryFileStorage` (listGalleries, listPictures, readPicture, writeThumbnail, writeFullSize, exists)
- `ExifExtractor` (extract)
- `LocationDescriptionFetcher` (fetch)
- `WeatherFetcher` (fetch)
- `ImageResizer` (resize)

Write unit tests for domain entity logic (average GPS calculation, `hasAllData()` guard, etc.).

### Step 3 — GCS file storage adapter
Implement `GalleryFileStorageWithGoogleStorage` using `spring-cloud-gcp-starter-storage`.
Logic:
- List folders in source bucket → one gallery each
- List `.jpg`/`.JPG` files per folder → pictures
- Read raw bytes of a picture
- Write thumbnail/full-size to processed bucket under `{gallery}/{filename}_{suffix}.jpg`
- `exists()` check used for idempotency

Implement `GalleryFileStorageInMemory` (test double, backed by a `Map` of byte arrays seeded from `src/test/resources/galleries/`).

Write an abstract contract test (`GalleryFileStorageAbstractTest`) that both adapters must pass.

### Step 4 — EXIF extraction adapter
Implement `ExtractExifDataWithMetadataExtractor` using `metadata-extractor`.
Extract: GPS (lat/lon), `DateTimeOriginal`, camera make/model, focal length, aperture (f-number), ISO.
Handle missing/malformed tags gracefully — return an `Optional.empty()` per field.

Write `ExtractExifDataWithMetadataExtractorTest` using real test JPEGs with known EXIF data.

### Step 5 — Image resizing adapter
Implement `ResizePictureWithScrimage`.
Rules: thumbnail max 400px on the longer side; full-size max 1920px on the longer side. Never upscale. Output quality 85% JPEG.

Implement `ResizePictureTestDouble` (returns a fixed small byte array, no I/O).

Write `ResizePictureWithScrimageTest` with a real test JPEG, asserting output dimensions stay within bounds.

### Step 6 — Location description adapter (OSM Nominatim)
Port: `LocationDescriptionFetcher.fetch(GpsCoordinates) → Optional<LocationDescription>`
Implement `FetchLocationDescriptionFromOSM` calling `https://nominatim.openstreetmap.org/reverse`.
Parse JSON response: use `display_name` for description, `address.city`/`address.country` as name fallback.

Implement `HTTPClient` port + `HTTPClientWithJavaHttpClient` adapter (wraps `java.net.http.HttpClient`).
Implement `HTTPClientThatAlwaysReturns` test double.

Write `FetchLocationDescriptionFromOSMTest` using the test-double HTTP client with a canned JSON response.

### Step 7 — Weather data adapter (Open-Meteo)
Port: `WeatherFetcher.fetch(GpsCoordinates, takenAt: OffsetDateTime) → Optional<WeatherData>`
Implement `FetchWeatherDataFromOpenMeteo` calling the Open-Meteo historical API.
Endpoint: `https://archive-api.open-meteo.com/v1/archive?latitude=…&longitude=…&start_date=…&end_date=…&hourly=temperature_2m,weathercode`
No API key needed. Pick the hourly entry closest to the picture's timestamp.

Write `FetchWeatherDataFromOpenMeteoTest` with a canned JSON test double.

### Step 8 — Firestore persistence adapter
Implement `GalleryRepositoryUsingFirestore` mapping domain objects to Firestore documents.
One collection `galleries`, sub-collection `pictures`.

Implement `GalleryRepositoryInMemory` (test double backed by `HashMap`).

Write abstract contract test `GalleryRepositoryAbstractTest` run against the in-memory implementation (Firestore adapter is covered by integration tests with real Firestore emulator if available, otherwise manual).

### Step 9 — Import use case and endpoint
Application service `ProcessUploadedGalleries`:
1. List galleries from GCS source bucket
2. For each gallery, list pictures
3. Skip pictures whose `Gallery` + `Picture` records already exist in Firestore AND have all data filled (`picture.hasAllData()`)
4. For new/incomplete pictures: extract EXIF → resize → upload processed images → fetch location → fetch weather → save to Firestore
5. After all pictures processed, compute and save gallery average GPS position

Inbound adapter: `ProcessUploadedGalleriesController` — `POST /import` — returns 202 Accepted and runs processing synchronously for simplicity (or async via `@Async` with a status endpoint if processing time is a concern).

Write `ProcessUploadedGalleriesTest` using all test doubles, verifying:
- Idempotency (already-processed pictures are skipped)
- Partial re-processing (missing fields are filled in)

### Step 10 — Read REST API
`GalleryController`:
- `GET /api/galleries` → list of `{id, name, averageGps, thumbnailCount}`
- `GET /api/galleries/{id}` → gallery detail with all pictures (id, thumbnailUrl, fullSizeUrl, gps, exif, location, weather)

Keep response DTOs as Java `record`s in `infrastructure/api`.

Write basic controller tests using MockMvc + in-memory repository.

### Step 11 — React + Vite frontend scaffold
Scaffold with `npm create vite@latest mappics-frontend -- --template react`.
Add dependencies: `leaflet`, `react-leaflet`, `react-router-dom`.
Configure Vite proxy to backend (`http://localhost:8080`) for local dev.
Set up a `VITE_API_BASE_URL` env var used in production build.

Define the route structure:
- `/` → `MapPage`
- `/gallery/:id` → `GalleryPage`

### Step 12 — Main page map (MapPage)
Fetch `GET /api/galleries` on load.
Render a Leaflet `MapContainer` (world view) with a `Marker` per gallery at its `averageGps`.
Clicking a marker shows a popup with the gallery name + a link to `/gallery/:id`.

### Step 13 — Gallery page (GalleryPage)
Fetch `GET /api/galleries/:id`.
Split layout:
- Top: Leaflet map zoomed to the gallery area, with a `Marker` per picture at its GPS position. Clicking a marker opens the `PictureOverlay`.
- Bottom: scrollable thumbnail strip. Clicking a thumbnail opens the same overlay.

### Step 14 — Picture overlay component
Full-screen overlay with two columns:
- Left: full-size image (from `fullSizeUrl`)
- Right: info panel
  - Location name + description (from `location`)
  - EXIF section: date, camera, focal length, aperture, ISO
  - Weather section: temperature, weather description

Keyboard: `Escape` closes; left/right arrow navigates between pictures in the gallery.

### Step 15 — Terraform: GCP foundation
Resources:
- GCP project (or use existing) + enable required APIs (Cloud Run, Firestore, Artifact Registry, GCS, Secret Manager)
- Two GCS buckets: `mappics-source`, `mappics-processed` (public-read for processed)
- Firestore database (Native mode, region)
- Artifact Registry repository for Docker images
- Service accounts: `mappics-backend` (with roles: `storage.objectViewer` on source, `storage.objectAdmin` on processed, `datastore.user`)

All in `terraform/gcp/`.

### Step 16 — Terraform: Cloud Run service
Resources:
- Cloud Run service `mappics-backend`: image from Artifact Registry, env vars from Secret Manager (if any), public ingress (`allUsers` invoker)
- IAM binding for public access

Add `Dockerfile` to backend: multi-stage build (Maven build → JRE 21 slim runtime image).

All in `terraform/gcp/`.

### Step 17 — Terraform: Firebase Hosting
Resources:
- Firebase project linked to the GCP project
- Hosting site with `firebase.json` rewrites (SPA: all routes → `index.html`)

Consider adding a `terraform/firebase/` subdirectory or using the Firebase CLI in the GitHub Action (whichever is simpler given Terraform Firebase provider limitations).

### Step 18 — GitHub Actions: backend pipeline
File: `.github/workflows/backend.yml`
Triggers: push to `main` (any change under `backend/`)

Steps:
1. Checkout
2. Set up Java 21
3. `./mvnw clean verify` (run all tests)
4. Authenticate to GCP via Workload Identity Federation (no long-lived service account keys)
5. Build and push Docker image to Artifact Registry
6. Deploy to Cloud Run (`gcloud run deploy`)

### Step 19 — GitHub Actions: frontend pipeline
File: `.github/workflows/frontend.yml`
Triggers: push to `main` (any change under `frontend/`)

Steps:
1. Checkout
2. `npm ci && npm run build`
3. Authenticate to GCP (same Workload Identity)
4. Deploy to Firebase Hosting (`firebase deploy --only hosting`)

### Step 20 — GitHub Actions: Terraform pipeline
File: `.github/workflows/terraform.yml`
Triggers: push/PR to `main` (any change under `terraform/`)

Steps:
- On PR: `terraform plan` with output posted as PR comment
- On merge to `main`: `terraform apply -auto-approve`

Use a dedicated Terraform service account + Workload Identity.

### Step 21 — Local development setup
`docker-compose.yml` at the repo root:
- Backend Spring Boot container with `spring.profiles.active=local`
- Optionally a Firestore emulator (`google/cloud-sdk` with `gcloud beta emulators firestore start`)

`local` profile wires stub adapters:
- `GalleryFileStorageInMemory` seeded from `src/test/resources/galleries/`
- `GalleryRepositoryInMemory`
- `HTTPClientThatAlwaysReturns` (canned OSM + Open-Meteo responses)

`.env-dist` documents all required env vars for prod profile.

`CLAUDE.md` documents: how to run locally, how to run tests, how to trigger an import, how to deploy.

---

## Monorepo Layout

```
mappics-v3/
├── backend/                  ← Spring Boot Maven project
│   ├── src/main/java/com/antodippo/mappics/
│   │   ├── domain/
│   │   ├── application/
│   │   └── infrastructure/
│   ├── src/test/java/...
│   └── Dockerfile
├── frontend/                 ← React + Vite project
│   ├── src/
│   └── vite.config.js
├── terraform/
│   ├── gcp/
│   └── firebase/
├── .github/workflows/
├── docker-compose.yml
└── CLAUDE.md
```

---

## External Service Notes

- **OSM Nominatim**: add `User-Agent: mappics-v3/1.0` header (required by OSM ToS). Rate-limit to 1 req/s during batch import.
- **Open-Meteo**: free with no API key; historical data available from 1940. Use `archive-api.open-meteo.com`, not `api.open-meteo.com`.
- **GCS processed bucket**: set bucket-level uniform access + `allUsers:objectViewer` so frontend can load images directly without a signed URL.
