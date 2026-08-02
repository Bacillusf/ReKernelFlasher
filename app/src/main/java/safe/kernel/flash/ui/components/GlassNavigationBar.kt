package safe.kernel.flash.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

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
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val navPadding = WindowInsets.navigationBars.asPaddingValues()
    val backgroundColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 10.dp + navPadding.calculateBottomPadding()),
        contentAlignment = Alignment.Center,
    ) {
        FloatingBottomBar(
            selectedIndex = { selectedIndex },
            onSelected = { index -> items.getOrNull(index)?.let(onItemClick) },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = true,
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarTab(
                    selected = index == selectedIndex,
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavigationBarTab(
    selected: Boolean,
    item: NavItem,
    onClick: () -> Unit,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }

    FloatingBottomBarItem(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = item.label,
            color = color,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
