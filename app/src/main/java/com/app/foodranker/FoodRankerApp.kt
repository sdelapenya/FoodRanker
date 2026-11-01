package com.app.foodranker

import android.app.Application
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
        ErrorMapper.initialize(this)
        // Los anuncios NO arrancan aquí. En el EEE y Reino Unido no se puede pedir ni uno
        // antes de que el usuario pase por el formulario de consentimiento, así que de
        // AdManager.initialize() se encarga ConsentManager cuando el SDK da permiso
        // (desde MainActivity, que es quien tiene Activity para enseñar el diálogo).
        CloudinaryManager.initialize(this)
        AnalyticsManager.initialize(this)
        RemoteConfigManager.initialize()
        NotificationHelper.createChannels(this)
        // El recordatorio diario solo se programa con sesión iniciada, igual que el token
        // FCM. Tras registrarse lo programa MainActivity, así que nadie se queda sin él.
        if (FirebaseAuth.getInstance().currentUser != null) {
            FoodRankerMessagingService.saveCurrentToken()
            FoodRankerMessagingService.saveCurrentLanguage()
            com.app.foodranker.utils.DailyReminderWorker.schedule(this)
        }
    }
}
