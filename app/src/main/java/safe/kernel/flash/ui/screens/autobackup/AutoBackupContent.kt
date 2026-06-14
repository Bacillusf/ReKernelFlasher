package safe.kernel.flash.ui.screens.autobackup

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.common.types.autobackup.AutoBackupRecord
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
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (viewModel.records.isEmpty()) {
        Spacer(Modifier.height(32.dp))
        Text(
            "暂无自动备份记录",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )
        return
    }

    for (record in viewModel.records) {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        try {
                            val file = File(record.path)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/octet-stream")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "文件不存在: ${record.path}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss秒")
                .withZone(ZoneId.systemDefault())
            val timeStr = formatter.format(Instant.ofEpochSecond(record.timestamp))

            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
            )
            Text(
                text = "auto-backup ${record.partition}${record.slot} stored in ${record.path}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                overflow = TextOverflow.Visible
            )
        }
    }
}
