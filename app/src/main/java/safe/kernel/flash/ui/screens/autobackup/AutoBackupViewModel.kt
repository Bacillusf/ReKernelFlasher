package safe.kernel.flash.ui.screens.autobackup

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.common.types.autobackup.AutoBackupRecord

class AutoBackupViewModel : ViewModel() {

    var records by mutableStateOf<List<AutoBackupRecord>>(emptyList())
        private set

    init {
        refresh()
    }

    fun refresh() {
        records = AutoBackupManager.getRecords()
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
