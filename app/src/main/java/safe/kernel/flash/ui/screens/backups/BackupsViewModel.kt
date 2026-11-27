package safe.kernel.flash.ui.screens.backups

import android.annotation.SuppressLint
import android.os.Build
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import safe.kernel.flash.SharedViewModels
import safe.kernel.flash.common.HistoryManager
import safe.kernel.flash.common.PartitionUtil
import safe.kernel.flash.common.extensions.ExtendedFile.outputStream
import safe.kernel.flash.common.extensions.ExtendedFile.readText
import safe.kernel.flash.common.types.backups.Backup
import safe.kernel.flash.common.types.history.HistoryEntry
import safe.kernel.flash.common.types.partitions.Partitions
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlin.DeprecationLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

class BackupsViewModel(
    context: Context,
    private val fileSystemManager: FileSystemManager,
    private val navController: NavController,
    private val _isRefreshing: MutableState<Boolean>,
    private val _backups: MutableMap<String, Backup>
) : ViewModel() {
    companion object {
        const val TAG: String = "KernelFlasher/BackupsState"
    }

    data class FullBackupPartition(
        val name: String,
        val sizeBytes: Long
    )

    data class FullBackupSummary(
        val platform: String,
        val sourceDir: String,
        val totalPartitions: Int,
        val skippedPartitions: List<String>,
        val backupPartitions: List<FullBackupPartition>,
        val estimatedBytes: Long
    ) {
        val backupCount: Int
            get() = backupPartitions.size
    }

    /** BPINFO.xml 里的一条分区记录。 */
    data class BpInfoEntry(
        val name: String,
        val sourcePath: String,
        val fileName: String,
        val sizeBytes: Long,
        val hash: String
    )

    private val _restoreOutput: SnapshotStateList<String> = mutableStateListOf()
    var currentBackup: String? = null
        set(value) {
            if (value != field) {
                if (_backups[value]?.hashes != null) {
                    PartitionUtil.AvailablePartitions.forEach { partitionName ->
                        if (_backups[value]!!.hashes!![partitionName] != null) {
                            _backupPartitions[partitionName] = true
                        }
                    }
                }
                field = value
            }
        }
    var wasRestored: Boolean? = null
    private val _backupPartitions: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    private val _fullBackupOutput: SnapshotStateList<String> = mutableStateListOf()
    private val fullBackupSkipList = listOf("userdata", "sdc")
    var fullBackupDirectory by mutableStateOf(defaultFullBackupDirectory())
        private set
    var fullBackupSummary by mutableStateOf<FullBackupSummary?>(null)
        private set
    var fullBackupWasSuccessful by mutableStateOf<Boolean?>(null)
        private set
    var fullBackupSuccessCount by mutableLongStateOf(0L)
        private set
    var fullBackupSkipCount by mutableLongStateOf(0L)
        private set
    var fullBackupFailCount by mutableLongStateOf(0L)
        private set
    // 分区勾选（扫描完成后默认全选）
    private val _fullBackupSelection: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    // 备份进度
    var fullBackupIsRunning by mutableStateOf(false)
        private set
    var fullBackupTotalCount by mutableIntStateOf(0)
        private set
    var fullBackupCompletedCount by mutableIntStateOf(0)
        private set
    var fullBackupCurrentPartition by mutableStateOf<String?>(null)
        private set
    var fullBackupCurrentProgress by mutableFloatStateOf(0f)
        private set
    var fullBackupCurrentCopiedBytes by mutableLongStateOf(0L)
        private set
    var fullBackupCurrentTotalBytes by mutableLongStateOf(0L)
        private set
    var fullBackupBpInfoPath by mutableStateOf<String?>(null)
        private set
    private val hashAlgorithm: String = "SHA-256"
    @Deprecated("Backup migration will be removed in the first stable release", level = DeprecationLevel.WARNING)
    private var _needsMigration: MutableState<Boolean> = mutableStateOf(false)

    val restoreOutput: List<String>
        get() = _restoreOutput
    val fullBackupOutput: List<String>
        get() = _fullBackupOutput
    val fullBackupSelection: Map<String, Boolean>
        get() = _fullBackupSelection
    val fullBackupSelectedPartitions: List<FullBackupPartition>
        get() = fullBackupSummary?.backupPartitions
            ?.filter { _fullBackupSelection[it.name] == true }
            .orEmpty()
    val fullBackupSelectedBytes: Long
        get() = fullBackupSelectedPartitions.sumOf { it.sizeBytes }
    val backupPartitions: MutableMap<String, Boolean>
        get() = _backupPartitions
    val isRefreshing: Boolean
        get() = _isRefreshing.value
    val backups: Map<String, Backup>
        get() = _backups
    @Deprecated("Backup migration will be removed in the first stable release")
    val needsMigration: Boolean
        get() = _needsMigration.value

    init {
        refresh(context)
    }

    fun refresh(context: Context) {
        val oldDir = context.getExternalFilesDir(null)
        val oldBackupsDir = File(oldDir, "backups")
        // Deprecated: Backup migration will be removed in the first stable release
        _needsMigration.value = oldBackupsDir.exists() && oldBackupsDir.listFiles()?.size!! > 0
        @SuppressLint("SdCardPath")
        val externalDir = File("/sdcard/ReKernelFlasher")
        val backupsDir = fileSystemManager.getFile("$externalDir/backups")
        if (backupsDir.exists()) {
            val children = backupsDir.listFiles()
            if (children != null) {
                for (child in children.sortedByDescending{it.name}) {
                    if (!child.isDirectory) {
                        continue
                    }
                    val jsonFile = child.getChildFile("backup.json")
                    if (jsonFile.exists()) {
                        _backups[child.name] = Json.decodeFromString(jsonFile.readText())
                    }
                }
            }
        }
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

    fun clearCurrent() {
        currentBackup = null
        clearRestore()
    }

    private fun addMessage(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _restoreOutput.add(message)
        }
    }

    @Suppress("FunctionName")
    private fun _clearRestore() {
        _restoreOutput.clear()
        wasRestored = null
    }

    private fun clearRestore() {
        _clearRestore()
        _backupPartitions.clear()
    }

    @Suppress("unused")
    @SuppressLint("SdCardPath")
    fun saveLog(context: Context) {
        launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm"))
            val log = File("/sdcard/Download/restore-log--$now.log")
            log.writeText(restoreOutput.joinToString("\n"))
            if (log.exists()) {
                log(context, "Saved restore log to $log")
            } else {
                log(context, "Failed to save $log", shouldThrow = true)
            }
        }
    }

    private fun restorePartitions(context: Context, source: ExtendedFile, slotSuffix: String): Partitions? {
        val partitions = HashMap<String, String>()
        for (partitionName in PartitionUtil.PartitionNames) {
            if (_backups[currentBackup]?.hashes == null || _backupPartitions[partitionName] == true) {
                val image = source.getChildFile("$partitionName.img")
                if (image.exists()) {
                    val blockDevice = PartitionUtil.findPartitionBlockDevice(context, partitionName, slotSuffix)
                    if (blockDevice != null && blockDevice.exists()) {
                        addMessage("Restoring $partitionName")
                        partitions[partitionName] = if (PartitionUtil.isPartitionLogical(context, partitionName)) {
                            PartitionUtil.flashLogicalPartition(context, image, blockDevice, partitionName, slotSuffix, hashAlgorithm) { message ->
                                addMessage(message)
                            }
                        } else {
                            PartitionUtil.flashBlockDevice(image, blockDevice, hashAlgorithm)
                        }
                    } else {
                        log(context, "Partition $partitionName was not found", shouldThrow = true)
                    }
                }
            }
        }
        if (partitions.isNotEmpty()) {
            return Partitions.from(partitions)
        }
        return null
    }

    fun restore(context: Context, slotSuffix: String) {
        launch {
            _clearRestore()
            @SuppressLint("SdCardPath")
            val externalDir = File("/sdcard/ReKernelFlasher")
            val backupsDir = fileSystemManager.getFile("$externalDir/backups")
            val backupDir = backupsDir.getChildFile(currentBackup!!)
            if (!backupDir.exists()) {
                log(context, "Backup $currentBackup does not exists", shouldThrow = true)
                return@launch
            }
            addMessage("Restoring backup $currentBackup")
            val hashes = restorePartitions(context, backupDir, slotSuffix)
            if (hashes == null) {
                log(context, "No partitions restored", shouldThrow = true)
            }
            addMessage("Backup $currentBackup restored")
            wasRestored = true
            HistoryManager.record(HistoryEntry.create("恢复备份 $currentBackup 到槽位 $slotSuffix"))
        }
    }

    fun delete(context: Context, callback: () -> Unit) {
        launch {
            @SuppressLint("SdCardPath")
            val externalDir = File("/sdcard/ReKernelFlasher")
            val backupsDir = fileSystemManager.getFile("$externalDir/backups")
            val backupDir = backupsDir.getChildFile(currentBackup!!)
            if (!backupDir.exists()) {
                log(context, "Backup $currentBackup does not exists", shouldThrow = true)
                return@launch
            }
            backupDir.deleteRecursively()
            _backups.remove(currentBackup!!)
            HistoryManager.record(HistoryEntry.create("删除备份 $currentBackup"))
            withContext(Dispatchers.Main) {
                callback.invoke()
            }
        }
    }


    fun updateFullBackupDirectory(path: String) {
        fullBackupDirectory = path
    }

    fun setFullBackupPartitionSelected(name: String, selected: Boolean) {
        _fullBackupSelection[name] = selected
    }

    fun toggleFullBackupPartition(name: String) {
        _fullBackupSelection[name] = _fullBackupSelection[name] != true
    }

    fun selectAllFullBackupPartitions() {
        fullBackupSummary?.backupPartitions?.forEach { _fullBackupSelection[it.name] = true }
    }

    fun clearFullBackupSelection() {
        fullBackupSummary?.backupPartitions?.forEach { _fullBackupSelection[it.name] = false }
    }

    fun resetFullBackupDirectory() {
        fullBackupDirectory = defaultFullBackupDirectory()
    }

    fun defaultFullBackupDirectory(): String {
        val model = sanitizeFileComponent(Build.MODEL.ifBlank { "unknown" })
        return "/sdcard/ReKernelFlasher/backups/${model}字库备份"
    }

    private fun sanitizeFileComponent(value: String): String {
        return value.trim()
            .replace(Regex("""[\/:*?"<>|]+"""), "-")
            .replace(Regex("""\s+"""), "-")
            .trim('-')
            .ifBlank { "unknown" }
    }
    private fun createUniqueDirectory(baseDir: String): String {
        val base = baseDir.trim().trimEnd('/')
        if (!fileSystemManager.getFile(base).exists()) return base
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm"))
        return "$base-$now"
    }

    private fun normalizeFullBackupDirectory(path: String): String {
        return path.trim().trimEnd('/').ifEmpty { defaultFullBackupDirectory() }
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return if (index == 0) {
            "${bytes} ${units[index]}"
        } else {
            String.format(Locale.US, "%.2f %s", value, units[index])
        }
    }

    private fun detectFullBackupSource(): Pair<String, String>? {
        // 与主页「设备平台」、刷写页「展示所有分区」共用同一套探测逻辑
        return PartitionUtil.detectPlatformSource()
    }

    private fun readPartitionSize(sourceDir: String, partitionName: String): Long {
        val quotedPath = shellQuote("$sourceDir/$partitionName")
        val d = "\$"
        val sizeText = Shell.cmd(
            "blockdev --getsize64 $quotedPath 2>/dev/null || " +
                    "cat /sys/class/block/${d}(basename ${d}(readlink -f $quotedPath))/size 2>/dev/null | awk '{print ${d}1 * 512}' || " +
                    "echo 0"
        ).exec().out.firstOrNull()?.trim().orEmpty()
        return sizeText.toLongOrNull() ?: 0L
    }

    fun prepareFullBackup(context: Context) {
        launch {
            _fullBackupOutput.clear()
            fullBackupWasSuccessful = null
            fullBackupSuccessCount = 0L
            fullBackupSkipCount = 0L
            fullBackupFailCount = 0L
            val source = detectFullBackupSource()
            if (source == null) {
                fullBackupSummary = null
                log(context, "不支持的平台", shouldThrow = true)
                return@launch
            }
            val uid = Shell.cmd("id -u").exec().out.firstOrNull()?.trim()
            if (uid != "0") {
                log(context, "需要ROOT权限", shouldThrow = true)
                return@launch
            }
            val (platform, sourceDir) = source
            val children = fileSystemManager.getFile(sourceDir).listFiles()?.map { it.name }?.sorted().orEmpty()
            val skipped = children.filter { it in fullBackupSkipList }
            val backupPartitions = children
                .filterNot { it in fullBackupSkipList }
                .map { FullBackupPartition(it, readPartitionSize(sourceDir, it)) }
            val estimatedBytes = backupPartitions.sumOf { it.sizeBytes }
            fullBackupSummary = FullBackupSummary(
                platform = platform,
                sourceDir = sourceDir,
                totalPartitions = children.size,
                skippedPartitions = skipped,
                backupPartitions = backupPartitions,
                estimatedBytes = estimatedBytes
            )
            fullBackupDirectory = normalizeFullBackupDirectory(fullBackupDirectory)
            // 扫描完成后默认全选可备份分区
            _fullBackupSelection.clear()
            backupPartitions.forEach { _fullBackupSelection[it.name] = true }
            fullBackupBpInfoPath = null
            fullBackupIsRunning = false
            fullBackupTotalCount = 0
            fullBackupCompletedCount = 0
            fullBackupCurrentPartition = null
            fullBackupCurrentProgress = 0f
            fullBackupCurrentCopiedBytes = 0L
            fullBackupCurrentTotalBytes = 0L
        }
    }

    private fun addFullBackupMessage(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _fullBackupOutput.add(message)
        }
    }

    fun clearFullBackupOutput() {
        _fullBackupOutput.clear()
        fullBackupWasSuccessful = null
        fullBackupSuccessCount = 0L
        fullBackupSkipCount = 0L
        fullBackupFailCount = 0L
    }

    /** 从日志页返回备份设置页（重新扫描分区并恢复默认全选）。 */
    fun returnToFullBackupSetup(context: Context) {
        clearFullBackupOutput()
        prepareFullBackup(context)
    }

    fun startFullBackup(context: Context) {
        val partitions = fullBackupSelectedPartitions
        if (partitions.isEmpty()) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "请先在分区列表中选择要备份的分区", Toast.LENGTH_SHORT).show()
            }
            return
        }
        launch {
            val source = detectFullBackupSource()
            if (source == null) {
                log(context, "不支持的平台", shouldThrow = true)
                return@launch
            }
            val uid = Shell.cmd("id -u").exec().out.firstOrNull()?.trim()
            if (uid != "0") {
                log(context, "需要ROOT权限", shouldThrow = true)
                return@launch
            }
            val (platform, sourceDir) = source
            val outDir = createUniqueDirectory(normalizeFullBackupDirectory(fullBackupDirectory))
            fullBackupDirectory = outDir
            fileSystemManager.getFile(outDir).mkdirs()

            fullBackupWasSuccessful = null
            fullBackupSuccessCount = 0L
            fullBackupSkipCount = (fullBackupSummary?.skippedPartitions?.size ?: 0).toLong()
            fullBackupFailCount = 0L
            fullBackupBpInfoPath = null
            fullBackupIsRunning = true
            fullBackupTotalCount = partitions.size
            fullBackupCompletedCount = 0
            fullBackupCurrentPartition = null
            fullBackupCurrentProgress = 0f
            fullBackupCurrentCopiedBytes = 0L
            fullBackupCurrentTotalBytes = 0L
            _fullBackupOutput.clear()

            listOf(
                "==================================================",
                "  字库备份",
                "  平台：$platform",
                "  源路径：$sourceDir",
                "  备份目录：$outDir",
                "  待备份分区：${partitions.size} 个（跳过 $fullBackupSkipCount 个）",
                "=================================================="
            ).forEach { addFullBackupMessage(it) }

            val bpInfoEntries = mutableListOf<BpInfoEntry>()
            try {
                partitions.forEachIndexed { index, partition ->
                    val srcPath = "$sourceDir/${partition.name}"
                    val dstPath = "$outDir/${partition.name}.img"
                    fullBackupCurrentPartition = partition.name
                    fullBackupCurrentProgress = 0f
                    fullBackupCurrentCopiedBytes = 0L
                    fullBackupCurrentTotalBytes = partition.sizeBytes

                    val copied = copyPartitionWithProgress(srcPath, dstPath, partition.sizeBytes)
                    if (copied) {
                        fullBackupCurrentProgress = 1f
                        val sizeBytes = runCatching { fileSystemManager.getFile(dstPath).length() }
                            .getOrDefault(partition.sizeBytes)
                        val hash = sha256Of(context, dstPath)
                        bpInfoEntries += BpInfoEntry(
                            name = partition.name,
                            sourcePath = srcPath,
                            fileName = "${partition.name}.img",
                            sizeBytes = sizeBytes,
                            hash = hash
                        )
                        fullBackupSuccessCount += 1
                        addFullBackupMessage("Backup '${partition.name}' ==>[OK]")
                    } else {
                        Shell.cmd("rm -f ${shellQuote(dstPath)}").exec()
                        fullBackupFailCount += 1
                        addFullBackupMessage("Backup '${partition.name}' ==>[FAIL]")
                    }

                    fullBackupCompletedCount = index + 1
                    fullBackupCurrentProgress = 0f
                    fullBackupCurrentCopiedBytes = 0L
                    fullBackupCurrentTotalBytes = 0L
                }

                val bpInfoPath = writeBpInfo(outDir, platform, sourceDir, bpInfoEntries)
                fullBackupBpInfoPath = bpInfoPath
                addFullBackupMessage("==================================================")
                addFullBackupMessage("  备份完成")
                addFullBackupMessage("  成功：$fullBackupSuccessCount 个")
                addFullBackupMessage("  跳过：$fullBackupSkipCount 个")
                addFullBackupMessage("  失败：$fullBackupFailCount 个")
                addFullBackupMessage("  分区信息：$bpInfoPath")
                addFullBackupMessage("  文件保存在：$outDir")
                addFullBackupMessage("==================================================")

                fullBackupWasSuccessful = fullBackupFailCount == 0L && bpInfoEntries.isNotEmpty()
                if (bpInfoEntries.isNotEmpty()) {
                    writeFullBackupRecord(outDir, platform)
                }
                HistoryManager.record(
                    HistoryEntry.create(
                        "字库备份 -> $outDir，成功 $fullBackupSuccessCount，跳过 $fullBackupSkipCount，失败 $fullBackupFailCount"
                    )
                )
                if (fullBackupWasSuccessful != true) {
                    log(context, "字库备份完成，但有 $fullBackupFailCount 个失败")
                }
            } finally {
                fullBackupIsRunning = false
                fullBackupCurrentPartition = null
                fullBackupCurrentProgress = 0f
                fullBackupCurrentCopiedBytes = 0L
                fullBackupCurrentTotalBytes = 0L
            }
        }
    }

    /** dd 复制单个分区，并在复制过程中轮询目标文件大小以驱动分进度条。 */
    private suspend fun copyPartitionWithProgress(
        srcPath: String,
        dstPath: String,
        totalBytes: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val dstFile = fileSystemManager.getFile(dstPath)
        val job = async(Dispatchers.IO) {
            Shell.cmd(
                "dd if=${shellQuote(srcPath)} of=${shellQuote(dstPath)} bs=4096 2>/dev/null && sync"
            ).exec().isSuccess
        }
        while (!job.isCompleted) {
            val copiedBytes = runCatching { dstFile.length() }.getOrDefault(0L)
            withContext(Dispatchers.Main) {
                fullBackupCurrentCopiedBytes = copiedBytes
                fullBackupCurrentProgress = if (totalBytes > 0L) {
                    (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
            delay(200)
        }
        job.await()
    }

    /** 优先用系统 sha256sum，缺失时回退到应用自带的 busybox。 */
    private fun sha256Of(context: Context, path: String): String {
        val quoted = shellQuote(path)
        val busybox = File(context.filesDir, "busybox").absolutePath
        val output = Shell.cmd(
            "sha256sum $quoted 2>/dev/null || ${shellQuote(busybox)} sha256sum $quoted 2>/dev/null"
        ).exec().out.firstOrNull()?.trim().orEmpty()
        return output.substringBefore(' ').lowercase()
    }

    /**
     * 在备份目录生成 BPINFO.xml（Backup Partition Information），
     * 记录每个分区的分区名 / 源备份节点路径 / img 文件名 / 大小（字节）/ 哈希。
     */
    private fun writeBpInfo(
        outDir: String,
        platform: String,
        sourceDir: String,
        entries: List<BpInfoEntry>
    ): String {
        val xml = BpInfoWriter.build(
            device = Build.MODEL,
            platform = platform,
            sourceDir = sourceDir,
            backupDir = outDir,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
            hashAlgorithm = hashAlgorithm,
            entries = entries
        )
        fileSystemManager.getFile(outDir)
            .getChildFile(BpInfoWriter.FILE_NAME)
            .outputStream()
            .use { it.write(xml.toByteArray(Charsets.UTF_8)) }
        return "$outDir/${BpInfoWriter.FILE_NAME}"
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeFullBackupRecord(outDir: String, platform: String) {
        val backupDir = fileSystemManager.getFile(outDir)
        if (!backupDir.exists()) return
        val backupName = backupDir.name
        val kernelVersion = Shell.cmd("echo $(uname -r) $(uname -v)").exec().out.firstOrNull()?.trim().orEmpty()
        val backup = Backup(
            name = backupName,
            type = "raw",
            kernelVersion = kernelVersion.ifBlank { platform },
            bootSha1 = null,
            filename = null,
            hashes = null,
            hashAlgorithm = hashAlgorithm
        )
        val jsonFile = backupDir.getChildFile("backup.json")
        val indentedJson = Json { prettyPrint = true }
        jsonFile.outputStream().use { it.write(indentedJson.encodeToString(backup).toByteArray(Charsets.UTF_8)) }
        _backups[backupName] = backup
    }

    @OptIn(ExperimentalSerializationApi::class)
    @SuppressLint("SdCardPath")
    @Deprecated("Backup migration will be removed in the first stable release")
    fun migrate(context: Context) {
        launch {
            val externalDir = fileSystemManager.getFile("/sdcard/ReKernelFlasher")
            if (!externalDir.exists()) {
                if (!externalDir.mkdir()) {
                    log(context, "Failed to create ReKernelFlasher dir on /sdcard", shouldThrow = true)
                }
            }
            val backupsDir = externalDir.getChildFile("backups")
            if (!backupsDir.exists()) {
                if (!backupsDir.mkdir()) {
                    log(context, "Failed to create backups dir", shouldThrow = true)
                }
            }
            val oldDir = context.getExternalFilesDir(null)
            val oldBackupsDir = File(oldDir, "backups")
            if (oldBackupsDir.exists()) {
                val indentedJson = Json { prettyPrint = true }
                val children = oldBackupsDir.listFiles()
                if (children != null) {
                    for (child in children.sortedByDescending{it.name}) {
                        if (!child.isDirectory) {
                            child.delete()
                            continue
                        }
                        val propFile = File(child, "backup.prop")
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val inputStream = FileInputStream(propFile)
                        val props = Properties()
                        @Suppress("BlockingMethodInNonBlockingContext")
                        props.load(inputStream)

                        val name = child.name
                        val type = props.getProperty("type", "raw")
                        val kernelVersion = props.getProperty("kernel")
                        val bootSha1 = if (type == "raw") props.getProperty("sha1") else null
                        val filename = if (type == "ak3") "ak3.zip" else null
                        propFile.delete()

                        val dest = backupsDir.getChildFile(child.name)
                        Shell.cmd("mv $child $dest").exec()
                        if (!dest.exists()) {
                            throw Error("Too slow")
                        }
                        val jsonFile = dest.getChildFile("backup.json")
                        val backup = Backup(name, type, kernelVersion, bootSha1, filename)
                        jsonFile.outputStream().use { it.write(indentedJson.encodeToString(backup).toByteArray(Charsets.UTF_8)) }
                        _backups[name] = backup
                    }
                }
                oldBackupsDir.delete()
            }
            SharedViewModels.mainViewModel.markRefreshNeeded()
            refresh(context)
        }
    }
}
