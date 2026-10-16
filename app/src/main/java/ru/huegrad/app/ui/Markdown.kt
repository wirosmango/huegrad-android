package ru.huegrad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ru.huegrad.app.forum.ForumIdentity

/*
 * Лёгкий Markdown для сообщений форума. Набор как на сайте (marked + DOMPurify):
 * **жирный**, *курсив*, `код`, ```блок кода```, [текст](ссылка), # заголовки (1-3),
 * > цитаты, - / 1. списки, ![alt](url) картинки отдельной строкой.
 * Одиночный перенос строки сохраняется (breaks: true). #N ведут на посты, ссылки http(s) кликабельны.
 */

internal sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Items(val ordered: Boolean, val items: List<String>) : MdBlock
    data class Image(val alt: String, val url: String) : MdBlock
}

private val headingRe = Regex("^(#{1,3})\\s+(.+)$")
private val quoteRe = Regex("^>\\s?(.*)$")
private val bulletRe = Regex("^\\s*[-*+]\\s+(.+)$")
private val orderedRe = Regex("^\\s*\\d+[.)]\\s+(.+)$")
private val imageRe = Regex("^!\\[([^\\]]*)]\\((https?://[^)\\s]+)\\)$")

internal fun parseMarkdown(src: String): List<MdBlock> {
    val lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val out = mutableListOf<MdBlock>()
    val para = mutableListOf<String>()
    fun flush() {
        if (para.isNotEmpty()) {
            out += MdBlock.Paragraph(para.joinToString("\n"))
            para.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                flush()
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code += lines[i]
                    i++
                }
                out += MdBlock.Code(code.joinToString("\n"))
                i++ // закрывающая ограда
            }
            trimmed.isEmpty() -> { flush(); i++ }
            headingRe.matches(trimmed) -> {
                flush()
                val m = headingRe.find(trimmed)!!
                out += MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim())
                i++
            }
            imageRe.matches(trimmed) -> {
                flush()
                val m = imageRe.find(trimmed)!!
                out += MdBlock.Image(m.groupValues[1], m.groupValues[2])
                i++
            }
            quoteRe.matches(trimmed) -> {
                flush()
                val q = mutableListOf<String>()
                while (i < lines.size) {
                    val m = quoteRe.find(lines[i].trim()) ?: break
                    q += m.groupValues[1]
                    i++
                }
                out += MdBlock.Quote(q.joinToString("\n"))
            }
            bulletRe.matches(line) || orderedRe.matches(line) -> {
                flush()
                val ordered = orderedRe.matches(line)
                val re = if (ordered) orderedRe else bulletRe
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val m = re.find(lines[i]) ?: break
                    items += m.groupValues[1]
                    i++
                }
                out += MdBlock.Items(ordered, items)
            }
            else -> { para += line; i++ }
        }
    }
    flush()
    return out
}

private class MdStyle(
    val link: TextLinkStyles,
    val codeBg: Color,
    val mention: SpanStyle,
    val mentionMe: SpanStyle, // упоминание текущего пользователя
    val myNick: String?,
)

private enum class Kind { Code, Link, Bold, ItalicStar, ItalicUnderscore, Ref, Mention, Url }

private val inlinePatterns = listOf(
    Regex("`([^`\\n]+)`") to Kind.Code,
    Regex("\\[([^\\]\\n]+)]\\((https?://[^)\\s]+)\\)") to Kind.Link,
    Regex("\\*\\*(.+?)\\*\\*|__(.+?)__") to Kind.Bold,
    Regex("(?<![*\\w])\\*(?![*\\s])(.+?)(?<![\\s*])\\*(?!\\*)") to Kind.ItalicStar,
    Regex("(?<![\\w_])_(?![_\\s])(.+?)(?<![\\s_])_(?![\\w_])") to Kind.ItalicUnderscore,
    Regex("#(\\d+)") to Kind.Ref,
    ForumIdentity.MENTION_RE to Kind.Mention,
    Regex("https?://[^\\s<]+") to Kind.Url,
)

private fun AnnotatedString.Builder.appendInline(text: String, st: MdStyle, onRef: (Int) -> Unit) {
    var pos = 0
    while (pos < text.length) {
        var best: Pair<MatchResult, Kind>? = null
        for ((re, kind) in inlinePatterns) {
            val m = re.find(text, pos) ?: continue
            if (best == null || m.range.first < best.first.range.first) best = m to kind
        }
        if (best == null) {
            append(text.substring(pos))
            return
        }
        val (m, kind) = best
        append(text.substring(pos, m.range.first))
        var end = m.range.last + 1
        when (kind) {
            Kind.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = st.codeBg)) {
                append(m.groupValues[1])
            }
            Kind.Link -> withLink(LinkAnnotation.Url(m.groupValues[2], st.link)) { append(m.groupValues[1]) }
            Kind.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(m.groupValues[1].ifEmpty { m.groupValues[2] }, st, onRef)
            }
            Kind.ItalicStar, Kind.ItalicUnderscore -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInline(m.groupValues[1], st, onRef)
            }
            Kind.Ref -> {
                val n = m.groupValues[1].toIntOrNull()
                if (n == null) append(m.value)
                else withLink(LinkAnnotation.Clickable("ref", st.link) { onRef(n) }) { append(m.value) }
            }
            Kind.Mention -> {
                // @ник или @[Ник с пробелом]; точки и дефисы на конце обычного ника остаются обычным текстом
                val bracket = m.groupValues[1]
                val nick = if (bracket.isNotEmpty()) bracket.trim() else m.groupValues[2].trimEnd('.', '-')
                val shownLength = if (bracket.isNotEmpty()) m.value.length else 1 + nick.length
                val mine = st.myNick != null && nick.equals(st.myNick, ignoreCase = true)
                withStyle(if (mine) st.mentionMe else st.mention) { append("@$nick") }
                end = m.range.first + shownLength
            }
            Kind.Url -> {
                val url = m.value.trimEnd('.', ',', ';', ':', '!', '?', ')')
                withLink(LinkAnnotation.Url(url, st.link)) { append(url) }
                end = m.range.first + url.length
            }
        }
        pos = end
    }
}

@Composable
fun MarkdownText(
    text: String,
    onRefClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    myNick: String? = null,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val scheme = MaterialTheme.colorScheme
    val st = MdStyle(
        link = TextLinkStyles(SpanStyle(color = scheme.primary, textDecoration = TextDecoration.Underline)),
        codeBg = scheme.surfaceVariant,
        mention = SpanStyle(
            color = scheme.primary,
            background = scheme.primary.copy(alpha = 0.12f),
            fontWeight = FontWeight.Medium,
        ),
        mentionMe = SpanStyle(
            color = scheme.onPrimary,
            background = scheme.primary,
            fontWeight = FontWeight.SemiBold,
        ),
        myNick = myNick,
    )
    fun inline(s: String): AnnotatedString = buildAnnotatedString { appendInline(s, st, onRefClick) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(inline(block.text), style = MaterialTheme.typography.bodyLarge)
                is MdBlock.Heading -> Text(
                    inline(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                )
                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(scheme.primary))
                    Text(
                        inline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                is MdBlock.Code -> Surface(shape = MaterialTheme.shapes.small, color = scheme.surfaceVariant) {
                    Text(
                        block.code,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(10.dp),
                    )
                }
                is MdBlock.Items -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    block.items.forEachIndexed { idx, item ->
                        Row {
                            Text(
                                if (block.ordered) "${idx + 1}.  " else "•  ",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(inline(item), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is MdBlock.Image -> AsyncImage(
                    model = block.url,
                    contentDescription = block.alt.ifBlank { null },
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}
