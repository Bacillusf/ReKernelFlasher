package safe.kernel.flash.ui.screens.main

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
@Composable
fun MainContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    DataCard(title = stringResource(R.string.device)) {
        val cardWidth = remember { mutableIntStateOf(0) }
        DataRow(stringResource(R.string.model), "${Build.MODEL} (${Build.DEVICE})", mutableMaxWidth = cardWidth)
        DataRow(stringResource(R.string.build_number), Build.ID, mutableMaxWidth = cardWidth)
        DataRow(stringResource(R.string.android_version), viewModel.androidVersion, mutableMaxWidth = cardWidth)
        DataRow(stringResource(R.string.app_version), viewModel.appVersion, mutableMaxWidth = cardWidth)
        DataRow(stringResource(R.string.kernel_version), viewModel.kernelVersion, mutableMaxWidth = cardWidth, clickable = true)
        DataRow(stringResource(R.string.root_manager), viewModel.rootManager, mutableMaxWidth = cardWidth)
        if (viewModel.isAb)
            DataRow(stringResource(R.string.slot_suffix), viewModel.slotSuffix, mutableMaxWidth = cardWidth)
        if (viewModel.halInfo != "")
            DataRow("Boot HAL version", viewModel.halInfo, mutableMaxWidth = cardWidth)
        if (viewModel.susfsVersion != "v0.0.0" && viewModel.susfsVersion != "Invalid")
            DataRow(stringResource(R.string.susfs_version), viewModel.susfsVersion, mutableMaxWidth = cardWidth)
    }
}
