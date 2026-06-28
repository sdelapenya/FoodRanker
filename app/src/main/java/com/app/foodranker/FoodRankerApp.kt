package com.app.foodranker

import android.app.Application
import com.app.foodranker.utils.AdManager
import com.app.foodranker.utils.AnalyticsManager
import com.app.foodranker.utils.BillingManager
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.FoodRankerMessagingService
import com.app.foodranker.utils.NotificationHelper
import com.app.foodranker.utils.RemoteConfigManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FoodRankerApp : Application() {

    // Eager injection ensures BillingManager.init{} runs at app startup
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        AdManager.initialize(this)
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)
        CloudinaryManager.initialize(this)
        AnalyticsManager.initialize(this)
        RemoteConfigManager.initialize()
        NotificationHelper.createChannels(this)
        com.app.foodranker.utils.DailyReminderWorker.schedule(this)
        if (FirebaseAuth.getInstance().currentUser != null) {
            FoodRankerMessagingService.saveCurrentToken()
        }
    }
}
