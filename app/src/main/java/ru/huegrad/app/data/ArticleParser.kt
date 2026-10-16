package ru.huegrad.app.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

sealed interface ArticleBlock {
    data class Heading(val level: Int, val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class Bullet(val text: String) : ArticleBlock
    data class Image(val url: String, val alt: String) : ArticleBlock
}

/** Загружает страницу сайта и превращает её в список нативных блоков. */
object ArticleParser {
    private val navHrefs = listOf(
        "index.html", "drochka.html", "mods/", "news.html", "history.html",
        "obrazovanie.html", "pizdjuk.html", "forum.html",
    )

    fun load(url: String): Result<List<ArticleBlock>> = runCatching {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Linux; Android) HuegradApp")
            .timeout(15_000)
            .get()
        doc.select("nav, header, footer, script, style, form, audio, button, noscript, iframe").remove()

        val blocks = mutableListOf<ArticleBlock>()
        for (el in doc.body().select("h1, h2, h3, h4, p, li, img")) {
            when (el.tagName()) {
                "img" -> {
                    val src = el.absUrl("src")
                    if (src.isNotBlank() && (blocks.lastOrNull() as? ArticleBlock.Image)?.url != src) {
                        blocks += ArticleBlock.Image(src, el.attr("alt"))
                    }
                }
                "h1", "h2", "h3", "h4" -> {
                    val t = el.text().trim()
                    if (t.isNotEmpty() && !isNavLink(el)) blocks += ArticleBlock.Heading(el.tagName()[1].digitToInt(), t)
                }
                else -> {
                    if (el.children().any { it.tagName() in setOf("p", "li", "ul", "ol") }) continue
                    val t = el.text().trim()
                    if (t.isEmpty() || isNavLink(el)) continue
                    blocks += if (el.tagName() == "li") ArticleBlock.Bullet(t) else ArticleBlock.Paragraph(t)
                }
            }
        }
        blocks
    }

    private fun isNavLink(el: Element): Boolean {
        val links = el.select("a")
        if (links.size != 1 || links.first()!!.text().trim() != el.text().trim()) return false
        val href = links.first()!!.attr("href")
        return navHrefs.any { href.endsWith(it) }
    }
}
