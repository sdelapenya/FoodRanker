package com.app.foodranker.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Comprueba que una foto de cámara moderna queda por debajo de lo que Cloudinary acepta.
 *
 * Existe porque una tester no podía publicar: se subía el fichero tal cual salía del móvil y
 * Cloudinary rechaza lo que pase de 10 MB (2026-09-21). El caso no se podía probar a mano —
 * hacerlo desde la app exige iniciar sesión con una cuenta real.
 */
@RunWith(AndroidJUnit4::class)
class ImageCompressorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Una foto tipo 12 MP con ruido, para que el JPEG no se comprima de forma irreal. */
    private fun crearFotoGrande(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val random = java.util.Random(42)
        for (i in 0 until 6000) {
            paint.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawCircle(
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * 90, paint
            )
        }
        val file = File(context.cacheDir, "test_origen_${width}x$height.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun unaFotoDeCamaraQuedaMuyPorDebajoDelLimiteDeCloudinary() {
        val origen = crearFotoGrande(4000, 3000)
        val tamOriginal = File(origen.path!!).length()

        val comprimida = ImageCompressor.compressForUpload(context, origen)

        assertNotNull("Debería haber comprimido la imagen", comprimida)
        val tamFinal = comprimida!!.length()

        println("ORIGINAL: ${tamOriginal / 1024} KB  ->  COMPRIMIDA: ${tamFinal / 1024} KB")

        assertTrue("La comprimida debe pesar menos que la original", tamFinal < tamOriginal)
        assertTrue(
            "Debe quedar por debajo del límite de 10 MB de Cloudinary (son ${tamFinal / 1024} KB)",
            tamFinal < 10 * 1024 * 1024
        )

        // Y el lado largo no debe pasar del máximo.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(comprimida.path, bounds)
        println("DIMENSIONES: ${bounds.outWidth}x${bounds.outHeight}")
        assertTrue(
            "El lado largo no debe pasar de 1920 (es ${bounds.outWidth}x${bounds.outHeight})",
            maxOf(bounds.outWidth, bounds.outHeight) <= 1920
        )
        assertTrue("Debe seguir siendo una imagen válida", bounds.outWidth > 0)

        comprimida.delete()
        File(origen.path!!).delete()
    }

    @Test
    fun unaFotoQueYaEsPequenaNoSeAmplia() {
        val origen = crearFotoGrande(800, 600)

        val comprimida = ImageCompressor.compressForUpload(context, origen)

        assertNotNull(comprimida)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(comprimida!!.path, bounds)
        println("PEQUEÑA: ${bounds.outWidth}x${bounds.outHeight}")
        assertTrue("No debe ampliarse", bounds.outWidth <= 800 && bounds.outHeight <= 600)

        comprimida.delete()
        File(origen.path!!).delete()
    }
}
