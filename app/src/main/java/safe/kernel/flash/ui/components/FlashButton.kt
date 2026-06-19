package safe.kernel.flash.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import safe.kernel.flash.MainActivity

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalUnitApi
@Composable
fun FlashButton(
    buttonText: String,
    validExtension: String,
    callback: (uri: Uri) -> Unit
) {
    val mainActivity = LocalContext.current as MainActivity
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        result.value = it
        if (it == null) {
            mainActivity.isAwaitingResult = false
        }
    }
    FilledTonalButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        onClick = {
            mainActivity.isAwaitingResult = true
            launcher.launch("*/*")
        }
    ) {
        Icon(
            Icons.Filled.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = buttonText,
            style = MaterialTheme.typography.titleMedium
        )
    }
    result.value?.let { uri ->
        if (mainActivity.isAwaitingResult) {
            val contentResolver = mainActivity.contentResolver
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }

            if (fileName != null && fileName.endsWith(validExtension, ignoreCase = true)) {
                callback.invoke(uri)
            } else {
                Toast.makeText(mainActivity.applicationContext, "Invalid file selected!", Toast.LENGTH_LONG).show()
            }
        }
        mainActivity.isAwaitingResult = false
        result.value = null
    }
}
