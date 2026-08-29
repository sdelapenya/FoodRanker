import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}


fun localProp(key: String): String {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { props.load(it) }
    return props.getProperty(key, "")
}

android {
    namespace = "com.app.foodranker"
    compileSdk = 36


    signingConfigs {
        create("release") {
            storeFile     = file(localProp("KEYSTORE_PATH"))
            storePassword = localProp("KEYSTORE_PASSWORD")
            keyAlias      = localProp("KEY_ALIAS")
            keyPassword   = localProp("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }


    defaultConfig {
        applicationId = "com.app.foodranker"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProp("CLOUDINARY_CLOUD_NAME")}\"")
        // Subida sin secret en el APK: crea un upload preset "Unsigned" en Cloudinary y pon su nombre aquí.
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProp("CLOUDINARY_UPLOAD_PRESET")}\"")
        // VISION_API_KEY eliminada a propósito: el cliente ya no llama a Vision.
        // Lo hace la CF `validateFoodImage` con el service account, porque una key en
        // BuildConfig acaba como literal en classes.dex (R8 no ofusca cadenas) y era
        // extraíble del bundle publicado, con el gasto facturado a este proyecto.
        // PEXELS_API_KEY solo la usa MealDBSeeder, que está tras `if (!BuildConfig.DEBUG) return`
        // en DiscoverViewModel — R8 elimina la clase entera en release, así que no
        // llega al binario (verificado buscándola en los dex del AAB).
        buildConfigField("String", "PEXELS_API_KEY",  "\"${localProp("PEXELS_API_KEY")}\"")
        // IDs de dispositivo de prueba de AdMob, separados por comas. Van en
        // local.properties (fuera de git) porque son de cada máquina/móvil, no del
        // proyecto. Solo se aplican en debug: pinchar anuncios reales en tus propias
        // pruebas es tráfico inválido y AdMob suspende cuentas por ello.
        buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", "\"${localProp("ADMOB_TEST_DEVICE_IDS")}\"")
        // Clave de Places para el cliente: solo autocompletado y búsqueda de locales
        // cercanos, que devuelven candidatos. Los datos canónicos del local los
        // resuelve la CF resolveVenue con una clave de servidor aparte. Restringir
        // esta por package + SHA-1 y ponerle tope de cuota (ver docs/VENUES.md).
        buildConfigField("String", "PLACES_API_KEY", "\"${localProp("PLACES_API_KEY")}\"")
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose BOM (controla versiones automáticamente)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Firebase BOM (controla versiones automáticamente)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // App Check — atestigua que quien llama a las callables es la app de verdad.
    // Play Integrity solo valida apps distribuidas por Google Play, así que en debug
    // (emulador, installDebug) hay que usar el proveedor de depuración o no hay token.
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Places — identidad canónica del local (ver docs/VENUES.md)
    implementation(libs.places)
    // Ubicación: searchNearby (Places API New) necesita coordenadas explícitas, a
    // diferencia del findCurrentPlace legacy que las resolvía por su cuenta.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Hilt (inyección de dependencias)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coil (carga de imágenes)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Deep Links y Dynamic Links Firebase
    implementation("com.google.firebase:firebase-dynamic-links-ktx")

    // Notificaciones push
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // Fuerza versiones mínimas de dependencias transitivas señaladas por Play Console:
    // recaptcha 18.1.2 (de firebase-auth) tenía una vulnerabilidad crítica parcheada en
    // la 18.4.0; soloader 0.10.1 (de Cloudinary -> Fresco) podía fallar en dispositivos
    // solo de 64 bits, corregido en la 0.10.4.
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
    implementation("com.facebook.soloader:soloader:0.10.4")

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Necesario para Hilt
kapt {
    correctErrorTypes = true
}