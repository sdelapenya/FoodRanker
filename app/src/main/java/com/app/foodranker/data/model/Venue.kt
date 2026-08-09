package com.app.foodranker.data.model

/**
 * Local canónico. El id del documento es el `place_id` de Google Places, que es
 * estable en el tiempo — por eso sirve como identidad canónica del sitio.
 *
 * Lo escribe SOLO la Cloud Function `resolveVenue` (Admin SDK). El cliente no puede
 * escribir aquí: este documento lo ven todos los usuarios, así que si el cliente
 * pudiera editarlo, cualquiera podría renombrar un local para todo el mundo.
 */
data class Venue(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    /** minúsculas y sin acentos — es la que se usa para liga y ranking por ciudad */
    val cityNormalized: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val plateCount: Int = 0,
    val createdAt: Long = 0L
)
