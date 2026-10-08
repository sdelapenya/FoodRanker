package com.app.foodranker.ui.screens.notifications

import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.R
import com.app.foodranker.data.model.FoodNotification
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.RewardManager
import com.app.foodranker.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlate: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ntf_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
                    }
                },
                actions = {
                    if (uiState.notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.ntf_clear),
                                tint = TextSecondary
                            )
                        }
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
                    Text(stringResource(R.string.ntf_empty), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.ntf_empty_hint), fontSize = 14.sp, color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(uiState.notifications, key = { _, notif -> notif.id }) { index, notif ->
                    // Se lee aqui, en ambito componible: dentro del onClick no se puede.
                    val avisoRechazado = stringResource(R.string.ntf_rejected_toast)
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
                                if (notif.type == "moderation_rejected") {
                                    // El plato fue eliminado — mostrar aviso en lugar de navegar
                                    android.widget.Toast.makeText(
                                        context,
                                        avisoRechazado,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else if (notif.type == "new_plate" && notif.plateCount > 1) {
                                    // Si anuncia varios platos, llevar a uno solo despista:
                                    // se va al perfil del autor, que es donde están todos.
                                    if (notif.fromUserId.isNotEmpty()) onNavigateToProfile(notif.fromUserId)
                                } else if (notif.type == "follow") {
                                    // No tiene plato: lleva al perfil de quien te sigue.
                                    if (notif.fromUserId.isNotEmpty()) onNavigateToProfile(notif.fromUserId)
                                } else if (notif.plateId.isNotEmpty()) {
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
            "comment"             -> "💬" to Color(0xFFE8F4FD)
            "follow"              -> "✨" to Color(0xFFF3E8FD)
            "league_result"       -> (if (notification.position == 1) "🥇" else "🏅") to Color(0xFFFFF4E0)
            "level_up"            -> "🎉" to Color(0xFFE8F5E9)
            "new_plate"           -> "🍽️" to Color(0xFFFFF1E6)
            "badge"               -> "🏅" to Color(0xFFFFF4E0)
            "moderation_approved" -> "✅" to Color(0xFFE8F5E9)
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
            // Sin remember: buildAnnotatedText es componible y su lambda no lo admite.
            // Compose ya evita rehacer el texto si la notificacion no cambia.
            val bodyText = buildAnnotatedText(notification)
            Text(
                bodyText,
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

// Construye el texto de la notificación según el tipo.
//
// Es componible porque los textos salen de los recursos. Al añadir un tipo hay que tocarlo
// aquí, en el switch de onNotificationCreated (servidor), en el escuchador de
// NotificationRepository y en el mapeo de NotificationsViewModel.
@Composable
private fun buildAnnotatedText(n: FoodNotification): String {
    val plato = "\"${n.plateName}\""
    return when (n.type) {
        "like" -> stringResource(R.string.ntf_like, n.fromUserName, plato)
        "rating" -> if (n.score > 0)
            stringResource(R.string.ntf_rating_score, n.fromUserName, plato, "%.1f".format(n.score))
        else
            stringResource(R.string.ntf_rating, n.fromUserName, plato)
        "comment" -> if (n.commentText.isNotBlank())
            stringResource(R.string.ntf_comment_text, n.fromUserName, plato, n.commentText)
        else
            stringResource(R.string.ntf_comment, n.fromUserName, plato)
        "follow" -> stringResource(R.string.ntf_follow, n.fromUserName)
        // El contador se acumula durante el día aunque solo suene el primero.
        "new_plate" -> if (n.plateCount > 1)
            stringResource(R.string.ntf_new_plates, n.fromUserName, n.plateCount)
        else
            stringResource(R.string.ntf_new_plate, n.fromUserName, plato)
        // El nombre del nivel y del logro los resuelve RewardManager, no el texto que mandó
        // el servidor: así la app enseña siempre su propia lista y no dos nombres distintos.
        "level_up" -> {
            val nivel = RewardManager.LEVELS.find { it.number == n.level }
            val etiqueta = if (nivel != null) "${nivel.emoji} " + stringResource(nivel.nameRes)
                           else n.level.toString()
            stringResource(R.string.ntf_level_up, etiqueta)
        }
        "badge" -> {
            val logro = RewardManager.getBadge(n.badgeId)
            val etiqueta = if (logro != null) "${logro.emoji} " + stringResource(logro.nameRes)
                           else n.plateName
            stringResource(R.string.ntf_badge, etiqueta)
        }
        "league_result" -> if (n.position == 1) stringResource(R.string.ntf_league_win)
                           else stringResource(R.string.ntf_league_pos, n.position)
        "moderation_approved" -> stringResource(R.string.ntf_approved, plato)
        "moderation_rejected" -> {
            val motivo = when {
                n.reasons.contains("not_food")  -> stringResource(R.string.ntf_reason_not_food)
                n.reasons.contains("adult")     -> stringResource(R.string.ntf_reason_adult)
                n.reasons.contains("violence")  -> stringResource(R.string.ntf_reason_violence)
                n.reasons.contains("racy")      -> stringResource(R.string.ntf_reason_racy)
                n.reasons.contains("no_image")  -> stringResource(R.string.ntf_reason_no_image)
                else -> stringResource(R.string.ntf_reason_other)
            }
            stringResource(R.string.ntf_rejected, plato, motivo)
        }
        // Respaldo para tipos que esta versión aún no conoce. Se comprueba el nombre porque
        // no todos los avisos van sobre un plato — el de seguidor no lo lleva, y sin esto se
        // leería: Nueva notificación sobre "".
        else -> if (n.plateName.isNotBlank()) stringResource(R.string.ntf_generic_about, plato)
                else stringResource(R.string.ntf_generic)
    }
}

@Composable
private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1)  -> stringResource(R.string.ntf_now)
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val m = TimeUnit.MILLISECONDS.toMinutes(diff)
            if (m == 1L) stringResource(R.string.ntf_min_ago) else stringResource(R.string.ntf_mins_ago, m.toInt())
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            if (h == 1L) stringResource(R.string.ntf_hour_ago) else stringResource(R.string.ntf_hours_ago, h.toInt())
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val d = TimeUnit.MILLISECONDS.toDays(diff)
            if (d == 1L) stringResource(R.string.ntf_day_ago) else stringResource(R.string.ntf_days_ago, d.toInt())
        }
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
