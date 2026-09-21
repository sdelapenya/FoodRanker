package com.app.foodranker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Reduce la foto antes de subirla a Cloudinary.
 *
 * Antes se subía el fichero **tal cual salía de la cámara**: los móviles actuales hacen fotos
 * de 8-15 MB y Cloudinary rechaza las de más de 10 MB, así que a quien tuviera buena cámara le
 * fallaba la publicación con un "Error al subir la imagen" que no explicaba nada (reportado
 * por una tester el 2026-09-21). Además gastaba cuota y ancho de banda para nada: la app
 * enseña las fotos a tamaño de móvil, no necesita 12 megapíxeles.
 *
 * Es **fail-open**: si algo sale mal devuelve null y se sube el original, porque más vale una
 * subida pesada que no poder publicar.
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"

    /** Lado largo máximo. De sobra para pantalla completa en cualquier móvil. */
    private const val MAX_DIMENSION = 1920

    /** 85 conserva la comida apetecible y recorta el peso una barbaridad. */
    private const val JPEG_QUALITY = 85

    /**
     * Devuelve un JPEG reducido en la caché, o null si no se pudo (entonces súbase el original).
     * El fichero vive en `cacheDir`, así que Android lo limpia solo si hace falta espacio.
     */
    fun compressForUpload(context: Context, uri: Uri): File? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // Al recomprimir se pierden los metadatos EXIF, y con ellos la orientación: sin
            // esto las fotos verticales se subirían giradas.
            val rotated = applyExifRotation(context, uri, decoded)
            val scaled = scaleToMaxDimension(rotated)

            val output = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(output).use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (scaled != decoded) scaled.recycle()
            decoded.recycle()

            Log.d(TAG, "Comprimida a ${output.length() / 1024} KB (${scaled.width}x${scaled.height})")
            output
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comprimir, se subirá el original: ${e.message}")
            null
        } catch (e: OutOfMemoryError) {
            // Una foto enorme en un móvil justo de memoria: mejor subir el original que morir.
            Log.w(TAG, "Sin memoria al comprimir, se subirá el original")
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var halfW = width / 2
        var halfH = height / 2
        while (halfW / sampleSize >= MAX_DIMENSION || halfH / sampleSize >= MAX_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMaxDimension(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val ratio = MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true
        )
    }
}
