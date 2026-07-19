package com.app.foodranker.ui.screens.premium

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.theme.*

/**
 * Pantalla huérfana: no hay navegación activa hacia Premium.
 * Stub honesto hasta entitlement real (Billing + rules server-side).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Próximamente", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Premium aún no está disponible. La app actual es gratis con anuncios en otros puntos; " +
                    "no hay suscripción ni “24h gratis” activos.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OrangePrimary.copy(alpha = 0.1f)
            ) {
                Text(
                    "Cuando esté listo, se anunciará en la app.",
                    color = OrangePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
