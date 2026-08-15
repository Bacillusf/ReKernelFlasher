package safe.kernel.flash.ui.screens.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.AutoBackupSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else true

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingRow(
            title = stringResource(R.string.auto_backup),
            subtitle = "刷写前自动 dd 备份目标分区",
            icon = Icons.Filled.Storage,
            trailing = {
                Switch(
                    checked = AutoBackupManager.isEnabled.value,
                    onCheckedChange = { AutoBackupManager.setEnabled(it) }
                )
            }
        )

        if (AutoBackupManager.isEnabled.value) {
            ListItem(
                title = stringResource(R.string.view_autobackup_records),
                subtitle = "查看历史自动备份",
                leadingIcon = Icons.Filled.History,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                onClick = { navController.navigate("autobackup") }
            )
        }

        if (!hasAllFilesAccess) {
            ListItem(
                title = "未授权必要权限",
                subtitle = "缺少「所有文件访问」权限，自动备份可能无法正常访问 /sdcard，点击前往系统设置授权",
                leadingIcon = Icons.Filled.WarningAmber,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer
                ),
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    runCatching { context.startActivity(intent) }
                }
            )
        }

        Text(
            text = "开启后，每次刷写分区镜像或 AnyKernel3 之前，会自动用 dd 备份目标分区，防止刷错后无法恢复。\n" +
                "备份保存在 /sdcard/ReKernelFlasher/Autobackup/，可在「查看自动备份记录」中查看，长按记录可打开对应备份镜像。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.padding(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}
