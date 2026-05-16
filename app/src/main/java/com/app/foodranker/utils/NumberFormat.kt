package com.app.foodranker.utils

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
