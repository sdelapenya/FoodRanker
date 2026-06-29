package com.app.foodranker.ui.screens.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.AdManager
import com.app.foodranker.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val billingViewModel: BillingViewModel = hiltViewModel()
    var watchingAd by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }
    val isPremium          by billingViewModel.isPremium.collectAsState()
    val isBillingAvailable by billingViewModel.isAvailable.collectAsState()
    val price              by billingViewModel.price.collectAsState()
    var billingError  by remember { mutableStateOf<String?>(null) }

    // Si el usuario cancela el diálogo nativo de Google Play y vuelve a esta pantalla,
    // no hay callback de "compra cancelada" — reactivamos el botón al volver a ON_RESUME.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isPurchasing = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header premium
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(OrangePrimary, OrangeDark)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 48.sp)
                    Text(
                        "FoodRanker Premium",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "La experiencia gastronómica completa",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Beneficios
            val benefits = listOf(
                "Sin anuncios" to "Disfruta sin interrupciones",
                "Estadísticas avanzadas" to "Analiza tus valoraciones en detalle",
                "Platos ilimitados" to "Sin límite de publicaciones al día",
                "Badge Premium" to "Destaca en la comunidad",
                "Acceso anticipado" to "Nuevas funciones antes que nadie"
            )

            benefits.forEach { (title, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(subtitle, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isPremium) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SuccessGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        "🎉 ¡Eres Premium! Gracias por apoyar FoodRanker",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                billingError?.let {
                    Text(it, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                }

                // Opción 1 — Ver anuncio (gratis temporal)
                OutlinedButton(
                    onClick = {
                        watchingAd = true
                        val activity = context as? Activity ?: run { watchingAd = false; return@OutlinedButton }
                        AdManager.showRewarded(
                            activity = activity,
                            onRewarded = { billingViewModel.grantTemporaryPremium() },
                            onDismiss = { watchingAd = false }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !watchingAd,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary)
                ) {
                    Text("📺 Ver anuncio — 24h Premium gratis", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opción 2 — Suscripción real con Google Play Billing
                Button(
                    onClick = {
                        isPurchasing = true
                        val activity = context as? Activity
                        if (activity == null) {
                            billingError = "No se pudo iniciar la compra. Inténtalo de nuevo."
                            isPurchasing = false
                        } else if (isBillingAvailable) {
                            val ok = billingViewModel.launchPurchase(activity)
                            if (!ok) {
                                billingError = "No se pudo iniciar la compra. Inténtalo de nuevo."
                                isPurchasing = false
                            }
                        } else {
                            billingError = "Las compras no están disponibles en este momento."
                            isPurchasing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isPurchasing,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "⭐ Suscribirse — $price",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text("Cancela cuando quieras", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}