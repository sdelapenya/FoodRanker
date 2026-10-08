package com.app.foodranker.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.app.foodranker.R

/**
 * Formato compacto estilo redes sociales: 999, 1.2K, 12K, 1.2M, 12M.
 * Útil para likes / votos cuando los números crecen.
 */
fun Int.formatCompact(): String = when {
    this < 1_000 -> this.toString()
    this < 1_000_000 -> {
        val k = this / 1000.0
        if (k >= 10) "${k.toInt()}K" else "%.1fK".format(k)
    }
    else -> {
        val m = this / 1_000_000.0
        if (m >= 10) "${m.toInt()}M" else "%.1fM".format(m)
    }
}

/**
 * "1 voto" / "2 votos" / "1,2K votos" — evita el "1 votos" que se veía antes.
 *
 * El singular lo decide el valor real, no el texto ya formateado, y lo resuelve el sistema
 * de plurales de Android: cada idioma tiene sus propias reglas y no todos parten en uno,
 * así que decidirlo con un `if` solo funciona de casualidad en castellano e inglés.
 *
 * Es componible porque necesita leer los recursos del idioma activo.
 */
@Composable
fun Int.votesLabel(): String =
    pluralStringResource(R.plurals.votes_count, this, formatCompact())
