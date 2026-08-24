package studio.gooduse.kitchenprep

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import studio.gooduse.kitchenprep.monetization.BillingController
import studio.gooduse.kitchenprep.monetization.ConsentController
import studio.gooduse.kitchenprep.monetization.NativeTestBanner
import studio.gooduse.kitchenprep.timers.TimerScheduler

private const val ASSET_URL = "file:///android_asset/kitchen_prep_board.html"

@Composable
fun KitchenPrepClosedTestApp(
    activity: MainActivity,
    billingController: BillingController,
    consentController: ConsentController,
) {
    val billing by billingController.state.collectAsState()
    val consent by consentController.state.collectAsState()
    val dark = isSystemInDarkTheme()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val timerScheduler = remember { TimerScheduler(activity.applicationContext) }

    SideEffect {
        activity.window.navigationBarColor = if (dark) AndroidColor.rgb(18, 22, 17) else AndroidColor.rgb(245, 240, 232)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            .isAppearanceLightNavigationBars = !dark
    }

    val bridge = remember(activity, billingController, consentController, timerScheduler) {
        NativeBridge(activity, billingController, consentController, timerScheduler)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = if (dark) Color(0xFF121611) else Color(0xFFF5F0E8)) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val tablet = maxWidth >= 840.dp

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(if (dark) AndroidColor.rgb(18, 22, 17) else AndroidColor.rgb(245, 240, 232))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.mediaPlaybackRequiresUserGesture = true
                            settings.allowContentAccess = false
                            settings.allowFileAccess = true
                            settings.setSupportMultipleWindows(false)
                            addJavascriptInterface(bridge, "AndroidBridge")
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url ?: return false
                                    if (uri.scheme == "https") {
                                        bridge.openExternal(uri.toString())
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    injectNativeHooks(view)
                                    applyPreviewState(view, dark)
                                    applyNativeState(view, billing.active, billing.verifiedThisSession, billing.formattedPrice, billing.billingPeriod, billing.lastError)
                                    webView = view
                                }
                            }
                            loadUrl(ASSET_URL)
                            webView = this
                        }
                    },
                    update = { view ->
                        view.setBackgroundColor(if (dark) AndroidColor.rgb(18, 22, 17) else AndroidColor.rgb(245, 240, 232))
                        applyPreviewState(view, dark)
                        applyNativeState(view, billing.active, billing.verifiedThisSession, billing.formattedPrice, billing.billingPeriod, billing.lastError)
                    },
                )

                // Real Google test inventory overlays the exact reserved ad rail from
                // the frozen HTML. The placeholder remains visible until a test ad
                // actually loads, avoiding layout shift or a blank rail.
                if (billing.verifiedThisSession && !billing.active && consent.canRequestAds) {
                    val railModifier = if (tablet) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(start = 104.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                            .height(50.dp)
                    }
                    NativeTestBanner(modifier = railModifier)
                }
            }
        }
    }

    BackHandler {
        val wv = webView
        if (wv == null) {
            activity.finish()
        } else {
            wv.evaluateJavascript("Boolean(window.kpbNativeBack && window.kpbNativeBack())") { result ->
                if (result != "true") activity.finish()
            }
        }
    }
}

private fun applyPreviewState(view: WebView, dark: Boolean) {
    val theme = if (dark) "dark" else "light"
    view.evaluateJavascript(
        "if(window.applyPreviewState){window.applyPreviewState({theme:'$theme',platform:'android'});}",
        null,
    )
}

private fun applyNativeState(
    view: WebView,
    active: Boolean,
    verified: Boolean,
    price: String?,
    period: String?,
    error: String?,
) {
    val safePrice = price.jsQuoted()
    val safePeriod = period.jsQuoted()
    val safeError = error.jsQuoted()
    view.evaluateJavascript(
        "if(window.kpbNativeApply){window.kpbNativeApply(${active},${verified},$safePrice,$safePeriod,$safeError);}",
        null,
    )
}

private fun String?.jsQuoted(): String {
    if (this == null) return "null"
    return buildString {
        append('\'')
        for (c in this@jsQuoted) {
            when (c) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
        append('\'')
    }
}

private fun injectNativeHooks(view: WebView) {
    view.evaluateJavascript(NATIVE_HOOKS_JS, null)
}

private val NATIVE_HOOKS_JS = """
(function(){
  if(window.__kpbNativeHooksInstalled) return;
  window.__kpbNativeHooksInstalled=true;

  window.open=function(url){
    try{ if(window.AndroidBridge && url) AndroidBridge.openExternal(String(url)); }catch(_){}
    return null;
  };

  var subscriptionButtons=Array.prototype.slice.call(document.querySelectorAll('[data-release-link="subscription"]'));
  if(subscriptionButtons[0]) subscriptionButtons[0].addEventListener('click',function(e){
    e.preventDefault(); e.stopImmediatePropagation();
    try{ AndroidBridge.purchaseRemoveAds(); }catch(_){}
  },true);
  if(subscriptionButtons[1]) subscriptionButtons[1].addEventListener('click',function(e){
    e.preventDefault(); e.stopImmediatePropagation();
    try{ AndroidBridge.manageSubscription(); }catch(_){}
  },true);

  Array.prototype.forEach.call(document.querySelectorAll('[data-privacy-choices]'),function(btn){
    btn.addEventListener('click',function(e){
      e.preventDefault(); e.stopImmediatePropagation();
      try{ AndroidBridge.showPrivacyOptions(); }catch(_){}
    },true);
  });

  var deleteConfirm=document.querySelector('[data-delete-confirm]');
  if(deleteConfirm) deleteConfirm.addEventListener('click',function(){
    try{ AndroidBridge.clearNativeTimerState(); }catch(_){}
  },true);

  var originalSave=window.saveSession;
  if(typeof originalSave==='function'){
    window.saveSession=function(){
      var result=originalSave.apply(this,arguments);
      try{ AndroidBridge.syncSession(JSON.stringify(window.kpbSessionState||{})); }catch(_){}
      return result;
    };
  }

  var lastNativeError=null;
  window.kpbNativeApply=function(active,verified,price,period,error){
    var ad=document.querySelector('.ad-reservation');
    var style=document.getElementById('kpb-native-sub-style');
    if(active){
      if(!style){
        style=document.createElement('style');
        style.id='kpb-native-sub-style';
        style.textContent='.ad-reservation{display:none!important}.shell{padding-bottom:92px!important}@media(min-width:840px){.shell{padding-bottom:24px!important}}';
        document.head.appendChild(style);
      }
      if(ad) ad.style.visibility='hidden';
    }else{
      if(style) style.remove();
      if(ad) ad.style.visibility=verified?'visible':'hidden';
    }

    if(error && error!==lastNativeError){
      lastNativeError=error;
      try{ if(typeof window.showReleaseToast==='function') window.showReleaseToast(error); }catch(_){}
    }

    if(subscriptionButtons[0]){
      subscriptionButtons[0].dataset.nativeActive=active?'1':'0';
      var small=subscriptionButtons[0].querySelector('small');
      if(small && price){
        var first=small.querySelector('span');
        if(first) first.textContent=price;
      }
    }
  };

  window.kpbNativeBack=function(){
    var active=document.querySelector('.screen.active');
    if(!active || active.id==='screen-home') return false;
    if(typeof window.setView==='function'){ window.setView('home'); return true; }
    return false;
  };

  try{ AndroidBridge.syncSession(JSON.stringify(window.kpbSessionState||{})); }catch(_){}
})();
""".trimIndent()

class NativeBridge(
    private val activity: MainActivity,
    private val billingController: BillingController,
    private val consentController: ConsentController,
    private val timerScheduler: TimerScheduler,
) {
    @JavascriptInterface
    fun openExternal(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.scheme != "https") return
        activity.runOnUiThread {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                // No external browser available. The app remains usable.
            }
        }
    }

    @JavascriptInterface
    fun purchaseRemoveAds() {
        activity.runOnUiThread { billingController.purchase(activity) }
    }

    @JavascriptInterface
    fun manageSubscription() {
        activity.runOnUiThread { billingController.openManageSubscription(activity) }
    }

    @JavascriptInterface
    fun showPrivacyOptions() {
        activity.runOnUiThread { consentController.showPrivacyOptions(activity) }
    }

    @JavascriptInterface
    fun syncSession(json: String) {
        val state = timerScheduler.syncSession(json)
        activity.runOnUiThread {
            if (state.keepAwake) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            if (state.shouldPromptNotifications && android.os.Build.VERSION.SDK_INT >= 33) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(activity, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val prefs = activity.getSharedPreferences("kpb_native", android.content.Context.MODE_PRIVATE)
                    if (!prefs.getBoolean("notification_prompted", false)) {
                        prefs.edit().putBoolean("notification_prompted", true).apply()
                        ActivityCompat.requestPermissions(activity, arrayOf(permission), 4102)
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun clearNativeTimerState() {
        timerScheduler.clearAll()
    }
}
