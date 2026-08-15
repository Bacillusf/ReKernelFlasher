package safe.kernel.flash.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import safe.kernel.flash.common.types.autobackup.AutoBackupItem
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

    /**
     * Backs up `boot` and `dtbo` into a single timestamped directory before flashing an
     * AnyKernel3 zip. Returns the backup timestamp (directory name) used for rollback, or `null`
     * when auto backup is disabled or nothing could be backed up.
     */
    @SuppressLint("SdCardPath")
    fun backupAk3(context: Context, slotSuffix: String): Long? {
        if (!isEnabled.value) {
            Log.d(TAG, "Auto backup disabled, skipping AK3 backup")
            return null
        }
        try {
            val timestamp = System.currentTimeMillis() / 1000
            val dirPath = "$BACKUP_DIR/$timestamp"
            Shell.cmd("mkdir -p $dirPath").exec()

            val items = mutableListOf<AutoBackupItem>()
            for (partitionName in listOf("boot", "dtbo")) {
                val blockDevice = PartitionUtil.findPartitionBlockDevice(context, partitionName, slotSuffix)
                if (blockDevice == null || !blockDevice.exists()) {
                    Log.w(TAG, "Skip $partitionName$slotSuffix: block device not found")
                    continue
                }
                val imgPath = "$dirPath/${partitionName}${slotSuffix}.img"
                val result = Shell.cmd("dd if=$blockDevice of=$imgPath bs=4096 && sync").exec()
                if (!result.isSuccess) {
                    Log.e(TAG, "dd failed for $partitionName: ${result.err.joinToString("\n")}")
                    continue
                }
                Shell.cmd("chmod 644 $imgPath").exec()
                items.add(AutoBackupItem(partitionName, slotSuffix, blockDevice.absolutePath, imgPath))
            }

            if (items.isEmpty()) {
                Shell.cmd("rm -rf $dirPath").exec()
                Log.e(TAG, "AK3 auto backup produced no images")
                return null
            }

            addToSummary(AutoBackupRecord.createAk3(timestamp, items))
            Log.d(TAG, "AK3 auto backup success: $dirPath (${items.size} partitions)")
            return timestamp
        } catch (e: Exception) {
            Log.e(TAG, "AK3 auto backup failed: ${e.message}", e)
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

    /**
     * Restores the partition image(s) associated with the given backup [timestamp]. For a plain
     * image backup this is a single partition; for an AK3 backup it restores every item (boot +
     * dtbo). Returns `true` only when every item was flashed back successfully.
     */
    @SuppressLint("SdCardPath")
    fun rollback(context: Context, timestamp: Long): Boolean {
        try {
            val record = getRecords().firstOrNull { it.timestamp == timestamp }
            if (record == null) {
                Log.e(TAG, "No auto backup record for timestamp $timestamp")
                return false
            }

            val items = when (record.kind) {
                AutoBackupRecord.KIND_AK3 -> record.items
                else -> listOf(AutoBackupItem(record.partition, record.slot, "", record.path))
            }.filter { it.partition.isNotEmpty() && it.path.isNotEmpty() }

            if (items.isEmpty()) {
                Log.e(TAG, "No items to rollback for timestamp $timestamp")
                return false
            }

            var success = true
            for (item in items) {
                val targetPath = if (item.source.isNotEmpty()) {
                    item.source
                } else {
                    PartitionUtil.findPartitionBlockDevice(context, item.partition, item.slot)?.absolutePath
                }
                if (targetPath.isNullOrEmpty()) {
                    Log.e(TAG, "Rollback: block device not found for ${item.partition}${item.slot}")
                    success = false
                    continue
                }
                val targetExists = Shell.cmd("test -e $targetPath && echo yes").exec().out.firstOrNull() == "yes"
                if (!targetExists) {
                    Log.e(TAG, "Rollback: target device missing $targetPath")
                    success = false
                    continue
                }
                val exists = Shell.cmd("test -f ${item.path} && echo yes").exec().out.firstOrNull() == "yes"
                if (!exists) {
                    Log.e(TAG, "Rollback: backup image missing ${item.path}")
                    success = false
                    continue
                }
                val result = Shell.cmd("dd if=${item.path} of=$targetPath bs=4096 && sync").exec()
                if (!result.isSuccess) {
                    Log.e(TAG, "Rollback dd failed for ${item.partition}${item.slot}: ${result.err.joinToString("\n")}")
                    success = false
                } else {
                    Log.d(TAG, "Rollback restored ${item.partition}${item.slot}")
                }
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Rollback failed: ${e.message}", e)
            return false
        }
    }
}
