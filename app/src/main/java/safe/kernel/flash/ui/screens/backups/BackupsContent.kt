package safe.kernel.flash.ui.screens.backups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.PartitionUtil
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.components.DataSet
import safe.kernel.flash.ui.components.ViewButton

@ExperimentalMaterial3Api
@Composable
fun ColumnScope.BackupsContent(
    viewModel: BackupsViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    val monoStyle = MaterialTheme.typography.titleSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    )

    val showDeleteConfirm = remember { mutableStateOf(false) }

    if (viewModel.currentBackup != null && viewModel.backups.containsKey(viewModel.currentBackup)) {
        DataCard(viewModel.currentBackup!!) {
            val cardWidth = remember { mutableIntStateOf(0) }
            val backupId = viewModel.currentBackup!!
            val currentBackup = viewModel.backups[backupId]
            if(currentBackup == null) return@DataCard
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
                                    value = hash.takeIf { it.isNotEmpty() }?.substring(0, 8) ?: "Hash not found!",
                                    valueStyle = monoStyle,
                                    mutableMaxWidth = hashWidth
                                )
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(!viewModel.isRefreshing) {
            Column {
                Spacer(Modifier.height(5.dp))
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
            detail = viewModel.currentBackup,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            onConfirm = {
                showDeleteConfirm.value = false
                viewModel.delete(context) { navController.popBackStack() }
            },
            onDismiss = { showDeleteConfirm.value = false }
        )
    } else {
        DataCard(stringResource(R.string.backups))
        AnimatedVisibility(viewModel.needsMigration) {
            Column {
                Spacer(Modifier.height(5.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onClick = { viewModel.migrate(context) }
                ) {
                    Text(stringResource(R.string.migrate))
                }
            }
        }
        if (viewModel.backups.isNotEmpty()) {
            for (id in viewModel.backups.keys.sortedByDescending { it }) {
                val currentBackup = viewModel.backups[id]!!
                Spacer(Modifier.height(16.dp))
                DataCard(
                    title = id,
                    button = {
                        AnimatedVisibility(!viewModel.isRefreshing) {
                            Column {
                                ViewButton(onClick = {
                                    navController.navigate("backups/$id")
                                })
                            }
                        }
                    }
                ) {
                    val cardWidth = remember { mutableIntStateOf(0) }
                    if (currentBackup.type == "raw") {
                        currentBackup.bootSha1?.takeIf { it.length >= 8 }?.let { sha1 ->
                            DataRow(
                                label = stringResource(R.string.boot_sha1),
                                value = sha1.substring(0, 8),
                                valueStyle = monoStyle,
                                mutableMaxWidth = cardWidth
                            )
                        }
                    }
                    DataRow(stringResource(R.string.kernel_version), currentBackup.kernelVersion, mutableMaxWidth = cardWidth, clickable = true)
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
}
