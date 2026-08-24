package studio.gooduse.kitchenprep.monetization

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import studio.gooduse.kitchenprep.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

private object MobileAdsBootstrap {
    private val initialized = AtomicBoolean(false)

    fun ensure(context: Context, onReady: () -> Unit) {
        if (initialized.get()) {
            onReady()
            return
        }
        MobileAds.setRequestConfiguration(
            MobileAds.getRequestConfiguration().toBuilder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build()
        )
        MobileAds.initialize(context.applicationContext) {
            initialized.set(true)
            onReady()
        }
    }
}

/**
 * Closed-testing only: loads Google's official demo banner ID. The HTML's fixed
 * 50dp rail remains the geometry source of truth, so the real test ad cannot shift
 * the approved layout.
 */
@Composable
fun NativeTestBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var retryAttempt by remember { mutableIntStateOf(0) }
    var retryJob by remember { mutableStateOf<Job?>(null) }

    val adView = remember {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.BANNER)
        }
    }

    DisposableEffect(adView) {
        var disposed = false

        fun load() {
            if (disposed) return
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                    retryAttempt = 0
                    retryJob?.cancel()
                    retryJob = null
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loaded = false
                    retryJob?.cancel()
                    retryJob = null
                    val nonRetriable = error.code == AdRequest.ERROR_CODE_NO_FILL ||
                        error.code == AdRequest.ERROR_CODE_INVALID_REQUEST
                    if (nonRetriable || retryAttempt >= 3 || disposed) return
                    val delayMs = (2_000L * (1L shl retryAttempt)).coerceAtMost(30_000L)
                    retryAttempt++
                    retryJob = scope.launch {
                        delay(delayMs)
                        if (!disposed) load()
                    }
                }
            }
            adView.loadAd(AdRequest.Builder().build())
        }

        MobileAdsBootstrap.ensure(context) { if (!disposed) load() }

        onDispose {
            disposed = true
            retryJob?.cancel()
            retryJob = null
            adView.destroy()
        }
    }

    Box(
        modifier = modifier.alpha(if (loaded) 1f else 0f)
            .background(Color(0xFFFFFCF8)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.size(width = 320.dp, height = 50.dp),
        )
    }
}
