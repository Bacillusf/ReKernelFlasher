package safe.kernel.flash.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 更新弹窗的 Markdown 渲染回归测试。
 * 样例取自本项目 docs/releases 里 release notes 的实际写法，外加 GitHub 常见的嵌套列表/代码块。
 */
class MarkdownTest {

    private val sample = """
        ## 重点更新

        修复 Android 13 以下因 **液态玻璃组件** 导致的启动闪退。

        - 新增内核压缩包回滚：`操作记录` 新增「刷入内核压缩包」条目
        - 云 G(O)KI 仓库增强
          - 管理器版本红色高亮
          - 最新版本五角星标记

        > 升级前建议先做一次自动备份。

        1. 打开设置
        2. 点击检查更新

        ```bash
        adb install -r app-release.apk
        ```

        ---

        详见 [发布页](https://github.com/Bacillusf/ReKernelFlasher/releases/tag/v2.8)。
    """.trimIndent()

    @Test
    fun parsesEveryBlockKindUsedInReleaseNotes() {
        val blocks = parseMarkdownBlocks(sample)
        assertEquals(1, blocks.count { it is MdBlock.Heading })
        assertEquals(4, blocks.count { it is MdBlock.Bullet })
        assertEquals(1, blocks.count { it is MdBlock.Quote })
        assertEquals(2, blocks.count { it is MdBlock.Numbered })
        assertEquals(1, blocks.count { it is MdBlock.CodeBlock })
        assertEquals(1, blocks.count { it is MdBlock.Rule })

        val heading = blocks.filterIsInstance<MdBlock.Heading>().first()
        assertEquals(2, heading.level)
        assertEquals("重点更新", heading.text)

        val bullets = blocks.filterIsInstance<MdBlock.Bullet>()
        assertEquals("新增内核压缩包回滚：`操作记录` 新增「刷入内核压缩包」条目", bullets[0].text)
        assertEquals(0, bullets[0].depth)
        // 缩进两个空格的子项 → depth 1
        assertEquals(1, bullets[2].depth)
        assertEquals("管理器版本红色高亮", bullets[2].text)

        val numbered = blocks.filterIsInstance<MdBlock.Numbered>()
        assertEquals(listOf("1.", "2."), numbered.map { it.marker })

        val code = blocks.filterIsInstance<MdBlock.CodeBlock>().first()
        assertEquals(listOf("adb install -r app-release.apk"), code.lines)

        val quote = blocks.filterIsInstance<MdBlock.Quote>().first()
        assertEquals("升级前建议先做一次自动备份。", quote.text)
    }

    @Test
    fun paragraphsAreNotSwallowedByNeighbouringBlocks() {
        val blocks = parseMarkdownBlocks(sample)
        val paragraphs = blocks.filterIsInstance<MdBlock.Paragraph>().map { it.text }
        assertTrue(paragraphs.any { it.contains("液态玻璃组件") })
        assertTrue(paragraphs.any { it.contains("详见") })
        // 列表/代码块前后的空行必须切段，否则整篇会挤成一段
        assertEquals(2, paragraphs.size)
    }

    @Test
    fun unclosedFenceStillRendersAsCode() {
        val blocks = parseMarkdownBlocks("说明\n```\nkeep me\n")
        assertEquals(1, blocks.count { it is MdBlock.CodeBlock })
        assertEquals(listOf("keep me"), blocks.filterIsInstance<MdBlock.CodeBlock>().first().lines)
    }

    @Test
    fun emptyNotesProduceNoBlocks() {
        assertTrue(parseMarkdownBlocks("").isEmpty())
        assertTrue(parseMarkdownBlocks("\n\n  \n").isEmpty())
    }

    @Test
    fun inlineFormattingIsStrippedButStyled() {
        val color = Color(0xFF123456)
        val bold = inlineMarkdown("修复 **Android 13** 闪退", color)
        assertEquals("修复 Android 13 闪退", bold.text)
        assertTrue(bold.spanStyles.any { it.item.fontWeight == FontWeight.Bold })

        val code = inlineMarkdown("执行 `dd` 备份", color)
        assertEquals("执行 dd 备份", code.text)

        val link = inlineMarkdown("见 [发布页](https://example.com)", color)
        assertEquals("见 发布页", link.text)
        assertTrue(link.spanStyles.any { it.item.color == color })

        val italic = inlineMarkdown("这是 *斜体*", color)
        assertEquals("这是 斜体", italic.text)
    }

    @Test
    fun plainTextPassesThroughUntouched() {
        val plain = inlineMarkdown("没有任何标记的一行", Color.Unspecified)
        assertEquals("没有任何标记的一行", plain.text)
        assertTrue(plain.spanStyles.isEmpty())
    }
}
