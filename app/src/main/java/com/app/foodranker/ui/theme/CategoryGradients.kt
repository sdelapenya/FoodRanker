package com.app.foodranker.ui.theme

import androidx.compose.ui.graphics.Color
import com.app.foodranker.data.model.PlateCategory

// Gradiente oscuro — usado en fondos full-screen (PlateDetail, DiscoverScreen ranking card)
fun PlateCategory.categoryGradient(): List<Color> = when (this) {
    PlateCategory.PASTA     -> listOf(Color(0xFFE8A838), Color(0xFF6B3A00))
    PlateCategory.SUSHI     -> listOf(Color(0xFF2D7AAF), Color(0xFF0A1628))
    PlateCategory.BURGER    -> listOf(Color(0xFFBF4828), Color(0xFF3A0A00))
    PlateCategory.PIZZA     -> listOf(Color(0xFFD44030), Color(0xFF3A0500))
    PlateCategory.TAPAS     -> listOf(Color(0xFFBF6840), Color(0xFF3A1800))
    PlateCategory.RAMEN     -> listOf(Color(0xFFD47828), Color(0xFF3A1200))
    PlateCategory.STEAK     -> listOf(Color(0xFF8B4040), Color(0xFF1A0505))
    PlateCategory.SEAFOOD   -> listOf(Color(0xFF2090B0), Color(0xFF001828))
    PlateCategory.DESSERT   -> listOf(Color(0xFFD46898), Color(0xFF3A0828))
    PlateCategory.BREAKFAST -> listOf(Color(0xFFD4A828), Color(0xFF3A2800))
    PlateCategory.SALAD     -> listOf(Color(0xFF4A9848), Color(0xFF081A05))
    PlateCategory.RICE      -> listOf(Color(0xFFD9A441), Color(0xFF3A2400))
    PlateCategory.FISH      -> listOf(Color(0xFF3E8FA8), Color(0xFF04202C))
    PlateCategory.VEGGIE    -> listOf(Color(0xFF5A9A4A), Color(0xFF0C1E06))
    PlateCategory.EGGS      -> listOf(Color(0xFFE0B13A), Color(0xFF3A2A00))
    PlateCategory.SOUP      -> listOf(Color(0xFFC06838), Color(0xFF301000))
    PlateCategory.SANDWICH  -> listOf(Color(0xFFC89050), Color(0xFF342000))
    PlateCategory.OTHER     -> listOf(Color(0xFF787888), Color(0xFF101018))
}

// Gradiente claro — usado en miniaturas de tarjeta (PlateCard, ProfileScreen grid)
fun PlateCategory.cardGradient(): List<Color> = when (this) {
    PlateCategory.PASTA     -> listOf(Color(0xFFE8A838), Color(0xFFBF7A1A))
    PlateCategory.SUSHI     -> listOf(Color(0xFF2D7AAF), Color(0xFF14486A))
    PlateCategory.BURGER    -> listOf(Color(0xFFBF4828), Color(0xFF8B2A10))
    PlateCategory.PIZZA     -> listOf(Color(0xFFD44030), Color(0xFFAA2015))
    PlateCategory.TAPAS     -> listOf(Color(0xFFBF6840), Color(0xFF8B3D20))
    PlateCategory.RAMEN     -> listOf(Color(0xFFD47828), Color(0xFFAA5000))
    PlateCategory.STEAK     -> listOf(Color(0xFF8B4040), Color(0xFF5C2020))
    PlateCategory.SEAFOOD   -> listOf(Color(0xFF2090B0), Color(0xFF0D5878))
    PlateCategory.DESSERT   -> listOf(Color(0xFFD46898), Color(0xFFA83868))
    PlateCategory.BREAKFAST -> listOf(Color(0xFFD4A828), Color(0xFFA07818))
    PlateCategory.SALAD     -> listOf(Color(0xFF4A9848), Color(0xFF2A6828))
    PlateCategory.RICE      -> listOf(Color(0xFFD9A441), Color(0xFFA87420))
    PlateCategory.FISH      -> listOf(Color(0xFF3E8FA8), Color(0xFF1E6076))
    PlateCategory.VEGGIE    -> listOf(Color(0xFF5A9A4A), Color(0xFF356A28))
    PlateCategory.EGGS      -> listOf(Color(0xFFE0B13A), Color(0xFFB08418))
    PlateCategory.SOUP      -> listOf(Color(0xFFC06838), Color(0xFF8E4420))
    PlateCategory.SANDWICH  -> listOf(Color(0xFFC89050), Color(0xFF996A30))
    PlateCategory.OTHER     -> listOf(Color(0xFF787888), Color(0xFF505060))
}
