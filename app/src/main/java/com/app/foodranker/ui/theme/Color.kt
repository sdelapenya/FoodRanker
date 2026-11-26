package com.app.foodranker.ui.theme

import androidx.compose.ui.graphics.Color

// Naranja principal (igual en ambos modos)
val OrangePrimary = Color(0xFFFF6B35)
val OrangeLight   = Color(0xFFFF8E53)
val OrangeDark    = Color(0xFFE85520)

// ── Modo claro ────────────────────────────────
val BackgroundLight = Color(0xFFF8F9FA)
val SurfaceWhite    = Color(0xFFFFFFFF)
val SurfaceMuted    = Color(0xFFEEF0F4)
val TextPrimary     = Color(0xFF1A1A2E)
val TextSecondary   = Color(0xFF6B7280)
val DividerColor    = Color(0xFFE5E7EB)

// ── Modo oscuro ───────────────────────────────
val BackgroundDark  = Color(0xFF0D0D0D)
val SurfaceDark     = Color(0xFF1C1C1E)
val SurfaceMutedDark = Color(0xFF2C2C2E)
val TextPrimaryDark = Color(0xFFF2F2F7)
val TextSecondaryDark = Color(0xFF9098A6)
val DividerDark     = Color(0xFF2C2C2E)

// Acento y estado (iguales en ambos modos)
val StarYellow  = Color(0xFFFFC107)
val SuccessGreen = Color(0xFF10B981)
val ErrorRed    = Color(0xFFEF4444)

// ── Acentos suaves ────────────────────────────────
// Las tarjetas "destacadas" (invitar a un amigo, el aviso de la liga, el podio) van
// sobre un fondo de color muy claro. Estaban escritas a mano en cada pantalla, y por
// eso no cambiaban con el tema: en oscuro quedaba un rectangulo crema con el texto
// del tema encima, casi blanco sobre casi blanco. Ahora son pares (fondo, texto) con
// un valor por modo. Comprobados con tools/ui/check_contrast.py.
val AccentSoftLight        = Color(0xFFFFF3E0)
val AccentSoftDark         = Color(0xFF3A2A1C)
val AccentSoftBorderLight  = Color(0xFFFFCC80)
val AccentSoftBorderDark   = Color(0xFF5C4327)
val OnAccentSoftLight      = Color(0xFF5D3A00)
val OnAccentSoftDark       = Color(0xFFF3DCC4)
val OnAccentSoftMutedLight = Color(0xFF7A5020)
val OnAccentSoftMutedDark  = Color(0xFFD2B394)

val SuccessSoftLight        = Color(0xFFEAF7EE)
val SuccessSoftDark         = Color(0xFF17291F)
val SuccessSoftBorderLight  = Color(0xFFB9DEC6)
val SuccessSoftBorderDark   = Color(0xFF2F4F3C)
val OnSuccessSoftLight      = Color(0xFF153B2A)
val OnSuccessSoftDark       = Color(0xFFD5EEE0)
val OnSuccessSoftMutedLight = Color(0xFF516D5E)
val OnSuccessSoftMutedDark  = Color(0xFF9EC6B0)

// El verde "fuerte" de los acentos (XP ganado, el usuario actual en la liga). El de
// modo claro no vale en oscuro: 0xFF168963 sobre fondo oscuro se queda en 3,4.
val SuccessStrongLight = Color(0xFF117A56)
val SuccessStrongDark  = Color(0xFF3FD69B)

// El naranja "fuerte", y es SOLO para texto. Mismo problema y misma solución que el verde
// de arriba: `OrangePrimary` (0xFFFF6B35) es el naranja de marca y vale como fondo, como
// icono y como logo, pero como LETRA sobre fondo claro se queda en 2,84 y WCAG AA pide
// 4,5. No se toca el de marca —cambiarlo sería cambiar la identidad de la app—, se añade
// una variante legible al lado.
//
// ⚠️ Hacen falta los dos, uno por tema: el oscurecido para claro no vale en oscuro ni al
// revés. Medido: 0xFFC64600 da 4,91 sobre blanco, y 0xFFFF8A50 da 8,03 sobre el fondo
// oscuro. Al cambiar cualquiera de los dos, pasar tools/ui/check_contrast.py.
val OrangeStrongLight = Color(0xFFC64600)
val OrangeStrongDark  = Color(0xFFFF8A50)
