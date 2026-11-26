package com.app.foodranker.ui.components

import com.app.foodranker.utils.votesLabel
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
import com.app.foodranker.data.model.categoryType
import com.app.foodranker.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow

/**
 * ⚠️ Los colores de esta tarjeta están clavados **a propósito**, y es el único sitio de la
 * app donde eso es correcto.
 *
 * El 2026-11-17 las 21 pantallas pasaron de colores fijos a `FoodTheme.colors`, para que el
 * modo oscuro funcione. Este fichero quedó fuera: no se dibuja para el usuario, se
 * convierte en la **imagen que se comparte** (ver `ShareManager`). Si siguiera el tema, la
 * misma imagen saldría clara o oscura según cómo tenga el móvil quien comparte, y además la
 * leería gente que no tiene la app. Una imagen que se publica tiene que salir igual
 * siempre.
 *
 * O sea: si alguien ve colores fijos aquí y los "arregla", rompe la función.
 */
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
                Text(plate.categoryType.emoji, fontSize = 80.sp)
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
                        color = FoodTheme.colors.onOrange,
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${plate.restaurantName} · ${plate.city}",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "★ ${"%.1f".format(plate.averageScore)}/10",
                        color = FoodTheme.colors.onOrange,
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
                        "👥 ${plate.totalRatings.votesLabel()}",
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
        color = BackgroundLight // tema-fijo: imagen que se comparte
    ) {
        Text(
            text = if (value != null) "$label: ${"%.1f".format(value)}" else label,
            fontSize = 11.sp,
            color = TextSecondary, // tema-fijo: imagen que se comparte
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
