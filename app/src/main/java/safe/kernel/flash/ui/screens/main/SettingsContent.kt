package safe.kernel.flash.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.theme.softShadow
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.SettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListItem(
            title = stringResource(R.string.check_for_updates),
            subtitle = "检查是否有新版本可用",
            leadingIcon = Icons.Filled.Download,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            enabled = !viewModel.settingsUpdateChecking && !viewModel.settingsUpdateDownloading,
            onClick = { viewModel.checkForSettingsUpdate() }
        )

        AnimatedVisibility(
            visible = viewModel.settingsUpdateChecking,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = viewModel.settingsUpdateFound && !viewModel.settingsUpdateDownloading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    "发现版本 v${viewModel.settingsUpdateVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Button(
                        onClick = { viewModel.startSettingsUpdateDownload(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.download))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.settingsUpdateFound = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.settingsUpdateSameVersion && !viewModel.settingsUpdateDownloading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    "已是最新版本 v${viewModel.settingsUpdateVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Button(
                        onClick = { viewModel.startSettingsUpdateDownload(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("重新安装")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.settingsUpdateSameVersion = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.settingsUpdateDownloading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                LinearProgressIndicator(
                    progress = { viewModel.settingsUpdateProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 14.dp, 16.dp, 4.dp)
                )
                Text(
                    "下载中 ${(viewModel.settingsUpdateProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 12.dp)
                )
            }
        }

        Spacer(Modifier.padding(0.dp))

        ListItem(
            title = stringResource(R.string.auto_backup_settings),
            subtitle = "刷写前自动备份目标分区",
            leadingIcon = Icons.Filled.Backup,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { navController.navigate("settings/autobackup") }
        )
        ListItem(
            title = stringResource(R.string.language_settings),
            subtitle = "选择应用显示语言",
            leadingIcon = Icons.Filled.Language,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { navController.navigate("settings/language") }
        )
        ListItem(
            title = stringResource(R.string.log_settings),
            subtitle = "保存内核日志 (ramoops / dmesg / logcat)",
            leadingIcon = Icons.Filled.Description,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = { navController.navigate("settings/logs") }
        )
        ListItem(
            title = stringResource(R.string.advanced_settings),
            subtitle = "界面缩放等高级选项",
            leadingIcon = Icons.Filled.Code,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { navController.navigate("settings/advanced") }
        )
        ListItem(
            title = "百宝箱",
            subtitle = "Payload-Dumper 解包等实用工具",
            leadingIcon = Icons.Filled.Build,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { navController.navigate("toolbox") }
        )
    }

    if (viewModel.settingsUpdateError != null) {
        AnimatedConfirmDialog(
            visible = true,
            title = "更新",
            message = viewModel.settingsUpdateError ?: "",
            confirmText = "确定",
            cancelText = stringResource(R.string.cancel),
            onConfirm = { viewModel.dismissSettingsUpdateError() },
            onDismiss = { viewModel.dismissSettingsUpdateError() }
        )
    }
}
