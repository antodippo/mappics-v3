# Mappics frontend — common tasks.
# Run `make` or `make help` for the list.

SHELL := /bin/bash
NPM   := npm

.DEFAULT_GOAL := help

.PHONY: help install dev build preview lint test test-watch basemaps basemaps-baseline clean

help: ## List available targets
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}'

## ---- Local development -------------------------------------------------------

install: ## Install dependencies from the lockfile (clean, reproducible)
	$(NPM) ci

dev: ## Run the Vite dev server (proxies /api → localhost:8081)
	$(NPM) run dev

build: ## Production build (bundle written to dist/)
	$(NPM) run build

preview: ## Serve the production build locally
	$(NPM) run preview

## ---- Quality -----------------------------------------------------------------

lint: ## Lint with ESLint
	$(NPM) run lint

test: ## Run the Vitest suite once (jsdom, CI-friendly)
	$(NPM) test

test-watch: ## Run Vitest in watch mode
	$(NPM) run test:watch

basemaps: ## Check the live basemap tiles against the recorded baseline
	node scripts/check-basemaps.mjs

basemaps-baseline: ## Re-record the basemap tile baseline (commit the result)
	node scripts/check-basemaps.mjs --update

clean: ## Remove build output
	rm -rf dist
