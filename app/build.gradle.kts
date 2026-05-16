plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}


fun localProp(key: String): String =
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.startsWith("$key=") }
        ?.substringAfter("=")
        ?.trim()
        ?: ""

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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProp("CLOUDINARY_CLOUD_NAME")}\"")
        // Subida sin secret en el APK: crea un upload preset "Unsigned" en Cloudinary y pon su nombre aquí.
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProp("CLOUDINARY_UPLOAD_PRESET")}\"")
        buildConfigField("String", "VISION_API_KEY",  "\"${localProp("VISION_API_KEY")}\"")
        buildConfigField("String", "PEXELS_API_KEY",  "\"${localProp("PEXELS_API_KEY")}\"")
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
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
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

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

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

    // Captura de pantalla (para tarjeta visual)
    implementation("androidx.compose.ui:ui-graphics:1.7.0")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("androidx.compose.animation:animation:1.6.0")
    implementation("androidx.compose.animation:animation-core:1.6.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
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