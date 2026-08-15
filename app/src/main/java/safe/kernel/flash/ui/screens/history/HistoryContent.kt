package safe.kernel.flash.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.common.types.history.HistoryEntry
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.theme.softShadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.HistoryContent(
    viewModel: HistoryViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var pendingRollback by remember { mutableStateOf<Long?>(null) }

    DataCard(title = stringResource(R.string.operation_history))

    if (viewModel.isEmpty) {
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.no_history),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Spacer(Modifier.height(8.dp))
        for (entry in viewModel.entries) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cornerRadius = 14.dp, alpha = 0.05f, offsetY = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp, 10.dp, 14.dp, 12.dp)) {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        overflow = TextOverflow.Visible
                    )
                    if (entry.kind == HistoryEntry.KIND_AK3 && entry.rollbackTimestamp > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { pendingRollback = entry.rollbackTimestamp },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(text = "回滚", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (!viewModel.isEmpty) {
        Spacer(Modifier.height(20.dp))
        ListItem(
            title = stringResource(R.string.clear_history),
            subtitle = "清空所有操作历史",
            leadingIcon = Icons.Filled.DeleteSweep,
            leadingColors = ListItemIconColors(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = { viewModel.clearAll() }
        )
    }

    AnimatedConfirmDialog(
        visible = pendingRollback != null,
        title = "回滚",
        message = "确定要恢复该操作对应的备份分区吗？将把备份镜像刷回原分区。",
        confirmText = "回滚",
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            val timestamp = pendingRollback
            pendingRollback = null
            timestamp?.let { viewModel.rollback(context, it) {} }
        },
        onDismiss = { pendingRollback = null }
    )
}
