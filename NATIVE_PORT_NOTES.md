# Kitchen Prep Board — native Android implementation

The Android runtime is now Jetpack Compose. The files under `compiler-input/` remain only as the frozen design/localization source: Gradle extracts the approved localization dictionary and launcher icon at build time, but the HTML itself is not packaged or executed.

Runtime architecture:
- Jetpack Compose + Material 3 adaptive navigation; no WebView, JavaScript bridge, or HTML runtime.
- Room persists boards and tasks; DataStore persists user settings. A fresh install starts with zero boards and zero tasks.
- Android share text and Paste both become editable native tasks before a board is created.
- Timers persist deadlines and restore background alerts after process death/reboot; timer expiry requests attention and never marks a task complete.
- The approved Kitchen Prep Board launcher image is also the native top-bar logo.
- The app is intentionally light-mode only: Compose, AppCompat and system bars are all locked to the approved light palette.
- One shared native `BoardArt` component is used everywhere a board visual appears, preventing Home/Boards drift.
- UMP gates ad requests; subscribers do not initialize the banner. Eligible free users receive a native non-personalized anchored-adaptive banner only after it loads.
- Google Play Billing remains the only paywall surface and only removes ads; core app features remain available.

Responsive architecture:
- Width and height are evaluated together. A short phone in landscape never becomes a tablet merely because its width is large.
- Compact/landscape mode reduces vertical spacing and hero height; it does not shrink live-task text.
- `>=600dp` uses the existing medium-width side-by-side hero treatment.
- `>=840dp` with non-compact height preserves the existing tablet rail/two-pane/four-lane treatment.
- Very wide content is capped and centered instead of stretching indefinitely.
- Touch actions retain 48–52dp minimum targets and translated labels may wrap rather than being globally scaled down.

Build policy:
- `verify-android.yml` compiles resources, manifest, Room/KSP and Kotlin without creating an APK.
- `build-apk.yml` is manual (`workflow_dispatch`) only. No APK build is started by these source changes.
