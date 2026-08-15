package safe.kernel.flash.ui.screens.history

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.common.HistoryManager

class HistoryViewModel : ViewModel() {

    val entries get() = HistoryManager.entries

    val isEmpty get() = entries.isEmpty()

    fun clearAll() {
        HistoryManager.clearAll()
    }

    fun rollback(context: Context, timestamp: Long, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = AutoBackupManager.rollback(context, timestamp)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, if (ok) "回滚成功" else "回滚失败", Toast.LENGTH_SHORT).show()
                onDone()
            }
        }
    }
}
