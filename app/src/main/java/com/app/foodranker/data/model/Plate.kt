package com.app.foodranker.data.model

data class Plate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: PlateCategory = PlateCategory.OTHER,
    val restaurantName: String = "",
    val restaurantAddress: String = "",
    val city: String = "",
    val country: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String = "",
    val addedByUserId: String = "",
    val addedByUserName: String = "",
    val averageScore: Double = 0.0,
    val totalRatings: Int = 0,
    val createdAt: Long = 0L,
    val likes: Int = 0,
    val likedByUsers: List<String> = emptyList(),
    val reportCount: Int = 0
)

enum class PlateCategory(val displayName: String, val emoji: String) {
    PASTA("Pasta", "🍝"),
    SUSHI("Sushi", "🍣"),
    BURGER("Hamburguesa", "🍔"),
    PIZZA("Pizza", "🍕"),
    TAPAS("Tapas", "🥘"),
    RAMEN("Ramen", "🍜"),
    STEAK("Carne", "🥩"),
    SEAFOOD("Mariscos", "🦞"),
    DESSERT("Postres", "🍰"),
    BREAKFAST("Desayuno", "🥐"),
    SALAD("Ensaladas", "🥗"),
    OTHER("Otros", "🍽️")
}
