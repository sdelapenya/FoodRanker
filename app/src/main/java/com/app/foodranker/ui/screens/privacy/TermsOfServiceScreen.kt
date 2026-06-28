package com.app.foodranker.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Términos de Servicio", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Última actualización: mayo 2025", fontSize = 12.sp, color = TextSecondary) }

            item { PrivacySection("1. Aceptación de los términos",
                "Al usar FoodRanker aceptas estos términos. Si no estás de acuerdo, no uses la aplicación. Nos reservamos el derecho de modificarlos en cualquier momento, notificándote previamente."
            ) }

            item { PrivacySection("2. Descripción del servicio",
                "FoodRanker es una plataforma para descubrir, compartir y puntuar platos de comida de todo el mundo. Permite subir fotos, añadir valoraciones y descubrir los mejores platos de tu ciudad y del mundo."
            ) }

            item { PrivacySection("3. Contenido del usuario",
                """Al publicar contenido en FoodRanker declaras que:

• El contenido es tuyo o tienes permiso para publicarlo.
• Las fotos son de platos de comida reales.
• La información sobre el restaurante y el plato es veraz.
• No publicas contenido ofensivo, ilegal o engañoso.

Nos reservamos el derecho de eliminar cualquier contenido que incumpla estas normas."""
            ) }

            item { PrivacySection("4. Contenido prohibido",
                """Está prohibido publicar:

• Fotos que no sean de comida o platos.
• Contenido sexual, violento u ofensivo.
• Información falsa sobre restaurantes.
• Spam o contenido promocional no autorizado.
• Contenido que infrinja derechos de autor.

Las infracciones pueden resultar en la suspensión permanente de la cuenta."""
            ) }

            item { PrivacySection("5. Sistema de reportes",
                "Los usuarios pueden reportar contenido inapropiado. Los platos con 3 o más reportes serán ocultados automáticamente mientras se revisan. El abuso del sistema de reportes puede resultar en la suspensión de la cuenta."
            ) }

            item { PrivacySection("6. Sistema de recompensas",
                "El sistema de XP, niveles y badges es de carácter informativo y no tiene valor monetario. Nos reservamos el derecho de modificar o eliminar el sistema de recompensas en cualquier momento."
            ) }

            item { PrivacySection("7. Limitación de responsabilidad",
                "FoodRanker no verifica la exactitud de la información publicada por los usuarios. La información sobre restaurantes y platos es responsabilidad de quien la publica. No somos responsables de experiencias negativas basadas en el contenido de la app."
            ) }

            item { PrivacySection("8. Propiedad intelectual",
                "FoodRanker y su logo son propiedad de sus creadores. El contenido publicado por usuarios (fotos, textos) sigue siendo propiedad del usuario, quien concede a FoodRanker una licencia para mostrarlo dentro de la app."
            ) }

            item { PrivacySection("9. Eliminación de cuenta",
                "Puedes eliminar tu cuenta desde tu perfil en cualquier momento. Al hacerlo, se borrarán permanentemente todos tus datos, platos y valoraciones."
            ) }

            item { PrivacySection("10. Contacto",
                "Para cualquier consulta sobre estos términos:\n\nfoodranker.app@gmail.com"
            ) }
        }
    }
}
