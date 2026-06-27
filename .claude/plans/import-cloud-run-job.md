# Import via Cloud Run Job

## Why
A full import took ~2h10m. Cause: `POST /import` runs the work on a background
thread after returning `202`, and the Cloud Run service has `cpu_idle = true`, so
CPU is throttled to near-zero when no request is in flight. The CPU-bound Scrimage
resize crawls (3–8 min/picture vs sub-second on full CPU). Network-bound steps
(exif/location/weather) wait on I/O and are unaffected.

Confirmed locally: resizing an 11–13 MB original takes ~0.5–2.8 s per operation on
full CPU — versus minutes on the throttled service.

## Approach
Run the import as a **Cloud Run Job**, which always has CPU allocated while it
runs and is billed only for those minutes (~cents/import) — versus ~$45–50/mo to
keep the web service CPU always-on 24/7. The web service keeps `cpu_idle = true`
and no longer does heavy work.

## How it works
- Same Docker image. The Job sets `MAPPICS_RUN_MODE=import` and
  `SPRING_MAIN_WEB_APPLICATION_TYPE=none`. `ImportJobRunner`
  (`infrastructure/job/ImportJobRunner.java`, an `ApplicationRunner` gated on
  `mappics.run-mode=import`) runs `GalleryImporter.importGalleries()` to completion
  and exits 0 (success) / 1 (failure).
- Job config (`infrastructure/gcp/cloud_run_job.tf`): SA `mappics-backend`,
  `timeout=3600s`, `max_retries=3` (import is idempotent), `cpu=2`, `memory=2Gi`.
  Image tag owned by CI (`backend.yml` runs `gcloud run jobs deploy`); Terraform
  ignores the image field.
- The `/import` HTTP endpoints are `@Profile("local")` only (`ImportController`,
  `ImportJobStore`, `ProcessUploadedGalleries`), so they don't exist in prod (404);
  use the Job instead. Local keeps the in-process `@Async` trigger + live polling.

## Running it
    gcloud run jobs execute mappics-import-job --region <region> --wait

## Watching progress
Cloud Logging (Log Explorer):

    resource.type="cloud_run_job"
    resource.labels.job_name="mappics-import-job"

Per-job filter (structured JSON / logstash): `jsonPayload.importJobId="<id>"`.
Key log lines: `Import started: N galleries`, `Gallery X done: N enriched`,
`Import completed in Nms`.

## Cost
Billed only while executing. A minutes-long import costs cents. No 24/7 CPU.

## Concurrency note
Unlike the old in-process path (which returned `409` via `hasRunningJob`), there
is no built-in guard against two simultaneous executions. Triggering the Job twice
at once would run two imports in parallel → up to 2 req/s to OSM Nominatim, over
its 1 req/s ToS. The import is idempotent so data won't corrupt, but **don't start
overlapping executions**.

Note: Cloud Run Jobs' `--parallelism` / `--tasks` only limit concurrency *within*
a single execution (and this job is single-task anyway); they do **not** stop two
separate executions from overlapping, and there is no native single-flight lock
across executions. If you ever need to enforce one-at-a-time, guard it externally —
e.g. check `gcloud run jobs executions list --job mappics-import-job` for a
`Running` execution before triggering, or trigger only from a single Cloud
Scheduler job.

## Optional: scheduled runs
Add a `google_cloud_scheduler_job` calling the Run Jobs API on a cron, gated
behind a `var.import_schedule` toggle. Off by default — not built.

## Follow-up
Decode-once-resize-twice (`ImageResizer.resizeToBounds`) decodes each JPEG once
instead of twice, halving per-picture decode CPU and lowering peak memory. Shipped
as a separate commit; not required for the 2h fix.
