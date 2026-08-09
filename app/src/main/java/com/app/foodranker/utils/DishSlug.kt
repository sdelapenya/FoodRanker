package com.app.foodranker.utils

import java.text.Normalizer

/**
 * Normaliza el nombre de un plato para construir el id determinista
 * `{placeId}__{dishSlug}` con el que Firestore impone el deduplicado: dos personas
 * añadiendo el mismo plato en el mismo local escriben en el mismo documento.
 *
 * Deliberadamente NO se quitan artículos ni palabras vacías: "la carbonara" y
 * "carbonara" son técnicamente distintos y fusionarlos a ciegas puede unir platos
 * que no lo son.
 *
 * Esto solo caza duplicados exactos tras normalizar — "Carbonara" y "Spaghetti
 * carbonara" siguen siendo dos platos. Es la red de seguridad; quien evita los
 * casi-duplicados es la UI que muestra los platos ya registrados en el local antes
 * de dejar crear uno nuevo.
 */
fun String.toDishSlug(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        // Descarta los diacríticos que NFD acaba de separar (tildes, diéresis...).
        // La ñ se descompone en n + tilde, así que "Ñoquis" y "Noquis" colapsan al
        // mismo slug — es lo que queremos: son el mismo plato mal escrito.
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        // Cualquier cosa que no sea letra o dígito pasa a ser separador
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

/**
 * Id de documento del plato. Vacío si falta algún componente, para que quien llame
 * pueda rechazar la creación en vez de escribir un id corrupto.
 */
fun plateDocId(venueId: String, dishName: String): String {
    val slug = dishName.toDishSlug()
    if (venueId.isBlank() || slug.isBlank()) return ""
    return "${venueId}__$slug"
}
