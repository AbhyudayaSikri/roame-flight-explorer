# Flight Explorer: quick start

Try both apps without building anything. A release has these files:

| File | What it is |
|---|---|
| `roame-flights-server.jar` | The local flights API (needs Java 17 or newer) |
| `fallback.py`, `flights.json` | The same API in Python 3.9+, for machines without Java |
| `FlightExplorer-android-debug.apk` | The Android app (debug build) |
| `FlightExplorer-ios-simulator.zip` | The iOS app for the iOS Simulator on an Apple silicon Mac |

The v1.0.0 release also has the two walkthrough videos below, and a ZIP of the repository.

## Walkthrough videos
Video 1: Android (Jetpack Compose), recorded by hand on an emulator. Video 2: iOS (SwiftUI), recorded automatically on a CI simulator by a slow-paced XCUITest walkthrough, because I verified iOS through CI rather than on a local Xcode install.

**Video 1: [`1-android-walkthrough.mp4`](https://github.com/AbhyudayaSikri/roame-flight-explorer/releases/download/v1.0.0/1-android-walkthrough.mp4)** (5:31, with narration)
- 0:00 Intro, on the emulator's home screen
- 1:00 Launch: placeholder rows while the flights load, then all 1,000 (1:25)
- 1:45 Map: each airport's flight count and lowest points, with nearby airports merged into one pill
- 2:00 Map selection: Lisbon's flights, then YYZ → LIS with its route and card (2:05)
- 2:10 Saving: "Saved · Remove", the Saved badge and an orange pill, then the Saved tab (2:15)
- 2:25 Search: "new york" gives 113, and adding To "tokyo" gives 7 (2:45)
- 2:50 The map frames Tokyo's two airports. JFK → NRT is selected and saved, with its route over the North Pacific (2:57)
- 3:04 Filters: Business, each chip's ×, the points limit with a live count, and two programs combined (3:35)
- 3:46 Relaunch with the emulator's Wi-Fi and mobile data off (3:56): force-quit (4:04), reopen (4:08), and the saved flights are kept (4:36)
  - The API stays reachable here through `adb reverse`, so the offline banner doesn't appear.
  - Video 2 shows the relaunch with the API stopped.
- 4:42 The map with the emulator offline: Tokyo's NRT and HND pills (5:00)
- 5:05 Debug menu: a failed save (5:13) and a failed remove (5:23), each with a message and rolled back

**Video 2: [`2-ios-walkthrough-ci.mp4`](https://github.com/AbhyudayaSikri/roame-flight-explorer/releases/download/v1.0.0/2-ios-walkthrough-ci.mp4)** (1:11, no sound)
- 0:00 First load: placeholder rows, then 1,000 flights (0:06)
- 0:10 Search: From "new york" gives 113, then To "tokyo"
- 0:18 Filters: the sheet's live count, 2 with Business (0:24)
- 0:30 Search "hnd": 3 flights
- 0:34 Map selection: the HND pill and its flights (0:40), then JFK → HND with its route and card (0:44)
- 0:50 Saving: "Saved · Remove" and the Saved badge, then the Saved tab (0:53)
- 0:54 Relaunch with the API stopped: the cached flights under "Offline · showing results from …" (0:56)
- 1:04 Offline search: "jfk" gives 113 (1:06)
- 1:08 The saved flight, still in the Saved tab offline

## 1. Start the API

```sh
java -jar roame-flights-server.jar
```

It serves `http://localhost:8080/flights`, and leaves the terminal running until Ctrl+C. Options:
- `--port=8080`
- `--mode=normal|empty|error|updated|slow`: `slow` answers 3 s late, `error` fails, `empty` has no flights, and `updated` adds 500 points to every flight.

Without Java, put `fallback.py` and `flights.json` in one folder and run `python3 fallback.py` (Windows: `py fallback.py`). It takes the same options.

On Windows, allow Java or Python through the firewall (private networks) if asked.

## 2a. Android emulator
1. Start an emulator in Android Studio (Device Manager).
2. Drag `FlightExplorer-android-debug.apk` onto the emulator window, or run `adb install FlightExplorer-android-debug.apk`.
3. Open Flight Explorer. The emulator reaches your computer as `10.0.2.2`, which is the app's default address, so the flights load.
4. If it says it can't reach the flights server, while `http://localhost:8080/health` opens on your computer, the emulator isn't reaching `10.0.2.2`:
   1. Run `adb reverse tcp:8080 tcp:8080`.
   2. In the app: ⋮ → **Server URL…** → `http://localhost:8080` → **Save**.
   3. Run the `adb reverse` command again after the emulator restarts.

## 2b. Android phone
1. Copy the APK to the phone and open it. Allow installing apps from that source when asked.
2. Put the phone on the same Wi-Fi as the computer running the API.
3. In the app: ⋮ (top right) → **Server URL…** → enter `http://<your computer's address>:8080` → **Save**.

## 2c. iOS Simulator (Mac with Xcode)
1. Unzip `FlightExplorer-ios-simulator.zip` to get `RoameStarter.app`.
2. Start a simulator: `open -a Simulator`, then pick an iPhone.
3. Install and open the app:
   ```sh
   xcrun simctl install booted RoameStarter.app
   xcrun simctl launch booted travel.roame.takehome.ios
   ```
   The simulator reaches the Mac as `localhost`, the app's default address, so the flights load.

The zip can't be installed on a physical iPhone:
- A simulator build is compiled for the iOS Simulator, which runs on the Mac. An iPhone needs code built for the iPhone itself.
- iOS installs only apps signed with a provisioning profile that includes that device (development, ad hoc, TestFlight or the App Store). This build isn't signed for any device.
- To run on an iPhone, build from source in Xcode with your own signing team. See README.md, "Run the iOS app".

## 3. When the defaults don't fit
If the app can't reach the API, for example on a phone or with the API on another computer, set its address in the debug menu:
- **Android:** ⋮ → Server URL…
- **iOS:** the ladybug, top left.

The address is checked as you type and kept across launches. "Use default" goes back to the build's address.

To find your computer's address on the network:
- macOS: `ipconfig getifaddr en0`
- Windows: `ipconfig`, then "IPv4 Address"
- Linux: `hostname -I`

The API listens on every interface.
