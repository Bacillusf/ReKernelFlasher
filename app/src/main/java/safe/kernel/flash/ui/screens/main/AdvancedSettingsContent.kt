package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.theme.softShadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.AdvancedSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    var localScale by remember(viewModel.dpiScale) { mutableFloatStateOf(viewModel.dpiScale) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListItem(
            title = stringResource(R.string.dpi_scale),
            subtitle = "${(localScale * 100).toInt()}% — 拖动调整，点击应用生效",
            leadingIcon = Icons.Filled.AspectRatio,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = {}
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Slider(
                value = localScale,
                onValueChange = { localScale = it },
                valueRange = 0.5f..1.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        ListItem(
            title = stringResource(R.string.apply),
            subtitle = "将缩放比例应用到当前会话",
            leadingIcon = Icons.Filled.Check,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { viewModel.applyDpiScale(localScale) }
        )
    }
}
