package safe.kernel.flash.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.SlotCard
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun FlashHomeContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val slotSfxA = if (viewModel.isAb) "_a" else ""

    SlotCard(
        title = stringResource(if (viewModel.isAb) R.string.slot_a else R.string.slot),
        viewModel = viewModel.slotA,
        navController = navController,
        isSlotScreen = true
    )
    Spacer(Modifier.height(8.dp))
    AnimatedVisibility(!viewModel.slotA.isRefreshing.value) {
        Column {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                onClick = { navController.navigate("slot$slotSfxA/flash") }
            ) { Text(stringResource(R.string.flash)) }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                onClick = { viewModel.slotA.clearFlash(context); navController.navigate("slot$slotSfxA/backup") }
            ) { Text(stringResource(R.string.backup)) }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                onClick = { navController.navigate("slot$slotSfxA/backups") }
            ) { Text(stringResource(R.string.restore)) }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                onClick = { if (!viewModel.slotA.isRefreshing.value) viewModel.slotA.getKernel(context) }
            ) { Text(stringResource(R.string.check_kernel_version)) }
            if (viewModel.slotA.hasVendorDlkm) {
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(!viewModel.slotA.isRefreshing.value) {
                    AnimatedVisibility(viewModel.slotA.isVendorDlkmMounted) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                            onClick = { viewModel.slotA.unmountVendorDlkm(context) }
                        ) { Text(stringResource(R.string.unmount_vendor_dlkm)) }
                    }
                    AnimatedVisibility(!viewModel.slotA.isVendorDlkmMounted && viewModel.slotA.isVendorDlkmMapped) {
                        Column {
                            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                                onClick = { viewModel.slotA.mountVendorDlkm(context) }
                            ) { Text(stringResource(R.string.mount_vendor_dlkm)) }
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                                onClick = { viewModel.slotA.unmapVendorDlkm(context) }
                            ) { Text(stringResource(R.string.unmap_vendor_dlkm)) }
                        }
                    }
                    AnimatedVisibility(!viewModel.slotA.isVendorDlkmMounted && !viewModel.slotA.isVendorDlkmMapped) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                            onClick = { viewModel.slotA.mapVendorDlkm(context) }
                        ) { Text(stringResource(R.string.map_vendor_dlkm)) }
                    }
                }
            }
        }
    }

    if (viewModel.isAb) {
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        SlotCard(
            title = stringResource(R.string.slot_b),
            viewModel = viewModel.slotB!!,
            navController = navController,
            isSlotScreen = true
        )
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(!viewModel.slotB!!.isRefreshing.value) {
            Column {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                    onClick = { navController.navigate("slot_b/flash") }
                ) { Text(stringResource(R.string.flash)) }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                    onClick = { viewModel.slotB!!.clearFlash(context); navController.navigate("slot_b/backup") }
                ) { Text(stringResource(R.string.backup)) }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                    onClick = { navController.navigate("slot_b/backups") }
                ) { Text(stringResource(R.string.restore)) }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                    onClick = { if (!viewModel.slotB!!.isRefreshing.value) viewModel.slotB!!.getKernel(context) }
                ) { Text(stringResource(R.string.check_kernel_version)) }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("history") }
    ) { Text(stringResource(R.string.operation_history)) }
}
