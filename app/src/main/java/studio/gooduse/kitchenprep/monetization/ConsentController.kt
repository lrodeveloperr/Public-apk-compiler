package studio.gooduse.kitchenprep.monetization

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class ConsentUiState(
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val initialized: Boolean = false,
    val lastError: String? = null,
)

class ConsentController(context: Context) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context.applicationContext)
    private val _state = MutableStateFlow(ConsentUiState())
    val state: StateFlow<ConsentUiState> = _state
    private var started = false

    fun attach(activity: Activity) {
        if (started) return
        started = true
        refresh(activity)
    }

    fun refresh(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                applyState(null)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    applyState(formError?.message)
                }
            },
            { error ->
                // A valid consent state from a previous session may still permit ads.
                applyState(error.message)
            },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            applyState(error?.message)
        }
    }

    private fun applyState(error: String?) {
        _state.value = ConsentUiState(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            initialized = true,
            lastError = error,
        )
    }
}
