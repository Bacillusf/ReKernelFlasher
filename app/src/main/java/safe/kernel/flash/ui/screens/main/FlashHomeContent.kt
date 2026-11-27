package safe.kernel.flash.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import safe.kernel.flash.ui.screens.slot.SlotViewModel
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun FlashHomeContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    // 活跃槽位永远排在第一位：AB 设备按 ro.boot.slot_suffix 决定顺序，非 AB 设备只有一份。
    val orderedSlots: List<Pair<String, SlotViewModel>> = if (viewModel.isAb) {
        if (viewModel.slotSuffix == "_b") listOf("_b" to viewModel.slotB!!, "_a" to viewModel.slotA)
        else listOf("_a" to viewModel.slotA, "_b" to viewModel.slotB!!)
    } else {
        listOf("" to viewModel.slotA)
    }

    orderedSlots.forEachIndexed { index, (slotSuffix, slotViewModel) ->
        if (index > 0) {
            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(20.dp))
        }
        SlotCard(
            title = stringResource(
                when (slotSuffix) {
                    "_a" -> R.string.slot_a
                    "_b" -> R.string.slot_b
                    else -> R.string.slot
                }
            ),
            viewModel = slotViewModel,
            navController = navController,
            isSlotScreen = true
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(!slotViewModel.isRefreshing.value) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SlotActionItems(
                    viewModel = viewModel,
                    slotViewModel = slotViewModel,
                    slotSuffix = slotSuffix,
                    navController = navController,
                    context = context
                )
            }
        }
    }

    Spacer(Modifier.height(28.dp))
    ListItem(
        title = stringResource(R.string.operation_history),
        subtitle = stringResource(R.string.operation_history_sub),
        leadingIcon = Icons.Filled.History,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { navController.navigate("history") }
    )
}

/**
 * 单个槽位的操作列表。文案由槽位后缀与是否为活跃槽位推导，
 * 因此活跃槽位排在前面或后面都不会再出现「刷写到 B 槽位」错位的问题。
 */
@ExperimentalMaterial3Api
@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun SlotActionItems(
    viewModel: MainViewModel,
    slotViewModel: SlotViewModel,
    slotSuffix: String,
    navController: NavController,
    context: android.content.Context
) {
    val isAb = viewModel.isAb
    val isActive = !isAb || slotSuffix == viewModel.slotSuffix
    val slotLabel = slotSuffix.removePrefix("_").uppercase()

    ListItem(
        title = stringResource(R.string.flash),
        subtitle = if (!isAb || isActive) stringResource(R.string.flash_pick_source)
            else stringResource(R.string.flash_to_slot, slotLabel),
        leadingIcon = Icons.Filled.Build,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        onClick = { navController.navigate("slot$slotSuffix/flash") }
    )
    ListItem(
        title = stringResource(R.string.backup),
        subtitle = when {
            !isAb -> stringResource(R.string.backup_current_partition)
            isActive -> stringResource(R.string.backup_current_slot)
            else -> stringResource(R.string.backup_slot, slotLabel)
        },
        leadingIcon = Icons.Filled.Backup,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        onClick = {
            slotViewModel.clearFlash(context)
            navController.navigate("slot$slotSuffix/backup")
        }
    )
    ListItem(
        title = stringResource(R.string.restore),
        subtitle = when {
            !isAb -> stringResource(R.string.restore_partition)
            isActive -> stringResource(R.string.restore_from_history)
            else -> stringResource(R.string.restore_slot, slotLabel)
        },
        leadingIcon = Icons.Filled.Restore,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        onClick = { navController.navigate("slot$slotSuffix/backups") }
    )
    ListItem(
        title = stringResource(R.string.check_kernel_version),
        subtitle = if (!isAb || isActive) stringResource(R.string.kernel_from_boot)
            else stringResource(R.string.check_slot_kernel, slotLabel),
        leadingIcon = Icons.Filled.Info,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { if (!slotViewModel.isRefreshing.value) slotViewModel.getKernel(context) }
    )
    if (slotViewModel.hasVendorDlkm) {
        VendorDlkmItems(slotViewModel, context)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun VendorDlkmItems(slotViewModel: SlotViewModel, context: android.content.Context) {
    if (slotViewModel.isVendorDlkmMounted) {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.unmount_vendor_dlkm),
            onClick = { slotViewModel.unmountVendorDlkm(context) }
        )
    } else if (slotViewModel.isVendorDlkmMapped) {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.mount_vendor_dlkm),
            onClick = { slotViewModel.mountVendorDlkm(context) }
        )
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.unmap_vendor_dlkm),
            onClick = { slotViewModel.unmapVendorDlkm(context) }
        )
    } else {
        ActionListItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.map_vendor_dlkm),
            onClick = { slotViewModel.mapVendorDlkm(context) }
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
