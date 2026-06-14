package safe.kernel.flash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    detail: String? = null,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.96f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, fontWeight = FontWeight.Bold)
                    if (!detail.isNullOrEmpty()) {
                        Text(detail, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                DialogButton(confirmText, onClick = onConfirm)
            },
            dismissButton = {
                DialogButton(cancelText, onClick = onDismiss)
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
