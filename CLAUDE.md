# Mappics v3 — Claude Code Guide

## Project overview

Map-based photo gallery. Java 21 + Spring Boot backend (hexagonal/DDD), React + Leaflet frontend, GCP (Cloud Run + Firestore + GCS + Firebase Hosting), Terraform IaC, GitHub Actions CI/CD.

## Monorepo layout

```
backend/     Spring Boot — Java 21, Maven
frontend/    React 18 + Vite + react-leaflet
infrastructure/gcp/  Terraform (Google provider ~> 6.0 + google-beta)
.github/workflows/   backend.yml · frontend.yml · terraform.yml
```

## Running locally

**Backend** (port 8081, seeds test galleries on startup):
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend** (proxies /api/* → localhost:8081):
```bash
cd frontend
npm run dev       # http://localhost:5173
```

**Both via Docker Compose** (backend only — run frontend with npm run dev):
```bash
docker-compose up --build
```

## Running tests

```bash
cd backend
./mvnw test        # unit tests (fast, no Docker)
./mvnw verify      # + Firestore emulator IT (needs Docker Desktop with default socket enabled)
make mutation      # PIT mutation testing (fails < 85% score; report in target/pit-reports/index.html)
```

## Triggering an import

The import processes all JPEGs in the GCS source bucket (prod) or the in-memory fixture store (local).

**Local** — runs in-process via the HTTP endpoint (`ImportController` is `@Profile("local")`):
```bash
curl -X POST http://localhost:8081/import   # no secret required
curl http://localhost:8081/import/{jobId}   # poll status
```

**Production** — runs as the `mappics-import-job` Cloud Run Job, NOT via the web service. The `/import` endpoints don't exist in prod (404): the web service has `cpu_idle=true` and would throttle the CPU-bound resize (turning a minutes-long import into hours). See `.claude/plans/import-cloud-run-job.md`.
```bash
gcloud run jobs execute mappics-import-job --region <region> --wait
```
Watch progress in Cloud Logging (`resource.type="cloud_run_job"`, filter `jsonPayload.importJobId="<id>"`).

The import is idempotent — already-processed pictures are skipped field by field.

## API endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/galleries` | List all galleries (id, name, averageGps, pictureCount) |
| GET | `/api/galleries/{id}` | Gallery detail with all pictures |
| POST | `/import` | Start async in-process import; 409 if one is already running. **Local profile only** (404 in prod — use the `mappics-import-job` Cloud Run Job) |
| GET | `/import/{jobId}` | Live progress. **Local profile only** |
| GET | `/local-images/{gallery}/{file}` | Serves fixture images (local profile only) |
| GET | `/actuator/health` | Health check (used by Cloud Run probes) |

## Key architecture decisions

- **Hexagonal (ports & adapters)**: domain has no Spring/GCP imports; ports are interfaces in `domain/`; adapters are in `infrastructure/`.
- **Local profile** (`application-local.properties`): `GalleryFileStorageInMemory`, `GalleryRepositoryInMemory`, `HTTPClientForLocalDev` (stubs OSM + Open-Meteo), OSM rate limit = 0 ms.
- **Prod profile**: `GalleryFileStorageWithGoogleStorage`, `GalleryRepositoryUsingFirestore`, `HTTPClientWithJavaHttpClient`, OSM rate limit = 1000 ms.
- **`LocalDevSeeder`**: walks `src/test/resources/galleries/**` at startup, extracts GPS via EXIF, seeds galleries into the in-memory repository. Does NOT resize images — thumbnails point to the original JPEG via `LocalImageController`.
- **Import idempotency**: `Picture.hasAllData()` gates the skip; ExifData is always non-null after extraction to avoid retries.
- **Async import**: `@Async` on `ProcessUploadedGalleries.processAsync()`; `ImportJob` tracks progress thread-safely with `volatile` + `AtomicInteger`.

## Domain model

```
Gallery          id, name, List<pictureIds>, GpsCoordinates averageGps
Picture          id, galleryId, filename, thumbnailUrl, fullSizeUrl,
                 GpsCoordinates (lat, lon, altitude),
                 ExifData (make, model, takenAt, focalLength, aperture, iso),
                 LocationDescription (name, shortDescription),
                 WeatherData (tempC, humidity, windSpeedKmh, wmoCode, description)
```

## External services

| Service | Rate limit | API key |
|---|---|---|
| OSM Nominatim (location) | 1 req/s (enforced in `ProcessUploadedGalleries`) | None — User-Agent header required |
| Open-Meteo archive (weather) | Generous free tier | None |

## Deploying

See `infrastructure/README.md` for Terraform setup, GCP resource creation, and GitHub Actions wiring. CI/CD pipelines:

- `backend.yml` — push to `main` touching `backend/**` → test → build → push Docker → deploy Cloud Run
- `frontend.yml` — push to `main` touching `frontend/**` → build (VITE_API_BASE_URL from Cloud Run) → Firebase deploy
- `terraform.yml` — PR touching `infrastructure/**` → plan as PR comment; merge → apply

## Java conventions

- Records for value objects; plain classes for aggregates.
- `Optional` as return type only — never as field or parameter.
- `LocalDateTime` for EXIF `takenAt` (EXIF has no timezone — intentional).
- Stream API for collection transforms; `forEach` only for side effects.
- No comments unless the WHY is non-obvious.

## Test conventions

- Test doubles (fakes) over mocks; see `infrastructure/` test packages.
- Abstract contract tests (`GalleryFileStorageAbstractTest`, `GalleryRepositoryAbstractTest`) run against every adapter implementation.
- `PictureBuilder` in `src/test/java/domain/` builds fully-populated `Picture` objects for tests.
- Integration tests tagged `@Tag("integration")` and run by Failsafe (`./mvnw verify`).
