package safe.kernel.flash.ui.screens.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.AutoBackupManager
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun ColumnScope.AutoBackupSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.auto_backup), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = AutoBackupManager.isEnabled.value,
            onCheckedChange = { AutoBackupManager.setEnabled(it) }
        )
    }
    if (AutoBackupManager.isEnabled.value) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
            onClick = { navController.navigate("autobackup") }
        ) { Text(stringResource(R.string.view_autobackup_records)) }
    }

    Spacer(Modifier.height(16.dp))
    val hasPermission = if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else true
    Text(stringResource(R.string.storage_permission), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        if (hasPermission) "已授权" else "未授权，点击下方按钮前往设置",
        style = MaterialTheme.typography.bodySmall,
        color = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )
    if (!hasPermission) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        ) { Text(stringResource(R.string.grant_storage_permission)) }
    }
}
