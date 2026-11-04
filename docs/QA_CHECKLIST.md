# QA checklist

Run on Android (Windows emulator, by hand) and iOS (CI XCUITest + screenshots).
Record results in `docs/QA_ANDROID.md` / `docs/QA_IOS.md` as: `ID | pass/fail | commit | note`.

## Launch and loading
- L1 Fresh install, server running → loading state, then all 1,000 flights, count shows 1,000.
- L2 Default filters show the full date range (Feb 1 – Apr 1 2027); no other filters are active.

## Search
- S1 Origin `  jfk ` → only JFK departures (trim + case-insensitive).
- S2 Origin `new york` → JFK flights (city match).
- S3 Destination `TOKYO` → both HND and NRT flights.
- S4 Origin + destination together → AND of both.
- S5 Search with no matches → "no results for these filters" empty state (not an error).

## Filters
- F1 Date range Feb 1 → Feb 1 → only Feb 1 flights (start and end inclusive).
- F2 Date range ending Apr 1 → Apr 1 flights included.
- F3 Max points set exactly to a known flight's price → that flight included.
- F4 Cabin = Business → only Business.
- F5 Programs Avios + Aeroplan → flights from either program (OR).
- F6 Cabin + program + points together → AND across all three.
- F7 Active filters visible as chips/summary; result count updates.
- F8 Reset → back to defaults, count 1,000.
- F9 Typing fast or changing filters quickly → final results match the last input; no jank.

## List / map / selection
- M1 Map shows one marker per destination airport with count + lowest points that match the current filters.
- M2 Pan and zoom are smooth.
- M3 Tap a marker → browse that destination's flights → pick one → route line + flight card.
- M4 Card shows origin, destination, date, cabin, points, program, Save/Remove.
- M5 Select on list → switch to map → same flight still selected, and the reverse.
- M6 Change filters so the selected flight is excluded → selection cleared on both views.
- M7 List and map always agree on the result set.

## Saved
- V1 Save from card → appears in Saved tab; saved state shown on list and map.
- V2 Save the same flight again → no duplicate; details updated.
- V3 Saved tab unaffected by current discovery filters.
- V4 Remove from Saved tab and from card.
- V5 Saved flight absent from latest API response (`--mode=empty`, then refresh) → still in Saved.
- V6 Simulated save/remove failure (debug toggle) → error shown, state rolled back.

## Offline / errors
- O1 Stop server → pull to refresh → existing data kept + stale/offline indicator.
- O2 Stop server → force-quit app → reopen → cached results + saved flights + stale indicator.
- O3 Offline: search and filter cached results; view saved; remove a saved flight.
- O4 Uninstall/clear data → launch with server stopped → understandable error + Retry; start server → Retry works.
- O5 `--mode=empty` on fresh install → "no flights available" state, distinct from the O4 error.
- O6 `--mode=slow` → loading indicator visible; UI stays responsive.
- O7 Rotate the device / background and foreground the app → no crash, state preserved.
