package com.app.foodranker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.foodranker.R

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular,   FontWeight.Normal),
    Font(R.font.poppins_medium,    FontWeight.Medium),
    Font(R.font.poppins_semibold,  FontWeight.SemiBold),
    Font(R.font.poppins_bold,      FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold,      fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold,      fontSize = 36.sp),
    headlineLarge  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp),
    headlineSmall  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold,      fontSize = 24.sp),
    titleLarge  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    bodyLarge  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall  = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
