package safe.kernel.flash.ui.screens.reboot

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.AnimatedConfirmDialog

@Composable
fun ColumnScope.RebootContent(
    viewModel: RebootViewModel,
    @Suppress("UNUSED_PARAMETER") ignoredNavController: NavController
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.showConfirm("") }
    ) {
        Text(stringResource(R.string.reboot))
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.showConfirm("recovery") }
    ) {
        Text(stringResource(R.string.reboot_recovery))
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.showConfirm("bootloader") }
    ) {
        Text(stringResource(R.string.reboot_bootloader))
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.showConfirm("download") }
    ) {
        Text(stringResource(R.string.reboot_download))
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.showConfirm("edl") }
    ) {
        Text(stringResource(R.string.reboot_edl))
    }

    AnimatedConfirmDialog(
        visible = viewModel.showConfirmDialog,
        title = "CAUTION!",
        message = viewModel.confirmMessage,
        confirmText = "重启",
        cancelText = stringResource(R.string.cancel),
        onConfirm = { viewModel.executeReboot() },
        onDismiss = { viewModel.hideConfirm() }
    )
}
