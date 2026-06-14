package safe.kernel.flash.ui.screens.main

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.common.LanguageManager
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun SettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("backups") }
    ) { Text(stringResource(R.string.backups)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("updates") }
    ) { Text(stringResource(R.string.updates)) }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

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
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

    var langExpanded by remember { mutableStateOf(false) }
    val activity = context as Activity
    val currentLang = LanguageManager.getCurrentLanguage()
    val langDisplay = when (currentLang) {
        "en" -> "English"
        "zh-TW" -> "繁體中文（台灣）"
        "zh-HK" -> "繁體中文（香港）"
        else -> "简体中文"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { langExpanded = true }) {
                Text(langDisplay)
            }
            DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                DropdownMenuItem(text = { Text("简体中文") }, onClick = {
                    langExpanded = false; LanguageManager.setLanguage("zh-CN", activity)
                })
                DropdownMenuItem(text = { Text("繁體中文（香港）") }, onClick = {
                    langExpanded = false; LanguageManager.setLanguage("zh-HK", activity)
                })
                DropdownMenuItem(text = { Text("繁體中文（台灣）") }, onClick = {
                    langExpanded = false; LanguageManager.setLanguage("zh-TW", activity)
                })
                DropdownMenuItem(text = { Text("English") }, onClick = {
                    langExpanded = false; LanguageManager.setLanguage("en", activity)
                })
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

    if (viewModel.hasRamoops) {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
            onClick = { viewModel.saveRamoops(context) }
        ) { Text(stringResource(R.string.save_ramoops)) }
        Spacer(Modifier.height(8.dp))
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.saveDmesg(context) }
    ) { Text(stringResource(R.string.save_dmesg)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.saveLogcat(context) }
    ) { Text(stringResource(R.string.save_logcat)) }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

    Text(text = stringResource(R.string.dpi_scale), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    var localScale by remember(viewModel.dpiScale) { mutableFloatStateOf(viewModel.dpiScale) }
    Slider(value = localScale, onValueChange = { localScale = it },
        valueRange = 0.5f..1.5f, modifier = Modifier.fillMaxWidth())
    Text(text = "${(localScale * 100).toInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.applyDpiScale(localScale) }
    ) { Text(stringResource(R.string.apply)) }
}
