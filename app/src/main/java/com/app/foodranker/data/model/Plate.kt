package com.app.foodranker.data.model

data class Plate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    /**
     * Id de la categoría tal cual viene de Firestore ("PASTA", "RICE"...). Es texto, NO el
     * enum, a propósito.
     *
     * El SDK de Firestore convierte los enums con valueOf y **lanza una excepción** si el
     * valor no está en la lista que conoce esa versión de la app (CustomClassMapper:
     * "Could not find enum value of ... for value ..." → athrow). Como las pantallas
     * convierten los platos en bloque, un solo plato con una categoría añadida después
     * dejaría el ranking ENTERO en blanco para quien no se hubiera actualizado.
     *
     * Con texto, una categoría desconocida cae en "Otros" y no pasa nada. Para usarla,
     * [categoryType].
     */
    val category: String = PlateCategory.OTHER.name,
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
    /** ISO del país del local (ES, GB...). Neutro: de aquí sale la moneda del precio. */
    val countryCode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String = "",
    val addedByUserId: String = "",
    val addedByUserName: String = "",
    /** La nota que se MUESTRA. Para ordenar se usa [rankingScore]. */
    val averageScore: Double = 0.0,
    val totalRatings: Int = 0,
    /**
     * Media bayesiana — lo que ORDENA el ranking. Un plato con pocos votos tira hacia la media
     * global, así que un único voto (el del autor al publicar) no puede encabezar nada.
     * Lo escribe solo Cloud Functions. Ver docs/RATINGS.md §1.5.
     */
    val rankingScore: Double = 0.0,
    /** Cuántos han dicho que SÍ lo volverían a pedir. */
    val wouldOrderAgainCount: Int = 0,
    /**
     * Cuántos han contestado a la pregunta — el denominador del %. No vale usar
     * [totalRatings]: las valoraciones anteriores al rediseño no la contestaron, y con ellas
     * en el denominador el porcentaje saldría hundido.
     */
    val wouldOrderAgainResponses: Int = 0,
    /** Mediana de los precios reportados, en céntimos. Null mientras nadie lo haya aportado. */
    val priceMedianCents: Int? = null,
    /** Cuántos han aportado precio: gobierna si se enseña fracción, media o nada. */
    val priceReportCount: Int = 0,
    val priceUpdatedAt: Long = 0L,
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
    // Añadidas tras ver que 28 de 50 platos reales caían en "Otros": 7 eran arroces y
    // paellas, 6 pescados, y el resto verduras, huevos y platos de cuchara. "Mariscos"
    // existía desde el principio, pero pescado no, que en una app española es raro.
    RICE("Arroces", "🍚"),
    FISH("Pescado", "🐟"),
    VEGGIE("Verduras", "🥦"),
    EGGS("Huevos", "🍳"),
    SOUP("Sopas y guisos", "🍲"),
    SANDWICH("Bocadillos", "🥪"),
    OTHER("Otros", "🍽️");

    companion object {
        /**
         * Convierte el id guardado en una categoría, cayendo en [OTHER] si no se reconoce.
         *
         * Esta es la única forma de leer la categoría: nunca `valueOf`, que revienta con los
         * ids añadidos en versiones posteriores. Ver el comentario de `Plate.category`.
         */
        fun fromId(id: String?): PlateCategory =
            entries.firstOrNull { it.name.equals(id?.trim(), ignoreCase = true) } ?: OTHER
    }
}

/**
 * Categoría del plato, ya resuelta. Va como extensión y no como propiedad del data class
 * porque Firestore serializa también los getters públicos: dentro de Plate acabaría como un
 * campo más del documento, y la lista blanca de `firestore.rules` rechazaría la publicación.
 */
val Plate.categoryType: PlateCategory get() = PlateCategory.fromId(category)
