package safe.kernel.flash.ui.screens.backups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.Card
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.FullBackupContent(
    viewModel: BackupsViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val showStartConfirm = remember { mutableStateOf(false) }
    val showPartitionList = remember { mutableStateOf(false) }
    val summary = viewModel.fullBackupSummary

    LaunchedEffect(Unit) {
        if (viewModel.fullBackupSummary == null && viewModel.fullBackupOutput.isEmpty()) {
            viewModel.prepareFullBackup(context)
        }
    }

    // 备份开始后进入日志页：顶部工作信息 / 备份结果 → 进度 → 日志
    if (viewModel.fullBackupOutput.isNotEmpty()) {
        FullBackupLogContent(viewModel)
        return
    }

    val selectedCount = viewModel.fullBackupSelectedPartitions.size
    val selectedSizeText = formatFullBackupBytes(viewModel.fullBackupSelectedBytes)

    DataCard(stringResource(R.string.full_backup)) {
        val cardWidth = remember { mutableIntStateOf(0) }
        if (summary != null) {
            DataRow(
                stringResource(R.string.platform),
                summary.platform,
                mutableMaxWidth = cardWidth
            )
            DataRow(stringResource(R.string.source_path), summary.sourceDir, mutableMaxWidth = cardWidth, clickable = true)
            DataRow(
                stringResource(R.string.total_partitions),
                stringResource(R.string.count_items, summary.totalPartitions),
                mutableMaxWidth = cardWidth
            )
            DataRow(
                stringResource(R.string.skipped_partitions),
                summary.skippedPartitions.ifEmpty { listOf(stringResource(R.string.none)) }.joinToString(" "),
                mutableMaxWidth = cardWidth
            )
            DataRow(
                stringResource(R.string.to_backup),
                stringResource(R.string.backup_selection_value, selectedCount, summary.backupCount),
                mutableMaxWidth = cardWidth
            )
            DataRow(stringResource(R.string.estimated_size), selectedSizeText, mutableMaxWidth = cardWidth)
        } else if (viewModel.isRefreshing) {
            Text(
                stringResource(R.string.detecting_platform),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                stringResource(R.string.no_backupable_partitions),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    DataCard(stringResource(R.string.backup_directory)) {
        OutlinedTextField(
            value = viewModel.fullBackupDirectory,
            onValueChange = { viewModel.updateFullBackupDirectory(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.custom_backup_dir)) },
            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
            supportingText = {
                Text(stringResource(R.string.backup_dir_hint, viewModel.defaultFullBackupDirectory()))
            }
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
                Text(stringResource(R.string.reset_dir))
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = !viewModel.isRefreshing,
                onClick = { viewModel.prepareFullBackup(context) }
            ) {
                Text(stringResource(R.string.rescan))
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    AnimatedVisibility(!viewModel.isRefreshing) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListItem(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.start_full_backup),
                    subtitle = summary?.let {
                        stringResource(
                            R.string.start_full_backup_selected,
                            selectedCount,
                            it.backupCount,
                            selectedSizeText
                        )
                    } ?: stringResource(R.string.start_full_backup_hint),
                    leadingIcon = Icons.Filled.Storage,
                    leadingColors = ListItemIconColors(
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    trailingIcon = Icons.Filled.PlayArrow,
                    enabled = summary != null && selectedCount > 0,
                    onClick = { showStartConfirm.value = true }
                )
                // 信息按钮：查看扫描到的分区列表并勾选
                FilledTonalIconButton(
                    modifier = Modifier.size(54.dp),
                    onClick = { showPartitionList.value = true },
                    enabled = summary != null,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.view_partition_list))
                }
            }
            ListItem(
                title = stringResource(R.string.refresh_partition_info),
                subtitle = stringResource(R.string.refresh_partition_info_sub),
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
        title = stringResource(R.string.start_full_backup),
        message = stringResource(R.string.start_backup_confirm_msg, selectedCount, selectedSizeText),
        detail = viewModel.fullBackupDirectory,
        confirmText = stringResource(R.string.start_backup_confirm_action),
        cancelText = stringResource(R.string.cancel),
        onConfirm = {
            showStartConfirm.value = false
            viewModel.startFullBackup(context)
        },
        onDismiss = { showStartConfirm.value = false }
    )

    if (showPartitionList.value) {
        FullBackupPartitionDialog(
            viewModel = viewModel,
            onDismiss = { showPartitionList.value = false }
        )
    }
}

/** 备份成功后悬浮在页面底部的「返回备份设置」按钮（由 RefreshableScreen 的 bottomOverlay 承载）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullBackupReturnButton(viewModel: BackupsViewModel) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = viewModel.fullBackupWasSuccessful != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { viewModel.returnToFullBackupSetup(context) }
        ) {
            Text(
                stringResource(R.string.back_to_backup_settings),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.FullBackupLogContent(viewModel: BackupsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FullBackupInfoCard(viewModel)
        FullBackupProgressCard(viewModel)
        FullBackupLogPanel(viewModel)
    }
}

/**
 * 页面顶部的信息栏：备份未完成时标题为「工作信息」，展示待备份分区总数；
 * 备份完成后原地替换为「备份结果」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullBackupInfoCard(viewModel: BackupsViewModel) {
    val finished = viewModel.fullBackupWasSuccessful != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Text(
            text = stringResource(if (finished) R.string.backup_result else R.string.job_info),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        val cardWidth = remember { mutableIntStateOf(0) }
        if (finished) {
            DataRow(
                stringResource(R.string.success),
                stringResource(R.string.count_items, viewModel.fullBackupSuccessCount),
                mutableMaxWidth = cardWidth
            )
            DataRow(
                stringResource(R.string.skipped),
                stringResource(R.string.count_items, viewModel.fullBackupSkipCount),
                mutableMaxWidth = cardWidth
            )
            DataRow(
                stringResource(R.string.failed),
                stringResource(R.string.count_items, viewModel.fullBackupFailCount),
                mutableMaxWidth = cardWidth
            )
        } else {
            DataRow(
                stringResource(R.string.total_to_backup),
                stringResource(R.string.count_items, viewModel.fullBackupTotalCount),
                mutableMaxWidth = cardWidth
            )
        }
        DataRow(
            stringResource(R.string.backup_directory),
            viewModel.fullBackupDirectory,
            mutableMaxWidth = cardWidth,
            clickable = true
        )
        viewModel.fullBackupBpInfoPath?.let { bpInfoPath ->
            DataRow(
                stringResource(R.string.bp_info),
                bpInfoPath,
                mutableMaxWidth = cardWidth,
                clickable = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullBackupProgressCard(viewModel: BackupsViewModel) {
    val total = viewModel.fullBackupTotalCount
    val completed = viewModel.fullBackupCompletedCount
    val totalProgress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val current = viewModel.fullBackupCurrentPartition
    val currentProgress = viewModel.fullBackupCurrentProgress
    val totalPercent = (totalProgress * 100).toInt()
    val currentPercent = (currentProgress * 100).toInt()

    DataCard(stringResource(R.string.progress)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.total_progress),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.progress_counts, completed, total, totalPercent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { totalProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.current_partition),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = current ?: if (viewModel.fullBackupIsRunning) stringResource(R.string.preparing) else "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.progress_percent, currentPercent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        if (current != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.progress_bytes,
                    formatFullBackupBytes(viewModel.fullBackupCurrentCopiedBytes),
                    formatFullBackupBytes(viewModel.fullBackupCurrentTotalBytes)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 日志区（无标题），位于进度条下方。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullBackupLogPanel(viewModel: BackupsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        viewModel.fullBackupOutput.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullBackupPartitionDialog(
    viewModel: BackupsViewModel,
    onDismiss: () -> Unit
) {
    val partitions = viewModel.fullBackupSummary?.backupPartitions.orEmpty()
    val selectedCount = viewModel.fullBackupSelectedPartitions.size
    val selectedBytes = viewModel.fullBackupSelectedBytes
    val allSelected = partitions.isNotEmpty() &&
        partitions.all { viewModel.fullBackupSelection[it.name] == true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = stringResource(R.string.partition_list),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.selection_summary,
                        selectedCount,
                        partitions.size,
                        formatFullBackupBytes(selectedBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 单个切换按钮：全选 → 全不选 → 全选
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                viewModel.clearFullBackupSelection()
                            } else {
                                viewModel.selectAllFullBackupPartitions()
                            }
                        }
                    ) {
                        Text(stringResource(if (allSelected) R.string.select_none else R.string.select_all))
                    }
                    Spacer(Modifier.weight(1f))
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(partitions, key = { it.name }) { partition ->
                        val checked = viewModel.fullBackupSelection[partition.name] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleFullBackupPartition(partition.name) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.setFullBackupPartitionSelected(partition.name, it) }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = partition.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatFullBackupBytes(partition.sizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        val skipped = viewModel.fullBackupSummary?.skippedPartitions.orEmpty()
                        if (skipped.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.skipped_hint, skipped.joinToString(" / ")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
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
