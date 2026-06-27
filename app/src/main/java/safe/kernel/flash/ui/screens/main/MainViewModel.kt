package safe.kernel.flash.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import safe.kernel.flash.AppUpdater
import safe.kernel.flash.BuildConfig
import safe.kernel.flash.common.AutoBackupManager
import safe.kernel.flash.common.HistoryManager
import safe.kernel.flash.common.PartitionUtil
import safe.kernel.flash.common.types.backups.Backup
import safe.kernel.flash.ui.screens.backups.BackupsViewModel
import safe.kernel.flash.ui.screens.reboot.RebootViewModel
import safe.kernel.flash.ui.screens.slot.SlotViewModel
import safe.kernel.flash.ui.screens.updates.UpdatesViewModel
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ExperimentalSerializationApi
class MainViewModel(
    context: Context,
    fileSystemManager: FileSystemManager,
    private val navController: NavController
) : ViewModel() {
    companion object {
        const val TAG: String = "KernelFlasher/MainViewModel"
    }
    val slotSuffix: String

    val kernelVersion: String
    val androidVersion: String = Build.VERSION.RELEASE
    val appVersionCode: Int = BuildConfig.VERSION_CODE
    val appVersion: String = BuildConfig.VERSION_NAME
    val appVersionFull: String = "${BuildConfig.VERSION_CODE}/v${BuildConfig.VERSION_NAME}"
    val halInfo: String
    val susfsVersion: String
    val rootManager: String
    val isAb: Boolean
    val slotA: SlotViewModel
    val slotB: SlotViewModel?
    val backups: BackupsViewModel
    val updates: UpdatesViewModel
    val reboot: RebootViewModel
    val hasRamoops: Boolean
    val avbVerityStatus: String
    val avbVerificationStatus: String

    private val _isRefreshing: MutableState<Boolean> = mutableStateOf(true)
    private val _isRefreshRequired = mutableStateOf(true)
    private var _error: String? = null
    private var _backups: MutableMap<String, Backup> = mutableMapOf()
    var showSlotIntentDialog: MutableState<Boolean> = mutableStateOf(false)

    var pendingFlashUri: Uri? = null
    var slotSuffixForFlash = mutableStateOf<String?>(null)

    val isRefreshing: Boolean
        get() = _isRefreshing.value
    val isRefreshRequired: Boolean
        get() = _isRefreshRequired.value
    val hasError: Boolean
        get() = _error != null
    val error: String
        get() = _error!!

    var dpiScale by mutableFloatStateOf(1.0f)
        private set

    fun applyDpiScale(scale: Float) {
        dpiScale = scale.coerceIn(0.5f, 1.5f)
    }

    // Update state
    var updateAvailable by mutableStateOf(false)
    var updateVersion by mutableStateOf("")
    var updateBody by mutableStateOf("")
    var updateDownloadUrl by mutableStateOf("")
    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableFloatStateOf(0f)

    fun setUpdateInfo(version: String, body: String, url: String) {
        updateVersion = version
        updateBody = body
        updateDownloadUrl = url
        updateAvailable = true
    }

    fun startDownload() { isDownloading = true; downloadProgress = 0f }
    fun updateDownloadProgress(p: Float) { downloadProgress = p }
    fun finishDownload() { isDownloading = false; updateAvailable = false }

    // Settings update check states
    var settingsUpdateChecking by mutableStateOf(false)
    var settingsUpdateFound by mutableStateOf(false)
    var settingsUpdateSameVersion by mutableStateOf(false)
    var settingsUpdateVersion by mutableStateOf("")
    var settingsUpdateDownloadUrl by mutableStateOf("")
    var settingsUpdateDownloading by mutableStateOf(false)
    var settingsUpdateProgress by mutableFloatStateOf(0f)
    var settingsUpdateError by mutableStateOf<String?>(null)

    fun checkForSettingsUpdate() {
        if (settingsUpdateChecking) return
        settingsUpdateChecking = true
        settingsUpdateFound = false
        settingsUpdateSameVersion = false
        settingsUpdateError = null
        viewModelScope.launch(Dispatchers.IO) {
            val hasNet = try { AppUpdater.hasActiveInternetConnection() } catch (_: Exception) { false }
            if (!hasNet) {
                withContext(Dispatchers.Main) {
                    settingsUpdateChecking = false
                    settingsUpdateError = "网络连接不可用，请检查网络后重试"
                }
                return@launch
            }
            val info = try { AppUpdater.fetchLatestUpdateInfo() } catch (_: Exception) { null }
            withContext(Dispatchers.Main) {
                if (info != null) {
                    settingsUpdateVersion = info.version
                    settingsUpdateDownloadUrl = info.downloadUrl
                    if (info.version == BuildConfig.VERSION_NAME) {
                        settingsUpdateSameVersion = true
                    } else {
                        settingsUpdateFound = true
                    }
                }
                settingsUpdateChecking = false
            }
        }
    }

    fun startSettingsUpdateDownload(context: Context) {
        if (settingsUpdateDownloading) return
        settingsUpdateDownloading = true
        settingsUpdateProgress = 0f
        settingsUpdateError = null
        viewModelScope.launch(Dispatchers.IO) {
            val progress = mutableFloatStateOf(0f)
            val pollJob = viewModelScope.launch(Dispatchers.Main) {
                while (settingsUpdateDownloading) {
                    settingsUpdateProgress = progress.floatValue
                    delay(100)
                }
            }
            AppUpdater.downloadWithProgress(
                context,
                settingsUpdateDownloadUrl,
                settingsUpdateVersion,
                progress,
                onComplete = { file ->
                    settingsUpdateDownloading = false
                    settingsUpdateFound = false
                    settingsUpdateSameVersion = false
                    AppUpdater.installApk(context, file)
                },
                onError = { error ->
                    settingsUpdateDownloading = false
                    settingsUpdateError = "下载失败: $error"
                }
            )
            pollJob.cancel()
        }
    }

    fun dismissSettingsUpdateError() { settingsUpdateError = null }

    fun markRefreshNeeded() {
        _isRefreshRequired.value = true
    }

    init {
        PartitionUtil.init(context, fileSystemManager)
        HistoryManager.init(fileSystemManager)
        AutoBackupManager.init(fileSystemManager)
        val bootctl = File(context.filesDir, "bootctl")
        halInfo = runCatching { Shell.cmd("$bootctl hal-info").exec().out[0].substringAfter("HAL Version: ").trim() }
            .recoverCatching { "" }
            .getOrDefault("")
        kernelVersion = Shell.cmd("echo $(uname -r) $(uname -v)").exec().out[0]
        susfsVersion = runCatching { Shell.cmd("susfsd version").exec().out[0] }
            .recoverCatching { Shell.cmd("ksu_susfs show version").exec().out[0] }
            .getOrDefault("v0.0.0")
        rootManager = runCatching {
            val ksu = Shell.cmd("test -d /data/adb/ksu && echo KernelSU || echo no").exec().out.firstOrNull() ?: "no"
            if (ksu == "KernelSU") "KernelSU"
            else {
                val ap = Shell.cmd("test -d /data/adb/ap && echo APatch || echo no").exec().out.firstOrNull() ?: "no"
                if (ap == "APatch") "APatch"
                else {
                    val magisk = Shell.cmd("test -d /data/adb/magisk && echo Magisk || echo Superuser").exec().out.firstOrNull() ?: "Superuser"
                    magisk
                }
            }
        }.getOrDefault("Unknown")
        slotSuffix = Shell.cmd("getprop ro.boot.slot_suffix").exec().out[0]
        backups = BackupsViewModel(context, fileSystemManager, navController, _isRefreshing, _backups)
        updates = UpdatesViewModel(context, fileSystemManager, navController, _isRefreshing)
        reboot = RebootViewModel(context, fileSystemManager, navController, _isRefreshing)
        isAb = slotSuffix.isNotEmpty()
        if (isAb) {
            val bootA = PartitionUtil.findPartitionBlockDevice(context, "boot", "_a")!!
            val bootB = PartitionUtil.findPartitionBlockDevice(context, "boot", "_b")!!
            val initBootA = PartitionUtil.findPartitionBlockDevice(context, "init_boot", "_a")
            val initBootB = PartitionUtil.findPartitionBlockDevice(context, "init_boot", "_b")
            slotA = SlotViewModel(context, fileSystemManager, navController, _isRefreshing, slotSuffix == "_a", "_a", bootA, initBootA, _backups)
            slotB = SlotViewModel(context, fileSystemManager, navController, _isRefreshing, slotSuffix == "_b", "_b", bootB, initBootB, _backups)
        } else {
            val boot = PartitionUtil.findPartitionBlockDevice(context, "boot", "")!!
            val initBoot = PartitionUtil.findPartitionBlockDevice(context, "init_boot", "")
            slotA = SlotViewModel(context, fileSystemManager, navController, _isRefreshing, true, "", boot, initBoot, _backups)
            if (slotA.hasError) {
                _error = slotA.error
            }
            slotB = null
        }

        hasRamoops = fileSystemManager.getFile("/sys/fs/pstore/console-ramoops-0").exists()

        // Detect AVB status from installed module
        avbVerityStatus = run {
            val avbctl = listOf(
                "/data/adb/modules/RKF/system/bin/avbctl",
                "/data/adb/modules/autodisableavb/system/bin/avbctl"
            ).firstOrNull { fileSystemManager.getFile(it).exists() } ?: return@run "不可用"

            val out = Shell.cmd("$avbctl get-verity 2>/dev/null").exec().out.joinToString(" ")
            if (out.contains("disabled", ignoreCase = true)) "已关闭" else "已开启"
        }
        avbVerificationStatus = run {
            val avbctl = listOf(
                "/data/adb/modules/RKF/system/bin/avbctl",
                "/data/adb/modules/autodisableavb/system/bin/avbctl"
            ).firstOrNull { fileSystemManager.getFile(it).exists() } ?: return@run "不可用"

            val out = Shell.cmd("$avbctl get-verification 2>/dev/null").exec().out.joinToString(" ")
            if (out.contains("disabled", ignoreCase = true)) "已关闭" else "已开启"
        }
        _isRefreshing.value = false
        _isRefreshRequired.value = false
    }

    fun refresh(context: Context) {
        if (!isRefreshRequired) return

        launch {
            slotA.refresh(context)
            if (isAb) {
                slotB!!.refresh(context)
            }
            backups.refresh(context)

            _isRefreshRequired.value = false
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            viewModelScope.launch(Dispatchers.Main) {
                _isRefreshing.value = true
            }
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
            viewModelScope.launch(Dispatchers.Main) {
                _isRefreshing.value = false
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun log(context: Context, message: String, shouldThrow: Boolean = false) {
        Log.d(TAG, message)
        if (!shouldThrow) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            throw Exception(message)
        }
    }

    fun saveRamoops(context: Context) {
        launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm"))
            @SuppressLint("SdCardPath")
            val ramoops = File("/sdcard/Download/console-ramoops--$now.log")
            Shell.cmd("cp /sys/fs/pstore/console-ramoops-0 $ramoops").exec()
            if (ramoops.exists()) {
                log(context, "Saved ramoops to $ramoops")
            } else {
                log(context, "Failed to save $ramoops", shouldThrow = true)
            }
        }
    }

    fun saveDmesg(context: Context) {
        launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm"))
            @SuppressLint("SdCardPath")
            val dmesg = File("/sdcard/Download/dmesg--$now.log")
            Shell.cmd("dmesg > $dmesg").exec()
            if (dmesg.exists()) {
                log(context, "Saved dmesg to $dmesg")
            } else {
                log(context, "Failed to save $dmesg", shouldThrow = true)
            }
        }
    }

    fun saveLogcat(context: Context) {
        launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm"))
            @SuppressLint("SdCardPath")
            val logcat = File("/sdcard/Download/logcat--$now.log")
            Shell.cmd("logcat -d > $logcat").exec()
            if (logcat.exists()) {
                log(context, "Saved logcat to $logcat")
            } else {
                log(context, "Failed to save $logcat", shouldThrow = true)
            }
        }
    }
}
