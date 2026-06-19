package safe.kernel.flash.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Dp = 24.dp,
    tint: Color? = null,
    borderWidth: Dp = 0.5.dp,
): Modifier = composed {
    val tokens = LocalGlassTokens.current
    val baseTint = tint ?: tokens.surface
    val border = tokens.outline

    val base = this
        .background(baseTint, shape)
        .border(borderWidth, border, shape)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.graphicsLayer {
            clip = true
            renderEffect = RenderEffect
                .createBlurEffect(blurRadius.toPx(), blurRadius.toPx(), Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        base
    }
}

fun Modifier.softShadow(
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    cornerRadius: Dp = 20.dp,
    offsetY: Dp = 6.dp,
): Modifier = drawBehind {
    val shadowColor = color.copy(alpha = alpha)
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(0f, offsetY.toPx()),
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}

fun Modifier.gradientBackground(
    colors: List<Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
    shape: Shape = RectangleShape
): Modifier = background(Brush.linearGradient(colors = colors, start = start, end = end), shape)
