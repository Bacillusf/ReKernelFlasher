package safe.kernel.flash.ui.screens.main

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.common.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
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
    val options = listOf(
        Triple("zh-CN", "简体中文", true),
        Triple("zh-HK", "繁體中文（香港）", false),
        Triple("zh-TW", "繁體中文（台灣）", false),
        Triple("en", "English", false)
    )

    Column {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { expanded = true }
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
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "语言",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    display,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (code, label, _) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (code == when (currentLang) {
                                        "zh-CN" -> "zh-CN"
                                        "zh-HK" -> "zh-HK"
                                        "zh-TW" -> "zh-TW"
                                        else -> "en"
                                    }
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            },
                            onClick = {
                                expanded = false
                                LanguageManager.setLanguage(code, activity)
                            }
                        )
                    }
                }
            }
        }
    }
}
