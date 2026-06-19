package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.SettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListItem(
            title = stringResource(R.string.auto_backup_settings),
            subtitle = "刷写前自动备份目标分区",
            leadingIcon = Icons.Filled.Backup,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { navController.navigate("settings/autobackup") }
        )
        ListItem(
            title = stringResource(R.string.language_settings),
            subtitle = "选择应用显示语言",
            leadingIcon = Icons.Filled.Language,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { navController.navigate("settings/language") }
        )
        ListItem(
            title = stringResource(R.string.log_settings),
            subtitle = "保存内核日志 (ramoops / dmesg / logcat)",
            leadingIcon = Icons.Filled.Description,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = { navController.navigate("settings/logs") }
        )
        ListItem(
            title = stringResource(R.string.advanced_settings),
            subtitle = "界面缩放等高级选项",
            leadingIcon = Icons.Filled.Code,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { navController.navigate("settings/advanced") }
        )
    }
}
