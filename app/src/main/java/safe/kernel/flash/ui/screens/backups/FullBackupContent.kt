package safe.kernel.flash.ui.screens.backups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.components.FlashList
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUnitApi::class)
@Composable
fun ColumnScope.FullBackupContent(
    viewModel: BackupsViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val showStartConfirm = remember { mutableStateOf(false) }
    val summary = viewModel.fullBackupSummary

    LaunchedEffect(Unit) {
        if (viewModel.fullBackupSummary == null && viewModel.fullBackupOutput.isEmpty()) {
            viewModel.prepareFullBackup(context)
        }
    }

    if (viewModel.fullBackupOutput.isNotEmpty()) {
        FlashList("全字库备份日志", viewModel.fullBackupOutput) {
            AnimatedVisibility(!viewModel.isRefreshing && viewModel.fullBackupWasSuccessful != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(Modifier.height(6.dp))
                    DataCard("备份结果") {
                        val cardWidth = remember { mutableIntStateOf(0) }
                        DataRow("成功", "${viewModel.fullBackupSuccessCount} 个", mutableMaxWidth = cardWidth)
                        DataRow("跳过", "${viewModel.fullBackupSkipCount} 个", mutableMaxWidth = cardWidth)
                        DataRow("失败", "${viewModel.fullBackupFailCount} 个", mutableMaxWidth = cardWidth)
                        DataRow("备份目录", viewModel.fullBackupDirectory, mutableMaxWidth = cardWidth, clickable = true)
                    }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        onClick = {
                            viewModel.clearFullBackupOutput()
                            viewModel.prepareFullBackup(context)
                        }
                    ) {
                        Text("返回备份设置")
                    }
                }
            }
        }
        return
    }

    DataCard("全自动字库备份") {
        val cardWidth = remember { mutableIntStateOf(0) }
        if (summary != null) {
            DataRow("平台", summary.platform, mutableMaxWidth = cardWidth)
            DataRow("源路径", summary.sourceDir, mutableMaxWidth = cardWidth, clickable = true)
            DataRow("总分区数", "${summary.totalPartitions} 个", mutableMaxWidth = cardWidth)
            DataRow("跳过分区", summary.skippedPartitions.ifEmpty { listOf("无") }.joinToString(" "), mutableMaxWidth = cardWidth)
            DataRow("将备份", "${summary.backupCount} 个", mutableMaxWidth = cardWidth)
            DataRow("预计占用", formatFullBackupBytes(summary.estimatedBytes), mutableMaxWidth = cardWidth)
        } else if (viewModel.isRefreshing) {
            Text(
                "正在检测平台、Root 权限和分区大小…",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "还没有检测到可备份分区，点击重新检测。",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    DataCard("备份目录") {
        OutlinedTextField(
            value = viewModel.fullBackupDirectory,
            onValueChange = { viewModel.updateFullBackupDirectory(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("自定义备份目录") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Folder, contentDescription = null) },
            supportingText = { Text("默认：/sdcard/ReKernelFlasher/full_backups/时间戳") }
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = !viewModel.isRefreshing,
                onClick = {
                    viewModel.resetFullBackupDirectory()
                    viewModel.prepareFullBackup(context)
                }
            ) {
                Text("重置目录")
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = !viewModel.isRefreshing,
                onClick = { viewModel.prepareFullBackup(context) }
            ) {
                Text("重新检测")
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    AnimatedVisibility(!viewModel.isRefreshing) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ListItem(
                title = "开始全字库备份",
                subtitle = summary?.let { "预计需要 ${formatFullBackupBytes(it.estimatedBytes)} 可用空间，跳过 userdata / sdc" }
                    ?: "先检测平台和分区大小",
                leadingIcon = Icons.Filled.Storage,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                trailingIcon = Icons.Filled.PlayArrow,
                enabled = summary != null,
                onClick = { showStartConfirm.value = true }
            )
            ListItem(
                title = "刷新分区信息",
                subtitle = "重新统计分区数量和预计占用空间",
                leadingIcon = Icons.Filled.Refresh,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = { viewModel.prepareFullBackup(context) }
            )
        }
    }

    AnimatedConfirmDialog(
        visible = showStartConfirm.value,
        title = "开始全字库备份",
        message = "将备份除 userdata / sdc 外的所有分区，预计占用 ${formatFullBackupBytes(summary?.estimatedBytes ?: 0L)}。",
        detail = viewModel.fullBackupDirectory,
        confirmText = "开始备份",
        cancelText = androidx.compose.ui.res.stringResource(R.string.cancel),
        onConfirm = {
            showStartConfirm.value = false
            viewModel.startFullBackup(context)
        },
        onDismiss = { showStartConfirm.value = false }
    )
}

private fun formatFullBackupBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) {
        "$bytes ${units[index]}"
    } else {
        String.format(Locale.US, "%.2f %s", value, units[index])
    }
}
