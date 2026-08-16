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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

    }
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
