package safe.kernel.flash.ui.theme

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * KSU-style liquid-glass surface for floating chrome.
 *
 * This modifier only paints behind content. It never applies RenderEffect or draws an overlay after
 * content, so text/icons remain sharp and readable. Use it on navigation chrome; normal content
 * cards should use Material surfaces.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 24.dp,
    tint: Color? = null,
    borderWidth: Dp = 0.5.dp,
    highlightAlpha: Float = 0.18f,
): Modifier = composed {
    val tokens = LocalGlassTokens.current
    val baseTint = tint ?: tokens.surface
    val border = tokens.outline
    val radiusPx = cornerRadius

    this
        .drawBehind {
            val radius = radiusPx.toPx()
            drawRoundRect(
                color = baseTint,
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        tokens.highlight.copy(alpha = highlightAlpha),
                        Color.Transparent,
                        tokens.scrim.copy(alpha = 0.04f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        .border(borderWidth, border, shape)
}

fun Modifier.softShadow(
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    cornerRadius: Dp = 20.dp,
    offsetY: Dp = 6.dp,
): Modifier = this

fun Modifier.gradientBackground(
    colors: List<Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
    shape: Shape = RectangleShape
): Modifier = background(Brush.linearGradient(colors = colors, start = start, end = end), shape)
