package safe.kernel.flash.ui.screens.toolbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import safe.kernel.flash.toolbox.rkp.RkpCommands
import safe.kernel.flash.toolbox.rkp.RkpLauncher
import safe.kernel.flash.toolbox.rkp.RkpLevel
import safe.kernel.flash.toolbox.rkp.RkpLogger
import safe.kernel.flash.toolbox.rkp.RkpPrefs
import safe.kernel.flash.toolbox.rkp.RkpShell
import java.io.File

private data class RkpLogEntry(val level: Int, val line: String)

/**
 * RKPConfig —— 从「True RKP」独立应用移植进百宝箱的页面，功能保持一致：
 * 读取/应用配置、续期签发、查看状态、生成 CSR、校验证书、工程模式、清空数据、
 * 清空/复制日志、设置 su 路径、日志落盘。
 *
 * 行为规格与原工具在真机上的实际表现逐一核对，详见 rkp 包内各类的注释。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.RkpConfigContent(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { RkpPrefs(context) }

    // ---- 配置状态 ----
    var enable by remember { mutableStateOf(prefs.enable()) }
    var host by remember { mutableStateOf(prefs.host()) }
    var strongbox by remember { mutableStateOf(prefs.strongbox()) }
    var tee by remember { mutableStateOf(prefs.tee()) }
    var timeout by remember { mutableStateOf(prefs.timeout().coerceIn(0, 60)) }

    // ---- 设备状态 ----
    var rootOk by remember { mutableStateOf<Boolean?>(null) }
    var hasStrongbox by remember { mutableStateOf(false) }
    var helperDesc by remember { mutableStateOf("") }

    // ---- 运行状态 ----
    val logLines = remember { mutableStateListOf<RkpLogEntry>() }
    var busy by remember { mutableStateOf(false) }
    var showSuDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var suPath by remember { mutableStateOf(prefs.su()) }

    val logScroll = rememberScrollState()
    val logFile = remember { File(context.filesDir, "rkp.log") }

    val logger = remember {
        object : RkpLogger {
            override fun log(level: Int, line: String) {
                scope.launch(Dispatchers.Main) {
                    logLines.add(RkpLogEntry(level, line))
                    if (logLines.size > 2000) logLines.removeRange(0, 500)
                }
                runCatching { logFile.appendText(line + "\n") }
            }
        }
    }

    // ---- 启动自检 ----
    LaunchedEffect(Unit) {
        helperDesc = RkpLauncher.helperDescription(context)
        hasStrongbox = runCatching {
            context.packageManager.hasSystemFeature("android.hardware.strongbox_keystore")
        }.getOrDefault(false)

        withContext(Dispatchers.IO) {
            val out = RkpShell.root(suPath, listOf("id"), 8000)
            val ok = out.stdoutText().contains("uid=0")
            withContext(Dispatchers.Main) { rootOk = ok }
            logger.log(
                if (ok) RkpLevel.OK else RkpLevel.WARN,
                if (ok) "root 可用：${out.stdoutText().trim()}" else "未获得 root：${out.stderrText().trim()}"
            )
        }
        logger.log("RKPConfig 已就绪")
        logger.log(RkpLevel.DIM, "命名空间助手：$helperDesc")
        logger.log(RkpLevel.DIM, "日志文件：${logFile.absolutePath}")
    }

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) logScroll.animateScrollTo(logScroll.maxValue)
    }

    // ---- 通用执行封装 ----
    fun runOp(
        name: String,
        cmds: List<String>,
        timeoutMs: Long,
        onResult: (RkpShell.Out) -> Unit = {},
    ) {
        if (busy) return
        busy = true
        scope.launch(Dispatchers.IO) {
            try {
                val out = RkpShell.root(
                    suPath, cmds, timeoutMs,
                    { line -> logger.log(RkpLevel.DIM, line) },
                    { line -> logger.log(RkpLevel.ERR, line) },
                )
                withContext(Dispatchers.Main) { onResult(out) }
                when {
                    out.suDenied -> logger.log(RkpLevel.ERR, "$name · 无法获取 root（可在下方设置 su 路径）")
                    out.timeout -> logger.log(RkpLevel.WARN, "$name · 超时")
                    else -> logger.log(RkpLevel.OK, "$name · 完成")
                }
            } catch (e: Exception) {
                logger.log(RkpLevel.ERR, "$name · 失败：${e.message}")
            } finally {
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    fun doLoad() {
        runOp("读取配置", RkpCommands.loadAll(), 15_000) { out ->
            val p = RkpCommands.parse(out)
            enable = p.enable
            host = p.host
            strongbox = p.strongbox
            tee = p.tee
            timeout = p.timeoutSec.coerceIn(0, 60)
            prefs.save(p.enable, p.host, p.strongbox, p.tee, p.timeoutSec)
            logger.log(RkpLevel.DIM, "getprop 结果 $p")
        }
    }

    fun doApply() {
        prefs.save(enable, host, strongbox, tee, timeout)
        runOp(
            "应用配置",
            RkpCommands.applyAll(enable, host, strongbox, tee, timeout),
            20_000,
        ) { out ->
            if (!out.suDenied && out.stderr.isEmpty()) {
                logger.log("配置已写入，部分参数可能需重启后生效")
            }
        }
    }

    fun doRenew() {
        val apk = RkpLauncher.materialize(context)
        if (apk == null) {
            logger.log(RkpLevel.ERR, "未找到 rkpdapp.apk：assets 里没有，设备上也没装 com.android.rkpdapp")
            return
        }
        logger.log(RkpLevel.DIM, "rkpdapp 来源：${apk.absolutePath}")
        val cmd = RkpLauncher.fullCommand(context, null)
        logger.log(RkpLevel.DIM, "命令：$cmd")
        runOp("续期签发", listOf(cmd), 180_000)
    }

    fun doDump() {
        val cmd = RkpLauncher.fullCommand(context, "dump")
        runOp("查看状态", listOf(RkpCommands.DUMP, cmd), 120_000)
    }

    fun doClear() {
        runOp("清空数据", RkpCommands.clearPackages(), 60_000)
    }

    fun copyLog() {
        val text = logLines.joinToString("\n") { it.line }
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as? android.content.ClipboardManager
        cm?.setPrimaryClip(android.content.ClipData.newPlainText("RKPConfig", text))
        logger.log(RkpLevel.OK, "日志已复制到剪贴板")
    }

    // ================================================================ UI

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(4.dp))

        // ---------- 状态 ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "RKPConfig",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "配置 Android 远程密钥分配（RKP），需要 root",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                StatusLine(
                    "ROOT",
                    when (rootOk) {
                        true -> "已就绪"
                        false -> "不可用"
                        null -> "检测中…"
                    },
                    when (rootOk) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                StatusLine(
                    "StrongBox",
                    if (hasStrongbox) "支持" else "不支持",
                    if (hasStrongbox) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusLine("命名空间助手", helperDesc.ifEmpty { "—" }, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- 配置 ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "远程密钥分配（RKP）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SwitchRow(
                    title = "启用 RKP",
                    subtitle = "remote_provisioning.enable_rkpd",
                    checked = enable,
                    onChange = { enable = it }
                )

                Text("服务器地址", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("remoteprovisioning.googleapis.com") },
                    supportingText = { Text("remote_provisioning.hostname") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = host == "remoteprovisioning.googleapis.com",
                        onClick = { host = "remoteprovisioning.googleapis.com" },
                        label = { Text("Google") }
                    )
                    FilterChip(
                        selected = host == "remoteprovisioning.grapheneos.org",
                        onClick = { host = "remoteprovisioning.grapheneos.org" },
                        label = { Text("GrapheneOS") }
                    )
                }

                if (hasStrongbox) {
                    SwitchRow(
                        title = "仅使用 StrongBox",
                        subtitle = "strongbox.rkp_only",
                        checked = strongbox,
                        onChange = { strongbox = it }
                    )
                }

                SwitchRow(
                    title = "仅使用 TEE",
                    subtitle = "tee.rkp_only",
                    checked = tee,
                    onChange = { tee = it }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("连接超时", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "connect_timeout_millis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "$timeout 秒",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = timeout.toFloat(),
                    onValueChange = { timeout = it.toInt().coerceIn(0, 60) },
                    valueRange = 0f..60f,
                    steps = 59
                )
            }
        }

        // ---------- 操作 ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RkpActionButton(
                        "读取配置",
                        Modifier.weight(1f),
                        filled = true,
                        enabled = !busy
                    ) { doLoad() }
                    RkpActionButton(
                        "应用配置",
                        Modifier.weight(1f),
                        filled = true,
                        enabled = !busy,
                        onLongClick = { showSuDialog = true }
                    ) { doApply() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RkpActionButton(
                        "续期签发",
                        Modifier.weight(1f),
                        filled = true,
                        enabled = !busy
                    ) { doRenew() }
                    RkpActionButton(
                        "查看状态",
                        Modifier.weight(1f),
                        enabled = !busy
                    ) { doDump() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RkpActionButton(
                        "生成 CSR",
                        Modifier.weight(1f),
                        enabled = !busy
                    ) { runOp("生成 CSR", listOf(RkpCommands.CSR), 30_000) }
                    RkpActionButton(
                        "校验证书",
                        Modifier.weight(1f),
                        enabled = !busy
                    ) { runOp("校验证书", listOf(RkpCommands.CERTIFY), 30_000) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RkpActionButton(
                        "工程模式",
                        Modifier.weight(1f),
                        enabled = !busy
                    ) { runOp("工程模式", listOf(RkpCommands.ENGINEER), 15_000) }
                    RkpActionButton(
                        "清空数据",
                        Modifier.weight(1f),
                        filled = true,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        enabled = !busy
                    ) { showClearConfirm = true }
                }

                Text(
                    "长按「应用配置」可设置 su 路径",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- 日志 ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "运行日志",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { copyLog() }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "复制日志",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        logLines.clear()
                        logger.log("清空日志")
                    }) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "清空日志",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                        .verticalScroll(logScroll)
                ) {
                    Column {
                        if (logLines.isEmpty()) {
                            Text(
                                "（暂无日志）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            logLines.takeLast(800).forEach { entry ->
                                Text(
                                    text = entry.line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = levelColor(entry.level)
                                )
                            }
                        }
                    }
                }

                Text(
                    "本工具会真实触发密钥签发，并修改系统属性",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ---------- 对话框 ----------
    if (showSuDialog) {
        var input by remember { mutableStateOf(suPath) }
        AlertDialog(
            onDismissRequest = { showSuDialog = false },
            title = { Text("SU PATH") },
            text = {
                Column {
                    Text("su 可执行文件的完整路径")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        placeholder = { Text("/system/bin/su") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = input.trim()
                    if (v.isNotEmpty()) {
                        suPath = v
                        prefs.saveSu(v)
                        logger.log(RkpLevel.OK, "su 路径已设为：$v")
                    }
                    showSuDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showSuDialog = false }) { Text("取消") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空数据") },
            text = { Text("将清空 rkpdapp 与 KeyAttestation 的应用数据，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    doClear()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ================================================================ 小组件

@Composable
private fun StatusLine(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RkpActionButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg = if (filled) containerColor else MaterialTheme.colorScheme.surface
    val fg = when {
        !filled -> MaterialTheme.colorScheme.onSurface
        else -> contentColor
    }.copy(alpha = if (enabled) 1f else 0.4f)

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = if (enabled) 1f else 0.5f), RoundedCornerShape(14.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick,
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun levelColor(level: Int): Color = when (level) {
    RkpLevel.OK -> MaterialTheme.colorScheme.primary
    RkpLevel.ERR -> MaterialTheme.colorScheme.error
    RkpLevel.WARN -> MaterialTheme.colorScheme.tertiary
    RkpLevel.DIM -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}
