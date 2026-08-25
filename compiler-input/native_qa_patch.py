from pathlib import Path

main_path = Path("app/src/main/java/studio/gooduse/kitchenprep/MainActivity.kt")
main = main_path.read_text(encoding="utf-8")
main = main.replace(
    "import androidx.core.view.WindowInsetsCompat\nimport androidx.core.view.WindowInsetsControllerCompat\n",
    "import androidx.core.view.WindowCompat\n",
)
main = main.replace(
    "        // The approved HTML contains its own platform-status chrome. Hiding the real\n"
    "        // status bar prevents duplicate chrome and preserves the frozen review layout.\n"
    "        WindowInsetsControllerCompat(window, window.decorView)\n"
    "            .hide(WindowInsetsCompat.Type.statusBars())\n",
    "        // Keep the real Android system bars visible and let Compose reserve safe insets.\n"
    "        // This prevents the app navigation from colliding with gesture/3-button navigation.\n"
    "        WindowCompat.setDecorFitsSystemWindows(window, false)\n",
)
main_path.write_text(main, encoding="utf-8")

app_path = Path("app/src/main/java/studio/gooduse/kitchenprep/KitchenPrepClosedTestApp.kt")
app = app_path.read_text(encoding="utf-8")
app = app.replace(
    "import android.content.ActivityNotFoundException\nimport android.content.Intent\n",
    "import android.content.ActivityNotFoundException\nimport android.content.ClipboardManager\nimport android.content.Context\nimport android.content.Intent\n",
)
app = app.replace(
    "        WindowInsetsControllerCompat(activity.window, activity.window.decorView)\n"
    "            .isAppearanceLightNavigationBars = !dark\n",
    "        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {\n"
    "            isAppearanceLightStatusBars = !dark\n"
    "            isAppearanceLightNavigationBars = !dark\n"
    "        }\n",
)
app = app.replace(
    "            BoxWithConstraints(Modifier.fillMaxSize()) {",
    "            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {",
)
needle = "    @JavascriptInterface\n    fun clearNativeTimerState() {\n        timerScheduler.clearAll()\n    }\n"
replacement = "    @JavascriptInterface\n    fun getClipboardText(): String {\n        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return \"\"\n        val clip = clipboard.primaryClip ?: return \"\"\n        if (clip.itemCount == 0) return \"\"\n        return clip.getItemAt(0).coerceToText(activity)?.toString().orEmpty()\n    }\n\n" + needle
if needle not in app:
    raise SystemExit("NativeBridge insertion point not found")
app = app.replace(needle, replacement)
app_path.write_text(app, encoding="utf-8")

print("native QA patch applied")
