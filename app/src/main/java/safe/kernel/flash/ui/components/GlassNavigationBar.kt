package safe.kernel.flash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.draw.clip

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun GlassNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val tokens = safe.kernel.flash.ui.theme.LocalGlassTokens.current

    val glassBarTint = if (isDark) Color(0xE61E1E1F) else Color(0xE6FFFFFF)
    val barBorder = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    val indicatorColor = MaterialTheme.colorScheme.primary
    val onIndicator = MaterialTheme.colorScheme.onPrimary
    val idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp + navPadding.calculateBottomPadding())
    ) {
        val barShape = RoundedCornerShape(36.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(barShape)
                .background(glassBarTint)
                .border(0.5.dp, barBorder, barShape)
                .let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.graphicsLayer {
                            clip = true
                            renderEffect = RenderEffect
                                .createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else it
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val tint by animateColorAsState(
                    targetValue = if (selected) onIndicator else idleColor,
                    label = "navItemColor"
                )
                val bgAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    label = "navItemBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = bgAlpha))
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
