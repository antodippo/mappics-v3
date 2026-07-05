# Fix Dependabot auto-merge issues

## Context

Dependabot + native auto-merge was set up in `fa161fe`, but three PRs misbehaved. All three are **`github-actions` ecosystem** updates (they edit files under `.github/workflows/`). Investigation against the live repo (`antodippo/mappics-v3`) shows two independent root causes, not three:

Branch protection on `main` requires exactly two checks — **`Test`** and **`Build check`** — with no required reviews, `enforce_admins=off`, `strict=off`. The auto-merge workflow (`.github/workflows/dependabot-auto-merge.yml`) enables GitHub native auto-merge using the default `GITHUB_TOKEN`.

### The three symptoms

| PR | Symptom | Root cause |
|----|---------|-----------|
| **#26** setup-java 4→5 | Green, auto-merge enabled, but stuck `BLOCKED` | Merge would modify `.github/workflows/backend.yml`; the `GITHUB_TOKEN` that enabled auto-merge cannot complete a merge that writes workflow files → GitHub blocks it. (Verified by elimination: `MERGEABLE`, both required checks pass, no required reviews.) |
| **#30** setup-buildx-action 3→4 | Auto-merge auto-disabled: *"Tried to create or update workflow without `workflows` permission"* | **Same root cause as #26**, surfaced explicitly. `GITHUB_TOKEN` has no `workflows` write scope (and cannot be granted one). |
| **#45** setup-terraform 3→4 | Merged despite a red ❌ `Plan` check | **Different, unrelated cause.** `Plan` is **not** a required check, so auto-merge ignored its failure and merged on `Test`+`Build check` green. The `Plan` failure itself was a **false negative**: the terraform job's GCP auth got an empty `secrets.WIF_PROVIDER` because *Actions secrets are not exposed to Dependabot-triggered runs*. Main is fine. |

**Two fixes** address all three: (A) a token that can merge workflow-file PRs → fixes #26/#30; (B) exclude infra PRs from auto-merge so a human gates them → fixes #45.

Decisions taken with the user: **(A) fine-grained PAT** (stored as a Dependabot secret); **(B) human review for infra** rather than making `Plan` a required check (which would permanently block Dependabot terraform-workflow PRs, since their `Plan` can never authenticate).

---

## Fix A — Auto-merge workflow-file PRs (#26, #30)

The default `GITHUB_TOKEN` can never write `.github/workflows/**`, so it can't complete the merge of any `github-actions` bump. Enable auto-merge with a **fine-grained PAT** that has workflow write.

### Manual steps (user, in GitHub UI)
1. Create a **fine-grained PAT** scoped to `antodippo/mappics-v3` with repository permissions: **Contents: Read/Write**, **Pull requests: Read/Write**, **Workflows: Read/Write**.
2. Add it under **Settings → Secrets and variables → Dependabot** (NOT Actions) as `DEPENDABOT_PAT`.
   - Critical: Dependabot-triggered runs only see **Dependabot** secrets; an Actions secret would resolve to empty and the fix would silently do nothing.

### Code change — `.github/workflows/dependabot-auto-merge.yml`
Change the `Enable auto-merge` step's `GH_TOKEN` from `${{ secrets.GITHUB_TOKEN }}` to `${{ secrets.DEPENDABOT_PAT }}`. (See combined workflow below.)

---

## Fix B — Infra PRs require human review (#45)

Keep `Plan` non-required. Instead, **skip enabling auto-merge** when a Dependabot PR touches infrastructure, so it stays open for a manual merge.

### Code change — `.github/workflows/dependabot-auto-merge.yml`
Add a step that inspects the PR's changed files and only enables auto-merge when none match `infrastructure/**` or `.github/workflows/terraform.yml`.

Full target workflow:

```yaml
name: Dependabot auto-merge

on: pull_request

permissions:
  contents: write
  pull-requests: write

jobs:
  auto-merge:
    runs-on: ubuntu-latest
    if: github.actor == 'dependabot[bot]'
    env:
      PR_URL: ${{ github.event.pull_request.html_url }}
    steps:
      # Infra changes (Terraform / the terraform workflow) are gated by a human,
      # not auto-merged. Their Plan check can't authenticate under Dependabot
      # (Actions secrets aren't exposed), so it can never be a trusted gate.
      - name: Detect infrastructure changes
        id: infra
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          files=$(gh pr view "$PR_URL" --json files -q '.files[].path')
          if echo "$files" | grep -qE '^infrastructure/|^\.github/workflows/terraform\.yml$'; then
            echo "infra=true" >> "$GITHUB_OUTPUT"
          else
            echo "infra=false" >> "$GITHUB_OUTPUT"
          fi

      - name: Enable auto-merge
        if: steps.infra.outputs.infra == 'false'
        run: gh pr merge --auto --squash "$PR_URL"
        env:
          GH_TOKEN: ${{ secrets.DEPENDABOT_PAT }}
```

### Optional but recommended — `.github/workflows/terraform.yml`
Add `if: github.actor != 'dependabot[bot]'` to the `terraform` job so it doesn't run on Dependabot PRs. This removes the misleading red ❌ `Plan` (which only fails because WIF secrets are unavailable), leaving a clean status for the human reviewer. A pure action-version bump has no `.tf` changes to plan anyway.

---

## Files to modify
- `.github/workflows/dependabot-auto-merge.yml` — token swap + infra-exclusion step (both fixes).
- `.github/workflows/terraform.yml` — skip job for Dependabot actor (optional cleanup).
- No branch-protection changes needed (required checks stay `Test` + `Build check`).

## Clean up the already-stuck PRs (after deploying the fix)
The new workflow only runs on *future* Dependabot `pull_request` events. For the currently-open ones:
- **#26, #30**: comment `@dependabot recreate` on each to regenerate the PR and re-trigger auto-merge with the PAT — or merge them manually (your account has workflow scope).
- **#45**: already merged; no action. Main is healthy (the ❌ was the secret false-negative, not a terraform break). Confirm with a manual `terraform.yml` `workflow_dispatch` run if you want certainty.

## Verification
1. **Secret wiring**: after adding `DEPENDABOT_PAT` as a Dependabot secret, comment `@dependabot recreate` on #26. The `Dependabot auto-merge` run should succeed and the `Enable auto-merge` step should execute (infra=false path). PR should auto-merge once `Test`+`Build check` are green.
2. **Infra gate**: on the next Dependabot PR touching `terraform.yml` (or trigger #45-like), confirm the auto-merge step is **skipped** (infra=true) and the PR stays open awaiting manual merge.
3. **No regression**: a normal npm/maven bump (e.g. a backend Maven PR) should still auto-merge as before.
4. Optionally validate the terraform skip: the `Plan` check should no longer appear/fail on Dependabot PRs.
