package safe.kernel.flash.ui.screens.backups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.PartitionUtil
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.components.DataSet
import safe.kernel.flash.ui.components.FlashList
import safe.kernel.flash.ui.components.SlotCard
import safe.kernel.flash.ui.components.ViewButton
import safe.kernel.flash.ui.screens.slot.SlotViewModel

@ExperimentalMaterial3Api
@ExperimentalUnitApi
@Composable
fun ColumnScope.SlotBackupsContent(
    slotViewModel: SlotViewModel,
    backupsViewModel: BackupsViewModel,
    slotSuffix: String,
    navController: NavController
) {
    val context = LocalContext.current

    val monoStyle = MaterialTheme.typography.titleSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    )
    val currentRoute = navController.currentDestination?.route.orEmpty()

    val showDeleteConfirm = remember { mutableStateOf(false) }
    val showRestoreConfirm = remember { mutableStateOf(false) }
    val showFlashAk3Confirm = remember { mutableStateOf(false) }
    val showFlashMkbootfsConfirm = remember { mutableStateOf(false) }

    if (!currentRoute.contains("/backups/{backupId}/restore")) {
        SlotCard(
            title = stringResource(if (slotSuffix == "_a") R.string.slot_a else if (slotSuffix == "_b") R.string.slot_b else R.string.slot),
            viewModel = slotViewModel,
            navController = navController,
            isSlotScreen = true,
            showDlkm = false,
        )
        Spacer(Modifier.height(16.dp))
        if (backupsViewModel.currentBackup != null && backupsViewModel.backups.containsKey(backupsViewModel.currentBackup)) {
            val currentBackup = backupsViewModel.backups.getValue(backupsViewModel.currentBackup!!)
            DataCard(backupsViewModel.currentBackup!!) {
                val cardWidth = remember { mutableIntStateOf(0) }
                DataRow(stringResource(R.string.backup_type), currentBackup.type, mutableMaxWidth = cardWidth)
                DataRow(stringResource(R.string.kernel_version), currentBackup.kernelVersion, mutableMaxWidth = cardWidth, clickable = true)
                if (currentBackup.type == "raw") {
                    currentBackup.bootSha1?.takeIf { it.length >= 8 }?.let { sha1 ->
                        DataRow(
                            label = stringResource(R.string.boot_sha1),
                            value = sha1.substring(0, 8),
                            valueStyle = monoStyle,
                            mutableMaxWidth = cardWidth
                        )
                    }
                    if (currentBackup.hashes != null) {
                        val hashWidth = remember { mutableIntStateOf(0) }
                        DataSet(stringResource(R.string.hashes)) {
                            for (partitionName in PartitionUtil.PartitionNames) {
                                val hash = currentBackup.hashes[partitionName]
                                if (hash != null) {
                                    DataRow(
                                        label = partitionName,
                                        value = hash.takeIf { it.isNotEmpty() && it.length >= 8 }?.substring(0, 8) ?: "Hash not found!",
                                        valueStyle = monoStyle,
                                        mutableMaxWidth = hashWidth
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(!slotViewModel.isRefreshing.value) {
                Column {
                    Spacer(Modifier.height(5.dp))
                    if (slotViewModel.isActive) {
                        if (currentBackup.type == "raw") {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                onClick = {
                                    navController.navigate("slot$slotSuffix/backups/${backupsViewModel.currentBackup!!}/restore")
                                }
                            ) {
                                Text(stringResource(R.string.restore))
                            }
                        } else if (currentBackup.type == "ak3") {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                onClick = { showFlashAk3Confirm.value = true }
                            ) {
                                Text(stringResource(R.string.flash))
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                onClick = { showFlashMkbootfsConfirm.value = true }
                            ) {
                                Text(stringResource(R.string.flash_ak3_zip_mkbootfs))
                            }
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        onClick = { showDeleteConfirm.value = true }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
            AnimatedConfirmDialog(
                visible = showDeleteConfirm.value,
                title = "CAUTION!",
                message = "确定要删除此备份吗？",
                detail = backupsViewModel.currentBackup,
                confirmText = stringResource(R.string.delete),
                cancelText = stringResource(R.string.cancel),
                onConfirm = {
                    showDeleteConfirm.value = false
                    backupsViewModel.delete(context) { navController.popBackStack() }
                },
                onDismiss = { showDeleteConfirm.value = false }
            )
            if (currentBackup.type == "ak3") {
                AnimatedConfirmDialog(
                    visible = showFlashAk3Confirm.value,
                    title = "CAUTION!",
                    message = "确定要刷写此 AK3 备份吗？",
                    detail = backupsViewModel.currentBackup,
                    confirmText = stringResource(R.string.flash),
                    cancelText = stringResource(R.string.cancel),
                    onConfirm = {
                        showFlashAk3Confirm.value = false
                        slotViewModel.flashAk3(context, backupsViewModel.currentBackup!!, currentBackup.filename!!)
                        navController.navigate("slot$slotSuffix/backups/${backupsViewModel.currentBackup!!}/flash/ak3") {
                            popUpTo("slot$slotSuffix")
                        }
                    },
                    onDismiss = { showFlashAk3Confirm.value = false }
                )
                AnimatedConfirmDialog(
                    visible = showFlashMkbootfsConfirm.value,
                    title = "CAUTION!",
                    message = "确定要刷写此 AK3 备份 (mkbootfs) 吗？",
                    detail = backupsViewModel.currentBackup,
                    confirmText = stringResource(R.string.flash),
                    cancelText = stringResource(R.string.cancel),
                    onConfirm = {
                        showFlashMkbootfsConfirm.value = false
                        slotViewModel.flashAk3_mkbootfs(context, backupsViewModel.currentBackup!!, currentBackup.filename!!)
                        navController.navigate("slot$slotSuffix/backups/${backupsViewModel.currentBackup!!}/flash/ak3") {
                            popUpTo("slot$slotSuffix")
                        }
                    },
                    onDismiss = { showFlashMkbootfsConfirm.value = false }
                )
            }
        } else {
            DataCard(stringResource(R.string.backups))
            val backups = backupsViewModel.backups.filter { it.value.bootSha1.isNullOrEmpty() || it.value.bootSha1.equals(slotViewModel.sha1) || it.value.type == "ak3" }
            if (backups.isNotEmpty()) {
                for (id in backups.keys.sortedByDescending { it }) {
                    Spacer(Modifier.height(16.dp))
                    DataCard(
                        title = id,
                        button = {
                            AnimatedVisibility(!slotViewModel.isRefreshing.value) {
                                ViewButton(onClick = {
                                    navController.navigate("slot$slotSuffix/backups/$id")
                                })
                            }
                        }
                    ) {
                        DataRow(stringResource(R.string.kernel_version), backups[id]!!.kernelVersion, clickable = true)
                    }
                }
            } else {
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(R.string.no_backups_found),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    } else if (navController.currentDestination!!.route!!.endsWith("/backups/{backupId}/restore")) {
        DataCard (stringResource(R.string.restore))
        Spacer(Modifier.height(5.dp))
        val disabledColor = ButtonDefaults.buttonColors(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface
        )
        val currentBackup = backupsViewModel.backups.getValue(backupsViewModel.currentBackup!!)
        if (currentBackup.hashes != null) {
            for (partitionName in PartitionUtil.PartitionNames) {
                val hash = currentBackup.hashes[partitionName]
                if (hash != null) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (backupsViewModel.backupPartitions[partitionName] == true) 1.0f else 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        colors = if (backupsViewModel.backupPartitions[partitionName] == true) ButtonDefaults.outlinedButtonColors() else disabledColor,
                        enabled = backupsViewModel.backupPartitions[partitionName] != null,
                        onClick = {
                            backupsViewModel.backupPartitions[partitionName] = !backupsViewModel.backupPartitions[partitionName]!!
                        },
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            Checkbox(backupsViewModel.backupPartitions[partitionName] == true, null,
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = -(16.dp)))
                            Text(partitionName, Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        } else {
            Text(
                stringResource(R.string.partition_selection_unavailable),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(5.dp))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            onClick = { showRestoreConfirm.value = true },
            enabled = currentBackup.hashes == null || (PartitionUtil.PartitionNames.none {
                currentBackup.hashes[it] != null && backupsViewModel.backupPartitions[it] == null
            } && backupsViewModel.backupPartitions.filter { it.value }.isNotEmpty())
        ) {
            Text(stringResource(R.string.restore))
        }
        AnimatedConfirmDialog(
            visible = showRestoreConfirm.value,
            title = "CAUTION!",
            message = "确定要恢复所选分区吗？",
            detail = backupsViewModel.currentBackup,
            confirmText = stringResource(R.string.restore),
            cancelText = stringResource(R.string.cancel),
            onConfirm = {
                showRestoreConfirm.value = false
                backupsViewModel.restore(context, slotSuffix)
                navController.navigate("slot$slotSuffix/backups/${backupsViewModel.currentBackup!!}/restore/restore") {
                    popUpTo("slot$slotSuffix")
                }
            },
            onDismiss = { showRestoreConfirm.value = false }
        )
    } else {
        FlashList(
            stringResource(R.string.restore),
            backupsViewModel.restoreOutput
        ) {
            AnimatedVisibility(!backupsViewModel.isRefreshing && backupsViewModel.wasRestored != null) {
                Column {
                    if (backupsViewModel.wasRestored != false) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            onClick = { navController.navigate("reboot") }
                        ) {
                            Text(stringResource(R.string.reboot))
                        }
                    }
                }
            }
        }
    }
}
