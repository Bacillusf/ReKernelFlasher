package safe.kernel.flash.ui.screens.slot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.components.SlotCard

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalUnitApi
@Composable
fun ColumnScope.SlotContent(
    viewModel: SlotViewModel,
    slotSuffix: String,
    navController: NavController
) {
    val context = LocalContext.current
    SlotCard(
        title = stringResource(
            when (slotSuffix) {
                "_a" -> R.string.slot_a
                "_b" -> R.string.slot_b
                else -> R.string.slot
            }
        ),
        viewModel = viewModel,
        navController = navController,
        isSlotScreen = true
    )
    Spacer(Modifier.height(14.dp))
    AnimatedVisibility(!viewModel.isRefreshing.value) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ListItem(
                title = stringResource(R.string.flash),
                subtitle = "选择 AK3 / 镜像 / KernelSU 驱动",
                leadingIcon = Icons.Filled.Build,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = { navController.navigate("slot$slotSuffix/flash") }
            )
            ListItem(
                title = stringResource(R.string.backup),
                subtitle = "备份当前槽位到本地",
                leadingIcon = Icons.Filled.Backup,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                onClick = {
                    viewModel.clearFlash(context)
                    navController.navigate("slot$slotSuffix/backup")
                }
            )
            ListItem(
                title = stringResource(R.string.restore),
                subtitle = "从历史备份恢复",
                leadingIcon = Icons.Filled.Restore,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                onClick = { navController.navigate("slot$slotSuffix/backups") }
            )
            ListItem(
                title = stringResource(R.string.check_kernel_version),
                subtitle = "从 boot 镜像提取内核版本",
                leadingIcon = Icons.Filled.Info,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    if (!viewModel.isRefreshing.value) viewModel.getKernel(context)
                }
            )
            if (viewModel.hasVendorDlkm) {
                VendorDlkmItems(viewModel, context, slotSuffix)
            }
        }
    }
}

@Composable
private fun VendorDlkmItems(
    viewModel: SlotViewModel,
    context: android.content.Context,
    slotSuffix: String
) {
    if (viewModel.isVendorDlkmMounted) {
        ListItem(
            title = stringResource(R.string.unmount_vendor_dlkm),
            leadingIcon = Icons.Filled.Storage,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { viewModel.unmountVendorDlkm(context) }
        )
    } else if (viewModel.isVendorDlkmMapped) {
        ListItem(
            title = stringResource(R.string.mount_vendor_dlkm),
            leadingIcon = Icons.Filled.Storage,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { viewModel.mountVendorDlkm(context) }
        )
        ListItem(
            title = stringResource(R.string.unmap_vendor_dlkm),
            leadingIcon = Icons.Filled.Storage,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { viewModel.unmapVendorDlkm(context) }
        )
    } else {
        ListItem(
            title = stringResource(R.string.map_vendor_dlkm),
            leadingIcon = Icons.Filled.Storage,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { viewModel.mapVendorDlkm(context) }
        )
    }
}
