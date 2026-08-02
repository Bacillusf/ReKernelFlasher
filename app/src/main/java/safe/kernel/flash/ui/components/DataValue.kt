package safe.kernel.flash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun RowScope.DataValue(
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    clickable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val modifier = Modifier
        .weight(1f)
        .alignByBaseline()
        .then(if (clickable) Modifier.clickable { expanded = !expanded } else Modifier)

    SelectionContainer(modifier) {
        Text(
            text = value,
            color = color,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
    }
}
