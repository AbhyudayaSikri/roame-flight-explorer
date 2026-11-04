# Overnight run log (Nov 4, 2026)

Branch `overnight`, cut from `main` at `6f91c42`. Scope: `docs/PLAN.md` steps 4–8, MUST-tier items only, one step at a time. Times are local (UTC-5).

## Status

| Step | State | Last green CI |
|---|---|---|
| 4. Local API (`:server`) | in progress | — |
| 5. Shared domain | pending | — |
| 6. Data layer + store | pending | — |
| 7. Android UI | pending | — |
| 8. iOS UI | pending | — |

## Log

- **00:40 Setup.** Created `overnight` from `main`. Both workflows now also run on pushes to `overnight`; before this they only ran on `main` and on PRs.

## Decisions

- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Generalize or revert this at merge time.
- **First push carries two commits:** the setup commit and the first step-4 commit. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't run until `:server` and the shared host-test task exist.

## Failures and fixes

None yet.
