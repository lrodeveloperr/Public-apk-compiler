package studio.gooduse.kitchenprep.monetization

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import studio.gooduse.kitchenprep.BuildConfig
import java.lang.ref.WeakReference


data class BillingUiState(
    val active: Boolean = false,
    val verifiedThisSession: Boolean = false,
    val ready: Boolean = false,
    val formattedPrice: String? = null,
    val billingPeriod: String? = null,
    val purchaseInProgress: Boolean = false,
    val lastError: String? = null,
)

class BillingController(
    context: Context,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("kpb_billing_cache", Context.MODE_PRIVATE)
    private var activityRef: WeakReference<Activity>? = null
    private var connectionInProgress = false
    private var started = false
    private var purchaseQueryGeneration = 0L
    private val afterConnected = mutableListOf<() -> Unit>()

    private val _state = MutableStateFlow(
        BillingUiState(active = prefs.getBoolean(KEY_ACTIVE, false))
    )
    val state: StateFlow<BillingUiState> = _state

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        if (!started) {
            started = true
            connect()
        }
    }

    fun reconcile() {
        if (billingClient.isReady) {
            queryPurchases()
            refreshOfferDetails()
        } else {
            connect()
        }
    }

    fun purchase(activity: Activity? = activityRef?.get()) {
        val resolvedActivity = activity ?: return
        val current = _state.value
        if (current.active || current.purchaseInProgress) return
        _state.value = current.copy(purchaseInProgress = true, lastError = null)
        launchPurchase(resolvedActivity)
    }

    fun openManageSubscription(activity: Activity) {
        val uri = Uri.parse(
            "https://play.google.com/store/account/subscriptions?sku=${BuildConfig.REMOVE_ADS_PRODUCT_ID}&package=${activity.packageName}"
        )
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun launchPurchase(activity: Activity) {
        if (!billingClient.isReady) {
            connect { launchPurchase(activity) }
            return
        }
        queryProduct(
            onLoaded = { details, offer ->
                val phase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                _state.value = _state.value.copy(
                    formattedPrice = phase?.formattedPrice,
                    billingPeriod = phase?.billingPeriod,
                    lastError = null,
                )
                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offer.offerToken)
                    .build()
                val result = billingClient.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productParams))
                        .build(),
                )
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> Unit
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        _state.value = _state.value.copy(purchaseInProgress = false, lastError = null)
                        queryPurchases()
                    }
                    else -> failPurchase(result.debugMessage.ifBlank { "Google Play could not start the purchase." })
                }
            },
            onUnavailable = ::failPurchase,
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _state.value = _state.value.copy(purchaseInProgress = false)
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                purchaseQueryGeneration++
                processPurchases(purchases, confirmedQuery = false)
            }
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(lastError = null)
            }
            result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _state.value = _state.value.copy(lastError = null)
                queryPurchases()
            }
            else -> {
                _state.value = _state.value.copy(
                    lastError = result.debugMessage.ifBlank { "Google Play purchase failed." },
                )
            }
        }
    }

    private fun connect(after: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _state.value = _state.value.copy(ready = true)
            queryPurchases()
            refreshOfferDetails()
            after?.invoke()
            return
        }

        if (after != null) afterConnected += after
        if (connectionInProgress) return
        connectionInProgress = true

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionInProgress = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(ready = true, lastError = null)
                    queryPurchases()
                    refreshOfferDetails()
                    val callbacks = afterConnected.toList()
                    afterConnected.clear()
                    callbacks.forEach { it.invoke() }
                } else {
                    afterConnected.clear()
                    _state.value = _state.value.copy(
                        ready = false,
                        verifiedThisSession = false,
                        purchaseInProgress = false,
                        lastError = result.debugMessage.ifBlank { "Google Play Billing is unavailable." },
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionInProgress = false
                _state.value = _state.value.copy(ready = false, verifiedThisSession = false)
            }
        })
    }

    private fun refreshOfferDetails() {
        if (!billingClient.isReady) return
        queryProduct(
            onLoaded = { _, offer ->
                val phase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                _state.value = _state.value.copy(
                    formattedPrice = phase?.formattedPrice,
                    billingPeriod = phase?.billingPeriod,
                )
            },
            onUnavailable = {
                // A catalog miss should not disrupt the core app. The purchase action
                // performs the same strict lookup and surfaces a user-facing failure.
                _state.value = _state.value.copy(formattedPrice = null, billingPeriod = null)
            },
        )
    }

    private fun queryProduct(
        onLoaded: (ProductDetails, ProductDetails.SubscriptionOfferDetails) -> Unit,
        onUnavailable: (String) -> Unit,
    ) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        ) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onUnavailable(result.debugMessage.ifBlank { "Google Play could not load subscription details." })
                return@queryProductDetailsAsync
            }
            val details = detailsResult.productDetailsList
                .firstOrNull { it.productId == BuildConfig.REMOVE_ADS_PRODUCT_ID }
            if (details == null) {
                onUnavailable("Subscription product is not available from Google Play.")
                return@queryProductDetailsAsync
            }
            // Exact base plan only. No trial/intro/alternate-plan fallback.
            val offer = details.subscriptionOfferDetails
                ?.firstOrNull {
                    it.basePlanId == BuildConfig.REMOVE_ADS_BASE_PLAN_ID && it.offerId == null
                }
            if (offer == null) {
                onUnavailable("The monthly remove-ads plan is not available from Google Play.")
                return@queryProductDetailsAsync
            }
            onLoaded(details, offer)
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(verifiedThisSession = false)
            return
        }
        val generation = ++purchaseQueryGeneration
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (generation != purchaseQueryGeneration) return@queryPurchasesAsync
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases, confirmedQuery = true)
            } else {
                _state.value = _state.value.copy(
                    verifiedThisSession = false,
                    lastError = result.debugMessage.ifBlank { "Google Play could not verify the subscription." },
                )
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, confirmedQuery: Boolean) {
        val purchased = purchases.filter {
            BuildConfig.REMOVE_ADS_PRODUCT_ID in it.products &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        purchased.filter { !it.isAcknowledged }.forEach(::acknowledgeWithRetry)

        val active = when {
            purchased.isNotEmpty() -> true
            confirmedQuery -> false
            else -> _state.value.active
        }
        val verified = _state.value.verifiedThisSession || confirmedQuery || purchased.isNotEmpty()
        _state.value = _state.value.copy(
            active = active,
            verifiedThisSession = verified,
            lastError = null,
        )
        prefs.edit().putBoolean(KEY_ACTIVE, active).apply()
    }

    private fun acknowledgeWithRetry(purchase: Purchase, attempt: Int = 0) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            when {
                result.responseCode == BillingClient.BillingResponseCode.OK -> Unit
                result.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> queryPurchases()
                isTransient(result.responseCode) && attempt < MAX_ACK_RETRIES -> {
                    scope.launch {
                        delay(ACK_RETRY_BASE_MS * (1L shl attempt))
                        acknowledgeWithRetry(purchase, attempt + 1)
                    }
                }
                else -> {
                    _state.value = _state.value.copy(
                        lastError = result.debugMessage.ifBlank {
                            "Google Play could not acknowledge the subscription. It will be retried later."
                        },
                    )
                }
            }
        }
    }

    private fun isTransient(code: Int): Boolean = code in setOf(
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        BillingClient.BillingResponseCode.ERROR,
    )

    private fun failPurchase(message: String) {
        _state.value = _state.value.copy(purchaseInProgress = false, lastError = message)
    }

    fun close() {
        afterConnected.clear()
        billingClient.endConnection()
    }

    private companion object {
        const val KEY_ACTIVE = "subscriber_active"
        const val MAX_ACK_RETRIES = 4
        const val ACK_RETRY_BASE_MS = 1_000L
    }
}
