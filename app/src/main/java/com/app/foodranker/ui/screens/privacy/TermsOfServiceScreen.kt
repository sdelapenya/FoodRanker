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
            item { Text("Última actualización: agosto 2026", fontSize = 12.sp, color = TextSecondary) }

            item { PrivacySection("1. Aceptación de los términos",
                "Al usar FoodRanker aceptas estos términos. Si no estás de acuerdo, no uses la aplicación. Nos reservamos el derecho de modificarlos en cualquier momento, notificándote previamente."
            ) }

            item { PrivacySection("2. Descripción del servicio",
                "FoodRanker es una plataforma para descubrir, compartir y puntuar platos de comida de todo el mundo. Permite subir fotos, añadir valoraciones y descubrir los mejores platos de tu ciudad y del mundo."
            ) }

            item { PrivacySection("3. Edad mínima",
                "Para usar FoodRanker debes tener al menos 13 años. Si eres menor de edad en tu país, necesitas el consentimiento de un padre, madre o tutor."
            ) }

            item { PrivacySection("4. Contenido del usuario",
                """Al publicar contenido en FoodRanker declaras que:

• El contenido es tuyo o tienes permiso para publicarlo.
• Las fotos son de platos de comida reales.
• La información sobre el restaurante y el plato es veraz.
• No publicas contenido ofensivo, ilegal o engañoso.

Nos reservamos el derecho de eliminar cualquier contenido que incumpla estas normas."""
            ) }

            item { PrivacySection("5. Contenido prohibido",
                """Está prohibido publicar:

• Fotos que no sean de comida o platos.
• Contenido sexual, violento u ofensivo.
• Información falsa sobre restaurantes.
• Spam o contenido promocional no autorizado.
• Contenido que infrinja derechos de autor.

Las infracciones pueden resultar en la suspensión permanente de la cuenta."""
            ) }

            item { PrivacySection("6. Revisión automática y sistema de reportes",
                """Antes de publicarse, cada foto se revisa automáticamente para comprobar que es comida y que no contiene material inapropiado. Si no lo es, se rechaza y el plato no se publica.

Además, los usuarios pueden reportar contenido inapropiado. Los platos con 3 o más reportes se ocultan automáticamente mientras se revisan. El abuso del sistema de reportes puede resultar en la suspensión de la cuenta."""
            ) }

            item { PrivacySection("7. Sistema de recompensas",
                "El sistema de XP, niveles, badges, ligas y retos es de carácter informativo y no tiene valor monetario ni puede canjearse por dinero o premios. Nos reservamos el derecho de modificar o eliminar el sistema de recompensas en cualquier momento."
            ) }

            item { PrivacySection("8. Publicidad",
                "La versión gratuita de FoodRanker muestra anuncios servidos por Google AdMob. Los anuncios son de terceros: no controlamos su contenido ni respaldamos los productos que aparecen en ellos."
            ) }

            item { PrivacySection("9. Suscripción Premium",
                """FoodRanker ofrece una suscripción opcional que elimina los anuncios y añade un distintivo en tu perfil.

• El precio y la periodicidad se muestran en la app antes de confirmar la compra, y los cobra Google Play en la moneda de tu país.
• La suscripción se renueva automáticamente hasta que la canceles.
• Puedes cancelarla en cualquier momento desde Google Play (Suscripciones). La cancelación evita el siguiente cobro; el periodo ya pagado se mantiene hasta su fin.
• Las devoluciones se rigen por la política de Google Play. FoodRanker no procesa pagos ni tiene acceso a tus datos bancarios.
• Si la suscripción caduca, tu cuenta y tu contenido siguen intactos: solo vuelven los anuncios."""
            ) }

            item { PrivacySection("10. Limitación de responsabilidad",
                "FoodRanker no verifica la exactitud de la información publicada por los usuarios. La información sobre restaurantes y platos es responsabilidad de quien la publica. No somos responsables de experiencias negativas basadas en el contenido de la app."
            ) }

            item { PrivacySection("11. Propiedad intelectual",
                "FoodRanker y su logo son propiedad de sus creadores. El contenido publicado por usuarios (fotos, textos) sigue siendo propiedad del usuario, quien concede a FoodRanker una licencia no exclusiva para almacenarlo y mostrarlo dentro de la app. Esa licencia termina cuando borras el contenido o tu cuenta."
            ) }

            item { PrivacySection("12. Eliminación de cuenta",
                "Puedes eliminar tu cuenta desde tu perfil en cualquier momento. Al hacerlo se borran permanentemente tu perfil, tus platos, tus valoraciones, tus comentarios y tus guardados. Eliminar la cuenta no cancela por sí solo una suscripción activa: hazlo desde Google Play."
            ) }

            item { PrivacySection("13. Ley aplicable",
                "Estos términos se rigen por la legislación española. Si alguna cláusula resultara inválida, el resto seguirá siendo aplicable."
            ) }

            item { PrivacySection("14. Contacto",
                "Para cualquier consulta sobre estos términos:\n\nfoodranker.app@gmail.com"
            ) }
        }
    }
}
