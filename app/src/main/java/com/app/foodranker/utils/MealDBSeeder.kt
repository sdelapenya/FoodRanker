package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random

object MealDBSeeder {

    private const val MEALDB  = "https://www.themealdb.com/api/json/v1/1"
    private const val PEXELS  = "https://api.pexels.com/v1/search"
    private val PEXELS_KEY get() = com.app.foodranker.BuildConfig.PEXELS_API_KEY

    // 14 categorías → ~112 platos con fotos de alta resolución de Pexels
    private val categories = listOf(
        "Seafood"       to PlateCategory.SEAFOOD,
        "Pasta"         to PlateCategory.PASTA,
        "Beef"          to PlateCategory.STEAK,
        "Dessert"       to PlateCategory.DESSERT,
        "Breakfast"     to PlateCategory.BREAKFAST,
        "Chicken"       to PlateCategory.OTHER,
        "Starter"       to PlateCategory.TAPAS,
        "Lamb"          to PlateCategory.STEAK,
        "Miscellaneous" to PlateCategory.OTHER,
        "Pork"          to PlateCategory.STEAK,
        "Vegan"         to PlateCategory.SALAD,
        "Vegetarian"    to PlateCategory.SALAD,
        "Side"          to PlateCategory.TAPAS,
        "Goat"          to PlateCategory.STEAK
    )

    // Búsqueda Pexels específica por categoría para fotos más relevantes
    private val pexelsQuery = mapOf(
        PlateCategory.SEAFOOD   to "seafood dish plate restaurant",
        PlateCategory.PASTA     to "pasta dish italian food",
        PlateCategory.STEAK     to "steak meat grilled food",
        PlateCategory.DESSERT   to "dessert cake sweet food",
        PlateCategory.BREAKFAST to "breakfast food plate morning",
        PlateCategory.TAPAS     to "appetizer tapas starter food",
        PlateCategory.SALAD     to "salad vegetarian healthy food",
        PlateCategory.SUSHI     to "sushi japanese food",
        PlateCategory.BURGER    to "burger sandwich food",
        PlateCategory.PIZZA     to "pizza italian food",
        PlateCategory.RAMEN     to "ramen noodles soup",
        PlateCategory.OTHER     to "food dish gourmet restaurant"
    )

    // Cache de imágenes Pexels por categoría para no repetir la misma foto
    private val pexelsImageCache = mutableMapOf<PlateCategory, MutableList<String>>()

    suspend fun seed(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }) {
        val db       = FirebaseFirestore.getInstance()
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerUid == null) {
            Log.w("MealDBSeeder", "No hay usuario autenticado")
            return
        }

        // Pre-cargar pool de imágenes Pexels para todas las categorías
        Log.d("MealDBSeeder", "Cargando imágenes de alta resolución de Pexels...")
        prefetchPexelsImages()

        var inserted = 0
        val total = categories.size * 8

        for ((categoryName, plateCategory) in categories) {
            try {
                val ids = fetchMealIds(categoryName).shuffled().take(8)
                for (mealId in ids) {
                    try {
                        val plate = fetchMealDetail(mealId, plateCategory, ownerUid) ?: continue
                        db.collection("plates").document(plate.id).set(plate).await()
                        inserted++
                        onProgress(inserted, total)
                    } catch (e: Exception) {
                        Log.e("MealDBSeeder", "Error meal $mealId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MealDBSeeder", "Error category $categoryName: ${e.message}")
            }
        }
        Log.d("MealDBSeeder", "Seeding completo: $inserted platos insertados")
        pexelsImageCache.clear()
    }

    /** Precarga ~15 fotos de Pexels por categoría en alta resolución */
    private suspend fun prefetchPexelsImages() = withContext(Dispatchers.IO) {
        val categoriesToFetch = PlateCategory.entries.distinct()

        for (category in categoriesToFetch) {
            try {
                val query  = pexelsQuery[category] ?: "food dish"
                val url    = java.net.URL("$PEXELS?query=${java.net.URLEncoder.encode(query, "UTF-8")}&per_page=15&orientation=portrait")
                val conn   = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", PEXELS_KEY)
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000

                if (conn.responseCode == 200) {
                    val json   = JSONObject(conn.inputStream.bufferedReader().readText())
                    val photos = json.optJSONArray("photos") ?: continue
                    val urls   = mutableListOf<String>()
                    for (i in 0 until photos.length()) {
                        val src = photos.getJSONObject(i).optJSONObject("src")
                        // large2x = ~1880px ancho, ideal para pantallas de alta densidad
                        val imgUrl = src?.optString("large2x")
                            ?: src?.optString("large")
                            ?: continue
                        if (imgUrl.isNotBlank()) urls.add(imgUrl)
                    }
                    if (urls.isNotEmpty()) {
                        pexelsImageCache[category] = urls.toMutableList()
                        Log.d("MealDBSeeder", "Pexels [$category]: ${urls.size} fotos cargadas")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w("MealDBSeeder", "Pexels error para $category: ${e.message}")
            }
        }
    }

    /** Devuelve la siguiente foto Pexels disponible para la categoría, sin repetir */
    private fun nextPexelsImage(category: PlateCategory): String? {
        val pool = pexelsImageCache[category] ?: return null
        if (pool.isEmpty()) return null
        return pool.removeAt(0) // FIFO — cada plato recibe una foto diferente
    }

    private suspend fun fetchMealIds(category: String): List<String> = withContext(Dispatchers.IO) {
        val json = java.net.URL("$MEALDB/filter.php?c=$category").readText()
        val arr  = JSONObject(json).optJSONArray("meals") ?: return@withContext emptyList()
        (0 until arr.length()).map { arr.getJSONObject(it).getString("idMeal") }
    }

    private suspend fun fetchMealDetail(
        id: String,
        category: PlateCategory,
        ownerUid: String
    ): Plate? = withContext(Dispatchers.IO) {
        try {
            val json = java.net.URL("$MEALDB/lookup.php?i=$id").readText()
            val meal = JSONObject(json).optJSONArray("meals")?.getJSONObject(0)
                ?: return@withContext null

            val area = meal.optString("strArea", "International")

            // Descripción más atractiva
            val rawDesc = meal.optString("strInstructions", "")
                .replace("\r\n", " ").replace("\n", " ").trim()
            val description = if (rawDesc.length > 220)
                rawDesc.take(220).trimEnd() + "..."
            else rawDesc

            // Imagen: Pexels alta resolución → fallback TheMealDB
            val imageUrl = nextPexelsImage(category)
                ?: meal.optString("strMealThumb", "")

            // Puntuaciones realistas para que el feed no muestre ★ 0.0
            val ratingCount = Random.nextInt(20, 380)
            val avgScore    = (Random.nextDouble(7.3, 9.85) * 10).toInt() / 10.0

            Plate(
                id               = "mdb_$id",
                name             = meal.optString("strMeal"),
                description      = description,
                category         = category,
                restaurantName   = areaToRestaurant(area),
                restaurantAddress = "",
                city             = areaToCity(area),
                country          = areaToCountry(area),
                imageUrl         = imageUrl,
                addedByUserId    = ownerUid,
                addedByUserName  = "FoodRanker Team",
                averageScore     = avgScore,
                totalRatings     = ratingCount,
                likes            = Random.nextInt(5, ratingCount),
                reportCount      = 0,
                status           = com.app.foodranker.data.model.PlateStatus.APPROVED,
                createdAt        = System.currentTimeMillis() - Random.nextLong(0, 90L * 24 * 3600 * 1000)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun areaToRestaurant(area: String) = when (area) {
        "Italian"    -> "Trattoria del Centro"
        "French"     -> "Brasserie Centrale"
        "Japanese"   -> "Izakaya Yuki"
        "Mexican"    -> "Cantina El Pueblo"
        "Indian"     -> "Spice Garden"
        "Chinese"    -> "Golden Dragon"
        "British"    -> "The Crown Pub"
        "American"   -> "Downtown Diner"
        "Spanish"    -> "Taberna El Rincón"
        "Greek"      -> "Taverna Kyria"
        "Thai"       -> "Bangkok Kitchen"
        "Moroccan"   -> "Riad Zahra"
        "Turkish"    -> "Kebab Sarayı"
        "Portuguese" -> "Tasca da Ribeira"
        "Croatian"   -> "Konoba Dalmatia"
        "Dutch"      -> "Café Amsterdam"
        "Egyptian"   -> "Al Azhar"
        "Filipino"   -> "Lutong Bahay"
        "Jamaican"   -> "Yard Vibes Kitchen"
        "Malaysian"  -> "Mamak Stall"
        "Polish"     -> "Karczma Polska"
        "Russian"    -> "Café Pushkin"
        "Tunisian"   -> "Le Médina"
        "Vietnamese" -> "Phở 24"
        else         -> "Local Restaurant"
    }

    private fun areaToCity(area: String) = when (area) {
        "Italian" -> "Roma"; "French" -> "París"; "Japanese" -> "Tokio"
        "Mexican" -> "Ciudad de México"; "Indian" -> "Mumbai"; "Chinese" -> "Shanghái"
        "British" -> "Londres"; "American" -> "Nueva York"; "Spanish" -> "Madrid"
        "Greek" -> "Atenas"; "Thai" -> "Bangkok"; "Moroccan" -> "Marrakech"
        "Turkish" -> "Estambul"; "Portuguese" -> "Lisboa"; "Croatian" -> "Dubrovnik"
        "Dutch" -> "Ámsterdam"; "Egyptian" -> "El Cairo"; "Filipino" -> "Manila"
        "Jamaican" -> "Kingston"; "Malaysian" -> "Kuala Lumpur"; "Polish" -> "Varsovia"
        "Russian" -> "Moscú"; "Tunisian" -> "Túnez"; "Vietnamese" -> "Hanói"
        else -> listOf("Nueva York", "Londres", "París", "Tokio", "Ciudad de México").random()
    }

    private fun areaToCountry(area: String) = when (area) {
        "Italian" -> "Italia"; "French" -> "Francia"; "Japanese" -> "Japón"
        "Mexican" -> "México"; "Indian" -> "India"; "Chinese" -> "China"
        "British" -> "Reino Unido"; "American" -> "EE.UU."; "Spanish" -> "España"
        "Greek" -> "Grecia"; "Thai" -> "Tailandia"; "Moroccan" -> "Marruecos"
        "Turkish" -> "Turquía"; "Portuguese" -> "Portugal"; "Croatian" -> "Croacia"
        "Dutch" -> "Países Bajos"; "Egyptian" -> "Egipto"; "Filipino" -> "Filipinas"
        "Jamaican" -> "Jamaica"; "Malaysian" -> "Malasia"; "Polish" -> "Polonia"
        "Russian" -> "Rusia"; "Tunisian" -> "Túnez"; "Vietnamese" -> "Vietnam"
        else -> listOf("EE.UU.", "Francia", "Italia", "Reino Unido", "España").random()
    }
}
