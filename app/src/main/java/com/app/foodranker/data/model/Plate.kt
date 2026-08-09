package com.app.foodranker.data.model

data class Plate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: PlateCategory = PlateCategory.OTHER,
    /**
     * place_id del local (doc id en `venues`). Junto con [dishSlug] forma el id de
     * este documento: `{venueId}__{dishSlug}`, de modo que el mismo plato en el mismo
     * local es siempre el mismo documento. Ver docs/VENUES.md.
     */
    val venueId: String = "",
    /** nombre del plato normalizado — ver `String.toDishSlug()` */
    val dishSlug: String = "",
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
    val reportCount: Int = 0,
    val status: String = ""
)

object PlateStatus {
    const val PENDING  = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

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
