package com.app.foodranker.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        } catch (_: IllegalStateException) {
            // Firestore ya arrancó: los settings no pueden cambiarse en este punto.
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