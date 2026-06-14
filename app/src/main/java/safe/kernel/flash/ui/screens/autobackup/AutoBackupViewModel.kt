package safe.kernel.flash.ui.screens.autobackup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
}
