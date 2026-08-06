package com.app.foodranker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.app.foodranker.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object FoodImageValidator {

    // Las listas de labels de comida y los niveles de SafeSearch rechazados viven
    // ahora solo en la CF (FOOD_KEYWORDS / FAIL_LIKELIHOODS en functions/src/index.ts),
    // compartidos con moderatePlateImage para que no puedan divergir.

    /**
     * Pre-filtro de UX: valida la foto antes de subirla para dar feedback inmediato.
     *
     * La llamada a Vision la hace la Cloud Function `validateFoodImage`, no este
     * cliente: así la API key de Vision no viaja en el APK (era extraíble
     * descompilando el bundle, y las llamadas se facturan a este proyecto).
     *
     * Sigue siendo fail-open — ante cualquier fallo se deja continuar, porque la
     * moderación de verdad la hace la CF `moderatePlateImage` al publicar, que es
     * fail-closed.
     */
    suspend fun validate(
        context: Context,
        imageUri: Uri,
        functions: FirebaseFunctions
    ): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!RemoteConfigManager.visionApiEnabled) return@withContext Pair(true, "")
            try {
                val base64 = encodeImageToBase64(context, imageUri)
                    ?: return@withContext Pair(false, "No se pudo leer la imagen 📸")

                val result = functions
                    .getHttpsCallable("validateFoodImage")
                    .call(mapOf("imageBase64" to base64))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any>
                    ?: return@withContext Pair(true, "")

                if (BuildConfig.DEBUG) android.util.Log.d("FoodValidator", "validateFoodImage → $data")

                if (data["ok"] as? Boolean != false) return@withContext Pair(true, "")

                when (data["reason"] as? String) {
                    "inappropriate" -> Pair(
                        false,
                        "Contenido inapropiado detectado ⚠️\nEsta foto no cumple nuestras normas de comunidad"
                    )
                    "not_food" -> Pair(
                        false,
                        "No hemos detectado comida en esta foto 🍽️\nPor favor sube una foto de un plato real"
                    )
                    else -> Pair(true, "")
                }
            } catch (e: Exception) {
                android.util.Log.e("FoodValidator", "Error validando imagen: ${e.message}")
                Pair(true, "") // No bloquear al usuario; modera moderatePlateImage
            }
        }

    private fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {
            // Paso 1: decodificar solo los bounds para calcular inSampleSize dinámicamente
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            val sampleSize = calculateInSampleSize(boundsOpts, reqWidth = 800, reqHeight = 800)

            // Paso 2: decodificar a la resolución calculada
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = opts.outHeight
        val width = opts.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

}
