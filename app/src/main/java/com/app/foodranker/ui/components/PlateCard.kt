package com.app.foodranker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.theme.AppElevation
import com.app.foodranker.ui.theme.OrangePrimary
import com.app.foodranker.ui.theme.SurfaceWhite
import com.app.foodranker.ui.theme.TextPrimary
import com.app.foodranker.ui.theme.TextSecondary
import com.app.foodranker.utils.formatCompact

// Gradiente por categoría — mucho más atractivo que emoji sobre blanco
private fun PlateCategory.placeholderGradient() = when (this) {
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
    PlateCategory.OTHER     -> listOf(Color(0xFF787888), Color(0xFF505060))
}

@Composable
private fun PlateImagePlaceholder(category: PlateCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(category.placeholderGradient())
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(category.emoji, fontSize = 42.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                category.displayName,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlateCard(
    plate: Plate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLike: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { pressed = false },
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .width(180.dp)
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.cardRaised)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                if (plate.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = plate.imageUrl,
                        contentDescription = plate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            )
                    )
                } else {
                    PlateImagePlaceholder(
                        category = plate.category,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "★ ${"%.1f".format(plate.averageScore)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    plate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plate.restaurantName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${plate.city}, ${plate.country}",
                    fontSize = 11.sp,
                    color = OrangePrimary,
                    maxLines = 1
                )
                if (onLike != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LikeRowSmall(likes = plate.likes, isLiked = isLiked, onLike = onLike)
                }
            }
        }
    }
}

private val RankGold = Color(0xFFC9A227)
private val RankSilver = Color(0xFF7A8699)
private val RankBronze = Color(0xFFB87333)

@Composable
fun PlateCardHorizontal(
    plate: Plate,
    position: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLike: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { pressed = false },
        label = "cardHScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 36.dp)
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                val (label, color, sizeSp) = when (position) {
                    1 -> Triple("1", RankGold, 19.sp)
                    2 -> Triple("2", RankSilver, 18.sp)
                    3 -> Triple("3", RankBronze, 18.sp)
                    else -> Triple("#$position", OrangePrimary, 12.sp)
                }
                Text(
                    text = label,
                    fontSize = sizeSp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (plate.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = plate.imageUrl,
                        contentDescription = plate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlateImagePlaceholder(
                        category = plate.category,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plate.restaurantName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${plate.city}, ${plate.country}", fontSize = 11.sp, color = OrangePrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "★ ${"%.1f".format(plate.averageScore)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = OrangePrimary
                )
                Text("${plate.totalRatings.formatCompact()} votos", fontSize = 10.sp, color = TextSecondary)
                if (onLike != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LikeRowSmall(likes = plate.likes, isLiked = isLiked, onLike = onLike)
                }
            }
        }
    }
}

@Composable
private fun LikeRowSmall(likes: Int, isLiked: Boolean, onLike: () -> Unit) {
    var likePressed by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (likePressed) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        finishedListener = { likePressed = false },
        label = "likeScale"
    )
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE53935) else TextSecondary,
        animationSpec = tween(200),
        label = "heartColor"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        IconButton(
            onClick = { likePressed = true; onLike() }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isLiked) "Quitar me gusta" else "Me gusta",
                tint = heartColor,
                modifier = Modifier.size(22.dp).scale(likeScale)
            )
        }
        Text(likes.formatCompact(), fontSize = 11.sp, color = heartColor, fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal)
    }
}
