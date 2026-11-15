package safe.kernel.flash.ui.screens.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 云 G(O)KI 仓库两个接口的解析规则回归测试。
 *
 * 样例数据全部取自线上真实返回（2026-11-15 抓的 product 分支清单与社区 JSON），
 * 覆盖 MIXKI 三种文件名形态（带 GKI_ 前缀 / 带 MTK_ 前缀 / 都不带）和 OORB 机型包。
 */
class RepoParserTest {

    private val rawPrefix = RepoParser.RAW_PREFIX

    private val officialPaths = listOf(
        "AnyKernel3_Official/MIXKI/6.1.x/KSU-Next/AK3_KSUNext_6.1.118_33548_NOSUSFS_KPN_LZ4_DS_IN_SSG_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.1.x/KSU-Next/AK3_KSUNext_6.1.57_33548_NOSUSFS_KPN_LZ4_DS_IN_SSG_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.1.x/KSU-Next/AK3_MTK_KSUNext_6.1.115_33548_NOSUSFS_KPN_LZ4_DS_IN_SSG_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.6.x/BakaSU/AK3_BAKASU_6.6.30_35259_SUSFS_KPN_LZ4_DS_IN_ADIOS_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.6.x/BakaSU/AK3_BAKASU_6.6.118_35259_SUSFS_KPN_LZ4_DS_IN_ADIOS_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.6.x/BakaSU/AK3_MTK_BAKASU_6.6.89_35259_SUSFS_KPN_LZ4_DS_IN_ADIOS_BBG.zip",
        "AnyKernel3_Official/MIXKI/6.12.x/BakaSU/AK3_GKI_BAKASU_6.12.69_35259_SUSFS_KPN_LZ4_DS_IN_ADIOS_BBG.zip",
        "AnyKernel3_Official/OORB/BakaSU/AK3_BAKASU_ONEP13T_35259_SUSFS_KPM_BBG_IN_FIX_HMBRID_DS.zip",
        "AnyKernel3_Official/OORB/BakaSU/AK3_BAKASU_ONEPACE5PRO_35259_SUSFS_KPM_BBG_IN_FIX_DS.zip",
        "AnyKernel3_Official/OORB/SukiSU/AK3_SUKISU_ONEP13R_41179_SUSFS_KPM_BBG_IN_FIX_DS.zip",
        "AnyKernel3_Official/OORB/SukiSU/AK3_SUKISU_ONEPNORD3_41179_SUSFS_KPM_BBG_IN_FIX_DS.zip",
    )

    private val officialSample = officialPaths.joinToString("\n") { rawPrefix + it }

    private val communitySample = """
        [
          {
            "id": 18,
            "filename": "AK3_OKI-6.12.58_Bacillusf_None_0000_DroidSpace-UFW-Support-NTsync.zip",
            "device": "OKI-6.12.58",
            "manager": "None",
            "manager_version": "0000",
            "features": ["DroidSpace", "UFW-Support", "NTsync"],
            "size": 20386655,
            "size_human": "19.4 MB",
            "sha256": "dcc92b40b7f0466d179bd1d7be47fe3b24cc8659392d11710d932bf34af68aa5",
            "author": {
              "login": "Bacillusf",
              "url": "https://github.com/Bacillusf",
              "avatar": "https://avatars.githubusercontent.com/u/266071904?v=4"
            },
            "download_url": "https://raw.githubusercontent.com/Bacillusf/RKF-Resource-Repo/product/AnyKernel3_community/AK3_OKI-6.12.58_Bacillusf_None_0000_DroidSpace-UFW-Support-NTsync.zip",
            "github_url": "https://github.com/Bacillusf/RKF-Resource-Repo/blob/product/AnyKernel3_community/AK3_OKI-6.12.58_Bacillusf_None_0000_DroidSpace-UFW-Support-NTsync.zip",
            "notes": "仅适用于OPPO/Realme/OnePlus内核版本为6.12.58的机型使用",
            "created_at": "2026-11-14T16:48:26+08:00",
            "published_at": "2026-11-14T16:48:35+08:00"
          },
          {
            "id": 13,
            "filename": "AK3_OKI-6.6.66_Bacillusf_None_0000_UFW-Support-NTsync-DroidSpace.zip",
            "device": "OKI-6.6.66",
            "manager": "BakaSU",
            "manager_version": "35259",
            "features": [],
            "size_human": "17.8 MB",
            "sha256": "aa",
            "author": {"login": "someone", "url": "https://github.com/someone"},
            "download_url": "https://raw.githubusercontent.com/Bacillusf/RKF-Resource-Repo/product/AnyKernel3_community/example.zip",
            "github_url": "https://github.com/Bacillusf/RKF-Resource-Repo/blob/product/AnyKernel3_community/example.zip",
            "notes": "",
            "created_at": "2026-11-14T16:38:43+08:00",
            "published_at": "2026-11-14T16:38:43+08:00"
          }
        ]
    """.trimIndent()

    @Test
    fun officialSplitsUniversalAndOnePlusByPath() {
        val items = RepoParser.parseOfficial(officialSample)
        assertEquals(11, items.size)
        assertEquals(7, items.count { it.kind == RepoKind.UNIVERSAL })
        assertEquals(4, items.count { it.kind == RepoKind.ONEPLUS })
        assertTrue(items.all { it.source == RepoSource.OFFICIAL })
        assertTrue(items.all { it.url.startsWith(rawPrefix) })
    }

    @Test
    fun officialUniversalKeepsSeriesAndKernelVersion() {
        val items = RepoParser.parseOfficial(officialSample)
        val kernel = items.first { it.filename.contains("BAKASU_6.6.118") }
        assertEquals(RepoKind.UNIVERSAL, kernel.kind)
        assertEquals("6.6.x", kernel.kernelSeries)
        assertEquals("6.6.118", kernel.kernelVersion)
        assertEquals("BakaSU", kernel.manager)
        assertEquals("35259", kernel.managerVer)
        assertNull(kernel.deviceModel)
        // 管理器:版本 是卡片第一颗红胶囊
        assertEquals("BakaSU:35259", kernel.managerVersion)
        // 内核通刷包的第二颗胶囊显示内核版本
        assertEquals("内核 6.6.118", kernel.line2)
    }

    @Test
    fun officialPlatformPrefixesAreNotTreatedAsFeatures() {
        val items = RepoParser.parseOfficial(officialSample)
        // MTK / GKI 前缀不解析成平台、也不进特性 chips（需求：平台不做区分）
        for (item in items) {
            assertFalse(item.filename.contains("MTK_") && item.features.contains("MTK"))
            assertFalse(item.features.contains("GKI"))
            assertFalse(item.features.contains("AK3"))
        }
        val mtk = items.first { it.filename.startsWith("AK3_MTK_KSUNext_6.1.115") }
        assertEquals("KSU-Next", mtk.manager)
        assertEquals("33548", mtk.managerVer)
        assertEquals("6.1.115", mtk.kernelVersion)
        assertEquals(listOf("NOSUSFS", "KPN", "LZ4", "SSG", "DS", "IN", "BBG"), mtk.features)
    }

    @Test
    fun officialOnePlusModelsAreHumanised() {
        val items = RepoParser.parseOfficial(officialSample)
        val onep = items.first { it.filename.contains("ONEP13T") }
        assertEquals(RepoKind.ONEPLUS, onep.kind)
        assertEquals("OnePlus 13T", onep.deviceModel)
        assertEquals("OnePlus 13T", onep.line2)
        // OORB 没有内核版本、也没有内核系列
        assertNull(onep.kernelVersion)
        assertNull(onep.kernelSeries)
        assertEquals(
            "OnePlus Ace 5 Pro",
            items.first { it.filename.contains("ONEPACE5PRO") }.deviceModel
        )
        assertEquals(
            "OnePlus Nord 3",
            items.first { it.filename.contains("ONEPNORD3") }.deviceModel
        )
    }

    @Test
    fun commonFeaturesAreSortedToTheEnd() {
        val items = RepoParser.parseOfficial(officialSample)
        val onep = items.first { it.filename.contains("ONEP13T") }
        assertEquals(
            listOf("SUSFS", "KPM", "HMBRID", "FIX", "DS", "IN", "BBG"),
            onep.features
        )
    }

    @Test
    fun latestIsOnePerSeriesAndManager() {
        val items = RepoParser.parseOfficial(officialSample)
        RepoParser.markLatest(items)
        val latest = items.filter { it.isLatest }
        // (6.1.x, KSU-Next) → 6.1.118 ; (6.6.x, BakaSU) → 6.6.118 ; (6.12.x, BakaSU) → 6.12.69
        assertEquals(3, latest.size)
        assertTrue(latest.all { it.kind == RepoKind.UNIVERSAL })
        assertEquals(
            setOf("6.1.118", "6.6.118", "6.12.69"),
            latest.mapNotNull { it.kernelVersion }.toSet()
        )
        // OnePlus 机型包永远不标最新
        assertTrue(items.filter { it.kind == RepoKind.ONEPLUS }.none { it.isLatest })
    }

    @Test
    fun communityParsesStructuredFields() {
        val items = RepoParser.parseCommunity(communitySample)
        assertEquals(2, items.size)
        val first = items[0]
        assertEquals(RepoSource.COMMUNITY, first.source)
        assertEquals(RepoKind.COMMUNITY, first.kind)
        assertEquals("OKI-6.12.58", first.deviceModel)
        assertEquals("6.12.58", first.kernelVersion)
        // manager = "None" → 视为无管理器，卡片显示「无管理器」
        assertNull(first.manager)
        assertEquals("无管理器", first.managerVersion)
        assertEquals("19.4 MB", first.sizeHuman)
        assertEquals(
            "dcc92b40b7f0466d179bd1d7be47fe3b24cc8659392d11710d932bf34af68aa5",
            first.sha256
        )
        assertEquals("Bacillusf", first.authorLogin)
        assertEquals("https://github.com/Bacillusf", first.authorUrl)
        assertTrue(first.notes!!.startsWith("仅适用于OPPO/Realme/OnePlus"))
        assertEquals("2026-11-14T16:48:35+08:00", first.publishedAt)
        assertNotNull(first.githubUrl)
        assertEquals(
            listOf("DroidSpace", "UFW-Support", "NTsync"),
            first.features
        )
        // 第二个包有真实管理器
        assertEquals("BakaSU", items[1].manager)
        assertEquals("BakaSU:35259", items[1].managerVersion)
    }

    @Test
    fun searchIndexCoversEveryVisibleField() {
        val community = RepoParser.parseCommunity(communitySample)[0]
        for (needle in listOf(
            "oki-6.12.58", "bacillusf", "droidspace", "6.12.58", "社区源", "社区", "仅适用于oppo"
        )) {
            assertTrue("searchBlob 缺少 $needle", community.searchBlob.contains(needle))
        }
        val onep = RepoParser.parseOfficial(officialSample).first { it.filename.contains("ONEP13T") }
        for (needle in listOf("oneplus 13t", "bakasu", "35259", "hmbrid", "官方源", "内核通刷")) {
            if (needle == "内核通刷") continue
            assertTrue("searchBlob 缺少 $needle", onep.searchBlob.contains(needle))
        }
    }

    @Test
    fun prettyModelCoversEveryCodeSpottedOnline() {
        assertEquals("OnePlus 13", RepoParser.prettyOnePlusModel("13"))
        assertEquals("OnePlus 13T", RepoParser.prettyOnePlusModel("13T"))
        assertEquals("OnePlus 11R", RepoParser.prettyOnePlusModel("11R"))
        assertEquals("OnePlus 10 Pro", RepoParser.prettyOnePlusModel("10PRO"))
        assertEquals("OnePlus Ace 5 Pro", RepoParser.prettyOnePlusModel("ACE5PRO"))
        assertEquals("OnePlus Ace 5 Race", RepoParser.prettyOnePlusModel("ACE5RACE"))
        assertEquals("OnePlus Ace 3V", RepoParser.prettyOnePlusModel("ACE3V"))
        assertEquals("OnePlus Ace Race V", RepoParser.prettyOnePlusModel("ACERACEV"))
        assertEquals("OnePlus Nord CE 4 Lite 5G", RepoParser.prettyOnePlusModel("NORDCE4LITE5G"))
        assertEquals("OnePlus Nord CE 6", RepoParser.prettyOnePlusModel("NORDCE6"))
        assertEquals("OnePlus Nord N30 SE 5G V", RepoParser.prettyOnePlusModel("NORDN30SE5GV"))
        assertEquals("OnePlus Pad 2 Pro", RepoParser.prettyOnePlusModel("PAD2PRO"))
        assertEquals("OnePlus Pad 2 MT6991", RepoParser.prettyOnePlusModel("PAD2MT6991"))
        assertEquals("OnePlus Pad Go 2", RepoParser.prettyOnePlusModel("PADGO2"))
        assertEquals("OnePlus Turbo 6X", RepoParser.prettyOnePlusModel("TURBO6X"))
        assertEquals("OnePlus Open", RepoParser.prettyOnePlusModel("OPEN"))
    }

    @Test
    fun ranksOrderSeriesAndVersionsNumerically() {
        assertTrue(RepoParser.seriesRank("6.12.x") > RepoParser.seriesRank("6.6.x"))
        assertTrue(RepoParser.seriesRank("6.6.x") > RepoParser.seriesRank("6.1.x"))
        assertTrue(RepoParser.kernelRank("6.6.118") > RepoParser.kernelRank("6.6.89"))
        assertTrue(RepoParser.kernelRank("6.12.23") > RepoParser.kernelRank("6.6.118"))
        assertEquals(-1, RepoParser.kernelRank(null))
        assertEquals(-1, RepoParser.seriesRank(null))
    }

    @Test
    fun unparsableLinesAreSkippedInsteadOfCrashing() {
        val messy = """
            $rawPrefix AnyKernel3_Official/MIXKI/6.6.x/BakaSU/AK3_BAKASU_6.6.89_35259_SUSFS_BBG.zip

            not-a-url
            https://example.com/foo.zip
            $rawPrefix AnyKernel3_Official/UNKNOWN/6.6.x/BakaSU/x.zip
            $rawPrefix AnyKernel3_Official/OORB/BakaSU/readme.txt
        """.trimIndent().replace("$rawPrefix ", rawPrefix)
        val items = RepoParser.parseOfficial(messy)
        assertEquals(1, items.size)
        assertEquals("6.6.x", items[0].kernelSeries)
    }

    @Test
    fun featureLabelsExpandTheThreeOfficialAbbreviations() {
        // 官方源：IN → BetterNet / DS → DroidSpace / LZ4 → Lz4-Zarm，其余 token 原样
        assertEquals("BetterNet", RepoParser.featureLabel("IN"))
        assertEquals("DroidSpace", RepoParser.featureLabel("DS"))
        assertEquals("Lz4-Zarm", RepoParser.featureLabel("LZ4"))
        assertEquals("SUSFS", RepoParser.featureLabel("SUSFS"))
        assertEquals("NOSUSFS", RepoParser.featureLabel("NOSUSFS"))
        assertEquals("SSG", RepoParser.featureLabel("SSG"))
        // 社区源已经是可读名，过一遍映射不会变样
        assertEquals("DroidSpace", RepoParser.featureLabel("DroidSpace"))
        assertEquals("UFW-Support", RepoParser.featureLabel("UFW-Support"))

        val item = RepoParser.parseOfficial(officialSample).first { it.filename.contains("AK3_BAKASU_6.6.118") }
        assertEquals(
            listOf("SUSFS", "KPN", "Lz4-Zarm", "ADIOS", "DroidSpace", "BetterNet", "BBG"),
            item.featureLabels
        )
    }

    @Test
    fun searchIndexAcceptsBothAbbreviationAndLabel() {
        val item = RepoParser.parseOfficial(officialSample).first { it.filename.contains("AK3_BAKASU_6.6.118") }
        // 缩写与展示名都要能搜到
        assertTrue(item.searchBlob.contains(" betternet"))
        assertTrue(item.searchBlob.contains(" droidspace"))
        assertTrue(item.searchBlob.contains(" lz4-zarm"))
        assertTrue(item.searchBlob.contains(" in "))
    }

    @Test
    fun communityInfoCarriesEverythingTheDownloadGateNeeds() {
        // 下载前弹窗要展示：机型 / 管理器 / 特性 / 体积 / 上传者 / 哈希 / 说明 / 时间
        val item = RepoParser.parseCommunity(communitySample)[0]
        assertNotNull(item.deviceModel)
        assertNotNull(item.featureLabels)
        assertNotNull(item.sizeHuman)
        assertNotNull(item.authorLogin)
        assertNotNull(item.sha256)
        assertNotNull(item.notes)
        assertNotNull(item.publishedAt)
        assertEquals(listOf("DroidSpace", "UFW-Support", "NTsync"), item.featureLabels)
    }
}
