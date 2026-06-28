package safe.kernel.flash.ui.screens.toolbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.theme.softShadow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.RkpFixContent(navController: NavController) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var showConfirm by remember { mutableStateOf(false) }
    val output = remember { mutableStateListOf<String>() }

    LaunchedEffect(page) {
        if (page != 1) return@LaunchedEffect
        output.clear()
        try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val dir = "/sdcard/ReKernelFlasher/persist_backup"
            Shell.cmd("mkdir -p $dir").exec()
            output.add("=== RKP 修复 ===")
            output.add("")
            output.add("→ [1/2] 备份 persist 分区...")
            val r1 = Shell.cmd("dd if=/dev/block/by-name/persist of=$dir/persist_orig.img 2>&1").exec()
            r1.out.forEach { output.add("  ${it.trim()}") }
            r1.err.forEach { if (it.isNotBlank()) output.add("  ! ${it.trim()}") }
            output.add(if (r1.isSuccess) "  ✓ 备份完成" else "  ! 备份可能失败")
            output.add("")
            output.add("→ [2/2] 执行 RKP 修复...")
            val r2 = Shell.cmd("LD_LIBRARY_PATH=/vendor/lib64/hw KmInstallKeybox Keybox_file Device_ID true rkp 2>&1").exec()
            r2.out.forEach { output.add("  ${it.trim()}") }
            r2.err.forEach { if (it.isNotBlank()) output.add("  ! ${it.trim()}") }
            output.add("")
            val combined = (r2.out + r2.err).joinToString(" ")
            when {
                combined.contains("InstallKeybox is done", true) -> output.add("✓ RKP 修复成功 — InstallKeybox is done")
                combined.contains("Error", true) || combined.contains("Failed", true) -> output.add("✗ RKP 修复失败")
                else -> output.add("! 执行完毕，请确认上方输出")
            }
            output.add("")
            output.add("--- 日志: $dir/rkp_fix_$now.log ---")
            // Write log via root to avoid EPERM
            try {
                val tmpLog = java.io.File(context.filesDir, "rkp_tmp.log")
                tmpLog.writeText(output.joinToString("\n"))
                Shell.cmd("cp $tmpLog $dir/rkp_fix_$now.log").exec()
                tmpLog.delete()
            } catch (_: Exception) {}
            page = 2
        } catch (e: Exception) {
            output.add("✗ 错误: ${e.message}")
            page = 2
        }
    }

    // Info page — NO verticalScroll, fixed by using ColumnScope which RefreshableScreen handles
    if (page == 0) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Icon(Icons.Filled.Security, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text("RKP 修复工具", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "本功能是实验性的，仅供尝鲜。\n仅适用于高通平台，联发科勿用！\n有可能引发设备问题。\n如因此功能出现问题，请勿提交该 issue。",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().softShadow(cornerRadius = 16.dp, alpha = 0.06f, offsetY = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "此功能帮助骁龙设备因解锁BL导致TEE假死、RKP不可用的问题。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "仅适用于 Qcom 平台，MediaTek 勿用。",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) { Text("开始", style = MaterialTheme.typography.titleMedium) }
        }
    }

    // Execute & result page — NO verticalScroll
    if (page >= 1) {
        Column {
            Spacer(Modifier.height(8.dp))
            Text("RKP 修复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    for (line in output) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (page == 1) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在执行...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (page == 2) {
                Button(
                    onClick = { navController.navigate("main") { popUpTo("main") { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("完成", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showConfirm) {
        AnimatedConfirmDialog(
            visible = true,
            title = "⚠ 警告",
            message = "此操作将备份 persist 分区并执行 RKP 修复。\n\n仅适用于高通骁龙平台！\n联发科设备切勿使用！",
            confirmText = "继续",
            cancelText = "取消",
            onConfirm = { showConfirm = false; page = 1 },
            onDismiss = { showConfirm = false },
            destructive = true
        )
    }
}
