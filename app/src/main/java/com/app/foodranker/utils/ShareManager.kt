package com.app.foodranker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.app.foodranker.data.model.Plate
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareManager {

    // Compartir texto simple (siempre funciona)
    /** Compartir enlace público del perfil (mismo dominio que los platos). */
    fun shareProfile(context: Context, userId: String, displayName: String) {
        val url = "https://foodranker.app/user/$userId"
        val text =
            "🍽️ Sigue el perfil de $displayName en FoodRanker\n$url\n\n#FoodRanker"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "$displayName en FoodRanker")
        }
        context.startActivity(Intent.createChooser(intent, "Compartir perfil"))
    }

    fun sharePlateText(context: Context, plate: Plate) {
        val text = buildShareText(plate)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir en..."))
    }

    // Compartir imagen + texto
    fun sharePlateWithImage(context: Context, plate: Plate, bitmap: Bitmap) {
        val uri = saveBitmapToCache(context, bitmap, "plate_${plate.id}.png")
        val text = buildShareText(plate)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir plato"))
    }

    // Compartir en Instagram Stories
    fun shareToInstagramStories(context: Context, bitmap: Bitmap, plate: Plate) {
        val uri = saveBitmapToCache(context, bitmap, "story_share.png")
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            context.startActivity(intent)
        } else {
            sharePlateText(context, plate)
        }
    }

    // Compartir imagen descargada de Cloudinary + texto.
    // Se declara suspend para que el caller la lance desde su propio scope
    // (viewModelScope o LaunchedEffect) y respete el ciclo de vida.
    suspend fun sharePlateWithImageUrl(context: Context, plate: Plate) {
        if (plate.imageUrl.isEmpty()) { sharePlateText(context, plate); return }
        try {
            val bitmap = withContext(Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeStream(
                    java.net.URL(plate.imageUrl).openStream()
                )
            } ?: run { sharePlateText(context, plate); return }

            // Overlay: añadir branding en la parte inferior
            val mutable = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(mutable)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(180, 0, 0, 0)
                isAntiAlias = true
            }
            val h = mutable.height.toFloat()
            val w = mutable.width.toFloat()
            canvas.drawRect(0f, h * 0.8f, w, h, paint)
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = h * 0.045f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText(plate.name, w * 0.05f, h * 0.87f, textPaint)
            textPaint.textSize = h * 0.035f
            textPaint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText("★ ${"%.1f".format(plate.averageScore)}  •  FoodRanker", w * 0.05f, h * 0.94f, textPaint)
            sharePlateWithImage(context, plate, mutable)
        } catch (e: Exception) {
            sharePlateText(context, plate)
        }
    }

    fun buildShareText(plate: Plate): String {
        return "🍽️ ¡Descubrí \"${plate.name}\" en ${plate.restaurantName}!\n" +
                "📍 ${plate.city}, ${plate.country}\n" +
                "★ Puntuación: ${"%.1f".format(plate.averageScore)}/10 " +
                "(${plate.totalRatings} valoraciones)\n\n" +
                "Descúbrelo y valóralo en FoodRanker 👇\n" +
                "https://foodranker.app/plate/${plate.id}\n\n" +
                "#FoodRanker #${plate.category.name.lowercase()} #${plate.city.replace(" ", "")}"
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cachePath, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}