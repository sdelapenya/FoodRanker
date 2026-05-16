package com.app.foodranker.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BillingManager {

    const val PRODUCT_PREMIUM_MONTHLY = "foodranker_premium_monthly"

    private val _isPremium    = MutableStateFlow(false)
    private val _isAvailable  = MutableStateFlow(false)
    private val _price        = MutableStateFlow("2,99 €/mes")
    private val _isLoading    = MutableStateFlow(false)

    val isPremium:   StateFlow<Boolean> = _isPremium
    val isAvailable: StateFlow<Boolean> = _isAvailable
    val price:       StateFlow<String>  = _price
    val isLoading:   StateFlow<Boolean> = _isLoading

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it, context) }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isAvailable.value = true
                    queryProductDetails()
                    restorePurchases(context)
                }
            }
            override fun onBillingServiceDisconnected() {
                _isAvailable.value = false
            }
        })
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

    private fun handlePurchase(purchase: Purchase, context: Context) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isPremium.value = true
            // Guardar en Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("isPremium", true)
            // Confirmar la compra con Google
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(ackParams) { }
            }
        }
    }

    private fun restorePurchases(context: Context) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(params) { _, purchases ->
            val hasActive = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _isPremium.value = hasActive
        }
    }
}
