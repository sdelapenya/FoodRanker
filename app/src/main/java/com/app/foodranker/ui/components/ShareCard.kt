package com.app.foodranker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.foodranker.data.model.Plate
import com.app.foodranker.ui.theme.*

@Composable
fun ShareCard(plate: Plate, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(320.dp)
            .height(400.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        Column {
            // Imagen con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(DividerColor)
            ) {
                if (plate.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = plate.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        plate.category.emoji,
                        fontSize = 72.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Gradiente sobre imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 100f
                            )
                        )
                )
                // Puntuación encima de imagen
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "★ ${"%.1f".format(plate.averageScore)}/10",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    plate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    plate.restaurantName,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    "📍 ${plate.city}, ${plate.country}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Mini scores
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniScoreBadge("😋 Sabor", plate.averageScore)
                    MiniScoreBadge("👥 ${plate.totalRatings} votos", null)
                }
            }
        }

        // Footer con branding
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = OrangePrimary
        ) {
            Text(
                "🍽️ FoodRanker — Los mejores platos del mundo",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun MiniScoreBadge(label: String, value: Double?) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BackgroundLight
    ) {
        Text(
            text = if (value != null) "$label: ${"%.1f".format(value)}" else label,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}