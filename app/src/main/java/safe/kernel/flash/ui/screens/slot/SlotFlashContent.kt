package safe.kernel.flash.ui.screens.slot

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.PartitionUtil
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.FlashButton
import safe.kernel.flash.ui.components.FlashList
import safe.kernel.flash.ui.components.SlotCard
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
    ExperimentalUnitApi::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.SlotFlashContent(
    viewModel: SlotViewModel,
    slotSuffix: String,
    navController: NavController
) {
    val context = LocalContext.current

    val isRefreshing by remember { derivedStateOf { viewModel.isRefreshing } }
    val currentRoute = navController.currentDestination?.route.orEmpty()

    val isAk3 = currentRoute.contains("ak3")
    val isFlashImage = currentRoute.endsWith("/flash/image")
    val isBackup = currentRoute.endsWith("/backup")
    val isBackupResult = currentRoute.endsWith("/backup/backup")
    val isFlashAk3 = currentRoute.endsWith("/flash/ak3")
    val isImageFlashResult = currentRoute.endsWith("/flash/image/flash")

    val isFlashScreen = currentRoute.endsWith("/flash")
    val isSlotScreen = !(isFlashAk3 || isImageFlashResult || isBackupResult)

    BackHandler(enabled = ((isFlashAk3 || isImageFlashResult || isBackupResult) && isRefreshing.value)) { }

    if (isSlotScreen) {
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
            isSlotScreen = true,
            showDlkm = false
        )
        Spacer(Modifier.height(16.dp))
        if (isFlashScreen) {
            DataCard(stringResource(R.string.flash))
            Spacer(Modifier.height(8.dp))
            FlashButton(stringResource(R.string.flash_ak3_zip), "zip", callback = { uri ->
                viewModel.flashActionType = "flashAk3"
                viewModel.flashActionURI = uri
                viewModel.showConfirmDialog()
            })
            Spacer(Modifier.height(8.dp))
            FlashButton(stringResource(R.string.flash_ak3_zip_mkbootfs), "zip", callback = { uri ->
                viewModel.flashActionType = "flashAk3_mkbootfs"
                viewModel.flashActionURI = uri
                viewModel.showConfirmDialog()
            })
            Spacer(Modifier.height(8.dp))
            FlashButton(stringResource(R.string.flash_ksu_lkm), "ko", callback = { uri ->
                viewModel.flashActionType = "flashKsuDriver"
                viewModel.flashActionURI = uri
                viewModel.showConfirmDialog()
            })
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                onClick = { navController.navigate("slot$slotSuffix/flash/image") }
            ) {
                Text(
                    stringResource(R.string.flash_partition_image),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.padding(6.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        } else if (isFlashImage) {
            DataCard(stringResource(R.string.flash_partition_image))
            Spacer(Modifier.height(8.dp))
            for (partitionName in PartitionUtil.AvailablePartitions) {
                FlashButton(partitionName, "img", callback = { uri ->
                    viewModel.flashActionType = "flashImage"
                    viewModel.flashActionURI = uri
                    viewModel.flashActionPartName = partitionName
                    viewModel.showConfirmDialog()
                })
                Spacer(Modifier.height(6.dp))
            }
        } else if (isBackup) {
            DataCard(stringResource(R.string.backup))
            Spacer(Modifier.height(8.dp))
            val showBackupConfirm = remember { mutableStateOf(false) }
            for (partitionName in PartitionUtil.AvailablePartitions) {
                PartitionCheckRow(
                    label = partitionName,
                    checked = viewModel.backupPartitions[partitionName] == true,
                    enabled = viewModel.backupPartitions[partitionName] != null,
                    onClick = {
                        viewModel.backupPartitions[partitionName] = !viewModel.backupPartitions[partitionName]!!
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                enabled = viewModel.backupPartitions.filter { it.value }.isNotEmpty(),
                onClick = { showBackupConfirm.value = true }
            ) {
                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.padding(6.dp))
                Text(stringResource(R.string.backup_now), style = MaterialTheme.typography.titleMedium)
            }
            AnimatedConfirmDialog(
                visible = showBackupConfirm.value,
                title = "警告",
                message = "确定要备份所选分区吗？",
                confirmText = stringResource(R.string.backup_now),
                cancelText = stringResource(R.string.cancel),
                onConfirm = {
                    showBackupConfirm.value = false
                    viewModel.backup(context)
                    navController.navigate("slot$slotSuffix/backup/backup") {
                        popUpTo("slot$slotSuffix")
                    }
                },
                onDismiss = { showBackupConfirm.value = false },
                destructive = true
            )
        }
    } else {
        Text("")
        FlashList(
            stringResource(if (isBackupResult) R.string.backup else R.string.flash),
            if (isAk3) viewModel.uiPrintedOutput else viewModel.flashOutput
        ) {
            AnimatedVisibility(!viewModel.isRefreshing.value && viewModel.wasFlashSuccess.value != null) {
                Column {
                    if (isAk3) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                            onClick = { viewModel.saveLog(context) }
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.padding(6.dp))
                            Text(stringResource(R.string.save_ak3_log), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (isAk3) {
                        AnimatedVisibility(
                            !currentRoute.endsWith("/backups/{backupId}/flash/ak3") && viewModel.wasFlashSuccess.value != false
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                                onClick = {
                                    viewModel.backupZip(context) {
                                        navController.navigate("slot$slotSuffix/backups") {
                                            popUpTo("slot$slotSuffix")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.padding(6.dp))
                                Text(stringResource(R.string.save_ak3_zip_as_backup), style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (viewModel.wasFlashSuccess.value == true && viewModel.showCautionDialog == true) {
                        AnimatedConfirmDialog(
                            visible = true,
                            title = "警告",
                            message = "你已刷写到了非活跃槽位！",
                            detail = "但活跃槽位在刷写后并未切换。\n请切换活跃槽位或返回系统更新器完成OTA。\n除非你知道自己在做什么，否则不要从这里重启。",
                            confirmText = "切换槽位",
                            cancelText = "取消",
                            onConfirm = {
                                viewModel.hideCautionDialog()
                                viewModel.switchSlot(context)
                            },
                            onDismiss = { viewModel.hideCautionDialog() }
                        )
                    }
                    if (viewModel.wasFlashSuccess.value != false && isBackupResult) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.padding(6.dp))
                            Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                            onClick = { navController.navigate("reboot") }
                        ) {
                            Text(stringResource(R.string.reboot), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    if (viewModel.showConfirmDialog == true) {
        fun resolveSourcePath(uri: Uri?): String {
            if (uri == null) return "unknown"
            val decoded = Uri.decode(uri.toString())
            return when {
                uri.scheme == "file" -> uri.path ?: decoded
                uri.toString().contains("com.android.externalstorage.documents") -> {
                    val docId = uri.lastPathSegment ?: return decoded
                    val colonIdx = docId.indexOf(':')
                    if (colonIdx >= 0) {
                        val root = docId.substring(0, colonIdx)
                        val subPath = docId.substring(colonIdx + 1)
                        val base = if (root == "primary") "/storage/emulated/0" else "/storage/$root"
                        "$base/$subPath"
                    } else decoded
                }
                else -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex("_data")
                            if (idx != -1) cursor.getString(idx) else null
                        } else null
                    } ?: decoded
                }
            }
        }

        val sourcePath = resolveSourcePath(viewModel.flashActionURI)

        val destPath = when (viewModel.flashActionType) {
            "flashImage" -> {
                val partitionName = viewModel.flashActionPartName ?: "unknown"
                PartitionUtil.findPartitionBlockDevice(context, partitionName, slotSuffix)?.path ?: "$partitionName$slotSuffix"
            }
            "flashAk3", "flashAk3_mkbootfs", "flashKsuDriver" -> viewModel.boot.absolutePath
            else -> "本机"
        }

        val isAk3 = viewModel.flashActionType in listOf("flashAk3", "flashAk3_mkbootfs")
        LaunchedEffect(viewModel.showConfirmDialog, isAk3) {
            if (viewModel.showConfirmDialog && isAk3) {
                viewModel.parseAk3Preview(context)
            }
        }

        val ak3Info = viewModel._ak3PreviewInfo.value
        val ak3Detail = if (isAk3 && ak3Info != null) {
            buildString {
                val status = if (ak3Info.isCompatible) "✓" else "✗"
                if (ak3Info.deviceNames.isNotEmpty()) {
                    append("目标设备: ${ak3Info.deviceNames.joinToString(", ")} $status")
                } else {
                    append("目标设备: Unknown $status")
                }
                if (ak3Info.block.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("目标分区: ${ak3Info.block}")
                }
                if (ak3Info.kernelString.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("内核标识: ${ak3Info.kernelString}")
                }
                if (isNotEmpty()) append("\n")
                append("设备检查: ${if (ak3Info.doDeviceCheck) "开启" else "关闭"}")
            }
        } else null

        val title = "警告"
        val message = when (viewModel.flashActionType) {
            "flashImage" -> "确定要将 $sourcePath 刷写到 $destPath 吗？"
            "flashAk3" -> "确定要将 $sourcePath 刷写到 $destPath 吗？"
            "flashAk3_mkbootfs" -> "确定要将 $sourcePath 以 mkbootfs 方式\n刷写到 $destPath 吗？"
            "flashKsuDriver" -> "确定要将 $sourcePath 作为 KernelSU 驱动\n刷写到 $destPath 吗？"
            else -> "确定要将 $sourcePath 刷写到本机吗？"
        }
        val confirmText = if (isAk3 && ak3Info != null && !ak3Info.isCompatible) "强行刷写（不兼容）" else "刷写"
        val cancelText = stringResource(R.string.cancel)

        AnimatedConfirmDialog(
            visible = true,
            title = title,
            message = message,
            detail = ak3Detail,
            confirmText = confirmText,
            cancelText = cancelText,
            onConfirm = {
                viewModel.hideConfirmDialog()
                val isOtherFlash = viewModel.flashActionType != "flashImage" && viewModel.flashActionURI != null
                val isPartitionFlash = viewModel.flashActionType == "flashImage" && viewModel.flashActionPartName != null && viewModel.flashActionURI != null

                if (isOtherFlash || isPartitionFlash) {
                    val uri = viewModel.flashActionURI!!
                    val partitionName: String? = viewModel.flashActionPartName

                    when (viewModel.flashActionType) {
                        "flashAk3" -> {
                            navController.navigate("slot$slotSuffix/flash/ak3") {
                                popUpTo("slot$slotSuffix")
                            }
                            viewModel.flashAk3(context, uri)
                        }
                        "flashAk3_mkbootfs" -> {
                            navController.navigate("slot$slotSuffix/flash/ak3") {
                                popUpTo("slot$slotSuffix")
                            }
                            viewModel.flashAk3_mkbootfs(context, uri)
                        }
                        "flashKsuDriver" -> {
                            navController.navigate("slot$slotSuffix/flash/image/flash") {
                                popUpTo("slot$slotSuffix")
                            }
                            viewModel.flashKsuDriver(context, uri)
                        }
                        "flashImage" -> {
                            navController.navigate("slot$slotSuffix/flash/image/flash") {
                                popUpTo("slot$slotSuffix")
                            }
                            viewModel.flashImage(context, uri, partitionName!!)
                        }
                    }
                    viewModel.flashActionType = ""
                    viewModel.flashActionURI = null
                    viewModel.flashActionPartName = null
                }
            },
            onDismiss = { viewModel.hideConfirmDialog() },
            destructive = true
        )
    }
}

@Composable
private fun PartitionCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(
                color = if (checked)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(Modifier.padding(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
            color = if (checked)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
