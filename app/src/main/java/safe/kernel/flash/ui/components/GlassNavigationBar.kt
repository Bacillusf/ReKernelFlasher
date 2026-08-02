package safe.kernel.flash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import safe.kernel.flash.ui.theme.LocalGlassTokens
import safe.kernel.flash.ui.theme.liquidGlass
import safe.kernel.flash.ui.theme.softShadow

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
    val tokens = LocalGlassTokens.current
    val indicatorColor = MaterialTheme.colorScheme.primary
    val selectedText = MaterialTheme.colorScheme.onPrimary
    val idleText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
    val barShape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 8.dp + navPadding.calculateBottomPadding())
            .softShadow(cornerRadius = 30.dp, alpha = 0.14f, offsetY = 8.dp)
            .liquidGlass(
                shape = barShape,
                tint = tokens.navContainer,
                blurRadius = 26.dp,
                highlightAlpha = 0.35f,
            )
            .height(60.dp)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val tint by animateColorAsState(
                    targetValue = if (selected) selectedText else idleText,
                    label = "navItemColor"
                )
                val bgAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    label = "navItemBg"
                )
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.94f,
                    label = "navItemScale"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = bgAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onItemClick(item) }
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size((22 * scale).dp)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
