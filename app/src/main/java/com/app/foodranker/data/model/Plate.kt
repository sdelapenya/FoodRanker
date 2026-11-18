package com.app.foodranker.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.app.foodranker.R

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

/**
 * El nombre va como referencia a recurso y no como texto: el enum se declara una sola
 * vez al cargar la clase, mucho antes de saber en qué idioma está el móvil, así que un
 * String aquí se quedaría clavado en castellano. Para pintarlo, [categoryName].
 */
enum class PlateCategory(@StringRes val nameRes: Int, val emoji: String) {
    PASTA(R.string.cat_pasta, "🍝"),
    SUSHI(R.string.cat_sushi, "🍣"),
    BURGER(R.string.cat_burger, "🍔"),
    PIZZA(R.string.cat_pizza, "🍕"),
    TAPAS(R.string.cat_tapas, "🥘"),
    RAMEN(R.string.cat_ramen, "🍜"),
    STEAK(R.string.cat_steak, "🥩"),
    SEAFOOD(R.string.cat_seafood, "🦞"),
    DESSERT(R.string.cat_dessert, "🍰"),
    BREAKFAST(R.string.cat_breakfast, "🥐"),
    SALAD(R.string.cat_salad, "🥗"),
    // Añadidas tras ver que 28 de 50 platos reales caían en "Otros": 7 eran arroces y
    // paellas, 6 pescados, y el resto verduras, huevos y platos de cuchara. "Mariscos"
    // existía desde el principio, pero pescado no, que en una app española es raro.
    RICE(R.string.cat_rice, "🍚"),
    FISH(R.string.cat_fish, "🐟"),
    VEGGIE(R.string.cat_veggie, "🥦"),
    EGGS(R.string.cat_eggs, "🍳"),
    SOUP(R.string.cat_soup, "🍲"),
    SANDWICH(R.string.cat_sandwich, "🥪"),
    OTHER(R.string.cat_other, "🍽️");

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

/** Nombre de la categoria en el idioma activo. */
@Composable
fun PlateCategory.categoryName(): String = stringResource(nameRes)

/**
 * Umbral de denuncias a partir del cual un plato deja de enseñarse.
 *
 * Es el mismo número que usan las denuncias de valoraciones y comentarios, pero vive
 * aquí porque el de platos es el único que decide si algo sale en los listados.
 */
const val UMBRAL_DENUNCIAS = 3

/**
 * ¿Se le puede enseñar este plato a cualquiera?
 *
 * ## Por qué hace falta el estado y no basta con las denuncias
 *
 * Un plato nace con `status = "pending"` y lo aprueba `moderatePlateImage`, un trigger
 * `onCreate` que pasa la foto por Vision (SafeSearch + detección de comida) y **borra**
 * lo que rechaza. No hay cola de revisión humana: lo normal es que `pending` dure los
 * segundos que tarde Vision.
 *
 * Aun así hay que filtrarlo, por dos motivos que no se ven leyendo solo el camino feliz:
 *
 *  1. Los listados que ordenan por `createdAt` ("recientes", "de a quien sigo") ponen el
 *     plato recién subido **el primero**. O sea, justo el que todavía no ha mirado nadie
 *     queda arriba del todo mientras se modera.
 *  2. Si la función no llega a correr — cuota agotada, caída de Vision, un despliegue a
 *     medias — el plato se queda en `pending` para siempre, y sin este filtro, visible
 *     para siempre.
 *
 * Hasta el 2026-11-18 no lo comprobaba **ningún** listado: ni Inicio, ni Descubrir, ni
 * Explorar, ni Tendencias, ni Cerca de ti. El único sitio de toda la app que exigía
 * `approved` era el contador de puesto en la ciudad de PlateDetailViewModel.
 *
 * ## Dónde NO se usa esto, a propósito
 *
 * - **Tu propio perfil.** Tu plato recién subido tiene que verse aunque esté pendiente,
 *   o parece que se ha perdido al publicarlo. En el perfil de otro sí se filtra.
 * - **Guardados y colecciones.** Son listas que el usuario ha montado a mano, y solo
 *   pudo guardar lo que ya podía ver.
 */
val Plate.esPublico: Boolean
    get() = status == PlateStatus.APPROVED && reportCount < UMBRAL_DENUNCIAS

/**
 * Deja solo los platos que se pueden enseñar. Ver [esPublico].
 *
 * Existe como función y no como una condición repetida en cada pantalla por lo mismo
 * que `Fallos.reportar`: la línea que se copia en once sitios es la que alguien olvida
 * en el doce. Si cambia el umbral o se añade otro estado, se cambia aquí.
 */
fun List<Plate>.soloPublicos(): List<Plate> = filter { it.esPublico }
