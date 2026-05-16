package com.app.foodranker

import android.app.Application
import com.app.foodranker.utils.AdManager
import com.app.foodranker.utils.AnalyticsManager
import com.app.foodranker.utils.BillingManager
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.RemoteConfigManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FoodRankerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crash reporting — activo siempre en producción
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        AdManager.initialize(this)
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)
        CloudinaryManager.initialize(this)
        BillingManager.initialize(this)
        AnalyticsManager.initialize(this)
        RemoteConfigManager.initialize()
        com.app.foodranker.utils.DailyReminderWorker.schedule(this)
    }
}
