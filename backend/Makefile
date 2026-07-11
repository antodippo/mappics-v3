# Mappics backend — common tasks.
# Run `make` or `make help` for the list. Override any variable on the command
# line, e.g. `make import-prod REGION=europe-west1 PROJECT=my-proj`.

SHELL    := /bin/bash
MVN      := ./mvnw

BASE_URL ?= http://localhost:8081
JOB_NAME ?= mappics-import-job
REGION   ?= $(shell gcloud config get-value run/region 2>/dev/null)
PROJECT  ?= $(shell gcloud config get-value project 2>/dev/null)
JOB      ?=

.DEFAULT_GOAL := help

.PHONY: help run test verify mutation build clean docker-up docker-down \
        import-local import-status-local \
        import-prod import-logs-prod import-executions-prod

help: ## List available targets
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}'

## ---- Local development -------------------------------------------------------

run: ## Run the backend locally (profile=local, port 8081, seeds test galleries)
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=local

test: ## Unit tests (fast, no Docker)
	$(MVN) test

verify: ## Unit + Firestore-emulator integration tests (needs Docker Desktop)
	$(MVN) verify

mutation: ## Mutation testing (PIT) — fails under 85% score; report in target/pit-reports/index.html
	$(MVN) test-compile pitest:mutationCoverage

build: ## Clean build the runnable jar
	$(MVN) clean package

clean: ## Remove build artifacts
	$(MVN) clean

docker-up: ## Build & start the backend via docker-compose
	docker-compose -f ../docker-compose.yml up --build

docker-down: ## Stop the docker-compose stack
	docker-compose -f ../docker-compose.yml down

## ---- Import: local -----------------------------------------------------------
## Uses the in-process /import endpoint (local profile only). The backend must
## already be running — start it with `make run` in another terminal.

import-local: ## Trigger a local import and follow its progress until done
	@echo "Triggering import at $(BASE_URL)/import ..."
	@response=$$(curl -s -X POST $(BASE_URL)/import); \
	jobId=$$(echo "$$response" | grep -o '"jobId":"[^"]*"' | head -1 | cut -d'"' -f4); \
	if [ -z "$$jobId" ]; then \
		echo "Could not start import. Is the backend running (make run)? Response:"; \
		echo "$$response"; exit 1; \
	fi; \
	echo "Started job $$jobId — polling..."; \
	while true; do \
		status_json=$$(curl -s $(BASE_URL)/import/$$jobId); \
		status=$$(echo "$$status_json" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4); \
		echo "  $$status_json"; \
		case "$$status" in \
			COMPLETED|FAILED) echo "Import $$status."; break;; \
		esac; \
		sleep 2; \
	done

import-status-local: ## One-shot status of a local job: make import-status-local JOB=<id>
	@if [ -z "$(JOB)" ]; then echo "Usage: make import-status-local JOB=<jobId>"; exit 1; fi
	@curl -s $(BASE_URL)/import/$(JOB); echo

## ---- Import: prod ------------------------------------------------------------
## Prod runs as the mappics-import-job Cloud Run Job (the /import endpoints 404
## in prod). Needs gcloud auth + a configured region/project (or pass REGION=/
## PROJECT=). DO NOT start overlapping executions: two concurrent imports would
## push OSM Nominatim past its 1 req/s ToS (the import itself is idempotent).

import-prod: ## Trigger the prod import job (returns immediately; track with import-logs-prod)
	@if [ -z "$(REGION)" ] || [ -z "$(PROJECT)" ]; then \
		echo "REGION/PROJECT not set. Configure gcloud or pass REGION=... PROJECT=..."; exit 1; \
	fi
	gcloud run jobs execute $(JOB_NAME) --region $(REGION) --project $(PROJECT)

import-logs-prod: ## Tail recent prod import-job logs (last hour)
	@if [ -z "$(PROJECT)" ]; then echo "PROJECT not set."; exit 1; fi
	gcloud logging read 'resource.type="cloud_run_job" resource.labels.job_name="$(JOB_NAME)"' \
		--project $(PROJECT) --limit 50 --freshness=1h

import-executions-prod: ## List prod import-job executions (check for a Running one before triggering)
	@if [ -z "$(REGION)" ] || [ -z "$(PROJECT)" ]; then \
		echo "REGION/PROJECT not set. Configure gcloud or pass REGION=... PROJECT=..."; exit 1; \
	fi
	gcloud run jobs executions list --job $(JOB_NAME) --region $(REGION) --project $(PROJECT)
