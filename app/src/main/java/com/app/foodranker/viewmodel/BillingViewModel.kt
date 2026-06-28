package com.app.foodranker.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.app.foodranker.utils.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val isPremium   = billingManager.isPremium
    val isAvailable = billingManager.isAvailable
    val price       = billingManager.price
    val isLoading   = billingManager.isLoading

    fun grantTemporaryPremium() = billingManager.grantTemporaryPremium()
    fun launchPurchase(activity: Activity) = billingManager.launchPurchase(activity)
}
