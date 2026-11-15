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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 内核仓库的数据源。
 *
 * 两个接口都是官网（reflasher.site）提供的，App 不再直连 GitHub：
 *  - OFFICIAL ：纯文本，一行一个 raw 直链，共 121 行；
 *  - COMMUNITY：JSON 数组，每条 15 个字段（作者 / 特性 / 哈希 / 补充说明等）。
 */
enum class RepoSource(
    val label: String,
    val endpoint: String,
    val cacheName: String,
    val stampName: String,
) {
    OFFICIAL(
        "官方源",
        "https://reflasher.site/resources/api/official",
        "repo_official.txt",
        "repo_official.stamp",
    ),
    COMMUNITY(
        "社区源",
        "https://reflasher.site/resources/api/community",
        "repo_community.json",
        "repo_community.stamp",
    )
}

/**
 * 条目的归类。官方源按路径判定：
 *  - 路径含 MIXKI → 内核通刷（通用的内核版本包）
 *  - 路径含 OORB  → OnePlus 各机型专用包
 * 社区源没有分类机制，统一按 COMMUNITY 处理。
 */
enum class RepoKind(val label: String) {
    UNIVERSAL("内核通刷"),
    ONEPLUS("OnePlus机型"),
    COMMUNITY("社区"),
}

/** 官方源的分类筛选（社区源无分类，该筛选项隐藏）。 */
enum class TypeFilter(val label: String, val kind: RepoKind?) {
    ALL("全部类型", null),
    UNIVERSAL("内核通刷", RepoKind.UNIVERSAL),
    ONEPLUS("OnePlus机型", RepoKind.ONEPLUS),
}

class RepoItem(
    val filename: String,
    val url: String,
    val source: RepoSource,
    val kind: RepoKind,
    val manager: String?,
    val managerVer: String?,
    val deviceModel: String?,
    val kernelVersion: String?,
    val kernelSeries: String?,
    val features: List<String>,
    val sizeHuman: String? = null,
    val sha256: String? = null,
    val authorLogin: String? = null,
    val authorUrl: String? = null,
    val notes: String? = null,
    val publishedAt: String? = null,
    val githubUrl: String? = null,
) {
    var isLatest by mutableStateOf(false)

    /** 特性 chips 的展示文案（IN→BetterNet / DS→DroidSpace / LZ4→Lz4-Zarm）。 */
    val featureLabels: List<String>
        get() = features.map { RepoParser.featureLabel(it) }

    /** 卡片上第一个红胶囊：管理器:管理器版本。社区源允许「无管理器」。 */
    val managerVersion: String
        get() {
            val m = manager ?: return "无管理器"
            val ver = managerVer ?: return m
            return "$m:$ver"
        }

    /** 卡片上第二个红胶囊：机型（OORB / 社区）或内核版本（MIXKI）。 */
    val line2: String
        get() = when {
            deviceModel != null -> deviceModel
            kernelVersion != null -> "内核 $kernelVersion"
            else -> "未知"
        }

    /** 精细化搜索用：把卡片上能看到、能想到的字段全部拼进索引。 */
    val searchBlob: String by lazy {
        buildString {
            append(filename).append(' ')
            deviceModel?.let { append(it).append(' ') }
            kernelVersion?.let { append(it).append(' ') }
            kernelSeries?.let { append(it).append(' ') }
            manager?.let { append(it).append(' ') }
            managerVer?.let { append(it).append(' ') }
            features.forEach { append(it).append(' ') }
            // 展示名也进索引，搜 BetterNet / DroidSpace / Lz4-Zarm 同样命中
            featureLabels.forEach { append(it).append(' ') }
            notes?.let { append(it).append(' ') }
            authorLogin?.let { append(it).append(' ') }
            append(kind.label).append(' ')
            append(source.label)
        }.lowercase()
    }
}

class DownloadState {
    var progress by mutableStateOf(0f)
    var downloading by mutableStateOf(false)
    var completed by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

/** 社区源的 ISO 时间 → 人话。 */
object RepoTime {
    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun parseIso(value: String?): OffsetDateTime? =
        if (value.isNullOrBlank()) null else runCatching { OffsetDateTime.parse(value) }.getOrNull()

    /** 「今天 16:48 / 昨天 16:48 / 11-12 16:48」。 */
    fun human(value: String?): String? {
        val odt = parseIso(value) ?: return value
        val zone = ZoneId.systemDefault()
        val time = odt.atZoneSameInstant(zone)
        val today = LocalDate.now(zone)
        return when (time.toLocalDate()) {
            today -> "今天 " + time.format(clockFormatter)
            today.minusDays(1) -> "昨天 " + time.format(clockFormatter)
            else -> time.format(dateFormatter)
        }
    }

    /** 信息表里的完整时间。 */
    fun full(value: String?): String {
        val odt = parseIso(value) ?: return value ?: "—"
        return odt.atZoneSameInstant(ZoneId.systemDefault()).format(fullFormatter)
    }

    fun clock(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(clockFormatter)
}

class RepoViewModel : ViewModel() {
    companion object {
        const val TAG: String = "KernelFlasher/RepoState"
        const val DOWNLOAD_DIR: String = "/sdcard/Download"
        const val LOCAL_DL_DIR: String = "repo_downloads"
    }

    private val client = OkHttpClient()
    private val officialItems = mutableListOf<RepoItem>()
    private val communityItems = mutableListOf<RepoItem>()
    private val loadedSources = mutableSetOf<RepoSource>()

    private val _allItems = mutableStateListOf<RepoItem>()
    private val _items = mutableStateListOf<RepoItem>()
    val downloadStates = mutableStateMapOf<String, DownloadState>()
    private var filesDir: File? = null

    var source by mutableStateOf(RepoSource.OFFICIAL)
        private set
    var query by mutableStateOf("")
    var typeFilter by mutableStateOf(TypeFilter.ALL)
        private set
    var versionKey by mutableStateOf<String?>(null)
        private set
    var managerKey by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var lastFetchedAt by mutableStateOf<Long?>(null)
        private set
    var pendingFlashItem by mutableStateOf<RepoItem?>(null)
        private set
    var infoItem by mutableStateOf<RepoItem?>(null)
        private set
    var downloadRequestItem by mutableStateOf<RepoItem?>(null)
        private set

    val items: List<RepoItem>
        get() = _items
    val allItems: SnapshotStateList<RepoItem>
        get() = _allItems

    /** 管理器筛选项由当前源的数据动态生成，网站以后加新管理器不用改 App。 */
    val managerOptions: List<String>
        get() = _allItems.mapNotNull { it.manager }.distinct().sortedBy { RepoParser.managerRank(it) }

    /** 内核系列筛选项同样动态生成（6.1.x / 6.6.x / 6.12.x …），新的排前面。 */
    val seriesOptions: List<String>
        get() = _allItems.mapNotNull { it.kernelSeries }.distinct().sortedByDescending { RepoParser.seriesRank(it) }

    val universalCount: Int
        get() = _allItems.count { it.kind == RepoKind.UNIVERSAL }

    val onePlusCount: Int
        get() = _allItems.count { it.kind == RepoKind.ONEPLUS }

    fun fetch(context: Context, force: Boolean = false) {
        if (isLoading) return
        val requested = source
        val alreadyLoaded = loadedSources.contains(requested)
        if (!alreadyLoaded) {
            loadCache(context, requested)
        } else if (!force) {
            return
        }

        isLoading = true
        loadError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body = httpGet(requested.endpoint)
                val parsed = when (requested) {
                    RepoSource.OFFICIAL -> RepoParser.parseOfficial(body)
                    RepoSource.COMMUNITY -> RepoParser.parseCommunity(body)
                }
                if (parsed.isEmpty()) throw IOException("接口返回为空")
                RepoParser.markLatest(parsed)
                saveCache(context, requested, body)
                withContext(Dispatchers.Main) {
                    replaceSourceItems(requested, parsed)
                    loadedSources.add(requested)
                    lastFetchedAt = System.currentTimeMillis()
                    if (source == requested) {
                        syncAllItems()
                        applyFilters()
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch ${requested.label} failed", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    loadError = e.message ?: "加载失败"
                    applyFilters()
                }
            }
        }
    }

    fun selectSource(context: Context, value: RepoSource) {
        if (source == value) return
        source = value
        typeFilter = TypeFilter.ALL
        versionKey = null
        managerKey = null
        loadError = null
        lastFetchedAt = runCatching {
            stampFile(context, value).takeIf { it.exists() }?.readText()?.toLongOrNull()
        }.getOrNull()
        syncAllItems()
        applyFilters()
        fetch(context)
    }

    fun selectTypeFilter(value: TypeFilter) {
        typeFilter = value
        versionKey = null
        applyFilters()
    }

    fun selectVersionFilter(value: String?) {
        versionKey = value
        applyFilters()
    }

    fun selectManagerFilter(value: String?) {
        managerKey = value
        applyFilters()
    }

    fun updateQuery(value: String) {
        query = value
        applyFilters()
    }

    fun showInfo(item: RepoItem) {
        infoItem = item
    }

    fun hideInfo() {
        infoItem = null
    }

    /** 社区源：点下载先把信息摆出来，滑到底部确认后才真正开始下载。 */
    fun requestDownload(item: RepoItem) {
        downloadRequestItem = item
    }

    fun cancelDownloadRequest() {
        downloadRequestItem = null
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

    private suspend fun httpGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ReKernelFlasher")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("获取失败: HTTP ${response.code}")
            return response.body.string()
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
            val body = response.body
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

    private fun replaceSourceItems(target: RepoSource, items: List<RepoItem>) {
        val list = if (target == RepoSource.OFFICIAL) officialItems else communityItems
        list.clear()
        list.addAll(items)
    }

    private fun syncAllItems() {
        val list = if (source == RepoSource.OFFICIAL) officialItems else communityItems
        _allItems.clear()
        _allItems.addAll(list)
    }

    private fun applyFilters() {
        val keyword = query.trim()
        val type = typeFilter
        val series = versionKey
        val manager = managerKey
        val filtered = _allItems.filter { item ->
            (keyword.isEmpty() || item.searchBlob.contains(keyword.lowercase())) &&
                (type == TypeFilter.ALL || item.kind == type.kind) &&
                (series == null || item.kernelSeries == series) &&
                (manager == null || item.manager == manager)
        }
        val sorted = if (source == RepoSource.OFFICIAL) {
            filtered.sortedWith { a, b ->
                var compare = a.kind.ordinal.compareTo(b.kind.ordinal)
                if (compare == 0) compare = RepoParser.seriesRank(b.kernelSeries).compareTo(RepoParser.seriesRank(a.kernelSeries))
                if (compare == 0) compare = RepoParser.managerRank(a.manager).compareTo(RepoParser.managerRank(b.manager))
                if (compare == 0) compare = RepoParser.kernelRank(b.kernelVersion).compareTo(RepoParser.kernelRank(a.kernelVersion))
                if (compare == 0) compare = a.filename.compareTo(b.filename)
                compare
            }
        } else {
            filtered
        }
        val (latest, rest) = sorted.partition { it.isLatest }
        _items.clear()
        _items.addAll(latest)
        _items.addAll(rest)
    }

    // ---------------------------------------------------------------- 缓存

    private fun cacheFile(context: Context, target: RepoSource): File =
        File(context.filesDir, target.cacheName)

    private fun stampFile(context: Context, target: RepoSource): File =
        File(context.filesDir, target.stampName)

    private fun saveCache(context: Context, target: RepoSource, body: String) {
        runCatching {
            cacheFile(context, target).writeText(body)
            stampFile(context, target).writeText(System.currentTimeMillis().toString())
        }.onFailure { Log.e(TAG, "save cache failed", it) }
    }

    private fun loadCache(context: Context, target: RepoSource) {
        runCatching {
            val file = cacheFile(context, target)
            if (!file.exists() || file.length() == 0L) return@runCatching
            val body = file.readText()
            val parsed = when (target) {
                RepoSource.OFFICIAL -> RepoParser.parseOfficial(body)
                RepoSource.COMMUNITY -> RepoParser.parseCommunity(body)
            }
            if (parsed.isEmpty()) return@runCatching
            RepoParser.markLatest(parsed)
            replaceSourceItems(target, parsed)
            loadedSources.add(target)
            if (source == target) {
                syncAllItems()
                applyFilters()
                lastFetchedAt = stampFile(context, target)
                    .takeIf { it.exists() }?.readText()?.toLongOrNull()
            }
        }.onFailure { Log.e(TAG, "load cache failed", it) }
    }
}
