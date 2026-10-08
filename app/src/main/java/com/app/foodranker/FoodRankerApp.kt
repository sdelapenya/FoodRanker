package com.app.foodranker

import android.app.Application
import com.app.foodranker.utils.AdManager
import com.app.foodranker.utils.AnalyticsManager
import com.app.foodranker.utils.BillingManager
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.ErrorMapper
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
        // Antes que cualquier otro uso de Firebase: las llamadas que salgan antes de
        // instalar el proveedor viajan sin token de App Check.
        AppCheckInstaller.install()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        // initialize() ya encadena las precargas cuando el SDK termina de arrancar,
        // en segundo plano. Llamarlas aquí las lanzaba antes de que estuviera listo.
        ErrorMapper.initialize(this)
        AdManager.initialize(this)
        CloudinaryManager.initialize(this)
        AnalyticsManager.initialize(this)
        RemoteConfigManager.initialize()
        NotificationHelper.createChannels(this)
        // El recordatorio diario solo se programa con sesión iniciada, igual que el token
        // FCM. Tras registrarse lo programa MainActivity, así que nadie se queda sin él.
        if (FirebaseAuth.getInstance().currentUser != null) {
            FoodRankerMessagingService.saveCurrentToken()
            com.app.foodranker.utils.DailyReminderWorker.schedule(this)
        }
    }
}
