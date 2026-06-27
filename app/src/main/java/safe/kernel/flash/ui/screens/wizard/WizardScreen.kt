package safe.kernel.flash.ui.screens.wizard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import safe.kernel.flash.BuildConfig
import safe.kernel.flash.ui.theme.softShadow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(navController: NavController) {
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        when (step) {
            1 -> WizardStep1(onNext = { step = 2 })
            2 -> WizardStep2(context, onNext = { step = 3 })
            3 -> WizardStep3(context, onNext = { step = 4 })
            4 -> WizardStep4(
                onStart = {
                    context.getSharedPreferences("wizard", 0)
                        .edit().putInt("version", BuildConfig.VERSION_CODE).apply()
                    navController.navigate("main") {
                        popUpTo("wizard") { inclusive = true }
                    }
                }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WizardStep1(onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isDark) Color(0xFF81C995) else MaterialTheme.colorScheme.primary

    Icon(
        Icons.Filled.Security, contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = accentColor
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "欢迎使用 ReKernelFlasher",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().softShadow(cornerRadius = 20.dp, alpha = 0.06f, offsetY = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "ReKernelFlasher 是一款强大的 Android 内核刷写工具，适用于已获取 Root 权限的设备。",
                style = MaterialTheme.typography.bodyLarge, color = textColor
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "• 支持刷写 AnyKernel3 (AK3) ZIP 包和分区镜像 (.img)\n" +
                "• 支持刷写 KernelSU LKM 驱动模块\n" +
                "• A/B 分区设备无缝更新支持\n" +
                "• 刷写前自动备份，历史操作记录\n" +
                "• 集成 Payload-Dumper 解包工具\n" +
                "• 自动禁用/隐藏 AVB2.0 校检",
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "在使用本软件之前需要 Root 权限和刷入后端模块，向导将引导你完成。",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) { Text("下一步", style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun WizardStep2(context: android.content.Context, onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    var checking by remember { mutableStateOf(false) }
    var rootType by remember { mutableStateOf<String?>(null) }
    var hasRoot by remember { mutableStateOf<Boolean?>(null) }

    Icon(
        Icons.Filled.Star, contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = if (isDark) Color(0xFF81C995) else MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Root 权限检查",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    Spacer(Modifier.height(16.dp))

    if (!checking && hasRoot == null) {
        Button(
            onClick = {
                checking = true
                val isKsu = Shell.cmd("test -f /data/adb/ksud && echo yes || echo no").exec().out.firstOrNull() == "yes"
                val isApatch = Shell.cmd("test -f /data/adb/apd && echo yes || echo no").exec().out.firstOrNull() == "yes"
                val isMagisk = Shell.cmd("test -f /data/adb/magisk && echo yes || echo no").exec().out.firstOrNull() == "yes"
                checking = false
                rootType = when {
                    isKsu -> "KernelSU"
                    isApatch -> "APatch"
                    isMagisk -> "Magisk"
                    else -> null
                }
                hasRoot = rootType != null || Shell.isAppGrantedRoot() == true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) { Text("开始检查", style = MaterialTheme.typography.titleMedium) }
    }

    if (checking) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp))
        Text("正在检查...", color = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (hasRoot == true && rootType != null) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text("$rootType Root 授权成功", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) { Text("下一步", style = MaterialTheme.typography.titleMedium) }
    }

    if (hasRoot == false) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "未检测到 Root 权限，请先获取 Root 后再使用本软件。",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun WizardStep3(context: android.content.Context, onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant
    var flashing by remember { mutableStateOf(false) }
    var flashDone by remember { mutableStateOf(false) }
    var flashError by remember { mutableStateOf<String?>(null) }
    var triggerFlash by remember { mutableIntStateOf(0) }
    val logLines = remember { mutableStateListOf<String>() }

    LaunchedEffect(triggerFlash) {
        if (triggerFlash == 0) return@LaunchedEffect
        flashing = true
        flashError = null
        logLines.clear()
        try {
            fun log(msg: String) { logLines.add(msg) }

            log("→ 正在复制模块文件...")
            val tmpFile = File(context.filesDir, "RKF.zip")
            context.assets.open("RKF.zip").use { input ->
                tmpFile.outputStream().use { output -> input.copyTo(output) }
            }

            log("→ 检测 Root 管理器...")
            val isKsu = Shell.cmd("test -f /data/adb/ksud && echo yes || echo no").exec().out.firstOrNull() == "yes"
            val isApatch = Shell.cmd("test -f /data/adb/apd && echo yes || echo no").exec().out.firstOrNull() == "yes"
            val isMagisk = Shell.cmd("test -f /data/adb/magisk && echo yes || echo no").exec().out.firstOrNull() == "yes"

            val installCmd = when {
                isKsu -> "/data/adb/ksud module install $tmpFile"
                isApatch -> "/data/adb/apd module install $tmpFile"
                isMagisk -> "/data/adb/magisk --install-module $tmpFile"
                else -> null
            }
            if (installCmd == null) {
                flashError = "无法识别 Root 管理器"
                flashing = false
                return@LaunchedEffect
            }

            val rootManager = when { isKsu -> "KernelSU"; isApatch -> "APatch"; else -> "Magisk" }
            log("→ 通过 $rootManager 安装模块...")
            val result = Shell.cmd(installCmd).exec()
            result.out.forEach { log("  $it") }
            result.err.forEach { log("  ! $it") }

            if (!result.isSuccess) {
                flashError = "安装失败"
                flashing = false
                return@LaunchedEffect
            }

            tmpFile.delete()
            log("→ 确保配置文件...")
            Shell.cmd("mkdir -p /data/adb/modules/RKF/config").exec()
            Shell.cmd("touch /data/adb/modules/RKF/config/avb_disable").exec()
            Shell.cmd("touch /data/adb/modules/RKF/config/avb_hide").exec()

            log("→ 刷新管理器...")
            Shell.cmd("killall magiskd 2>/dev/null; killall ksud 2>/dev/null; killall apd 2>/dev/null").exec()
            log("  ✓ 模块安装完成")

            flashDone = true
        } catch (e: Exception) {
            logLines.add("✗ 错误: ${e.message}")
            flashError = "安装失败: ${e.message}"
        }
        flashing = false
    }

    Icon(
        Icons.Filled.Download, contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = if (isDark) Color(0xFF81C995) else MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "安装后端模块",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    Spacer(Modifier.height(16.dp))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().softShadow(cornerRadius = 20.dp, alpha = 0.06f, offsetY = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "需要刷入后端模块以启用以下功能：",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "• 守护进程：刷写时保持应用后台运行，通知栏显示刷写状态\n" +
                "• AVB2.0 工具：自动禁用 AVB 校检，防止刷写后变砖\n" +
                "• 隐藏 AVB 状态：隐藏已关闭校检的痕迹",
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
                lineHeight = 22.sp
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    if (flashDone) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text("模块安装完成", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) { Text("下一步", style = MaterialTheme.typography.titleMedium) }
    } else if (!flashing) {
        Button(
            onClick = { triggerFlash++ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) { Text("刷写", style = MaterialTheme.typography.titleMedium) }
    }

    if (flashing || logLines.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                if (flashing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                    Text("正在安装...", color = subColor, fontSize = 12.sp)
                }
                if (logLines.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    for (line in logLines) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 16.sp),
                            color = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (flashError != null) {
        Text(flashError!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WizardStep4(onStart: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant

    Icon(
        Icons.Filled.CheckCircle, contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = if (isDark) Color(0xFF81C995) else MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "一切就绪",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "后端模块已安装完成。\n\n现在可以开始使用 ReKernelFlasher 了。",
        style = MaterialTheme.typography.bodyLarge,
        color = subColor,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) { Text("开始使用应用", style = MaterialTheme.typography.titleMedium) }
}
