package com.app.foodranker.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.app.foodranker.R
import com.app.foodranker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pp_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.legal_back), tint = TextPrimary)
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
                    stringResource(R.string.pp_updated),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            // El orden y el contenido salen de los recursos: el texto legal cambia por
            // idioma y conviene poder revisarlo sin tocar la pantalla.
            items(
                listOf(
                    R.string.pp_0_t to R.string.pp_0_b,
                    R.string.pp_1_t to R.string.pp_1_b,
                    R.string.pp_2_t to R.string.pp_2_b,
                    R.string.pp_3_t to R.string.pp_3_b,
                    R.string.pp_4_t to R.string.pp_4_b,
                    R.string.pp_5_t to R.string.pp_5_b,
                    R.string.pp_6_t to R.string.pp_6_b,
                    R.string.pp_7_t to R.string.pp_7_b,
                    R.string.pp_8_t to R.string.pp_8_b,
                    R.string.pp_9_t to R.string.pp_9_b,
                    R.string.pp_10_t to R.string.pp_10_b,
                    R.string.pp_11_t to R.string.pp_11_b,
                    R.string.pp_12_t to R.string.pp_12_b,
                    R.string.pp_13_t to R.string.pp_13_b,
                )
            ) { (titulo, cuerpo) ->
                PrivacySection(stringResource(titulo), stringResource(cuerpo))
            }
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
