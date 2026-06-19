package safe.kernel.flash.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.components.SlotCard
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun FlashHomeContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val slotSfxA = if (viewModel.isAb) "_a" else ""

    SlotCard(
        title = stringResource(if (viewModel.isAb) R.string.slot_a else R.string.slot),
        viewModel = viewModel.slotA,
        navController = navController,
        isSlotScreen = true
    )
    Spacer(Modifier.height(12.dp))
    AnimatedVisibility(!viewModel.slotA.isRefreshing.value) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ListItem(
                title = stringResource(R.string.flash),
                subtitle = "选择 AK3 / 镜像 / KernelSU 驱动刷入",
                leadingIcon = Icons.Filled.Build,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = { navController.navigate("slot$slotSfxA/flash") }
            )
            ListItem(
                title = stringResource(R.string.backup),
                subtitle = "备份当前分区到本地",
                leadingIcon = Icons.Filled.Backup,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                onClick = {
                    viewModel.slotA.clearFlash(context)
                    navController.navigate("slot$slotSfxA/backup")
                }
            )
            ListItem(
                title = stringResource(R.string.restore),
                subtitle = "从历史备份恢复分区",
                leadingIcon = Icons.Filled.Restore,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                onClick = { navController.navigate("slot$slotSfxA/backups") }
            )
            ListItem(
                title = stringResource(R.string.check_kernel_version),
                subtitle = "从 boot 镜像中提取内核版本",
                leadingIcon = Icons.Filled.Info,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { if (!viewModel.slotA.isRefreshing.value) viewModel.slotA.getKernel(context) }
            )
            if (viewModel.slotA.hasVendorDlkm) {
                VendorDlkmItems(viewModel, context)
            }
        }
    }

    if (viewModel.isAb) {
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))
        SlotCard(
            title = stringResource(R.string.slot_b),
            viewModel = viewModel.slotB!!,
            navController = navController,
            isSlotScreen = true
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(!viewModel.slotB!!.isRefreshing.value) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ListItem(
                    title = stringResource(R.string.flash),
                    subtitle = "刷写到 B 槽位",
                    leadingIcon = Icons.Filled.Build,
                    leadingColors = ListItemIconColors(
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = { navController.navigate("slot_b/flash") }
                )
                ListItem(
                    title = stringResource(R.string.backup),
                    subtitle = "备份 B 槽位",
                    leadingIcon = Icons.Filled.Backup,
                    leadingColors = ListItemIconColors(
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    onClick = {
                        viewModel.slotB!!.clearFlash(context)
                        navController.navigate("slot_b/backup")
                    }
                )
                ListItem(
                    title = stringResource(R.string.restore),
                    subtitle = "从历史恢复 B 槽位",
                    leadingIcon = Icons.Filled.Restore,
                    leadingColors = ListItemIconColors(
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    onClick = { navController.navigate("slot_b/backups") }
                )
                ListItem(
                    title = stringResource(R.string.check_kernel_version),
                    subtitle = "检查 B 槽位内核版本",
                    leadingIcon = Icons.Filled.Info,
                    leadingColors = ListItemIconColors(
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = {
                        if (!viewModel.slotB!!.isRefreshing.value) viewModel.slotB!!.getKernel(context)
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(28.dp))
    ListItem(
        title = stringResource(R.string.operation_history),
        subtitle = "查看所有刷写 / 备份 / 重启操作",
        leadingIcon = Icons.Filled.History,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { navController.navigate("history") }
    )
}

@Composable
private fun VendorDlkmItems(viewModel: MainViewModel, context: android.content.Context) {
    if (viewModel.slotA.isVendorDlkmMounted) {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.unmount_vendor_dlkm),
            onClick = { viewModel.slotA.unmountVendorDlkm(context) }
        )
    } else if (viewModel.slotA.isVendorDlkmMapped) {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.mount_vendor_dlkm),
            onClick = { viewModel.slotA.mountVendorDlkm(context) }
        )
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.unmap_vendor_dlkm),
            onClick = { viewModel.slotA.unmapVendorDlkm(context) }
        )
    } else {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.map_vendor_dlkm),
            onClick = { viewModel.slotA.mapVendorDlkm(context) }
        )
    }
}

@Composable
private fun ActionListItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        title = title,
        leadingIcon = icon,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = onClick
    )
}
