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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import safe.kernel.flash.ui.components.UpdateNotesDialog
import safe.kernel.flash.ui.theme.softShadow
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.SettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var showUpdateNotes by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListItem(
            title = stringResource(R.string.check_for_updates),
            subtitle = stringResource(R.string.settings_check_update_subtitle),
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
                    stringResource(R.string.update_banner_title, viewModel.settingsUpdateVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Button(
                        onClick = { showUpdateNotes = true },
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
                    stringResource(R.string.settings_up_to_date, viewModel.settingsUpdateVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Button(
                        onClick = { showUpdateNotes = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.reinstall))
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
                    stringResource(R.string.downloading_percent, (viewModel.settingsUpdateProgress * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 12.dp)
                )
            }
        }

        Spacer(Modifier.padding(0.dp))

        ListItem(
            title = stringResource(R.string.auto_backup_settings),
            subtitle = stringResource(R.string.settings_auto_backup_subtitle),
            leadingIcon = Icons.Filled.Backup,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { navController.navigate("settings/autobackup") }
        )
        ListItem(
            title = stringResource(R.string.language_settings),
            subtitle = stringResource(R.string.settings_language_subtitle),
            leadingIcon = Icons.Filled.Language,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            onClick = { navController.navigate("settings/language") }
        )
        ListItem(
            title = stringResource(R.string.log_settings),
            subtitle = stringResource(R.string.settings_log_subtitle),
            leadingIcon = Icons.Filled.Description,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = { navController.navigate("settings/logs") }
        )
        ListItem(
            title = stringResource(R.string.advanced_settings),
            subtitle = stringResource(R.string.settings_advanced_subtitle),
            leadingIcon = Icons.Filled.Code,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { navController.navigate("settings/advanced") }
        )
        ListItem(
            title = stringResource(R.string.toolbox_title),
            subtitle = stringResource(R.string.toolbox_subtitle),
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
            title = stringResource(R.string.updates),
            message = viewModel.settingsUpdateError ?: "",
            confirmText = stringResource(R.string.confirm),
            cancelText = stringResource(R.string.cancel),
            onConfirm = { viewModel.dismissSettingsUpdateError() },
            onDismiss = { viewModel.dismissSettingsUpdateError() }
        )
    }

    UpdateNotesDialog(
        visible = showUpdateNotes,
        version = viewModel.settingsUpdateVersion,
        notes = viewModel.settingsUpdateBody,
        onConfirm = {
            showUpdateNotes = false
            viewModel.startSettingsUpdateDownload(context)
        },
        onDismiss = { showUpdateNotes = false }
    )
}
