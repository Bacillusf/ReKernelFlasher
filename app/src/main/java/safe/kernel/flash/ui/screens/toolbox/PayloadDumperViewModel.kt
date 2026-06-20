package safe.kernel.flash.ui.screens.toolbox

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PayloadDumperViewModel : ViewModel() {

    data class PartitionInfo(val name: String, val size: String)

    var payloadPath by mutableStateOf("")
    var payloadFilename by mutableStateOf("")
    var partitions = mutableStateListOf<PartitionInfo>()
    var selectedPartitions = mutableStateMapOf<String, Boolean>()
    var searchQuery by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isExtracting by mutableStateOf(false)
    var extractComplete by mutableStateOf(false)
    var extractOutput = mutableStateListOf<String>()
    var error by mutableStateOf<String?>(null)

    val filteredPartitions: List<PartitionInfo>
        get() = if (searchQuery.isBlank()) partitions.toList()
                else partitions.filter { it.name.contains(searchQuery, ignoreCase = true) }

    val hasSelection: Boolean
        get() = selectedPartitions.values.any { it }

    val selectedNames: String
        get() = selectedPartitions.filter { it.value }.keys.joinToString(", ")

    fun loadPayload(context: Context, uri: Uri) {
        isLoading = true
        partitions.clear()
        selectedPartitions.clear()
        searchQuery = ""
        extractComplete = false
        error = null
        payloadPath = ""
        payloadFilename = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolvedPath = resolvePath(context, uri)
                if (resolvedPath == null) {
                    // Copy to app files dir
                    val tmpFile = File(context.filesDir, "payload_tmp.bin")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    payloadPath = tmpFile.absolutePath
                    payloadFilename = uri.lastPathSegment ?: "payload.bin"
                } else {
                    payloadPath = resolvedPath
                    payloadFilename = File(resolvedPath).name
                }

                val dumper = File(context.filesDir, "payload-dumper-go")
                val cmd = "$dumper -l $payloadPath"
                val result = Shell.cmd(cmd).exec()

                withContext(Dispatchers.Main) {
                    if (!result.isSuccess) {
                        val errText = result.err.joinToString("\n").ifEmpty { "Unknown error" }
                        isLoading = false
                        error = "解析失败: $errText"
                        return@withContext
                    }

                    val output = result.out.joinToString("\n")
                    val lines = output.lines()
                    val foundIdx = lines.indexOfFirst { it.trim().startsWith("Found partitions") }
                    if (foundIdx >= 0) {
                        val partsBuilder = StringBuilder()
                        // Also check same line after colon
                        val sameLine = lines[foundIdx].substringAfter(":").trim()
                        if (sameLine.isNotEmpty()) partsBuilder.append(sameLine)
                        // Collect partition entries from lines after "Found partitions:"
                        for (i in (foundIdx + 1) until lines.size) {
                            val line = lines[i].trim()
                            if (line.isEmpty() || line.startsWith("Payload") || line.startsWith("Number of")) break
                            partsBuilder.append(line)
                        }
                        val partRegex = Regex("""(\S+)\s*\(([^)]+)\)""")
                        partRegex.findAll(partsBuilder.toString()).forEach { match ->
                            val name = match.groupValues[1].trimEnd(',')
                            val size = match.groupValues[2]
                            partitions.add(PartitionInfo(name, size))
                            selectedPartitions[name] = false
                        }
                    }

                    if (partitions.isEmpty()) {
                        error = "未找到任何分区"
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    error = "解析失败: ${e.message}"
                }
            }
        }
    }

    fun togglePartition(name: String) {
        selectedPartitions[name] = !(selectedPartitions[name] ?: false)
    }

    fun selectAll() {
        val filtered = filteredPartitions
        for (p in filtered) {
            selectedPartitions[p.name] = true
        }
    }

    fun deselectAll() {
        selectedPartitions.keys.forEach { selectedPartitions[it] = false }
    }

    fun clearImgFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imgDir = File("/sdcard/ReKernelFlasher/img")
                if (imgDir.exists()) {
                    Shell.cmd("rm -f /sdcard/ReKernelFlasher/img/*").exec()
                } else {
                    Shell.cmd("mkdir -p /sdcard/ReKernelFlasher/img").exec()
                }
            } catch (_: Exception) {}
        }
    }

    fun startExtraction(context: Context) {
        isExtracting = true
        extractComplete = false
        extractOutput.clear()
        error = null

        val parts = selectedPartitions.filter { it.value }.keys.joinToString(",")
        val imgDir = "/sdcard/ReKernelFlasher/img"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                clearImgFolderSync()
                val dumper = File(context.filesDir, "payload-dumper-go")
                val cmd = "$dumper -p $parts -o $imgDir $payloadPath"
                val result = Shell.cmd(cmd).exec()

                withContext(Dispatchers.Main) {
                    result.out.forEach { extractOutput.add(it) }
                    if (!result.isSuccess) {
                        result.err.forEach { extractOutput.add(it) }
                    }
                    isExtracting = false
                    extractComplete = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    extractOutput.add("错误: ${e.message}")
                    isExtracting = false
                    extractComplete = true
                }
            }
        }
    }

    private suspend fun clearImgFolderSync() {
        try {
            val imgDir = File("/sdcard/ReKernelFlasher/img")
            if (!imgDir.exists()) {
                Shell.cmd("mkdir -p /sdcard/ReKernelFlasher/img").exec()
            }
            Shell.cmd("rm -f /sdcard/ReKernelFlasher/img/*").exec()
        } catch (_: Exception) {}
    }

    private fun resolvePath(context: Context, uri: Uri): String? {
        return when {
            uri.scheme == "file" -> uri.path
            uri.toString().contains("com.android.externalstorage.documents") -> {
                val docId = uri.lastPathSegment ?: return null
                val colonIdx = docId.indexOf(':')
                if (colonIdx >= 0) {
                    val root = docId.substring(0, colonIdx)
                    val subPath = docId.substring(colonIdx + 1)
                    if (root == "primary") "/storage/emulated/0/$subPath"
                    else "/storage/$root/$subPath"
                } else null
            }
            else -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex("_data")
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            }
        }
    }
}
