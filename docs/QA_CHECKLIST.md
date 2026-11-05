# QA checklist

Run on Android (Windows emulator, by hand) and iOS (CI XCUITest plus screenshots; PLAN §8.3 maps test classes to IDs).
Record results in `docs/QA_ANDROID.md` / `docs/QA_IOS.md` as: `ID | pass/fail | commit | note`.

**Tiers.** Every MUST item has to pass on both apps before any SHOULD or COULD work starts (PLAN §0). SHOULD and COULD items are checked only if they were built.

**Expected numbers** come from `fixtures/flights.json` with the server in `--mode=normal` and default filters, unless an item says otherwise. On iOS, the smoothness items (L3, M2, F9) are judged from the demo recording.

## API
- A1 [MUST] `curl -i http://localhost:8080/flights` → `200`, `Content-Type: application/json`, 1,000 flights. `/health` → `ok`.
- A2 [MUST] The single documented command starts the API on each machine: `./gradlew :server:run` (Mac/CI) and `gradlew.bat :server:run` (Windows).

## Launch and loading
- L1 [MUST] Fresh install, server running → loading state, then all 1,000 flights; the count shows 1,000.
- L2 [MUST] Default filters cover the full date range (Feb 1 – Apr 1, 2027); no filter chips are shown.
- L3 [MUST] Fling the full 1,000-row list from top to bottom and back → smooth, no blank rows, no stutter.
- L4 [SHOULD] The first load (fresh install, `--mode=slow`) shows grey placeholder rows in the list's shape instead of a spinner, then the real rows, with no jump in the layout.

## Search
- S1 [MUST] Origin `  jfk ` → 113 flights, all departing JFK (trim + case-insensitive).
- S2 [MUST] Origin `new york` → the same 113 JFK flights (city match).
- S3 [MUST] Destination `TOKYO` → 110 flights, to both HND and NRT.
- S4 [MUST] Origin `jfk` + destination `tokyo` → 7 flights (AND of both).
- S5 [MUST] Origin `xyz` → a "no matching flights" empty state with Reset, not an error.
- S6 [SHOULD] Search rule:
  - Destination `dub` → 29 (Dublin only, not Dubai); `du` → 69 (Dublin + Dubai).
  - Origin `fra` → no matches; `fran` → 123 (San Francisco).

## Filters
- F1 [MUST] Date range Feb 1 → Feb 1 → 17 flights, all on Feb 1 (start and end inclusive).
- F2 [MUST] Date range Apr 1 → Apr 1 → 21 flights (the end date is included).
- F3 [MUST] Max points 9,000 → exactly 1 flight (LAX → MEX, Feb 18, 9,000 pts). Max 8,500 → no matches.
- F4 [MUST] Cabin Business → 344 flights. Cabin Economy → 467, with no Premium Economy flights included.
- F5 [MUST] Programs Avios + Aeroplan → 386 flights, each from one of the two (OR).
- F6 [MUST] Cabin Business + program Avios + max 60,000 → 32 flights (AND across all three).
- F7 [MUST] Active filters are visible as chips/summary; the result count updates with every change.
- F8 [MUST] Reset → search fields and every filter back to defaults; count 1,000.
- F9 [MUST] Typing fast or changing filters quickly → the final results match the last input; no jank. Drag the points slider fast back and forth: the thumb follows the finger and never jumps back.
- F10 [MUST] Select a program, then deselect it → all programs are included again (1,000). "Mileage Plan" (203) and "MileagePlus" (199) are separate options.
- F11 [SHOULD] Each chip's × removes only its own filter. From `jfk` + Business gives 35. × on Business gives 113, and From still says jfk. × on From empties the field, and all 1,000 show.
- F12 [SHOULD] −/+ beside the points value move the limit 500 at a time. From "No limit", − gives "Up to 132,500 pts" (999 flights), and + gives "No limit" again (1,000). At 9,000, − is disabled.

## List / map / selection
- M1 [MUST] The map shows one marker per destination airport with count + lowest points, matching the current filters.
  - 24 markers at defaults.
  - Tokyo shows two markers: HND "55 · 16.5k" and NRT "55 · 19.5k".
- M2 [MUST] Pan and zoom are smooth.
- M3 [MUST] Tap a marker → browse that destination's flights → pick one → route line + flight card.
- M4 [MUST] The card shows origin, destination, date, cabin, points, program and Save/Remove.
- M5 [MUST] Select on the list → switch to the map → the same flight is still selected, and the reverse.
- M6 [MUST] Change filters so the selected flight is excluded → the selection is cleared on both views.
- M7 [MUST] The list and the map always agree on the result set (marker counts add up to the result count).
- M8 [MUST] Select a trans-Pacific flight (e.g. SFO → NRT or JFK → SYD) → one continuous arc over the Pacific, never a line across the whole map.
- M9 [SHOULD] After selecting, the camera frames the whole route, including M8's routes.
- M10 [SHOULD] Switch to Map, open the Saved tab, come back to Explore: it's still on Map.

## Saved
- V1 [MUST] Save from the card → it appears in the Saved tab; the saved state shows on list rows and on the map.
- V2 [MUST] Saving again updates without duplicates:
  1. Save a flight.
  2. Restart the server with `--mode=updated`, then pull to refresh.
  3. The card shows "Update saved". Tap it.
  4. The Saved tab shows the new points (+500) and still exactly one entry.
- V3 [MUST] The Saved tab is unaffected by the current discovery filters.
- V4 [MUST] Remove works from the Saved tab and from the card.
- V5 [MUST] A saved flight absent from the latest API response (`--mode=empty`, then refresh) → still in Saved.
- V6 [MUST] Simulated save failure and remove failure (Android debug menu; iOS `-failSaves` / `-failRemoves`) → an error is shown and the state rolls back.

## Offline / errors
- O1 [MUST] Stop the server → pull to refresh → existing data kept + stale/offline indicator.
- O2 [MUST] Stop the server → force-quit the app → reopen → cached results + saved flights + stale indicator.
- O3 [MUST] Offline: search and filter cached results; view saved flights; remove a saved flight.
- O4 [MUST] Clear app data (Android "Clear storage"; iOS `-resetData`) → launch with the server stopped → understandable error + Retry. Start the server → Retry works. (iOS CI uses `--mode=error`, then `normal`, through the control endpoint.)
- O5 [MUST] `--mode=empty` on a fresh install → "No flights available", visibly different from the O4 error.
- O6 [SHOULD] `--mode=slow` → the loading indicator shows for ~3 s; the UI stays responsive.
- O7 [MUST] Rotate the device / background and foreground the app → no crash, state preserved.
- O8 [SHOULD] Offline, Map tab → markers and the selected route still render. A blank background is fine; tiles aren't required.
- O9 [SHOULD] Android, Developer options → "Don't keep activities" on. Search From `jfk`, press Home, then return. The From field and the results agree: the field is empty and all flights show. Turn the option off afterwards.

## Polish
- P1 [MUST] Spacing, typography and hierarchy are consistent across list rows, the card, sheets and the Saved tab.
- P2 [MUST] Every tap gives visible feedback. Save/Remove show progress and can't be double-submitted.
- P3 [COULD] Dark mode: everything is legible, including the map and markers.
- P4 [COULD] Largest system font size: rows and the card don't truncate the route, date or points.
- P5 [COULD] TalkBack / VoiceOver read the route, date, points and saved state.
- P6 [SHOULD] The launcher (Android) and home screen (iOS) show the app as "Flight Explorer".
- P7 [SHOULD] Haptics, on a real phone only (emulators and simulators have none): Save and Update saved give a firm tap, Remove (card or Saved tab) a light one. A failure message, with debug "Fail saves" on, gives an error buzz.

## Tooling (debug builds)
- T1 [SHOULD] StrictMode (Android): no disk or network access on the main thread in logcat during a full pass. Debug builds enable it at startup (`FlightExplorerApplication`) and only log. Watch `adb logcat -s StrictMode`, and file `qa` issues for stack traces in `travel.roame.takehome`; ones only inside MapLibre or the framework are worth a note.
- T2 [COULD] LeakCanary (Android): no leaks after rotating, switching tabs and opening and closing the map.
- T3 [SHOULD] Debug menu → Server URL (Android ⋮; iOS ladybug, top left):
  - Enter the computer's LAN address, such as `http://192.168.1.20:8080`: the flights load from it.
  - Relaunch: the same address is still used.
  - An invalid entry (`ftp://x`, `host:70000`) says what's wrong and can't be saved.
  - "Use default" goes back to the build's address.
  - On a physical device, allow Local Network access when asked.
- T4 [SHOULD] Test tags (Android): run `adb shell uiautomator dump /sdcard/ui.xml` and `adb pull /sdcard/ui.xml`.
  - On Explore, the file has `resource-id="search.origin"`, `results.count` and `tab.saved`.
  - With the filter sheet open, it also has `filters.done` and `filters.cabin.any`.
