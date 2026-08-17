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
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Política de Privacidad", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Última actualización: agosto 2026",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            item { PrivacySection("1. Información que recopilamos",
                """Al usar FoodRanker recopilamos:

• Datos de tu cuenta de Google (nombre, dirección de correo y foto de perfil) al iniciar sesión.
• Fotos que subes voluntariamente de platos de comida.
• Valoraciones, comentarios y el nombre y dirección del local que indicas al publicar un plato.
• Tu ubicación aproximada o precisa, solo si nos das permiso y solo mientras buscas locales cercanos (ver el punto 2).
• Un identificador del dispositivo para enviarte notificaciones (token de Firebase Cloud Messaging).
• Datos de uso y de diagnóstico: pantallas visitadas, eventos básicos e informes de fallos."""
            ) }
            item { PrivacySection("2. Ubicación",
                """FoodRanker pide permiso de ubicación con un único fin: encontrar los locales que tienes cerca cuando publicas un plato, para que no tengas que escribir la dirección a mano.

• El permiso es opcional. Si lo rechazas, puedes seguir usando la app y buscar el local escribiendo su nombre.
• La ubicación se usa en el momento de la búsqueda y se envía a Google Places para obtener la lista de locales cercanos.
• No guardamos tu ubicación ni construimos un historial de dónde has estado.
• Lo que sí queda guardado es el local que tú eliges y la ciudad de ese local, porque son parte del plato que publicas y se muestran públicamente."""
            ) }
            item { PrivacySection("3. Cómo usamos tu información",
                """Usamos tus datos para:

• Mostrar tu perfil y tus platos publicados en la app.
• Permitir que otros usuarios vean tus valoraciones y fotos.
• Calcular el ranking de platos de tu ciudad y la liga semanal.
• Enviarte notificaciones sobre likes y valoraciones en tus platos.
• Detectar fallos y entender qué partes de la app se usan, para mejorarla."""
            ) }
            item { PrivacySection("4. Qué NO hacemos",
                """• No vendemos tus datos personales ni los cedemos a terceros con fines comerciales.
• No publicamos tu dirección de correo. El correo se queda en el sistema de autenticación de Google y no se guarda en la base de datos que consultan otros usuarios, así que nadie puede verlo desde la app.
• No guardamos tu ubicación ni te seguimos en segundo plano."""
            ) }
            item { PrivacySection("5. Fotos que subes y revisión automática",
                """Al subir una foto confirmas que:

• Eres el propietario de la imagen o tienes permiso para publicarla.
• Autorizas a FoodRanker a mostrarla dentro de la app.

Antes de publicarse, cada foto se analiza automáticamente con Google Cloud Vision para comprobar que es comida y que no tiene contenido inapropiado. Si no lo es, se rechaza y no se publica. Este análisis es automático y ninguna persona revisa la imagen en ese paso.

Las fotos publicadas se almacenan en Cloudinary y son visibles públicamente dentro de la app."""
            ) }
            item { PrivacySection("6. Servicios de terceros",
                """FoodRanker utiliza los siguientes servicios, cada uno con su propia política de privacidad:

• Firebase (Google) — inicio de sesión, base de datos, funciones de servidor, notificaciones push, estadísticas de uso (Analytics), informes de fallos (Crashlytics) y configuración remota.
• Google Places y servicios de ubicación (Google) — búsqueda de locales cercanos y ficha del local.
• Google Cloud Vision (Google) — revisión automática de las fotos.
• Cloudinary — almacenamiento de las imágenes publicadas.
• Google AdMob (Google) — publicidad. Puede recopilar identificadores del dispositivo para mostrar anuncios, según su propia política.
• Google Play Facturación (Google) — gestión de la suscripción Premium. El pago lo procesa Google Play; FoodRanker no recibe ni almacena datos de tu tarjeta.

Te recomendamos revisar las políticas de privacidad de cada servicio."""
            ) }
            item { PrivacySection("7. Publicidad",
                "La app muestra anuncios a través de Google AdMob. Si te suscribes a Premium, los anuncios dejan de mostrarse. AdMob puede usar identificadores de tu dispositivo para personalizar los anuncios; puedes limitarlo desde los ajustes de privacidad de Android de tu teléfono."
            ) }
            item { PrivacySection("8. Seguridad",
                "Tomamos medidas razonables para proteger tu información. El acceso a tu cuenta está protegido por Google Sign-In, y los datos que solo debe escribir el servidor (puntuaciones, XP, estado Premium) no se pueden modificar desde la app. Aun así, ningún sistema es 100% seguro."
            ) }
            item { PrivacySection("9. Conservación y eliminación de datos",
                """Conservamos tus datos mientras tengas la cuenta activa.

• Puedes borrar cualquier plato o valoración que hayas publicado en cualquier momento.
• Puedes eliminar tu cuenta desde tu perfil. Al hacerlo se borran tu perfil, tus platos, tus valoraciones, tus comentarios y tus guardados.
• Los informes de fallos y las estadísticas de uso son anónimos o agregados y pueden conservarse según los plazos de Firebase."""
            ) }
            item { PrivacySection("10. Tus derechos",
                """Puedes en cualquier momento:

• Acceder a tus datos y corregir tu perfil desde la app.
• Retirar el permiso de ubicación o de notificaciones desde los ajustes de Android.
• Eliminar los platos y valoraciones que hayas publicado.
• Eliminar tu cuenta y todos tus datos desde tu perfil, o escribiendo a: foodranker.app@gmail.com

Si resides en la Unión Europea, tienes además derecho a solicitar una copia de tus datos, a oponerte a su tratamiento y a presentar una reclamación ante tu autoridad de protección de datos."""
            ) }
            item { PrivacySection("11. Menores de edad",
                "FoodRanker no está dirigida a menores de 13 años. No recopilamos intencionadamente datos de menores. Si crees que un menor nos ha facilitado datos, escríbenos y los eliminaremos."
            ) }
            item { PrivacySection("12. Cambios en esta política",
                "Podemos actualizar esta política ocasionalmente. Te notificaremos sobre cambios significativos. El uso continuado de la app implica la aceptación de la política actualizada."
            ) }
            item { PrivacySection("13. Contacto",
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
