package com.app.foodranker.ui.screens.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.data.model.FoodNotification
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlate: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Sin notificaciones", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Cuando alguien interactúe con tus platos\naparecerá aquí", fontSize = 14.sp, color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(uiState.notifications) { index, notif ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(notif.id) {
                        kotlinx.coroutines.delay(index * 40L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(initialOffsetX = { -60 }, animationSpec = tween(300)) + fadeIn(tween(300))
                    ) {
                        NotificationItem(
                            notification = notif,
                            onClick = {
                                // Solo navegamos al plato si sigue existiendo (no en rechazos).
                                if (notif.type != "moderation_rejected" && notif.plateId.isNotEmpty()) {
                                    onNavigateToPlate(notif.plateId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: FoodNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (!notification.isRead) OrangePrimary.copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val (icon, iconBg) = when (notification.type) {
            "like"                -> "❤️" to Color(0xFFFFEBEE)
            "rating"              -> "⭐"  to Color(0xFFFFF8E1)
            "moderation_rejected" -> "⚠️" to Color(0xFFFDECEA)
            else                  -> "🔔" to Color(0xFFEEEEEE)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedText(notification),
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                timeAgo(notification.createdAt),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Punto no leído
        if (!notification.isRead) {
            Box(
                modifier = Modifier.size(8.dp).background(OrangePrimary, CircleShape)
            )
        }
    }
    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
}

// Construye el texto de la notificación según el tipo. Reutilizable y
// fácil de extender si añadimos más tipos en el futuro.
private fun buildAnnotatedText(n: FoodNotification): String = when (n.type) {
    "like" -> "${n.fromUserName} le ha dado like a \"${n.plateName}\""
    "rating" -> buildString {
        append(n.fromUserName)
        append(" ha valorado \"${n.plateName}\"")
        if (n.score > 0) append(" con ${"%.1f".format(n.score)}")
    }
    "moderation_rejected" -> {
        val reasonText = when {
            n.reasons.contains("not_food")  -> "no parece comida"
            n.reasons.contains("adult")     -> "contenido inapropiado"
            n.reasons.contains("violence")  -> "contenido violento"
            n.reasons.contains("racy")      -> "contenido sugerente"
            n.reasons.contains("no_image")  -> "no tenía imagen"
            else -> "no superó la moderación"
        }
        "Tu plato \"${n.plateName}\" fue rechazado: $reasonText"
    }
    else -> "Nueva notificación sobre \"${n.plateName}\""
}

private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1)  -> "Ahora mismo"
        diff < TimeUnit.HOURS.toMillis(1)    -> "Hace ${TimeUnit.MILLISECONDS.toMinutes(diff)} min"
        diff < TimeUnit.DAYS.toMillis(1)     -> "Hace ${TimeUnit.MILLISECONDS.toHours(diff)} h"
        diff < TimeUnit.DAYS.toMillis(7)     -> "Hace ${TimeUnit.MILLISECONDS.toDays(diff)} días"
        else -> SimpleDateFormat("dd MMM", Locale("es")).format(Date(timestamp))
    }
}
