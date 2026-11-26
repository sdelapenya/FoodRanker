import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
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

    // Play reparte el AAB en trozos y, por defecto, instala SOLO el idioma del móvil.
    // Con el selector de idioma eso se rompe: quien tenga el móvil en castellano y ponga
    // la app en inglés no tendría esos recursos instalados y seguiría viéndola en
    // castellano. Son dos idiomas y unos pocos KB de texto, así que van los dos siempre.
    // Lo recomienda la propia documentación de "Per-app language preferences".
    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Encendido desde la v29, con `res/raw/keep.xml` como red.
            //
            // Estuvo apagado porque BORRABA cinco cadenas que el código sí pide (cd_like,
            // cd_unlike, vm_saved, vm_unsaved, vm_save_fail): salían en su lista de
            // "Unused resources" aunque las clases compiladas leen esos campos de R, y
            // desaparecían de resources.arsc. Una de ellas la pinta cada tarjeta de plato,
            // así que era un Resources.NotFoundException en la pantalla principal, en
            // release y nunca en debug.
            //
            // `keep.xml` las fija por nombre, que es la solución documentada para
            // exactamente este caso y que nunca se había probado. Medido: 7.396.463 bytes
            // apagado frente a 6.952.132 encendido, 434 KB menos, y las 598 cadenas y
            // plurales del proyecto siguen en el APK con sus dos idiomas.
            //
            // ⚠️ Si se añade una cadena que el reductor no vea, va a `keep.xml`, no aquí.
            // Al tocar texto o recursos: comprobar el APK con tools/i18n/check_apk.py.
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            // ⚠️ Esta linea HOY NO HACE NADA, y el aviso de Play sobre los simbolos de
            // depuracion que sale en cada version NO se puede quitar. Comprobado en el AAB
            // de la v28: 8 ficheros .so y cero entradas en
            // BUNDLE-METADATA/com.android.tools.build.nativesymbols/.
            //
            // El motivo: `debugSymbolLevel` solo simboliza el codigo nativo que compila
            // ESTE proyecto con externalNativeBuild (CMake/ndk-build), y no hay ninguno.
            // Los .so que viajan en el bundle son de dependencias —hoy, libandroidx.
            // graphics.path.so y libdatastore_shared_counter.so, los dos de AndroidX—, y
            // llegan de Maven ya sin simbolos. AGP no puede inventarselos.
            //
            // (El comentario anterior decia que sin esto los fallos llegan como direcciones
            // de memoria, dando a entender que CON esto llegan simbolizados. No es cierto:
            // llegan igual. Tambien nombraba Firebase, AdMob y soloader, que no son los que
            // aportan los .so.)
            //
            // Se deja puesta porque no estorba, no engorda lo que se instala y el dia que
            // entre codigo nativo propio hara falta. No perder tiempo con el aviso.
            ndk {
                debugSymbolLevel = "FULL"
            }
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
        versionCode = 28
        versionName = "1.11"
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
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
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")

    // Login nativo de Google (Credential Manager). Se intenta ANTES del flujo por
    // navegador de Firebase, que se queda como respaldo: ese depende de Chrome Custom Tabs
    // y falla con navegadores que no los soportan (el de Xiaomi, por ejemplo).
    // Ojo histórico: estas librerías estuvieron fijadas en 1.3.0 porque Kapt no leía sus
    // metadatos de Kotlin. El proyecto ya usa KSP, así que esa atadura no existe.
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-firestore")
    // Aquí había un `firebase-storage`. Se fue en la v29: lo único que lo tocaba era un
    // `provideFirebaseStorage` en FirebaseModule que NADIE inyectaba. Las imágenes van a
    // Cloudinary desde el principio, y el proyecto no tiene ni reglas de Storage
    // desplegadas (la Rules API solo devuelve `cloud.firestore`), así que si algún día
    // alguien lo hubiera usado habría fallado en tiempo de ejecución.
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")

    // App Check — atestigua que quien llama a las callables es la app de verdad.
    // Play Integrity solo valida apps distribuidas por Google Play, así que en debug
    // (emulador, installDebug) hay que usar el proveedor de depuración o no hay token.
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Places — identidad canónica del local (ver docs/VENUES.md)
    implementation(libs.places)
    // Ubicación: searchNearby (Places API New) necesita coordenadas explícitas, a
    // diferencia del findCurrentPlace legacy que las resolvía por su cuenta.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Hilt (inyección de dependencias)
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-android-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coil (carga de imágenes)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Notificaciones push
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")
    // Id estable de esta instalación: es el id del documento de cada móvil en
    // users/{uid}/dispositivos. Viene de serie con firebase-messaging, pero se declara
    // explícito porque lo usamos directamente — si algún día messaging deja de
    // arrastrarlo, mejor que falle aquí y no en tiempo de ejecución.
    implementation("com.google.firebase:firebase-installations")

    // AdMob
    // CMP certificada de Google para el consentimiento del EEE y Reino Unido. Sin ella
    // AdMob limita los anuncios en Europa, que es donde está casi todo el público.

    // Cloudinary. Se excluye Fresco: entra por cloudinary-android-download, que la app
    // no usa (solo MediaManager para subir; las imágenes se muestran con Coil), y sus
    // .so están alineadas a 4 KB — Play avisa de que la app puede fallar en dispositivos
    // con páginas de memoria de 16 KB. Al quitarlo desaparecen libimagepipeline.so,
    // libnative-filters.so y libnative-imagetranscoder.so, las tres mal alineadas.
    implementation("com.cloudinary:cloudinary-android:2.3.1") {
        exclude(group = "com.facebook.fresco")
    }

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Aquí había un `billing-ktx:8.0.0`. Se fue en la v29, al retirarse la suscripción
    // Premium: no quedaba ni un uso en todo el proyecto —ni `BillingClient`, ni
    // `BillingManager`, ni un import— y aun así seguía viajando en el binario publicado.
    //
    // ⚠️ El motivo por el que no bastaba con no usarla: declaraba en el manifest fusionado
    // el permiso `com.android.vending.BILLING`, el `InAppBillingService.BIND`, el servicio
    // del *billing test companion* y DOS activities (`ProxyBillingActivity` y `...V2`). Y
    // esas activities del manifest son justo lo que impedía a R8 tirar las clases, que
    // seguían en el dex. O sea: la app publicada declaraba que hacía facturación mientras
    // la ficha de Play decía que no hay compras. Verificado en el AAB de la v28.
    //
    // Si algún día se vuelve a monetizar, esto vuelve, y con él hay que volver a declarar
    // las compras en Play y rehacer la declaración de operador.
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
