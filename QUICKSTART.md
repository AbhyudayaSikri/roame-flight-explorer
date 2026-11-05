# Flight Explorer: quick start

Try both apps without building anything. A release has these files:

| File | What it is |
|---|---|
| `roame-flights-server.jar` | The local flights API (needs Java 17 or newer) |
| `fallback.py`, `flights.json` | The same API in Python 3.9+, for machines without Java |
| `FlightExplorer-android-debug.apk` | The Android app (debug build) |
| `FlightExplorer-ios-simulator.zip` | The iOS app for the iOS Simulator on an Apple silicon Mac |

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
