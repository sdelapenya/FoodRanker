package com.app.foodranker.data.model

data class Report(
    val id: String = "",
    val plateId: String = "",
    // Vacío si el reporte es de un plato. Si es de un comentario, plateId sigue
    // guardando el plato al que pertenece (contexto) y commentId identifica el comentario.
    val commentId: String = "",
    val reportedByUserId: String = "",
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
