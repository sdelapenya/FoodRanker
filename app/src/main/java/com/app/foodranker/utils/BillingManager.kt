package com.app.foodranker.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PRODUCT_PREMIUM_MONTHLY = "foodranker_premium_monthly"
        private const val PREFS_NAME = "billing_prefs"
        private const val KEY_TEMP_PREMIUM_UNTIL = "temp_premium_until"
    }

    private val _isPremium   = MutableStateFlow(false)
    private val _isAvailable = MutableStateFlow(false)
    private val _price       = MutableStateFlow("2,99 €/mes")
    private val _isLoading   = MutableStateFlow(false)

    val isPremium:   StateFlow<Boolean> = _isPremium
    val isAvailable: StateFlow<Boolean> = _isAvailable
    val price:       StateFlow<String>  = _price
    val isLoading:   StateFlow<Boolean> = _isLoading

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var reconnectAttempt = 0

    init {
        restoreTemporaryPremium()
        billingClient = BillingClient.newBuilder(context)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases()
            .build()

        connect()
    }

    private fun connect() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempt = 0
                    _isAvailable.value = true
                    queryProductDetails()
                    restorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                _isAvailable.value = false
                // Reconexión con backoff exponencial (máx. ~60s) según recomienda Google.
                val delayMs = minOf(60_000L, 1_000L * (1L shl reconnectAttempt))
                reconnectAttempt = minOf(reconnectAttempt + 1, 6)
                handler.postDelayed({ connect() }, delayMs)
            }
        })
    }

    fun grantTemporaryPremium() {
        val until = System.currentTimeMillis() + 24L * 60 * 60 * 1000
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TEMP_PREMIUM_UNTIL, until).apply()
        _isPremium.value = true
    }

    private fun restoreTemporaryPremium() {
        val until = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TEMP_PREMIUM_UNTIL, 0L)
        if (until > System.currentTimeMillis()) _isPremium.value = true
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_PREMIUM_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()

        billingClient?.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                productDetails = details[0]
                details[0].subscriptionOfferDetails?.firstOrNull()?.pricingPhases
                    ?.pricingPhaseList?.firstOrNull()?.formattedPrice?.let {
                        _price.value = "$it/mes"
                    }
            }
        }
    }

    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            ))
            .build()

        return billingClient?.launchBillingFlow(activity, params)
            ?.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isPremium.value = true
            // isPremium en Firestore solo debe escribirse desde Cloud Functions via Admin SDK
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(ackParams) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        android.util.Log.e("Billing", "Acknowledge failed (${result.responseCode}): ${result.debugMessage}")
                    }
                }
            }
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val hasActive = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _isPremium.value = hasActive
        }
    }
}
