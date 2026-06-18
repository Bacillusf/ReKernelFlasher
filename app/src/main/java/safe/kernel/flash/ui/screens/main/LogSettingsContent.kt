package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R

@Composable
fun ColumnScope.LogSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
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
}
