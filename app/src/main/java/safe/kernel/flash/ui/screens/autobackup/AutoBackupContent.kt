package safe.kernel.flash.ui.screens.autobackup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import safe.kernel.flash.common.types.autobackup.AutoBackupRecord
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoBackupContent(
    viewModel: AutoBackupViewModel,
    @Suppress("UNUSED_PARAMETER") navController: NavController
) {
    val context = LocalContext.current
    var pendingRollback by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    var pendingFile by remember { mutableStateOf<File?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val file = pendingFile ?: return@rememberLauncherForActivityResult
        pendingFile = null
        if (granted) {
            openFile(context, file)
        } else {
            Toast.makeText(context, "需要存储权限才能打开文件", Toast.LENGTH_SHORT).show()
        }
    }

    if (viewModel.records.isEmpty()) {
        Spacer(Modifier.height(32.dp))
        Text("暂无自动备份记录", modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, fontStyle = FontStyle.Italic)
        return
    }

    for (record in viewModel.records) {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = {},
                    onLongClick = {
                        val path = record.path
                        if (path.isEmpty()) {
                            Toast.makeText(context, "该记录为多分区备份，请使用回滚", Toast.LENGTH_SHORT).show()
                            return@combinedClickable
                        }
                        val file = File(path)
                        if (!file.exists()) {
                            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
                            return@combinedClickable
                        }
                        Shell.cmd("chmod 644 $file").exec()
                        openFile(context, file)
                    }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss秒")
                .withZone(ZoneId.systemDefault())
            val timeStr = formatter.format(Instant.ofEpochSecond(record.timestamp))
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)) {
                Text(text = timeStr, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = recordDescription(record),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                    overflow = TextOverflow.Visible)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { pendingRollback = record.timestamp },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(text = "回滚", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    AnimatedConfirmDialog(
        visible = pendingRollback != null,
        title = "回滚",
        message = "确定要恢复该备份对应的分区吗？将把备份镜像刷回原分区。",
        confirmText = "回滚",
        cancelText = "取消",
        destructive = true,
        onConfirm = {
            val timestamp = pendingRollback
            pendingRollback = null
            timestamp?.let { viewModel.rollback(context, it) {} }
        },
        onDismiss = { pendingRollback = null }
    )
}

private fun recordDescription(record: AutoBackupRecord): String {
    return if (record.kind == AutoBackupRecord.KIND_AK3) {
        record.items.joinToString("\n") { "${it.source} -> ${it.path}" }
    } else {
        "auto-backup ${record.partition}${record.slot} stored in ${record.path}"
    }
}

private fun openFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
