package safe.kernel.flash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val MiuixLight = lightColorScheme(
    primary = Color(0xFF34A853),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0DA),
    onPrimaryContainer = Color(0xFF092610),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E8EB),
    onSecondaryContainer = Color(0xFF1A1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF5F6368),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1A1A1A),
    outline = Color(0xFFD2D2D7),
    outlineVariant = Color(0xFFE6E8EB),
    error = Color(0xFFEA4335),
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFF8C1D18),
)

private val MiuixDark = darkColorScheme(
    primary = Color(0xFF81C995),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF1A3825),
    onPrimaryContainer = Color(0xFFC4EED0),
    secondary = Color(0xFFBDC1C6),
    onSecondary = Color(0xFF303134),
    secondaryContainer = Color(0xFF303134),
    onSecondaryContainer = Color(0xFFE8EAED),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF9AA0A6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EAED),
    outline = Color(0xFF3C4043),
    outlineVariant = Color(0xFF303134),
    error = Color(0xFFF28B82),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val MiuixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val MiuixTypography = Typography(
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelMedium = TextStyle(fontSize = 11.sp),
    labelSmall = TextStyle(fontSize = 9.sp),
)

@Composable
fun MiuixThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MiuixDark else MiuixLight
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MiuixShapes,
        typography = MiuixTypography,
        content = content
    )
}
