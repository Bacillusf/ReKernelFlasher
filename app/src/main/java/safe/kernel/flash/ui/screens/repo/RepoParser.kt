package safe.kernel.flash.ui.screens.repo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import java.io.IOException

/**
 * 两个接口的解析层（纯函数，不碰 Android API，因此可以单测）。
 *
 * 官方源是纯文本链接清单，元数据全在路径和文件名里：
 *   AnyKernel3_Official/MIXKI/6.6.x/BakaSU/AK3_BAKASU_6.6.89_35259_SUSFS_KPN_LZ4_DS_IN_ADIOS_BBG.zip
 *   AnyKernel3_Official/OORB/BakaSU/AK3_BAKASU_ONEP13T_35259_SUSFS_KPM_BBG_IN_FIX_HMBRID_DS.zip
 *
 * 社区源是 JSON 数组，字段齐全（作者 / 特性 / 哈希 / 说明 / 时间）。
 */
internal object RepoParser {

    const val RAW_PREFIX: String =
        "https://raw.githubusercontent.com/Bacillusf/RKF-Resource-Repo/product/"

    private val MANAGER_VERSION_REGEX = Regex("""^\d{4,6}$""")
    private val KERNEL_VERSION_REGEX = Regex("""(?<![\d.])(\d+\.\d+\.\d+)(?![\d.])""")
    private val SERIES_REGEX = Regex("""^\d+\.\d+\.x$""")
    private val NUMERIC_REGEX = Regex("""^\d+(\.\d+)*$""")
    private val DEVICE_REGEX = Regex("""_ONEP([A-Za-z0-9]+)(?=_|$)""")

    private val MANAGER_LABELS = mapOf(
        "BAKASU" to "BakaSU",
        "KSUNEXT" to "KSU-Next",
        "KSU-NEXT" to "KSU-Next",
        "KSUN" to "KSUN",
        "SUKISU" to "SukiSU",
        "KSU" to "KSU",
        "RESUKI" to "ReSukiSU",
        "RESUKISU" to "ReSukiSU",
        "NONE" to null,
    )

    private val MANAGER_ORDER =
        listOf("KSU-Next", "KSU", "SukiSU", "ReSukiSU", "BakaSU", "KSUN")

    /**
     * 特性展示顺序：少见的、有信息量的放前面；DS / IN / BBG 在官方源里 100% 出现，
     * 统一压到最后，免得每张卡片前三个标签都一样。
     */
    private val FEATURE_ORDER = listOf(
        "SUSFS", "NOSUSFS", "KPM", "KPN", "LZ4", "HMBRID", "FIX", "SSG", "BBR", "ADIOS",
        "DROIDSPACE", "UFW-SUPPORT", "NTSYNC", "DS", "IN", "BBG",
    )

    fun normalizeManager(raw: String?): String? {
        val key = raw?.trim()?.uppercase().orEmpty()
        if (key.isEmpty()) return null
        return if (MANAGER_LABELS.containsKey(key)) MANAGER_LABELS[key] else raw?.trim()
    }

    fun managerRank(manager: String?): Int {
        if (manager == null) return 99
        val index = MANAGER_ORDER.indexOf(manager)
        return if (index < 0) 50 else index
    }

    fun kernelRank(version: String?): Int {
        val parts = version?.split('.') ?: return -1
        if (parts.size < 3) return -1
        val major = parts[0].toIntOrNull() ?: return -1
        val minor = parts[1].toIntOrNull() ?: return -1
        val patch = parts[2].toIntOrNull() ?: return -1
        return major * 1_000_000 + minor * 1_000 + patch
    }

    fun seriesRank(series: String?): Int {
        val head = series?.removeSuffix(".x") ?: return -1
        val parts = head.split('.')
        if (parts.size < 2) return -1
        val major = parts[0].toIntOrNull() ?: return -1
        val minor = parts[1].toIntOrNull() ?: return -1
        return major * 1000 + minor
    }

    fun featureRank(feature: String): Int {
        val index = FEATURE_ORDER.indexOf(feature.uppercase())
        return if (index < 0) 80 else index
    }

    fun sortFeatures(features: List<String>): List<String> =
        features.distinct().sortedWith(compareBy({ featureRank(it) }, { it }))

    /** 官方源：一行一个 raw 直链。 */
    fun parseOfficial(body: String): List<RepoItem> {
        val result = ArrayList<RepoItem>()
        for (rawLine in body.lineSequence()) {
            val line = rawLine.trim()
            if (!line.startsWith("http", ignoreCase = true)) continue
            if (!line.endsWith(".zip", ignoreCase = true)) continue
            val path = if (line.startsWith(RAW_PREFIX)) {
                line.removePrefix(RAW_PREFIX)
            } else {
                line.substringAfter("/product/", "")
            }
            if (path.isEmpty()) continue
            val segments = path.split('/')
            if (segments.size < 4) continue
            val filename = segments.last()
            val kind = when {
                segments.contains("MIXKI") -> RepoKind.UNIVERSAL
                segments.contains("OORB") -> RepoKind.ONEPLUS
                else -> continue
            }
            val tokens = filename.removeSuffix(".zip").split('_')
            val manager = normalizeManager(segments.getOrNull(segments.size - 2))
                ?: tokens.firstNotNullOfOrNull { normalizeManager(it) }
            result.add(
                RepoItem(
                    filename = filename,
                    url = line,
                    source = RepoSource.OFFICIAL,
                    kind = kind,
                    manager = manager,
                    managerVer = tokens.firstOrNull { it.matches(MANAGER_VERSION_REGEX) },
                    deviceModel = if (kind == RepoKind.ONEPLUS) parseDeviceModel(filename) else null,
                    kernelVersion = KERNEL_VERSION_REGEX.find(filename)?.groupValues?.get(1),
                    kernelSeries = if (kind == RepoKind.UNIVERSAL) {
                        segments.getOrNull(2)?.takeIf { it.matches(SERIES_REGEX) }
                    } else {
                        null
                    },
                    features = sortFeatures(collectFeatures(tokens)),
                )
            )
        }
        return result
    }

    /** 社区源：JSON 数组，每条 15 个字段。 */
    fun parseCommunity(body: String): List<RepoItem> {
        val json = Json { ignoreUnknownKeys = true }
        val array = try {
            json.parseToJsonElement(body).jsonArray
        } catch (e: Exception) {
            throw IOException("解析社区数据失败", e)
        }
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val filename = obj.string("filename") ?: return@mapNotNull null
            val downloadUrl = obj.string("download_url") ?: return@mapNotNull null
            val author = obj["author"] as? JsonObject
            val featuresElement = obj["features"]
            val features = if (featuresElement is JsonArray) {
                featuresElement.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            } else {
                emptyList()
            }
            val device = obj.string("device")
            RepoItem(
                filename = filename,
                url = downloadUrl,
                source = RepoSource.COMMUNITY,
                kind = RepoKind.COMMUNITY,
                manager = normalizeManager(obj.string("manager")),
                managerVer = obj.string("manager_version"),
                deviceModel = device?.takeIf { it.isNotBlank() },
                kernelVersion = KERNEL_VERSION_REGEX.find(device.orEmpty())?.groupValues?.get(1),
                kernelSeries = null,
                features = sortFeatures(features),
                sizeHuman = obj.string("size_human"),
                sha256 = obj.string("sha256"),
                authorLogin = author?.string("login"),
                authorUrl = author?.string("url"),
                notes = obj.string("notes"),
                publishedAt = obj.string("published_at"),
                githubUrl = obj.string("github_url"),
            )
        }
    }

    /**
     * 「最新版」只标官方源的内核通刷包：同一（内核系列 + 管理器）里内核版本最高的那一个。
     * 管理器版本号在同一管理器的所有包里是常量（35259 / 33548 …），拿它判重会满屏星；
     * OORB 每个机型只有一个包、社区源没有可比性，都不标。
     */
    fun markLatest(items: List<RepoItem>) {
        items.forEach { it.isLatest = false }
        val groups = items
            .filter { it.kind == RepoKind.UNIVERSAL && it.kernelVersion != null }
            .groupBy { (it.kernelSeries ?: "") + "|" + (it.manager ?: "") }
        for (group in groups.values) {
            group.maxByOrNull { kernelRank(it.kernelVersion) }?.isLatest = true
        }
    }

    private fun parseDeviceModel(filename: String): String? {
        val code = DEVICE_REGEX.find(filename)?.groupValues?.get(1) ?: return null
        return prettyOnePlusModel(code)
    }

    /**
     * 特性 = 文件名里除结构 token 之外的全部 token。
     * 结构 token：AK3 / GKI / MTK（平台不做区分，按需求直接忽略）、ONEP、管理器名、纯数字版本号。
     */
    fun collectFeatures(tokens: List<String>): List<String> {
        val result = LinkedHashSet<String>()
        for (raw in tokens) {
            val token = raw.uppercase()
            if (token.isEmpty()) continue
            if (token == "AK3" || token == "GKI" || token == "MTK" || token == "ZIP") continue
            if (token.startsWith("ONEP")) continue
            if (MANAGER_LABELS.containsKey(token)) continue
            if (token.matches(NUMERIC_REGEX)) continue
            result.add(token)
        }
        return result.toList()
    }

    /**
     * 把 `ONEP13T` / `ONEPACE5PRO` / `ONEPPAD2MT6991` 这类机型代码拆成可读名字。
     * 词典贪心扫描 + 两条合并规则：`CE/N/MT` 贴住后面的数字，数字贴住后面的单字母后缀
     * （T/R/V/X/S/G/E/A），于是 13T → "13T"、ACE5PRO → "Ace 5 Pro"、5G 不会被拆散。
     */
    fun prettyOnePlusModel(code: String): String {
        val dictionary = listOf(
            "NORDCE" to "Nord CE",
            "ACERACE" to "Ace Race",
            "ACEPRO" to "Ace Pro",
            "NORD" to "Nord",
            "ACE" to "Ace",
            "PADGO" to "Pad Go",
            "PAD" to "Pad",
            "TURBO" to "Turbo",
            "OPEN" to "Open",
            "PRO" to "Pro",
            "RACE" to "Race",
            "ULTRA" to "Ultra",
            "LITE" to "Lite",
            "CE" to "CE",
            "GO" to "Go",
            "SE" to "SE",
            "MT" to "MT",
            "5G" to "5G",
        )
        val source = code.uppercase()
        val tokens = mutableListOf<String>()
        var index = 0
        while (index < source.length) {
            val char = source[index]
            if (char.isDigit()) {
                var end = index
                while (end < source.length && source[end].isDigit()) end++
                tokens.add(source.substring(index, end))
                index = end
                continue
            }
            // NORDN30SE5GV → Nord / N / 30 / SE / 5G / V，把 N 单独拆出来才能和 30 贴住
            if (source.startsWith("NORDN", index)) {
                tokens.add("Nord")
                tokens.add("N")
                index += 5
                continue
            }
            val hit = dictionary.firstOrNull { source.startsWith(it.first, index) }
            if (hit != null) {
                tokens.add(hit.second)
                index += hit.first.length
                continue
            }
            tokens.add(char.toString())
            index++
        }
        val suffixes = setOf("T", "R", "V", "X", "S", "G", "E", "A")
        val merged = mutableListOf<String>()
        for (token in tokens) {
            val last = merged.lastOrNull()
            if (last != null && token.firstOrNull()?.isDigit() == true &&
                (last == "CE" || last == "N" || last == "MT")
            ) {
                merged[merged.size - 1] = last + token
                continue
            }
            if (last != null && last.all { it.isDigit() } &&
                token.length == 1 && suffixes.contains(token)
            ) {
                merged[merged.size - 1] = last + token
                continue
            }
            merged.add(token)
        }
        return "OnePlus " + merged.joinToString(" ")
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull
}
