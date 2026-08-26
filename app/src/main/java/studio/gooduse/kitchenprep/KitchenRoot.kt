package studio.gooduse.kitchenprep

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studio.gooduse.kitchenprep.monetization.BillingController
import studio.gooduse.kitchenprep.monetization.ConsentController

private const val ONBOARDING_PREFS = "kitchen_prep_onboarding"
private const val ONBOARDING_ACCEPTED = "accepted_v1"

@Composable
fun KitchenAppRoot(
    activity: MainActivity,
    billingController: BillingController,
    consentController: ConsentController,
    viewModel: KitchenViewModel,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var onboardingAccepted by remember {
        mutableStateOf(
            context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
                .getBoolean(ONBOARDING_ACCEPTED, false)
        )
    }

    if (onboardingAccepted) {
        KitchenPrepClosedTestApp(
            activity = activity,
            billingController = billingController,
            consentController = consentController,
            viewModel = viewModel,
        )
        return
    }

    val strings = remember { KitchenStrings.load(context) }
    val languageTag = settings.languageTag.ifBlank { "en" }
    val tr: Translate = remember(strings, languageTag) {
        { key, fallback -> strings.text(languageTag, key, fallback) }
    }
    val rtl = strings.isRtl(languageTag)

    SideEffect {
        activity.window.statusBarColor = KitchenColors.Canvas.toArgb()
        activity.window.navigationBarColor = KitchenColors.Canvas.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    KitchenTheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            OnboardingScreen(
                tr = tr,
                onTerms = { activity.openExternal(BuildConfig.TERMS_URL) },
                onSafety = { activity.openExternal(BuildConfig.SAFETY_URL) },
                onPrivacy = { activity.openExternal(BuildConfig.PRIVACY_POLICY_URL) },
                onComplete = {
                    context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(ONBOARDING_ACCEPTED, true)
                        .apply()
                    onboardingAccepted = true
                },
            )
        }
    }
}
