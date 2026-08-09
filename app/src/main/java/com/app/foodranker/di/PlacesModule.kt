package com.app.foodranker.di

import android.content.Context
import com.app.foodranker.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {

    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
        // initialize() es idempotente pero avisa si se llama dos veces; al ir dentro
        // de un @Singleton solo se ejecuta una vez por proceso.
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.PLACES_API_KEY)
        }
        return Places.createClient(context)
    }
}
