# Kitchen Prep Board — Native Jetpack port

This branch is the native Android implementation. The HTML files under `compiler-input/` are retained only as visual/design reference and are not packaged into the app.

Runtime architecture:
- Jetpack Compose UI only; no WebView, JavaScript bridge, or HTML runtime.
- DataStore-backed local state; fresh installs start with zero user boards and zero active tasks.
- Native Android navigation, system insets, timers/alarms, per-app locales, AdMob test banner, UMP, and Play Billing.
- One shared Kitchen Prep Board logo resource is used by launcher, phone top bars, and tablet rail.
- One native board-art model is reused everywhere a board appears, so Home and Boards cannot drift to different images for the same board.
- APK workflow is manual-only. Do not run it until explicitly requested.
