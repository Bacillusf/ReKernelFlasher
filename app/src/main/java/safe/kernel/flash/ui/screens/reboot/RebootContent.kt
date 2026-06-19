package safe.kernel.flash.ui.screens.reboot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.RebootContent(
    viewModel: RebootViewModel,
    @Suppress("UNUSED_PARAMETER") ignoredNavController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListItem(
            title = stringResource(R.string.reboot),
            subtitle = "重启到系统",
            leadingIcon = Icons.Filled.PowerSettingsNew,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { viewModel.showConfirm("") }
        )
        ListItem(
            title = stringResource(R.string.reboot_recovery),
            subtitle = "重启到恢复模式",
            leadingIcon = Icons.Filled.Restore,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { viewModel.showConfirm("recovery") }
        )
        ListItem(
            title = stringResource(R.string.reboot_bootloader),
            subtitle = "重启到 Bootloader (fastboot)",
            leadingIcon = Icons.Filled.Settings,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = { viewModel.showConfirm("bootloader") }
        )
        ListItem(
            title = stringResource(R.string.reboot_download),
            subtitle = "重启到 Download 模式",
            leadingIcon = Icons.Filled.Download,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { viewModel.showConfirm("download") }
        )
        ListItem(
            title = stringResource(R.string.reboot_edl),
            subtitle = "重启到紧急下载模式",
            leadingIcon = Icons.Filled.BugReport,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = { viewModel.showConfirm("edl") }
        )
    }

    AnimatedConfirmDialog(
        visible = viewModel.showConfirmDialog,
        title = "警告",
        message = viewModel.confirmMessage,
        confirmText = "重启",
        cancelText = stringResource(R.string.cancel),
        onConfirm = { viewModel.executeReboot() },
        onDismiss = { viewModel.hideConfirm() },
        destructive = true
    )
}
