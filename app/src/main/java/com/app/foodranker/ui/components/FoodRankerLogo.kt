package com.app.foodranker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.theme.OrangePrimary
import com.app.foodranker.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BrandNavy = Color(0xFF11122E)

@Composable
fun FoodRankerLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 132.dp,
    onDark: Boolean = false,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FoodRankerMark(modifier = Modifier.size(markSize))

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Food",
                color = if (onDark) Color.White else OrangePrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
            Text(
                text = "Ranker",
                color = if (onDark) Color.White.copy(alpha = 0.94f) else TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
        }

        if (showTagline) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "EAT. RATE. REPEAT.",
                color = if (onDark) Color.White.copy(alpha = 0.78f) else BrandNavy.copy(alpha = 0.64f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
fun FoodRankerMark(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val orange = OrangePrimary
        val navy = BrandNavy

        val dome = Path().apply {
            moveTo(w * 0.18f, h * 0.62f)
            cubicTo(w * 0.18f, h * 0.36f, w * 0.34f, h * 0.23f, w * 0.50f, h * 0.23f)
            cubicTo(w * 0.66f, h * 0.23f, w * 0.82f, h * 0.36f, w * 0.82f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.62f)
            close()
        }
        drawPath(dome, orange)

        drawRoundRect(
            color = orange,
            topLeft = Offset(w * 0.43f, h * 0.17f),
            size = Size(w * 0.14f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )

        drawArc(
            color = Color.White.copy(alpha = 0.88f),
            startAngle = 198f,
            sweepAngle = 42f,
            useCenter = false,
            topLeft = Offset(w * 0.27f, h * 0.31f),
            size = Size(w * 0.42f, h * 0.42f),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )

        val trend = Path().apply {
            moveTo(w * 0.29f, h * 0.63f)
            lineTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.47f, h * 0.55f)
            lineTo(w * 0.61f, h * 0.42f)
            lineTo(w * 0.68f, h * 0.47f)
            lineTo(w * 0.78f, h * 0.35f)
        }
        drawPath(
            path = trend,
            color = Color.White,
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.78f, h * 0.35f)
                lineTo(w * 0.765f, h * 0.46f)
                lineTo(w * 0.69f, h * 0.38f)
                close()
            },
            color = Color.White
        )
        drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.40f, h * 0.52f))
        drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.61f, h * 0.42f))

        drawRoundRect(
            color = navy,
            topLeft = Offset(w * 0.12f, h * 0.66f),
            size = Size(w * 0.76f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.40f, h * 0.78f)
                lineTo(w * 0.60f, h * 0.78f)
                cubicTo(w * 0.61f, h * 0.84f, w * 0.70f, h * 0.82f, w * 0.72f, h * 0.88f)
                lineTo(w * 0.28f, h * 0.88f)
                cubicTo(w * 0.30f, h * 0.82f, w * 0.39f, h * 0.84f, w * 0.40f, h * 0.78f)
                close()
            },
            color = navy
        )

        drawPath(starPath(Offset(w * 0.50f, h * 0.07f), w * 0.115f, w * 0.05f), orange)
    }
}

private fun starPath(center: Offset, outerRadius: Float, innerRadius: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + i * PI / 5.0
        val point = Offset(
            x = center.x + (cos(angle) * radius).toFloat(),
            y = center.y + (sin(angle) * radius).toFloat()
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
