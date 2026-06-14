package safe.kernel.flash.ui.screens.reboot

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import safe.kernel.flash.common.HistoryManager
import safe.kernel.flash.common.types.history.HistoryEntry
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RebootViewModel(
    @Suppress("UNUSED_PARAMETER") ignoredContext: Context,
    @Suppress("unused") private val fileSystemManager: FileSystemManager,
    private val navController: NavController,
    private val _isRefreshing: MutableState<Boolean>
) : ViewModel() {
    companion object {
        const val TAG: String = "KernelFlasher/RebootState"
    }

    val isRefreshing: Boolean
        get() = _isRefreshing.value

    var showConfirmDialog by mutableStateOf(false)
        private set

    private var rebootDestination = mutableStateOf("")

    val confirmTitle: String
        get() = "CAUTION!"

    val confirmMessage: String
        get() = when (rebootDestination.value) {
            "recovery" -> "确定要重启到 Recovery 模式吗？"
            "bootloader" -> "确定要重启到 Bootloader 模式吗？"
            "download" -> "确定要重启到 Download 模式吗？"
            "edl" -> "确定要重启到 EDL 模式吗？"
            else -> "确定要重启系统吗？"
        }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                block()
            } catch (e: Exception) {
                withContext (Dispatchers.Main) {
                    Log.e(TAG, e.message, e)
                    navController.navigate("error/${e.message}") {
                        popUpTo("main")
                    }
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun reboot(destination: String = "") {
        launch {
            HistoryManager.record(HistoryEntry.create(
                "重启到 ${
                    when (destination) {
                        "" -> "系统"
                        "recovery" -> "Recovery"
                        "bootloader" -> "Bootloader"
                        "download" -> "Download"
                        "edl" -> "EDL"
                        else -> destination
                    }
                }"
            ))
            if (destination == "recovery") {
                Shell.cmd("/system/bin/input keyevent 26").submit()
            }
            Shell.cmd("/system/bin/svc power reboot $destination || /system/bin/reboot $destination").submit()
        }
    }

    fun showConfirm(destination: String) {
        rebootDestination.value = destination
        showConfirmDialog = true
    }

    fun hideConfirm() {
        showConfirmDialog = false
    }

    fun executeReboot() {
        showConfirmDialog = false
        reboot(rebootDestination.value)
    }

    fun rebootSystem() {
        reboot()
    }

    fun rebootRecovery() {
        reboot("recovery")
    }

    fun rebootBootloader() {
        reboot("bootloader")
    }

    fun rebootDownload() {
        reboot("download")
    }

    fun rebootEdl() {
        reboot("edl")
    }
}
