# Road GPS Survey

Android field-survey tracker for road and civil-engineering work. The app records accepted GNSS fixes in metres, displays distance/STA/active time/accuracy/speed, persists surveys with Room, and continues location tracking in a foreground service when the Activity is not visible.

## Key behavior

- Tracking states are `IDLE`, `TRACKING`, `PAUSED`, and `FINISHED`.
- `PAUSE + MARK` obtains a current high-accuracy fix, persists `POINT_###`, then pauses distance and active time.
- `RESUME` requires a fresh fix and uses it as a new origin. No distance is calculated across a paused gap.
- Active time uses `SystemClock.elapsedRealtime()`, with five-second persistent checkpoints.
- Accepted fixes require usable accuracy, the configured maximum accuracy, meaningful movement, chronological order, and a plausible displacement/time relationship.
- Distance is stored as `Double` metres; STA is calculated independently of the UI from `startChainageM + totalDistanceM`.
- Completed surveys export to CSV, GPX, or KMZ through Android's document picker.
- History offers HAPUS for finished ruas with confirmation. Deletion removes the ruas and its recorded track samples, but preserves all marked points with their original ruas name. Active and paused surveys are protected. Previously exported files are unaffected.
- The dashboard fills the available screen with stable metric-card sizes, equal secondary cards, and single-line fitted values. Landscape uses a wider layout; small windows allow metric scrolling while keeping recording controls visible.

## Version 1.0.5 workflow

- UI, notifications, floating controls, and messages use Bahasa Indonesia. Android-owned permission and file-picker dialogs follow the device configuration.
- MULAI starts recording; TANDAI saves a marked point without pausing time or distance. JEDA + TITIK retains the existing pause-and-mark operation; LANJUT resumes it. The title/notification/overlay display the saved marker count.
- SELESAI stops recording and persists the data before asking for Nama Ruas. Enter a name or explicitly choose NAMA OTOMATIS, then select CSV, GPX, KMZ, CSV/GPX/KMZ together, or NANTI. The filename uses the ruas name. Multiple formats use sequential document pickers. Cancelling export keeps internal survey data.
- Pending naming/export prompts are stored in Room and return on reopening. Finishing through the notification also leaves a completion notification linking back to the app.
- KMZ export opens directly in Google Earth-style viewers with the recorded road track as a LineString and saved points as Placemarks. Saved points keep their original recorded GNSS coordinates and are not snapped to the road line.
- Saved points from a finished ruas can be deleted independently without deleting the road survey or its GPX/track data first. Markers belonging to an active recording remain protected until the survey is finished.
- RIWAYAT → TITIK TERSIMPAN displays marked points, including those whose ruas was deleted; their name, note, type, WGS84 coordinates, original STA, and original ruas name remain available.
- Day mode is white with black text from 06:00 through 17:59; night mode is black with white text from 18:00 through 05:59, using the phone's local clock/timezone. This applies to all screens and the floating panel, and updates while open. Empty details explicitly show “Belum ada titik penanda.”
- Database version 2 migrates existing version-1 data without destructive recreation; marked points use a nullable ruas association so deletion no longer cascades to markers.

## Filtering assumptions

Defaults exposed in Settings:

- GPS interval: `1 s`
- Maximum accepted horizontal accuracy: `10 m`
- Minimum movement: `2 m`
- Starting STA: `0+000`

The filter also uses an uncertainty-aware movement floor and a documented `250 km/h` operational ceiling to reject impossible GNSS jumps for this road-survey use case. That ceiling is a data-quality guard, not a roadway design-speed value.

## Permissions

- Fine/coarse location: GNSS acquisition. Fine location is preferred; coarse permission alone may not provide survey-quality fixes.
- Foreground service/location: keeps an active survey running with the screen off or another app open.
- Notifications (Android 13+): shows ongoing survey status and pause/resume/finish actions.
- Display over other apps: requested only when the user enables the optional floating tracker.

Background-location permission is not requested; tracking is performed by a visible location foreground service started while the app is in use.

## Build

The repository includes a Gradle wrapper and a project-local JDK/Android SDK under the ignored `.tooling` directory on the prepared workstation.

```powershell
$env:JAVA_HOME = 'D:\DEV\GPS-APK\.tooling\jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME = 'D:\DEV\GPS-APK\.tooling\android-sdk'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
```

The installable, debug-signed APK is generated at:

`app/build/outputs/apk/debug/app-debug.apk`

Release builds are signed locally when the ignored signing.properties and .signing/roadgps-release.jks files are present. Keep the keystore and credentials private and backed up; they are never committed to Git.

## Verification scope

Automated tests cover STA formatting, distance accumulation, mark-only continuity, pause/resume gap exclusion, GNSS filtering, monotonic active-time accumulation/checkpointing, and marker numbering. Robolectric tests exercise the real version-1 database migration, marker preservation and active-session protection, persisted finish prompts, naming/export choices, day/night empty-detail rendering, and portrait/landscape/small-window dashboard geometry. Lint, debug assembly, and R8-minified release assembly are also part of verification.

The real-device field scenario still needs validation on the intended handset because GNSS chipset behavior, Android vendor battery policy, notification presentation, and overlay behavior cannot be proven by JVM tests alone.
