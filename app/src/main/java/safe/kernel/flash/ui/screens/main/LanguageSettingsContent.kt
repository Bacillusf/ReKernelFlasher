package safe.kernel.flash.ui.screens.main

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.LanguageManager

@Composable
fun ColumnScope.LanguageSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as Activity
    var expanded by remember { mutableStateOf(false) }
    val currentLang = LanguageManager.getCurrentLanguage()
    val display = when (currentLang) {
        "en" -> "English"
        "zh-TW" -> "繁體中文（台灣）"
        "zh-HK" -> "繁體中文（香港）"
        else -> "简体中文"
    }
    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
    androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(display) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("简体中文") }, onClick = { expanded = false; LanguageManager.setLanguage("zh-CN", activity) })
            DropdownMenuItem(text = { Text("繁體中文（香港）") }, onClick = { expanded = false; LanguageManager.setLanguage("zh-HK", activity) })
            DropdownMenuItem(text = { Text("繁體中文（台灣）") }, onClick = { expanded = false; LanguageManager.setLanguage("zh-TW", activity) })
            DropdownMenuItem(text = { Text("English") }, onClick = { expanded = false; LanguageManager.setLanguage("en", activity) })
        }
    }
}
