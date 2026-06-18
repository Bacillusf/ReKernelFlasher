package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun SettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("settings/autobackup") }
    ) { Text(stringResource(R.string.auto_backup_settings)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("settings/language") }
    ) { Text(stringResource(R.string.language_settings)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("settings/logs") }
    ) { Text(stringResource(R.string.log_settings)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { navController.navigate("settings/advanced") }
    ) { Text(stringResource(R.string.advanced_settings)) }
}
