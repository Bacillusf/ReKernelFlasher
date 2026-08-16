package safe.kernel.flash.ui.screens.toolbox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import safe.kernel.flash.AdbTcpService
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.ToolboxContent(
    navController: NavController
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(4.dp))

        ListItem(
            title = "解包",
            subtitle = "Payload-Dumper 解包和解包记录",
            leadingIcon = Icons.Filled.FolderOpen,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { navController.navigate("toolbox/unpack") }
        )

        Spacer(Modifier.height(4.dp))

        ListItem(
            title = "字库备份",
            subtitle = "备份全部分区，记录会显示在备份页面",
            leadingIcon = Icons.Filled.Storage,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { navController.navigate("toolbox/full_backup") }
        )

        Spacer(Modifier.height(4.dp))

        ListItem(
            title = "修复 RKP",
            subtitle = "修复骁龙设备解锁 BL 导致的 TEE/RKP 问题（实验性）",
            leadingIcon = Icons.Filled.Build,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = { navController.navigate("toolbox/rkp_fix") }
        )

        ListItem(
            title = "开启高通Diag端口",
            subtitle = "开启骁龙设备 Diag 调试端口（QPST/QXDM）",
            leadingIcon = Icons.Filled.Usb,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = { navController.navigate("toolbox/diag_port") }
        )

        WirelessDebugCard()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WirelessDebugCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var portText by remember { mutableStateOf("") }
    var pendingPort by remember { mutableStateOf<Int?>(null) }

    val started = AdbTcpService.isRunning.value
    val startedPort = AdbTcpService.currentPort.value

    val portNumber = portText.toIntOrNull()
    val isValid = portNumber != null && portNumber in 1024..65535
    val showInvalid = portText.isNotEmpty() && !isValid

    LaunchedEffect(started) {
        if (!started) {
            portText = ""
        }
    }

    fun startAdb(port: Int) {
        scope.launch(Dispatchers.IO) {
            val result = Shell.cmd("setprop service.adb.tcp.port $port && stop adbd && start adbd").exec()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    AdbTcpService.start(context, port)
                } else {
                    Toast.makeText(context, "开启失败：${result.err.joinToString("\n")}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val port = pendingPort
        pendingPort = null
        if (port != null) {
            if (granted) {
                startAdb(port)
            } else {
                Toast.makeText(context, "请开启通知权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onSend() {
        val port = portNumber ?: return
        if (port !in 1024..65535) return
        if (hasNotificationPermission(context)) {
            startAdb(port)
        } else {
            pendingPort = port
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "开启无线调试端口",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "在指定端口开启 adbd 服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { newValue -> portText = newValue.filter { it.isDigit() }.take(5) },
                    modifier = Modifier.weight(1f),
                    enabled = !started,
                    singleLine = true,
                    label = { Text("端口") },
                    placeholder = { Text("1024-65535") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onSend() },
                    enabled = isValid && !started
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (isValid && !started) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }

            if (showInvalid && !started) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "端口不合法，请输入1024-65535之间的数字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (started) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "adbd服务已在${startedPort}上开启",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { AdbTcpService.stop(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("停止")
                }
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun ColumnScope.UnpackHubContent(
    navController: NavController
) {
    val context = LocalContext.current
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        result.value = it
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(4.dp))

        ListItem(
            title = "解包",
            subtitle = "选择 payload.bin 并提取 img 分区文件",
            leadingIcon = Icons.Filled.FolderOpen,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { launcher.launch("*/*") }
        )

        ListItem(
            title = "解包记录",
            subtitle = "查看已解包的 img 文件",
            leadingIcon = Icons.Filled.Inventory2,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = { navController.navigate("toolbox/unpack_records") }
        )
    }

    result.value?.let { uri ->
        val fileName = uri.lastPathSegment ?: ""
        if (fileName.endsWith(".bin", ignoreCase = true)) {
            navController.navigate("toolbox/payload?uri=${Uri.encode(uri.toString())}")
        } else {
            Toast.makeText(context, "请选择 .bin 文件", Toast.LENGTH_SHORT).show()
        }
        result.value = null
    }
}
