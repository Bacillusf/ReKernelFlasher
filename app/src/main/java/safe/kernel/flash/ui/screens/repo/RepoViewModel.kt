package safe.kernel.flash.ui.screens.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import safe.kernel.flash.SharedViewModels
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class RepoType(val label: String) {
    GKI("(O)GKI"), OKI("OKI")
}

enum class RepoManager(val label: String) {
    RESUKI("ReSukiSU"), SUKISU("SukiSU"), KSU("KSU"), KSUN("KSUN")
}

enum class TypeFilter(val label: String) {
    ALL("全部"), GKI("(O)GKI"), OKI("OKI")
}

enum class VersionFilter(val label: String, val key: String?) {
    V61("6.1.x系列", "6.1."),
    V66("6.6.x系列", "6.6."),
    V612("6.12.x系列", "6.12."),
    ONEPLUS("OnePlus全部机型", null),
    ALL("全部", null)
}

enum class ManagerFilter(val label: String) {
    ALL("全部"), KSU("KSU"), KSUN("KSUN"), SUKISU("SukiSU"), RESUKI("ReSukiSU")
}

class RepoItem(
    val filename: String,
    val url: String,
    val tag: String,
    val type: RepoType,
    val manager: RepoManager?,
    val deviceModel: String?,
    val kernelVersion: String?,
    val features: List<String>
) {
    val line2: String
        get() {
            val m = manager?.label ?: "未知管理器"
            return if (deviceModel != null) "$m · $deviceModel" else "$m · 内核 $kernelVersion"
        }
}

class DownloadState {
    var progress by mutableStateOf(0f)
    var downloading by mutableStateOf(false)
    var completed by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

class RepoViewModel : ViewModel() {
    companion object {
        const val TAG: String = "KernelFlasher/RepoState"
        const val REPO_URL: String = "https://api.github.com/repos/Bacillusf/RKF-AK3-REPO/releases"
        const val DOWNLOAD_DIR: String = "/sdcard/Download"
        const val LOCAL_DL_DIR: String = "repo_downloads"
    }

    private val client = OkHttpClient()
    private val _allItems = mutableStateListOf<RepoItem>()
    private val _items = mutableStateListOf<RepoItem>()
    val downloadStates = mutableStateMapOf<String, DownloadState>()
    private var filesDir: File? = null

    var query by mutableStateOf("")
    var typeFilter by mutableStateOf(TypeFilter.ALL)
    var versionFilter by mutableStateOf(VersionFilter.ALL)
    var managerFilter by mutableStateOf(ManagerFilter.ALL)
    var isLoading by mutableStateOf(false)
    var loadError by mutableStateOf<String?>(null)
    var pendingFlashItem by mutableStateOf<RepoItem?>(null)

    val items: List<RepoItem>
        get() = _items
    val allItems: SnapshotStateList<RepoItem>
        get() = _allItems

    val versionOptions: List<VersionFilter>
        get() = if (typeFilter == TypeFilter.GKI) {
            listOf(VersionFilter.V61, VersionFilter.V66, VersionFilter.V612, VersionFilter.ALL)
        } else {
            listOf(VersionFilter.V61, VersionFilter.V66, VersionFilter.V612, VersionFilter.ONEPLUS, VersionFilter.ALL)
        }

    fun fetch(context: Context) {
        if (isLoading) return
        if (_allItems.isEmpty()) {
            loadCache(context)
        }
        isLoading = true
        loadError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body = fetchReleasesJson()
                val items = parseReleases(body)
                withContext(Dispatchers.Main) {
                    _allItems.clear()
                    _allItems.addAll(items)
                    saveCache(context, body)
                    applyFilters()
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch failed", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    loadError = e.message ?: "加载失败"
                    applyFilters()
                }
            }
        }
    }

    fun selectTypeFilter(value: TypeFilter) {
        typeFilter = value
        versionFilter = VersionFilter.ALL
        applyFilters()
    }

    fun selectVersionFilter(value: VersionFilter) {
        versionFilter = value
        applyFilters()
    }

    fun selectManagerFilter(value: ManagerFilter) {
        managerFilter = value
        applyFilters()
    }

    fun updateQuery(value: String) {
        query = value
        applyFilters()
    }

    fun download(context: Context, item: RepoItem) {
        val state = downloadStates.getOrPut(item.url) { DownloadState() }
        if (state.downloading || state.completed) return
        filesDir = context.filesDir
        state.downloading = true
        state.progress = 0f
        state.error = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = downloadToFile(context, item, state)
                Shell.cmd("mkdir -p $DOWNLOAD_DIR").exec()
                val dest = "$DOWNLOAD_DIR/${item.filename}"
                val res = Shell.cmd("cp -f '${file.absolutePath}' '$dest'").exec()
                if (!res.isSuccess) throw IOException("保存到下载目录失败")
                withContext(Dispatchers.Main) {
                    state.downloading = false
                    state.completed = true
                    pendingFlashItem = item
                }
            } catch (e: Exception) {
                Log.e(TAG, "download failed", e)
                withContext(Dispatchers.Main) {
                    state.downloading = false
                    state.error = e.message ?: "下载失败"
                }
            }
        }
    }

    fun clearPending() {
        pendingFlashItem = null
    }

    fun flashToActiveSlot(item: RepoItem): String {
        val mainVM = SharedViewModels.mainViewModel
        val suffix = mainVM.slotSuffix
        val slotVM = if (mainVM.isAb && suffix == "_b") mainVM.slotB else mainVM.slotA
        slotVM?.flashActionType = "flashAk3"
        val localFile = filesDir?.let { File(File(it, LOCAL_DL_DIR), item.filename) }
        slotVM?.flashActionURI = if (localFile != null && localFile.exists()) {
            Uri.fromFile(localFile)
        } else {
            Uri.fromFile(File(DOWNLOAD_DIR, item.filename))
        }
        slotVM?.showConfirmDialog()
        return suffix
    }

    private suspend fun fetchReleasesJson(): String {
        val request = Request.Builder()
            .url(REPO_URL)
            .header("User-Agent", "ReKernelFlasher")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("获取失败: HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("响应为空")
        }
    }

    private suspend fun downloadToFile(context: Context, item: RepoItem, state: DownloadState): File {
        val dir = File(context.filesDir, LOCAL_DL_DIR)
        dir.mkdirs()
        val file = File(dir, item.filename)
        val request = Request.Builder()
            .url(item.url)
            .header("User-Agent", "ReKernelFlasher")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("下载失败: HTTP ${response.code}")
            val body = response.body ?: throw IOException("下载失败")
            val total = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) state.progress = downloaded.toFloat() / total
                    }
                }
            }
        }
        return file
    }

    private fun parseReleases(body: String): List<RepoItem> {
        val result = mutableListOf<RepoItem>()
        try {
            val json = Json { ignoreUnknownKeys = true }
            val releases = json.parseToJsonElement(body).jsonArray
            for (release in releases) {
                val tag = release.jsonObject["tag_name"]?.jsonPrimitive?.content ?: continue
                val assets = release.jsonObject["assets"]?.jsonArray ?: continue
                for (asset in assets) {
                    val name = asset.jsonObject["name"]?.jsonPrimitive?.content ?: continue
                    val url = asset.jsonObject["browser_download_url"]?.jsonPrimitive?.content ?: continue
                    if (!name.startsWith("AK3") || !name.endsWith(".zip")) continue
                    result.add(parseItem(tag, name, url))
                }
            }
        } catch (e: Exception) {
            throw IOException("解析仓库数据失败", e)
        }
        return result
    }

    private fun parseItem(tag: String, name: String, url: String): RepoItem {
        val tokens = name.uppercase().split("_")
        val type = if (tokens.contains("GKI")) RepoType.GKI else RepoType.OKI
        val manager = when {
            tokens.contains("KSUNEXT") -> RepoManager.KSUN
            tokens.contains("RESUKI") -> RepoManager.RESUKI
            tokens.contains("SUKISU") -> RepoManager.SUKISU
            tokens.contains("KSU") -> RepoManager.KSU
            else -> null
        }
        val isOplusBuild = tag.contains("OPPO-OPlus-Realme")
        val isOnePlusBuild = tag.contains("OORB-ONEP")
        val deviceModel = if (isOnePlusBuild) extractDeviceModel(name) else null
        val kernelVersion = if (isOplusBuild) extractKernelVersion(name) else null
        val features = buildList {
            if (tokens.contains("SUSFS")) add("SusFS支持")
            if (tokens.contains("KPN")) add("KPN支持")
            if (tokens.contains("KPM")) add("KPM支持")
            if (tokens.contains("LZ4")) add("LZ4-zarm支持")
            if (tokens.contains("DS")) add("DroidSpace支持")
            if (tokens.contains("ADIOS")) add("ADIOS")
            if (tokens.contains("FIX")) add("Unicode字符串修复")
            if (tokens.contains("HMBRID")) add("风驰支持")
            if (tokens.contains("IN")) add("网络功能扩展")
            if (isOplusBuild && !contains("风驰支持")) add("风驰支持")
        }
        return RepoItem(name, url, tag, type, manager, deviceModel, kernelVersion, features)
    }

    private fun extractDeviceModel(name: String): String? {
        val m = Regex("""^AK3_(?:GKI_|MTK_)?[A-Za-z]+_([A-Za-z0-9]+)""").find(name) ?: return null
        val code = m.groupValues[1]
        return if (code.startsWith("ONEP")) "OnePlus" + code.removePrefix("ONEP").lowercase() else code
    }

    private fun extractKernelVersion(name: String): String? {
        return Regex("""(\d+\.\d+\.\d+)""").find(name)?.groupValues?.get(1)
    }

    private fun applyFilters() {
        val q = query.trim()
        val t = typeFilter
        val v = versionFilter
        val m = managerFilter
        _items.clear()
        _items.addAll(_allItems.filter { item ->
            (q.isEmpty() || item.filename.contains(q, ignoreCase = true)) &&
                (t == TypeFilter.ALL || item.type == if (t == TypeFilter.GKI) RepoType.GKI else RepoType.OKI) &&
                when (v) {
                    VersionFilter.ALL -> true
                    VersionFilter.ONEPLUS -> item.deviceModel != null
                    else -> item.kernelVersion?.startsWith(v.key!!) == true
                } &&
                when (m) {
                    ManagerFilter.ALL -> true
                    ManagerFilter.KSU -> item.manager == RepoManager.KSU
                    ManagerFilter.KSUN -> item.manager == RepoManager.KSUN
                    ManagerFilter.SUKISU -> item.manager == RepoManager.SUKISU
                    ManagerFilter.RESUKI -> item.manager == RepoManager.RESUKI
                }
        })
    }

    private fun cacheFile(context: Context): File = File(context.filesDir, "repo_cache.json")

    private fun loadCache(context: Context) {
        try {
            val f = cacheFile(context)
            if (f.exists()) {
                val items = parseReleases(f.readText())
                _allItems.clear()
                _allItems.addAll(items)
                applyFilters()
            }
        } catch (e: Exception) {
            Log.e(TAG, "load cache failed", e)
        }
    }

    private fun saveCache(context: Context, body: String) {
        try {
            cacheFile(context).writeText(body)
        } catch (e: Exception) {
            Log.e(TAG, "save cache failed", e)
        }
    }
}
