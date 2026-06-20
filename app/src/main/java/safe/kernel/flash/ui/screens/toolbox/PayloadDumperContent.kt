package safe.kernel.flash.ui.screens.toolbox

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.PayloadDumperContent(
    navController: NavController,
    uriString: String
) {
    val context = LocalContext.current
    val viewModel: PayloadDumperViewModel = viewModel(viewModelStoreOwner = context as ComponentActivity)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        val uri = Uri.parse(uriString)
        viewModel.loadPayload(context, uri)
    }

    if (viewModel.isLoading) {
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "正在解析 ${viewModel.payloadFilename.ifEmpty { "payload.bin" }}...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }

    val errMsg = viewModel.error
    if (errMsg != null && viewModel.partitions.isEmpty()) {
        AnimatedConfirmDialog(
            visible = true,
            title = "错误",
            message = errMsg,
            confirmText = "返回",
            cancelText = "取消",
            onConfirm = { navController.popBackStack() },
            onDismiss = { navController.popBackStack() }
        )
        return
    }

    OutlinedTextField(
        value = viewModel.searchQuery,
        onValueChange = { viewModel.searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜索分区...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
    )

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = { viewModel.selectAll() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) { Text("全选", fontSize = 13.sp) }
        FilledTonalButton(
            onClick = { viewModel.deselectAll() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) { Text("取消全选", fontSize = 13.sp) }
    }

    Spacer(Modifier.height(4.dp))

    Text(
        "${viewModel.filteredPartitions.size} 个分区",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )

    Spacer(Modifier.height(4.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.weight(1f)
    ) {
        items(viewModel.filteredPartitions, key = { it.name }) { part ->
            PartitionCheckRow(
                label = "${part.name} (${part.size})",
                checked = viewModel.selectedPartitions[part.name] == true,
                onClick = { viewModel.togglePartition(part.name) }
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    var showConfirm by remember { mutableStateOf(false) }

    FilledTonalButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        enabled = viewModel.hasSelection,
        onClick = { showConfirm = true }
    ) {
        Text("解包", style = MaterialTheme.typography.titleMedium)
    }

    if (showConfirm) {
        AnimatedConfirmDialog(
            visible = true,
            title = "确认解包",
            message = "已选择的分区: ${viewModel.selectedNames}",
            confirmText = "解包",
            cancelText = "取消",
            onConfirm = {
                showConfirm = false
                navController.navigate("toolbox/payload/extract") {
                    popUpTo("toolbox")
                }
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun PartitionCheckRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (checked)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            color = if (checked)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
