package com.app.foodranker.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Política de Privacidad", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Última actualización: mayo 2025",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            item { PrivacySection("1. Información que recopilamos",
                """Al usar FoodRanker recopilamos:

• Datos de cuenta de Google (nombre, email, foto de perfil) al iniciar sesión.
• Fotos que subes voluntariamente de platos de comida.
• Valoraciones y comentarios que publicas sobre platos.
• Datos de uso básicos para mejorar la app."""
            ) }
            item { PrivacySection("2. Cómo usamos tu información",
                """Usamos tus datos para:

• Mostrar tu perfil y tus platos publicados en la app.
• Permitir que otros usuarios vean tus valoraciones y fotos.
• Enviarte notificaciones sobre likes y valoraciones en tus platos.
• Mejorar la experiencia de la aplicación."""
            ) }
            item { PrivacySection("3. Fotos que subes",
                """Al subir una foto confirmas que:

• Eres el propietario de la imagen o tienes permiso para publicarla.
• Autorizas a FoodRanker a mostrarla dentro de la app.

Las fotos se almacenan de forma segura en Cloudinary (cloudinary.com)."""
            ) }
            item { PrivacySection("4. Servicios de terceros",
                """FoodRanker utiliza los siguientes servicios:

• Firebase (Google) — autenticación y base de datos.
• Cloudinary — almacenamiento de imágenes.
• Google AdMob — publicidad. AdMob puede recopilar datos para personalizar anuncios según su política de privacidad.

Te recomendamos revisar las políticas de privacidad de cada servicio."""
            ) }
            item { PrivacySection("5. Seguridad",
                "Tomamos medidas razonables para proteger tu información. Sin embargo, ningún sistema es 100% seguro. El acceso a tu cuenta está protegido por Google Sign-In."
            ) }
            item { PrivacySection("6. Tus derechos",
                """Puedes en cualquier momento:

• Eliminar los platos y valoraciones que hayas publicado.
• Cerrar sesión y dejar de usar la app.
• Solicitar la eliminación de tu cuenta y datos escribiendo a: foodranker.app@gmail.com"""
            ) }
            item { PrivacySection("7. Menores de edad",
                "FoodRanker no está dirigida a menores de 13 años. No recopilamos intencionadamente datos de menores."
            ) }
            item { PrivacySection("8. Cambios en esta política",
                "Podemos actualizar esta política ocasionalmente. Te notificaremos sobre cambios significativos. El uso continuado de la app implica la aceptación de la política actualizada."
            ) }
            item { PrivacySection("9. Contacto",
                "Si tienes preguntas sobre esta política de privacidad, escríbenos a:\n\nfoodranker.app@gmail.com"
            ) }
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Text(content, fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp)
        HorizontalDivider(color = DividerColor)
    }
}
