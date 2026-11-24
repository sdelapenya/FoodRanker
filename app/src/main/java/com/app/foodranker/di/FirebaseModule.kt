package com.app.foodranker.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Los ajustes (caché persistente de 50 MB) NO se aplican aquí: para cuando Hilt
    // construye este singleton, `FoodRankerApp.onCreate()` ya ha tocado Firestore al
    // guardar el token del dispositivo, y entonces `setFirestoreSettings` lanza
    // IllegalStateException y la caché se queda sin encender. Se aplican en
    // `FirestoreCache.aplicar()`, que es la primera sentencia de `onCreate()`.
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("europe-west1")
}