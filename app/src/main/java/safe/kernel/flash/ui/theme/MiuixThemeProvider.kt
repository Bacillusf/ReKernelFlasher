package safe.kernel.flash.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

data class GlassTokens(
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val onSurface: Color
)

val LocalGlassTokens = compositionLocalOf {
    GlassTokens(
        surface = Color.White,
        surfaceVariant = Color.White,
        outline = Color.Black,
        outlineVariant = Color.Black,
        onSurface = Color.Black
    )
}

private val MiuixLight = lightColorScheme(
    primary = Color(0xFF34A853),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0DA),
    onPrimaryContainer = Color(0xFF092610),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E8EB),
    onSecondaryContainer = Color(0xFF1A1C1E),
    tertiary = Color(0xFF4A6FA5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF0A1F3D),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF5F6368),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1A1A1A),
    outline = Color(0xFFD2D2D7),
    outlineVariant = Color(0xFFE6E8EB),
    error = Color(0xFFEA4335),
    onError = Color.White,
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFF8C1D18)
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
    tertiary = Color(0xFF9CC0F5),
    onTertiary = Color(0xFF1A2D4D),
    tertiaryContainer = Color(0xFF2C3E5C),
    onTertiaryContainer = Color(0xFFD6E4FF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1E1E1F),
    onSurfaceVariant = Color(0xFF9AA0A6),
    background = Color(0xFF0E0E0F),
    onBackground = Color(0xFFE8EAED),
    outline = Color(0xFF3C4043),
    outlineVariant = Color(0xFF2A2A2C),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

@Composable
fun MiuixThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MiuixDark
        else -> MiuixLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val glassTokens = if (darkTheme) {
        GlassTokens(
            surface = Color(0xCC1E1E1F),
            surfaceVariant = Color(0xB32C2C2E),
            outline = Color(0x33FFFFFF),
            outlineVariant = Color(0x1FFFFFFF),
            onSurface = Color(0xFFE8EAED)
        )
    } else {
        GlassTokens(
            surface = Color(0xCCFFFFFF),
            surfaceVariant = Color(0xB3F2F2F7),
            outline = Color(0x33000000),
            outlineVariant = Color(0x1F000000),
            onSurface = Color(0xFF1A1A1A)
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}
