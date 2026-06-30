package com.app.foodranker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.app.foodranker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FoodImageValidator {

    private val FOOD_LABELS = setOf(
        "food", "dish", "cuisine", "fast food", "junk food", "snack", "ingredient",
        "comfort food", "street food", "baked goods", "seafood", "meat", "vegetable",
        "fruit", "salad", "pasta", "pizza", "burger", "sushi", "ramen", "dessert",
        "breakfast", "lunch", "dinner", "meal", "restaurant", "bakery", "ice cream",
        "cake", "bread", "soup", "sandwich", "noodle", "rice", "curry", "steak",
        "chocolate", "plate", "bowl", "tableware", "cheese", "egg", "sauce",
        "produce", "condiment", "appetizer", "taco", "burrito", "shrimp", "fish",
        "chicken", "beef", "pork", "lamb", "coffee", "beverage", "drink", "juice",
        "dairy", "butter", "cream", "honey", "cooking", "recipe", "gourmet"
    )

    // Niveles de SafeSearch que se consideran inaceptables
    private val REJECTED_LEVELS = setOf("LIKELY", "VERY_LIKELY")

    suspend fun validate(context: Context, imageUri: Uri): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!RemoteConfigManager.visionApiEnabled) return@withContext Pair(true, "")
            try {
                val base64 = encodeImageToBase64(context, imageUri)
                    ?: return@withContext Pair(false, "No se pudo leer la imagen 📸")

                val response = callVisionApi(base64)
                    ?: return@withContext Pair(true, "") // Si Vision falla, permitir

                if (com.app.foodranker.BuildConfig.DEBUG) android.util.Log.d("FoodValidator", "Vision response: $response")

                val responses = response.optJSONArray("responses")
                    ?: return@withContext Pair(true, "")
                val first = responses.optJSONObject(0)
                    ?: return@withContext Pair(true, "")

                // 1. SafeSearch — rechazar contenido inapropiado siempre
                val safeSearch = first.optJSONObject("safeSearchAnnotation")
                if (safeSearch != null) {
                    val adult    = safeSearch.optString("adult", "UNKNOWN")
                    val violence = safeSearch.optString("violence", "UNKNOWN")
                    val racy     = safeSearch.optString("racy", "UNKNOWN")
                    if (com.app.foodranker.BuildConfig.DEBUG) android.util.Log.d("FoodValidator", "SafeSearch — adult:$adult violence:$violence racy:$racy")
                    if (adult in REJECTED_LEVELS || violence in REJECTED_LEVELS || racy in REJECTED_LEVELS) {
                        return@withContext Pair(false, "Contenido inapropiado detectado ⚠️\nEsta foto no cumple nuestras normas de comunidad")
                    }
                }

                // 2. Labels — verificar que hay comida con score >= min_image_score
                val labels = first.optJSONArray("labelAnnotations") ?: JSONArray()
                val minScore = RemoteConfigManager.minImageScore
                if (com.app.foodranker.BuildConfig.DEBUG) android.util.Log.d("FoodValidator", "Labels count: ${labels.length()}, minScore: $minScore")

                val isFood = (0 until labels.length()).any { i ->
                    val labelObj = labels.getJSONObject(i)
                    val description = labelObj.optString("description", "").lowercase()
                    val score = labelObj.optDouble("score", 0.0).toFloat()
                    score >= minScore && FOOD_LABELS.any { description.contains(it) }
                }

                if (isFood) Pair(true, "")
                else Pair(false, "No hemos detectado comida en esta foto 🍽️\nPor favor sube una foto de un plato real")

            } catch (e: Exception) {
                android.util.Log.e("FoodValidator", "Error Vision API: ${e.message}")
                Pair(true, "") // En caso de error de red, no bloquear al usuario
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

    private fun callVisionApi(base64Image: String): JSONObject? {
        val apiKey = BuildConfig.VISION_API_KEY
        val endpoint = "https://vision.googleapis.com/v1/images:annotate?key=$apiKey"

        val body = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", JSONObject().put("content", base64Image))
                    put("features", JSONArray().apply {
                        put(JSONObject().put("type", "LABEL_DETECTION").put("maxResults", 10))
                        put(JSONObject().put("type", "SAFE_SEARCH_DETECTION"))
                    })
                })
            })
        }.toString()

        val conn = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                JSONObject(text)
            } else {
                android.util.Log.e("FoodValidator", "Vision HTTP ${conn.responseCode}")
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
