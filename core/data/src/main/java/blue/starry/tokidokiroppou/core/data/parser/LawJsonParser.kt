package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class LawParseResult(
    val articles: List<Article>,
)

@Singleton
class LawJsonParser @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String, lawCode: LawCode): LawParseResult {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val lawFullText = root["law_full_text"]?.jsonObject ?: return LawParseResult(emptyList())

        val articles = mutableListOf<Article>()
        collectArticles(lawFullText, lawCode, articles)
        return LawParseResult(articles = articles)
    }

    private fun collectArticles(node: JsonObject, lawCode: LawCode, articles: MutableList<Article>) {
        val tag = node["tag"]?.jsonPrimitive?.content ?: return

        if (tag == "Article") {
            parseArticle(node, lawCode)?.let { articles.add(it) }
            return
        }

        val children = node["children"]?.jsonArray ?: return
        for (child in children) {
            if (child is JsonObject) {
                collectArticles(child, lawCode, articles)
            }
        }
    }

    private fun parseArticle(node: JsonObject, lawCode: LawCode): Article? {
        val num = node["attr"]?.jsonObject?.get("Num")?.jsonPrimitive?.content ?: ""
        val children = node["children"]?.jsonArray ?: return null

        var articleTitle = ""
        var articleCaption = ""
        val paragraphs = mutableListOf<Article.Paragraph>()
        var paragraphNum = 0

        for (child in children) {
            if (child !is JsonObject) continue
            when (child["tag"]?.jsonPrimitive?.content) {
                "ArticleTitle" -> articleTitle = extractText(child)
                "ArticleCaption" -> articleCaption = extractText(child)
                "Paragraph" -> {
                    paragraphNum++
                    parseParagraph(child, paragraphNum, paragraphs)
                }
            }
        }

        if (articleTitle.isEmpty() || paragraphs.isEmpty()) return null

        // 「削除」のみの条文を除外
        if (paragraphs.size == 1 && paragraphs.first().text.trim() == "削除") return null

        return Article(
            lawCode = lawCode,
            articleNumber = num,
            articleTitle = articleTitle,
            articleCaption = articleCaption,
            paragraphs = paragraphs,
        )
    }

    private fun parseParagraph(node: JsonObject, num: Int, paragraphs: MutableList<Article.Paragraph>) {
        val children = node["children"]?.jsonArray ?: return
        val sentenceText = StringBuilder()
        val items = mutableListOf<String>()

        for (child in children) {
            if (child !is JsonObject) continue
            when (child["tag"]?.jsonPrimitive?.content) {
                "ParagraphSentence" -> sentenceText.append(extractSentenceText(child))
                "Item" -> parseItem(child)?.let { items.add(it) }
            }
        }

        val text = sentenceText.toString()
        if (text.isEmpty() || text.trim() == "略") return

        val fullText = if (items.isNotEmpty()) {
            text + items.joinToString("") { "\n　$it" }
        } else {
            text
        }

        paragraphs.add(Article.Paragraph(number = num, text = fullText))
    }

    private fun parseItem(node: JsonObject): String? {
        val children = node["children"]?.jsonArray ?: return null
        var title = ""
        val sentenceText = StringBuilder()

        for (child in children) {
            if (child !is JsonObject) continue
            when (child["tag"]?.jsonPrimitive?.content) {
                "ItemTitle" -> title = extractText(child)
                "ItemSentence" -> sentenceText.append(extractSentenceText(child))
            }
        }

        val text = sentenceText.toString()
        if (text.isEmpty() || text.trim() == "略") return null

        return "$title　$text"
    }

    private fun extractSentenceText(node: JsonObject): String {
        val children = node["children"]?.jsonArray ?: return ""
        return children.joinToString("") { child ->
            if (child is JsonObject && child["tag"]?.jsonPrimitive?.content == "Sentence") {
                extractText(child)
            } else if (child is JsonPrimitive) {
                child.content
            } else {
                ""
            }
        }
    }

    private fun extractText(node: JsonElement): String {
        return when (node) {
            is JsonPrimitive -> node.content
            is JsonArray -> node.joinToString("") { extractText(it) }
            is JsonObject -> {
                val children = node["children"]?.jsonArray ?: return ""
                children.joinToString("") { extractText(it) }
            }
            else -> ""
        }
    }
}
