#!/usr/bin/env node
// Basemap tile health check.
//
// A dead tile provider is easy to spot; a provider that changes what it serves
// is not. In August 2026 CARTO started stamping "API KEY REQUIRED" into keyless
// tiles and kept returning 200 OK with a well-formed PNG, so status codes,
// content types, uptime checks and Leaflet's tileerror event were all blind to
// it — the text was pixels. Comparing the tile bytes against a recorded
// baseline is the only cheap signal that catches that class of failure.
//
//   node scripts/check-basemaps.mjs            verify against the baseline
//   node scripts/check-basemaps.mjs --update   re-record the baseline
//
// Exits 0 when every tile matches, 1 when one does not, and 2 when no baseline
// has been recorded yet. The third code keeps a first run from looking like a
// regression: basemap-health.yml seeds the baseline on a 2 and only alerts on a 1.
//
// Providers do legitimately re-render their tiles, so a hash mismatch means
// "look at this", not always "broken". Confirm the tile looks right, then
// re-record with --update and commit the result.

import { createHash } from 'node:crypto'
import { readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { BASEMAPS } from '../src/basemaps.js'

const BASELINE = fileURLToPath(new URL('./basemap-baseline.json', import.meta.url))

// A low zoom keeps the tile small and stable: imagery is re-flown far more
// often at street level than at continental scale.
const PROBE = { z: 4, x: 8, y: 5 }

// Anything smaller than this is an error placeholder, not a map tile.
const MIN_BYTES = 500

const expand = (url, subdomains) =>
  url
    .replace('{s}', (subdomains ?? 'abc')[0])
    .replace('{z}', PROBE.z)
    .replace('{x}', PROBE.x)
    .replace('{y}', PROBE.y)
    .replace('{r}', '')

function probes() {
  return BASEMAPS.flatMap(base => [
    { id: base.key, url: expand(base.url, base.subdomains) },
    ...(base.overlays ?? []).map((overlay, i) => ({
      id: `${base.key}:overlay-${i}`,
      url: expand(overlay.url, base.subdomains),
    })),
  ])
}

async function fetchTile({ id, url }) {
  const response = await fetch(url, {
    headers: { 'User-Agent': 'mappics-basemap-health-check' },
  })
  if (!response.ok) return { id, url, error: `HTTP ${response.status} ${response.statusText}` }

  const type = response.headers.get('content-type') ?? ''
  if (!type.startsWith('image/')) return { id, url, error: `content-type is "${type}", not an image` }

  const bytes = Buffer.from(await response.arrayBuffer())
  if (bytes.length < MIN_BYTES) {
    return { id, url, error: `only ${bytes.length} bytes — too small to be a real tile` }
  }

  return { id, url, bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') }
}

const settle = async probe => {
  try {
    return await fetchTile(probe)
  } catch (cause) {
    return { ...probe, error: `request failed: ${cause.message}` }
  }
}

const results = await Promise.all(probes().map(settle))

if (process.argv.includes('--update')) {
  const broken = results.filter(r => r.error)
  if (broken.length) {
    for (const { id, url, error } of broken) console.error(`✗ ${id}\n  ${url}\n  ${error}`)
    console.error('\nRefusing to record a baseline from unreachable tiles.')
    process.exit(1)
  }

  const capturedAt = new Date().toISOString().slice(0, 10)
  const baseline = Object.fromEntries(
    results.map(({ id, url, sha256, bytes }) => [id, { url, sha256, bytes, capturedAt }]))
  await writeFile(BASELINE, JSON.stringify(baseline, null, 2) + '\n')
  console.log(`Recorded ${results.length} tiles to ${BASELINE}`)
  process.exit(0)
}

const baseline = JSON.parse(await readFile(BASELINE, 'utf8'))

if (Object.keys(baseline).length === 0) {
  console.log('No baseline recorded yet — nothing to compare against.')
  console.log('Record one with: node scripts/check-basemaps.mjs --update')
  process.exit(2)
}

const failures = []

for (const result of results) {
  const { id, url } = result
  const expected = baseline[id]

  if (result.error) {
    failures.push(`✗ ${id} — ${result.error}\n  ${url}`)
  } else if (!expected) {
    failures.push(`✗ ${id} — no baseline recorded. Run with --update.\n  ${url}`)
  } else if (expected.url !== url) {
    failures.push(`✗ ${id} — url changed since the baseline. Run with --update.\n  was ${expected.url}\n  now ${url}`)
  } else if (expected.sha256 !== result.sha256) {
    failures.push(
      `✗ ${id} — tile content changed since ${expected.capturedAt}.\n` +
      `  ${url}\n` +
      `  expected ${expected.sha256} (${expected.bytes} bytes)\n` +
      `  received ${result.sha256} (${result.bytes} bytes)\n` +
      '  Open the url and check for a watermark or a dead provider. If the tile\n' +
      '  is fine, re-record with: node scripts/check-basemaps.mjs --update')
  } else {
    console.log(`✓ ${id} (${result.bytes} bytes)`)
  }
}

if (failures.length) {
  console.error(`\n${failures.length} of ${results.length} basemap tiles failed:\n`)
  console.error(failures.join('\n\n'))
  process.exit(1)
}

console.log(`\nAll ${results.length} basemap tiles match the baseline.`)
