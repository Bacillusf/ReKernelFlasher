package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.LogSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (viewModel.hasRamoops) {
            ListItem(
                title = stringResource(R.string.save_ramoops),
                subtitle = "保存内核 panic / oops 日志",
                leadingIcon = Icons.Filled.BugReport,
                leadingColors = ListItemIconColors(
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer
                ),
                onClick = { viewModel.saveRamoops(context) }
            )
        }
        ListItem(
            title = stringResource(R.string.save_dmesg),
            subtitle = "保存内核环形缓冲区日志",
            leadingIcon = Icons.Filled.Save,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { viewModel.saveDmesg(context) }
        )
        ListItem(
            title = stringResource(R.string.save_logcat),
            subtitle = "保存 Android 系统日志",
            leadingIcon = Icons.Filled.Article,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { viewModel.saveLogcat(context) }
        )
    }
}
