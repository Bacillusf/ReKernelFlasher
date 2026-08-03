package safe.kernel.flash.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WizardScreen(
    navController: NavController,
    onDependenciesReady: (suspend () -> Boolean)? = null,
    onComplete: (() -> Unit)? = null
) {
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

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(140))
            },
            label = "wizardStep"
        ) { currentStep ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentStep) {
                    1 -> WizardStep1(onNext = { step = 2 })
                    2 -> WizardStep2(
                        context = context,
                        onDependenciesReady = onDependenciesReady,
                        onNext = { step = 3 }
                    )
                    3 -> WizardStep3(
                        context,
                        onNext = { step = 4 },
                        onSkip = {
                            context.getSharedPreferences("wizard", 0)
                                .edit().putInt("version", BuildConfig.VERSION_CODE).commit()
                            if (onComplete != null) {
                                onComplete()
                            } else {
                                navController.navigate("main") {
                                    popUpTo("wizard") { inclusive = true }
                                }
                            }
                        }
                    )
                    4 -> WizardStep4(
                        onStart = {
                            context.getSharedPreferences("wizard", 0)
                                .edit().putInt("version", BuildConfig.VERSION_CODE).commit()
                            if (onComplete != null) {
                                onComplete()
                            } else {
                                navController.navigate("main") {
                                    popUpTo("wizard") { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
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
private fun WizardStep2(
    context: android.content.Context,
    onDependenciesReady: (suspend () -> Boolean)?,
    onNext: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isDark) Color(0xFF81C995) else MaterialTheme.colorScheme.primary
    var checking by remember { mutableStateOf(true) }
    var checkDone by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("正在检测依赖") }
    var progress by remember { mutableStateOf(0.08f) }
    var checkTrigger by remember { mutableIntStateOf(0) }
    var rootManager by remember { mutableStateOf<String?>(null) }
    var hasBackendPackage by remember { mutableStateOf(false) }
    var hasWorkDir by remember { mutableStateOf(false) }
    var dependenciesReadyDispatched by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 260),
        label = "dependencyProgress"
    )

    LaunchedEffect(checkTrigger) {
        checking = true
        checkDone = false
        checkError = null
        try {
            statusText = "正在请求 Root 授权"
            progress = 0.16f
            val rootGranted = withContext(Dispatchers.IO) {
                Shell.getShell()
                var granted = false
                for (attempt in 0 until 8) {
                    granted = Shell.isAppGrantedRoot() == true
                    if (granted) break
                    Thread.sleep(250)
                }
                granted
            }
            delay(80)

            statusText = "正在识别 Root 管理器"
            progress = 0.45f
            val detectedRoot = withContext(Dispatchers.IO) {
                val isKsu = Shell.cmd("test -f /data/adb/ksud && echo yes || echo no").exec().out.firstOrNull() == "yes"
                val isApatch = Shell.cmd("test -f /data/adb/apd && echo yes || echo no").exec().out.firstOrNull() == "yes"
                val isMagisk = Shell.cmd("test -f /data/adb/magisk/magisk && echo yes || echo no").exec().out.firstOrNull() == "yes"
                when {
                    isKsu -> "KernelSU"
                    isApatch -> "APatch"
                    isMagisk -> "Magisk"
                    else -> null
                }
            }
            rootManager = detectedRoot
            delay(120)

            statusText = "正在检查后端模块包"
            progress = 0.68f
            hasBackendPackage = withContext(Dispatchers.IO) {
                runCatching { context.assets.open("RKF.zip").use { true } }.getOrDefault(false)
            }
            delay(120)

            statusText = "正在检查本地工作目录"
            progress = 0.78f
            hasWorkDir = withContext(Dispatchers.IO) { context.filesDir.exists() && context.filesDir.canWrite() }
            delay(80)

            if (!rootGranted) {
                checkError = "未获得 Root 授权，请先在 Root 管理器中允许本应用。"
            } else if (detectedRoot == null) {
                checkError = "未识别到 KernelSU、APatch 或 Magisk 管理器。"
            } else if (!hasBackendPackage) {
                checkError = "未找到内置后端模块包 RKF.zip。"
            } else if (!hasWorkDir) {
                checkError = "本地工作目录不可写。"
            } else {
                statusText = "正在完成后端初始化"
                progress = 0.9f
                val backendReady = if (!dependenciesReadyDispatched) {
                    dependenciesReadyDispatched = true
                    onDependenciesReady?.invoke() ?: true
                } else {
                    true
                }
                if (backendReady) {
                    statusText = "依赖检测完成"
                    progress = 1f
                    checkDone = true
                } else {
                    dependenciesReadyDispatched = false
                    checkError = "后端初始化失败，请点击重新检测。"
                    progress = 0.86f
                }
            }
        } catch (e: Exception) {
            dependenciesReadyDispatched = false
            checkError = "依赖检测失败: ${e.message}"
        }
        checking = false
    }

    Icon(
        Icons.Filled.Star, contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = accentColor
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "依赖检测",
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
                "正在检查刷写所需的基础依赖。",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "• Root 授权状态\n" +
                "• Root 管理器类型\n" +
                "• 内置后端模块包\n" +
                "• 本地工作目录权限",
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
                lineHeight = 22.sp
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checkError == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (checkError == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = checkError ?: statusText,
                fontWeight = FontWeight.SemiBold,
                color = if (checkError == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (checkError == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
            )
        }
    }

    if (checkDone) {
        Spacer(Modifier.height(4.dp))
        Text(
            "${rootManager ?: "Root"} 已就绪，后端模块包已找到。",
            style = MaterialTheme.typography.bodySmall,
            color = subColor,
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onNext,
        enabled = checkDone && !checking,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) { Text("下一步", style = MaterialTheme.typography.titleMedium) }

    if (checkError != null) {
        OutlinedButton(
            onClick = {
                checking = true
                checkDone = false
                checkError = null
                statusText = "正在检测依赖"
                progress = 0.08f
                dependenciesReadyDispatched = false
                checkTrigger++
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) { Text("重新检测", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun WizardStep3(
    context: android.content.Context,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onSurfaceVariant
    var flashing by remember { mutableStateOf(false) }
    var flashDone by remember { mutableStateOf(false) }
    var flashError by remember { mutableStateOf<String?>(null) }
    var triggerFlash by remember { mutableIntStateOf(0) }
    val logLines = remember { mutableStateListOf<String>() }
    var selectedMeta by remember { mutableStateOf<String?>(null) }
    var detectedRoot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isKsu = Shell.cmd("test -f /data/adb/ksud && echo yes || echo no").exec().out.firstOrNull() == "yes"
        val isApatch = Shell.cmd("test -f /data/adb/apd && echo yes || echo no").exec().out.firstOrNull() == "yes"
        val isMagisk = Shell.cmd("test -f /data/adb/magisk/magisk && echo yes || echo no").exec().out.firstOrNull() == "yes"
        detectedRoot = when {
            isKsu -> "KernelSU"
            isApatch -> "APatch"
            isMagisk -> "Magisk"
            else -> null
        }
    }
    val showMetaOptions = detectedRoot != null && detectedRoot != "Magisk"

    LaunchedEffect(triggerFlash) {
        if (triggerFlash == 0) return@LaunchedEffect
        flashing = true
        flashError = null
        logLines.clear()
        try {
            fun log(msg: String) { logLines.add(msg) }

            log("→ 检测 Root 管理器...")
            val isKsu = Shell.cmd("test -f /data/adb/ksud && echo yes || echo no").exec().out.firstOrNull() == "yes"
            val isApatch = Shell.cmd("test -f /data/adb/apd && echo yes || echo no").exec().out.firstOrNull() == "yes"
            val isMagisk = Shell.cmd("test -f /data/adb/magisk/magisk && echo yes || echo no").exec().out.firstOrNull() == "yes"

            fun installCmd(file: File): String? = when {
                isKsu -> "/data/adb/ksud module install $file"
                isApatch -> "/data/adb/apd module install $file"
                isMagisk -> "/data/adb/magisk/magisk --install-module $file"
                else -> null
            }

            val rootManager = when { isKsu -> "KernelSU"; isApatch -> "APatch"; isMagisk -> "Magisk"; else -> null }
            if (rootManager == null) {
                flashError = "无法识别 Root 管理器"
                flashing = false
                return@LaunchedEffect
            }

            // 1. 后端模块优先刷写
            log("→ 正在复制后端模块文件...")
            val tmpFile = File(context.filesDir, "RKF.zip")
            context.assets.open("RKF.zip").use { input ->
                tmpFile.outputStream().use { output -> input.copyTo(output) }
            }
            log("→ 通过 $rootManager 安装后端模块...")
            val cmd = installCmd(tmpFile)!!
            val result = Shell.cmd(cmd).exec()
            result.out.forEach { log("  $it") }
            result.err.forEach { log("  ! $it") }

            if (!result.isSuccess) {
                tmpFile.delete()
                flashError = "后端模块安装失败"
                flashing = false
                return@LaunchedEffect
            }
            tmpFile.delete()
            log("→ 确保配置文件...")
            Shell.cmd("mkdir -p /data/adb/modules/RKF/config").exec()
            Shell.cmd("touch /data/adb/modules/RKF/config/avb_disable").exec()
            Shell.cmd("touch /data/adb/modules/RKF/config/avb_hide").exec()
            log("  ✓ 后端模块安装完成")

            // 2. 根据选择刷写 META 模块
            val metaAsset = when (selectedMeta) {
                "magic_mount" -> "model/magic_mount_rs.zip" to "MagicMountRS"
                "overlayfs" -> "model/overlayfs.zip" to "Meta-OverlayFS"
                else -> null
            }
            if (metaAsset != null) {
                val (asset, name) = metaAsset
                log("→ 正在复制 META 模块 ($name)...")
                val metaFile = File(context.filesDir, "meta.zip")
                context.assets.open(asset).use { input ->
                    metaFile.outputStream().use { output -> input.copyTo(output) }
                }
                log("→ 通过 $rootManager 安装 META 模块 ($name)...")
                val metaResult = Shell.cmd(installCmd(metaFile)!!).exec()
                metaResult.out.forEach { log("  $it") }
                metaResult.err.forEach { log("  ! $it") }
                metaFile.delete()
                if (!metaResult.isSuccess) {
                    log("  ! META 模块安装失败，忽略继续")
                } else {
                    log("  ✓ META 模块 ($name) 安装完成")
                }
            } else {
                log("→ 未选择 META 模块，跳过")
            }

            log("→ 刷新管理器...")
            Shell.cmd("killall magiskd 2>/dev/null; killall ksud 2>/dev/null; killall apd 2>/dev/null").exec()
            log("  ✓ 全部完成")

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

    Spacer(Modifier.height(12.dp))

    if (showMetaOptions) {
        Text(
            "可选安装 META",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "按需选择附加模块，可不选；选中一项后另一项禁用，再次点击取消",
            style = MaterialTheme.typography.bodySmall,
            color = subColor
        )
        Spacer(Modifier.height(8.dp))

        MetaOptionCard(
            iconVector = Icons.Filled.Storage,
            name = "MagicMountRS",
            subtitle = "Magic Mount 重定向方案",
            selected = selectedMeta == "magic_mount",
            disabled = selectedMeta != null && selectedMeta != "magic_mount",
            enabled = !flashing && !flashDone,
            onClick = {
                selectedMeta = if (selectedMeta == "magic_mount") null else "magic_mount"
            }
        )
        Spacer(Modifier.height(8.dp))
        MetaOptionCard(
            iconVector = Icons.Filled.Layers,
            name = "Meta-OverlayFS",
            subtitle = "OverlayFS 挂载方案",
            selected = selectedMeta == "overlayfs",
            disabled = selectedMeta != null && selectedMeta != "overlayfs",
            enabled = !flashing && !flashDone,
            onClick = {
                selectedMeta = if (selectedMeta == "overlayfs") null else "overlayfs"
            }
        )

        Spacer(Modifier.height(12.dp))
    }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { triggerFlash++ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) { Text("刷写", style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) { Text("不刷写", style = MaterialTheme.typography.titleMedium) }
        }
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
private fun MetaOptionCard(
    iconVector: ImageVector,
    name: String,
    subtitle: String,
    selected: Boolean,
    disabled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val alpha = if (disabled) 0.4f else 1f
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .background(containerColor.copy(alpha = alpha), RoundedCornerShape(18.dp))
            .let { if (!disabled && enabled) it.clickable { onClick() } else it }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
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
