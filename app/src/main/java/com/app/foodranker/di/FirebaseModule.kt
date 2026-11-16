package com.app.foodranker.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.app.foodranker.utils.Fallos

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().also { db ->
        // FoodRankerMessagingService.saveCurrentToken() accede a FirebaseFirestore
        // directamente (via getInstance) desde Application.onCreate() — antes de que
        // Hilt haya construido este singleton — provocando que Firestore "arranque"
        // antes de que lleguemos aquí. Si ya arrancó, setFirestoreSettings lanza
        // IllegalStateException; la capturamos porque la caché por defecto es aceptable.
        try {
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    persistentCacheSettings {
                        setSizeBytes(50L * 1024 * 1024) // 50 MB
                    }
                )
                .build()
        } catch (e: IllegalStateException) {
            // Firestore ya arrancó antes de que Hilt aplicara los settings (race con
            // FoodRankerMessagingService). La caché persistente no pudo configurarse.
            //
            // Se reporta porque es una carrera: no falla siempre, y cuando pasa la app se
            // queda SIN caché offline entera, que es justo lo que no se nota mirándola en
            // el sofá con wifi. Sin esto no había forma de saber a cuánta gente le ocurre.
            Fallos.reportar(e, "firestore: settings no aplicados, sin caché persistente")
            Log.w("FirebaseModule", "Firestore settings skipped — already initialized (persistent cache inactive)")
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("europe-west1")
}