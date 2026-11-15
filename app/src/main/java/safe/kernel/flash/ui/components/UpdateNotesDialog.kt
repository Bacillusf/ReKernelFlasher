package safe.kernel.flash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import safe.kernel.flash.R
import safe.kernel.flash.ui.theme.softShadow

/**
 * 更新弹窗：把 GitHub release 的 body（Markdown）渲染出来，**必须滑到底部**「更新」才可点。
 * 点「更新」后由调用方启动下载流程（首页走 startDownload，设置页走 startSettingsUpdateDownload）。
 */
@Composable
fun UpdateNotesDialog(
    visible: Boolean,
    version: String,
    notes: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val scrollState = rememberScrollState()
    val reachedBottom by remember {
        derivedStateOf {
            // 内容不满一屏时 maxValue == 0，直接算已读到底
            scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 12
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .softShadow(cornerRadius = 22.dp, alpha = 0.18f, offsetY = 10.dp)
        ) {
            Column(Modifier.heightIn(max = 560.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.update_dialog_title, version),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.update_dialog_notes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.repo_close))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    MarkdownBody(
                        markdown = notes.ifBlank { stringResource(R.string.update_dialog_empty) },
                        textColor = MaterialTheme.colorScheme.onSurface,
                        mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (reachedBottom) {
                            stringResource(R.string.update_gate_done)
                        } else {
                            stringResource(R.string.update_gate_hint)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reachedBottom) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    DialogButton(buttonText = stringResource(R.string.cancel), onClick = onDismiss)
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = reachedBottom,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.update_confirm), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Markdown */

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String, val depth: Int) : MdBlock
    data class Numbered(val marker: String, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class CodeBlock(val lines: List<String>) : MdBlock
    data object Rule : MdBlock
}

private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
private val bulletRegex = Regex("""^(\s*)[-*+]\s+(.*)$""")
private val numberedRegex = Regex("""^(\s*)(\d+[.)])\s+(.*)$""")
private val quoteRegex = Regex("""^>\s?(.*)$""")
private val ruleRegex = Regex("""^\s*([-*_])\1{2,}\s*$""")
private val fenceRegex = Regex("""^\s*```.*$""")
private val inlineRegex = Regex(
    """\*\*(.+?)\*\*|__([^_]+)__|`([^`]+)`|\[([^\]]+)]\(([^)]*)\)|\*([^*]+)\*"""
)

/** 极简 Markdown → 块列表。只覆盖 release notes 实际会用到的语法。 */
internal fun parseMarkdownBlocks(body: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }

    for (rawLine in body.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
        val line = rawLine.trimEnd()
        if (fenceRegex.matches(line)) {
            if (inCode) {
                blocks.add(MdBlock.CodeBlock(trimBlankEdges(code)))
                code.clear()
                inCode = false
            } else {
                flushParagraph()
                inCode = true
            }
            continue
        }
        if (inCode) {
            code.add(line)
            continue
        }
        when {
            line.isBlank() -> flushParagraph()
            ruleRegex.matches(line) -> {
                flushParagraph()
                blocks.add(MdBlock.Rule)
            }
            headingRegex.matches(line) -> {
                flushParagraph()
                val match = headingRegex.find(line)!!
                blocks.add(
                    MdBlock.Heading(
                        level = match.groupValues[1].length,
                        text = match.groupValues[2].trim()
                    )
                )
            }
            bulletRegex.matches(line) -> {
                flushParagraph()
                val match = bulletRegex.find(line)!!
                blocks.add(
                    MdBlock.Bullet(
                        text = match.groupValues[2].trim(),
                        depth = match.groupValues[1].replace("\t", "  ").length / 2
                    )
                )
            }
            numberedRegex.matches(line) -> {
                flushParagraph()
                val match = numberedRegex.find(line)!!
                blocks.add(
                    MdBlock.Numbered(
                        marker = match.groupValues[2],
                        text = match.groupValues[3].trim()
                    )
                )
            }
            quoteRegex.matches(line) -> {
                flushParagraph()
                blocks.add(MdBlock.Quote(quoteRegex.find(line)!!.groupValues[1]))
            }
            else -> paragraph.add(line.trim())
        }
    }
    flushParagraph()
    if (code.isNotEmpty()) blocks.add(MdBlock.CodeBlock(trimBlankEdges(code)))
    return blocks
}

private fun trimBlankEdges(lines: List<String>): List<String> {
    val start = lines.indexOfFirst { it.isNotBlank() }
    if (start < 0) return emptyList()
    val end = lines.indexOfLast { it.isNotBlank() }
    return lines.subList(start, end + 1).toList()
}

@Composable
private fun MarkdownBody(
    markdown: String,
    textColor: Color,
    mutedColor: Color,
    accentColor: Color,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    Spacer(Modifier.height(if (block.level <= 2) 10.dp else 6.dp))
                    Text(
                        text = inlineMarkdown(block.text, accentColor),
                        style = if (block.level <= 2) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(Modifier.height(2.dp))
                }
                is MdBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, accentColor),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
                is MdBlock.Bullet -> Row(Modifier.padding(start = (block.depth * 12).dp, top = 1.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = accentColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = inlineMarkdown(block.text, accentColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
                is MdBlock.Numbered -> Row(Modifier.padding(top = 1.dp)) {
                    Text(
                        text = block.marker,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = inlineMarkdown(block.text, accentColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
                is MdBlock.Quote -> Row(
                    Modifier
                        .padding(vertical = 2.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(accentColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = inlineMarkdown(block.text, accentColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        fontStyle = FontStyle.Italic
                    )
                }
                is MdBlock.CodeBlock -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        for (line in block.lines) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }
                    }
                }
                MdBlock.Rule -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

/** 行内语法：**粗体** / __粗体__ / `代码` / [文字](链接) / *斜体*。 */
internal fun inlineMarkdown(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    for (match in inlineRegex.findAll(text)) {
        if (match.range.first > index) append(text.substring(index, match.range.first))
        val groups = match.groupValues
        when {
            groups[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[1]) }
            groups[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[2]) }
            groups[3].isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            ) { append(groups[3]) }
            groups[4].isNotEmpty() -> withStyle(
                SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)
            ) { append(groups[4]) }
            groups[6].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(groups[6]) }
        }
        index = match.range.last + 1
    }
    if (index < text.length) append(text.substring(index))
}
