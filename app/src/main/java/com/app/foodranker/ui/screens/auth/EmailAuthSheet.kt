package com.app.foodranker.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.R
import com.app.foodranker.ui.theme.FoodTheme
import com.app.foodranker.ui.theme.OrangePrimary

/** Mínimo que pedimos nosotros. Firebase acepta desde 6; 8 es la recomendación actual. */
private const val MIN_PASSWORD = 8

/**
 * Entrada y alta con correo y contraseña.
 *
 * Va en una hoja inferior y no en una pantalla propia para no tocar el grafo de
 * navegación: el camino de Google, que es el que usa todo el mundo, se queda exactamente
 * como estaba. Por qué existe este camino en absoluto está contado en
 * `AuthRepository.signInWithEmail`.
 *
 * A diferencia de [AuthScreen], que tiene su propio degradado naranja fijo, esta hoja sí
 * sigue el tema: en oscuro sale oscura. Por eso aquí se usa `FoodTheme.colors.*` y no las
 * constantes del tema claro — ver `tools/ui/check_theme_colors.py`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthSheet(
    cargando: Boolean,
    mensajeError: String?,
    avisoResetEnviado: Boolean,
    onEntrar: (email: String, password: String) -> Unit,
    onRegistrar: (nombre: String, email: String, password: String) -> Unit,
    onRestablecer: (email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var modoAlta by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var avisoLocal by remember { mutableStateOf<String?>(null) }

    val textoFaltaCorreo = stringResource(R.string.auth_reset_need_email)

    val emailValido = email.trim().let { it.contains("@") && it.substringAfterLast("@").contains(".") }
    val puedeEnviar = !cargando &&
        emailValido &&
        password.length >= MIN_PASSWORD &&
        (!modoAlta || nombre.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FoodTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Nada de esto es decoración. Sin el scroll no hay forma de alcanzar
                // el botón ni los enlaces con el teclado abierto: en modo alta son tres
                // campos y no caben. Y el inset tiene que ser la UNIÓN de teclado y
                // barra de navegación, no sólo `imePadding`: con el teclado cerrado y
                // navegación de tres botones, «¿Aún no tienes cuenta? Créala» cae
                // dentro de la franja de la barra y deja de ser pulsable — comprobado
                // en el Redmi con la v23 ya firmada por Play, donde el toque se lo
                // comió el botón de inicio. Unirlos en vez de sumarlos evita el hueco
                // doble cuando el teclado sí está abierto, porque entonces el IME ya
                // cubre la barra. Mismo fallo que el botón de guardar de la v22.
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (modoAlta) R.string.auth_sheet_title_signup else R.string.auth_sheet_title_signin
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FoodTheme.colors.textPrimary
            )

            Spacer(Modifier.height(20.dp))

            if (modoAlta) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.auth_field_name)) },
                    singleLine = true,
                    enabled = !cargando,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; avisoLocal = null },
                label = { Text(stringResource(R.string.auth_field_email)) },
                singleLine = true,
                enabled = !cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_field_password)) },
                supportingText = if (modoAlta) {
                    { Text(stringResource(R.string.auth_password_hint)) }
                } else null,
                singleLine = true,
                enabled = !cargando,
                visualTransformation = if (verPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(
                            imageVector = if (verPassword) Icons.Outlined.VisibilityOff
                                          else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (verPassword) R.string.auth_hide_password
                                else R.string.auth_show_password
                            ),
                            tint = FoodTheme.colors.textSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (modoAlta) onRegistrar(nombre, email, password)
                    else onEntrar(email, password)
                },
                enabled = puedeEnviar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (modoAlta) R.string.auth_action_signup else R.string.auth_action_signin
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // El aviso de "te hemos mandado el correo" y el de error comparten sitio: no
            // pueden darse a la vez, porque el envío o sale bien o deja un error.
            val aviso = avisoLocal ?: mensajeError
            if (avisoResetEnviado) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_reset_sent),
                    color = FoodTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (aviso != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = aviso,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!modoAlta) {
                TextButton(
                    onClick = {
                        if (email.isBlank()) avisoLocal = textoFaltaCorreo
                        else onRestablecer(email)
                    },
                    enabled = !cargando
                ) {
                    Text(
                        stringResource(R.string.auth_forgot_password),
                        fontSize = 13.sp,
                        color = FoodTheme.colors.textSecondary
                    )
                }
            }

            TextButton(
                onClick = { modoAlta = !modoAlta; avisoLocal = null },
                enabled = !cargando
            ) {
                Text(
                    text = stringResource(
                        if (modoAlta) R.string.auth_switch_to_signin
                        else R.string.auth_switch_to_signup
                    ),
                    fontSize = 13.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
