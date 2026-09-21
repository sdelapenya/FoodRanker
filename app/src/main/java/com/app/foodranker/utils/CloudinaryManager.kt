package com.app.foodranker.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.app.foodranker.BuildConfig

object CloudinaryManager {

    private const val TAG = "CloudinaryManager"
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
        if (cloudName.isEmpty() || preset.isEmpty()) {
            Log.e(TAG, "Cloudinary: configura CLOUDINARY_CLOUD_NAME y CLOUDINARY_UPLOAD_PRESET en local.properties")
        }
        MediaManager.init(context, mapOf("cloud_name" to cloudName))
        initialized = true
    }

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onProgress: (percent: Int) -> Unit = {}
    ) {
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()

        // Se sube una versión reducida, no el fichero de la cámara: Cloudinary rechaza lo que
        // pase de 10 MB y los móviles de hoy llegan de sobra. Si la compresión falla se sube
        // el original, que es lo que se hacía antes. Ver ImageCompressor.
        val compressed = ImageCompressor.compressForUpload(context, imageUri)
        val source = compressed?.let { Uri.fromFile(it) } ?: imageUri

        val request = MediaManager.get()
            .upload(source)
            .option("folder", "foodranker/plates")
            .option("resource_type", "image")

        val uploadRequest = if (preset.isNotEmpty()) {
            request.unsigned(preset)
        } else {
            request
        }

        uploadRequest
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    if (totalBytes > 0) onProgress(((bytes * 100) / totalBytes).toInt())
                }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    compressed?.delete()
                    val url = resultData["secure_url"] as? String ?: ""
                    if (url.isBlank()) { onError("No se pudo obtener la URL de la imagen"); return }
                    onSuccess(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    // El motivo real de Cloudinary se perdía: al usuario se le enseña un
                    // mensaje genérico y no quedaba rastro de POR QUÉ falló, así que un fallo
                    // de subida era imposible de diagnosticar. Ahora al menos va al log.
                    Log.e(TAG, "Fallo de subida (código ${error.code}): ${error.description}")
                    compressed?.delete()
                    onError(error.description)
                }
                // onReschedule ocurre con mala red. Sin este callback la coroutine
                // que espera onSuccess/onError quedaría suspendida para siempre.
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.w(TAG, "Subida reprogramada (código ${error.code}): ${error.description}")
                    compressed?.delete()
                    onError(error.description)
                }
            })
            .dispatch(context)
    }
}
