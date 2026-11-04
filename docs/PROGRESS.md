# Overnight run log (Nov 4, 2026)

Branch `overnight`, cut from `main` at `6f91c42`. Scope: `docs/PLAN.md` steps 4–8, MUST-tier items only, one step at a time. Times are local (UTC-5).

## Status

| Step | State | Last green CI |
|---|---|---|
| 4. Local API (`:server`) | done 00:56 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065144) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065124) |
| 5. Shared domain | in progress | — |
| 6. Data layer + store | pending | — |
| 7. Android UI | pending | — |
| 8. iOS UI | pending | — |

## Log

- **00:40 Setup.** Created `overnight` from `main`. Both workflows now also run on pushes to `overnight`; before this they only ran on `main` and on PRs.
- **00:40–00:56 Step 4 (local API).**
  - `:server` is a Ktor CIO app. `GET /flights` serves the fixture byte-for-byte as `application/json`; `GET /health` returns `ok`.
  - Modes: `--mode=empty` (200 with no flights), `error` (500) and `updated` (every flight +500 points).
  - `POST /control/mode?value=…` exists only with `--allow-control`.
  - Every request is logged with method, path, mode and status.
  - 13 tests: one per mode, a byte-for-byte fixture check, the control endpoint on and off, and argument parsing.
  - Smoke test of `./gradlew :server:run`: `curl` got 200 `application/json` with 1,000 flights, byte-identical to the fixture, and the control endpoint switched modes.
  - `ci.yml` now runs the full pre-push check: `:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`.

## Decisions

- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Generalize or revert this at merge time.
- **First push carries three commits:** setup, version catalog and server. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't pass until `:server` and the shared host-test task exist.
- **Version catalog in step 4, not 5.0:** `gradle/libs.versions.toml` now holds every §3.1 version, and all build files use it. The server was the first module to need new versions.
- **Host tests enabled in step 4:** `withHostTest {}` went in early so `:shared:testAndroidHostTest` exists for the pre-push check. With no test sources it reports NO-SOURCE.
- **Tier skips (`:server`):** `--mode=slow` (SHOULD) and the Python fallback server (COULD) aren't built. `CLAUDE.md` still lists `slow`, but the server rejects it.
- **Server bytecode targets JVM 17**, so it also runs on Android Studio's bundled JDK on Windows. `slf4j-simple` is there only so Ktor's startup lines print instead of an SLF4J warning.
- **Working ahead:** while a step's CI runs, I draft the next step in the working tree. It's only committed and pushed once the current step is green.

## Failures and fixes

None yet.
