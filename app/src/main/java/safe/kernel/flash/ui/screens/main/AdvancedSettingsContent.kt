package safe.kernel.flash.ui.screens.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import safe.kernel.flash.R

@Composable
fun ColumnScope.AdvancedSettingsContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    Text(text = stringResource(R.string.dpi_scale), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    var localScale by remember(viewModel.dpiScale) { mutableFloatStateOf(viewModel.dpiScale) }
    Slider(value = localScale, onValueChange = { localScale = it },
        valueRange = 0.5f..1.5f, modifier = Modifier.fillMaxWidth())
    Text(text = "${(localScale * 100).toInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        onClick = { viewModel.applyDpiScale(localScale) }
    ) { Text(stringResource(R.string.apply)) }
}
