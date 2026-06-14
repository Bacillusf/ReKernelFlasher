package safe.kernel.flash.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import safe.kernel.flash.R
import safe.kernel.flash.ui.components.DataCard

@ExperimentalMaterial3Api
@Composable
fun ColumnScope.HistoryContent(
    viewModel: HistoryViewModel,
    navController: NavController
) {
    DataCard(title = stringResource(R.string.operation_history))

    if (viewModel.isEmpty) {
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.no_history),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )
    } else {
        for (entry in viewModel.entries) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
                    )
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }

    if (!viewModel.isEmpty) {
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            onClick = { viewModel.clearAll() }
        ) {
            Text(stringResource(R.string.clear_history))
        }
    }
}
