package safe.kernel.flash.ui.screens.history

import androidx.lifecycle.ViewModel
import safe.kernel.flash.common.HistoryManager

class HistoryViewModel : ViewModel() {

    val entries get() = HistoryManager.entries

    val isEmpty get() = entries.isEmpty()

    fun clearAll() {
        HistoryManager.clearAll()
    }
}
