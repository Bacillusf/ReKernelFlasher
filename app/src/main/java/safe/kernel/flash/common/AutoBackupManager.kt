package safe.kernel.flash.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import safe.kernel.flash.common.types.autobackup.AutoBackupRecord
import java.io.File
import java.util.Base64

object AutoBackupManager {
    private const val TAG = "AutoBackupManager"
    @SuppressLint("SdCardPath")
    private const val BACKUP_DIR = "/sdcard/ReKernelFlasher/Autobackup"
    @SuppressLint("SdCardPath")
    private const val CONFIG_FILE = "/sdcard/ReKernelFlasher/autobackup_enabled"
    @SuppressLint("SdCardPath")
    private const val SUMMARY_FILE = "/sdcard/ReKernelFlasher/Autobackup/summary.json"

    val isEnabled = mutableStateOf(false)
    private var fileSystemManager: FileSystemManager? = null

    fun init(fs: FileSystemManager) {
        fileSystemManager = fs
        isEnabled.value = Shell.cmd("test -f $CONFIG_FILE && echo yes").exec().out.firstOrNull() == "yes"
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled.value = enabled
        try {
            Shell.cmd("mkdir -p ${File(CONFIG_FILE).parent}").exec()
            if (enabled) {
                Shell.cmd("echo 1 > $CONFIG_FILE").exec()
            } else {
                Shell.cmd("rm -f $CONFIG_FILE").exec()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
        }
    }

    @SuppressLint("SdCardPath")
    fun backup(context: Context, partitionName: String, slotSuffix: String): String? {
        if (!isEnabled.value) {
            Log.d(TAG, "Auto backup disabled, skipping")
            return null
        }
        try {
            val timestamp = System.currentTimeMillis() / 1000
            val dirPath = "$BACKUP_DIR/$timestamp"
            Shell.cmd("mkdir -p $dirPath").exec()

            val blockDevice = PartitionUtil.findPartitionBlockDevice(context, partitionName, slotSuffix)
            if (blockDevice == null) {
                Log.e(TAG, "Partition $partitionName$slotSuffix not found")
                return null
            }
            if (!blockDevice.exists()) {
                Log.e(TAG, "Block device ${blockDevice.path} does not exist")
                return null
            }

            val imgPath = "$dirPath/${partitionName}${slotSuffix}.img"
            val result = Shell.cmd("dd if=$blockDevice of=$imgPath bs=4096 && sync").exec()
            if (!result.isSuccess) {
                Log.e(TAG, "dd failed: ${result.err.joinToString("\n")}")
                return null
            }
            Shell.cmd("chmod 644 $imgPath").exec()

            Shell.cmd("echo \"timestamp: $timestamp\" > $dirPath/backup.yml").exec()
            Shell.cmd("echo \"partition: $partitionName\" >> $dirPath/backup.yml").exec()
            Shell.cmd("echo \"slot: $slotSuffix\" >> $dirPath/backup.yml").exec()
            Shell.cmd("echo \"path: $imgPath\" >> $dirPath/backup.yml").exec()

            addToSummary(AutoBackupRecord.create(timestamp, partitionName, slotSuffix, imgPath))

            Log.d(TAG, "Auto backup success: $imgPath")
            return imgPath
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup failed: ${e.message}", e)
            return null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun addToSummary(record: AutoBackupRecord) {
        try {
            Shell.cmd("mkdir -p $BACKUP_DIR").exec()
            val records = getRecords().toMutableList()
            records.add(0, record)
            val indentedJson = Json { prettyPrint = true }
            val json = indentedJson.encodeToString(records)
            val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
            Shell.cmd("echo $encoded | base64 -d > $SUMMARY_FILE").exec()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update summary: ${e.message}", e)
        }
    }

    @SuppressLint("SdCardPath")
    fun getRecords(): List<AutoBackupRecord> {
        return try {
            val exists = Shell.cmd("test -f $SUMMARY_FILE && echo yes").exec().out.firstOrNull() == "yes"
            if (!exists) return emptyList()
            val content = Shell.cmd("cat $SUMMARY_FILE").exec().out.joinToString("\n")
            if (content.isBlank()) return emptyList()
            Json.decodeFromString<List<AutoBackupRecord>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read summary: ${e.message}", e)
            emptyList()
        }
    }
}
