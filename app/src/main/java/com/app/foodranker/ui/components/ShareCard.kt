package com.app.foodranker.ui.components

import com.app.foodranker.utils.formatCompact
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
fun ShareCard(plate: Plate, cityRank: Int = 0, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(320.dp)
            .height(480.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Full-bleed background image
        if (plate.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = plate.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1F19), Color(0xFF2E3A2E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(plate.category.emoji, fontSize = 80.sp)
            }
        }

        // Dark gradient overlay — lighter at top, heavy at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.80f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Top bar: FoodRanker logo + rank badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    "🍽️ FoodRanker",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            if (cityRank > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "#$cityRank en ${plate.city}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Bottom info block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                plate.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                maxLines = 2
            )
            Text(
                "${plate.restaurantName} · ${plate.city}",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "★ ${"%.1f".format(plate.averageScore)}/10",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        "👥 ${plate.totalRatings.formatCompact()} votos",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
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
