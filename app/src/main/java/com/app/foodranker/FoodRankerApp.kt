package com.app.foodranker

import android.app.Application
import com.app.foodranker.utils.AnalyticsManager
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.ErrorMapper
import com.app.foodranker.utils.FirestoreCache
import com.app.foodranker.utils.FoodRankerMessagingService
import com.app.foodranker.utils.NotificationHelper
import com.app.foodranker.utils.RemoteConfigManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FoodRankerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // LO PRIMERO, y el orden importa: los ajustes de Firestore solo se pueden aplicar
        // antes de la primera operación contra Firestore, y más abajo en este mismo método
        // `saveCurrentToken()` escribe el token del dispositivo. Si esto se mueve detrás,
        // la app se queda sin caché offline para todo el que tenga sesión. Ver FirestoreCache.
        FirestoreCache.aplicar()
        // Antes que cualquier otro uso de Firebase: las llamadas que salgan antes de
        // instalar el proveedor viajan sin token de App Check.
        AppCheckInstaller.install()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        ErrorMapper.initialize(this)
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
