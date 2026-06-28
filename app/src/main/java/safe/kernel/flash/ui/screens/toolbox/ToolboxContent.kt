package safe.kernel.flash.ui.screens.toolbox

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.ToolboxContent(
    navController: NavController
) {
    val context = LocalContext.current
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        result.value = it
    }

    // Toggle states read via root
    var avbDisabled by remember { mutableStateOf(false) }
    var avbHidden by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        avbDisabled = Shell.cmd("test -f /data/adb/modules/RKF/config/avb_disable && echo yes || echo no").exec().out.firstOrNull() == "yes"
        avbHidden = Shell.cmd("test -f /data/adb/modules/RKF/config/avb_hide && echo yes || echo no").exec().out.firstOrNull() == "yes"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(4.dp))

        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            onClick = { launcher.launch("*/*") }
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Payload-Dumper 解包", style = MaterialTheme.typography.titleMedium)
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

        Spacer(Modifier.height(4.dp))

        ListItem(
            title = "自动禁用 AVB2.0",
            subtitle = if (avbDisabled) "每次开机自动关闭 AVB 校检" else "已禁用",
            leadingIcon = Icons.Filled.Build,
            leadingColors = ListItemIconColors(
                container = if (avbDisabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                content = if (avbDisabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingContent = {
                Switch(
                    checked = avbDisabled,
                    onCheckedChange = { checked ->
                        val flagFile = "/data/adb/modules/RKF/config/avb_disable"
                        if (checked) {
                            Shell.cmd("mkdir -p /data/adb/modules/RKF/config && touch $flagFile").exec()
                        } else {
                            Shell.cmd("rm -f $flagFile").exec()
                        }
                        avbDisabled = checked
                        Toast.makeText(context, "重启后生效", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        )
        ListItem(
            title = "隐藏 AVB 状态",
            subtitle = if (avbHidden) "隐藏已关闭 AVB 校检的痕迹" else "已禁用",
            leadingIcon = Icons.Filled.VisibilityOff,
            leadingColors = ListItemIconColors(
                container = if (avbHidden) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                content = if (avbHidden) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingContent = {
                Switch(
                    checked = avbHidden,
                    onCheckedChange = { checked ->
                        val flagFile = "/data/adb/modules/RKF/config/avb_hide"
                        if (checked) {
                            Shell.cmd("mkdir -p /data/adb/modules/RKF/config && touch $flagFile").exec()
                        } else {
                            Shell.cmd("rm -f $flagFile").exec()
                        }
                        avbHidden = checked
                        Toast.makeText(context, "重启后生效", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
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
}
