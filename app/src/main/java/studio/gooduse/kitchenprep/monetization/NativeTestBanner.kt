package studio.gooduse.kitchenprep.monetization

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
 * Native anchored-adaptive Google test banner.
 *
 * The host reserves no visible rail while an eligible request is loading and expands to
 * the SDK-computed adaptive height only after onAdLoaded. No-fill collapses the
 * rail again. Refresh cadence is left entirely to the Google Mobile Ads SDK.
 */
@Composable
fun NativeTestBanner(
    modifier: Modifier = Modifier,
    onLoadedChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val currentCallback by rememberUpdatedState(onLoadedChanged)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var loaded by remember(widthDp) { mutableStateOf(false) }

        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(adSize)
            }
        }

        DisposableEffect(adView) {
            var disposed = false
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (!disposed) {
                        loaded = true
                        currentCallback(true)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!disposed) {
                        loaded = false
                        currentCallback(false)
                    }
                }
            }

            MobileAdsBootstrap.ensure(context) {
                if (!disposed) {
                    val nonPersonalizedExtras = Bundle().apply { putString("npa", "1") }
                    val request = AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, nonPersonalizedExtras)
                        .build()
                    adView.loadAd(request)
                }
            }

            onDispose {
                disposed = true
                currentCallback(false)
                adView.destroy()
            }
        }

        val railHeight = if (loaded) adSize.height.dp else 0.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(railHeight)
                .clipToBounds()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .requiredSize(width = adSize.width.dp, height = adSize.height.dp)
                    .alpha(if (loaded) 1f else 0f),
            )
        }
    }
}
