package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
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
    val headings: List<StructureHeading>,
    /** 条文に対応する orderIndex のマップ (articleNumber -> orderIndex) */
    val articleOrderIndices: Map<String, Int>,
)

@Singleton
class LawJsonParser @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    // 構造ノードのタグ名と対応する見出しタグ・レベルの対応表
    private val structureTagMap = mapOf(
        "Part" to ("PartTitle" to StructureHeading.Level.Part),
        "Chapter" to ("ChapterTitle" to StructureHeading.Level.Chapter),
        "Section" to ("SectionTitle" to StructureHeading.Level.Section),
        "Subsection" to ("SubsectionTitle" to StructureHeading.Level.Subsection),
        "Division" to ("DivisionTitle" to StructureHeading.Level.Division),
    )

    fun parse(jsonString: String, lawCode: LawCode): LawParseResult {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val lawFullText = root["law_full_text"]?.jsonObject
            ?: return LawParseResult(emptyList(), emptyList(), emptyMap())

        val articles = mutableListOf<Article>()
        val headings = mutableListOf<StructureHeading>()
        val articleOrderIndices = mutableMapOf<String, Int>()
        val orderCounter = intArrayOf(0) // 可変カウンター

        collectArticles(lawFullText, lawCode, articles, headings, articleOrderIndices, orderCounter)
        return LawParseResult(
            articles = articles,
            headings = headings,
            articleOrderIndices = articleOrderIndices,
        )
    }

    private fun collectArticles(
        node: JsonObject,
        lawCode: LawCode,
        articles: MutableList<Article>,
        headings: MutableList<StructureHeading>,
        articleOrderIndices: MutableMap<String, Int>,
        orderCounter: IntArray,
        supplementaryProvisionLabel: String? = null,
    ) {
        val tag = node["tag"]?.jsonPrimitive?.content ?: return

        // 条文ノードの場合
        if (tag == "Article") {
            parseArticle(node, lawCode, supplementaryProvisionLabel)?.let { article ->
                val index = orderCounter[0]++
                articles.add(article)
                // 附則の条文は附則ラベル付きでキーを区別する
                val key = if (article.supplementaryProvisionLabel != null) {
                    "${article.supplementaryProvisionLabel}:${article.articleNumber}"
                } else {
                    article.articleNumber
                }
                articleOrderIndices[key] = index
            }
            return
        }

        // 構造ノード（Part, Chapter, Section 等）の場合、見出しを収集
        val structureInfo = structureTagMap[tag]
        if (structureInfo != null) {
            val (titleTag, level) = structureInfo
            val children = node["children"]?.jsonArray
            if (children != null) {
                for (child in children) {
                    if (child is JsonObject && child["tag"]?.jsonPrimitive?.content == titleTag) {
                        val titleText = extractText(child)
                        if (titleText.isNotEmpty()) {
                            headings.add(
                                StructureHeading(
                                    lawCode = lawCode,
                                    title = titleText,
                                    level = level,
                                    orderIndex = orderCounter[0]++,
                                )
                            )
                        }
                        break
                    }
                }
            }
        }

        // 附則ノードの見出しを収集
        if (tag == "SupplProvision") {
            val children = node["children"]?.jsonArray
            val supplTitle = children?.firstOrNull { child ->
                child is JsonObject && child["tag"]?.jsonPrimitive?.content == "SupplProvisionLabel"
            }
            val titleText = if (supplTitle is JsonObject) {
                extractText(supplTitle)
            } else {
                "附則"
            }
            headings.add(
                StructureHeading(
                    lawCode = lawCode,
                    title = titleText,
                    level = StructureHeading.Level.SupplementaryProvision,
                    orderIndex = orderCounter[0]++,
                )
            )
        }

        val label = if (tag == "SupplProvision") {
            node["attr"]?.jsonObject?.get("AmendLawNum")?.jsonPrimitive?.content
        } else {
            supplementaryProvisionLabel
        }

        val children = node["children"]?.jsonArray ?: return
        for (child in children) {
            if (child is JsonObject) {
                collectArticles(child, lawCode, articles, headings, articleOrderIndices, orderCounter, label)
            }
        }
    }

    private fun parseArticle(node: JsonObject, lawCode: LawCode, supplementaryProvisionLabel: String? = null): Article? {
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
            supplementaryProvisionLabel = supplementaryProvisionLabel,
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
                val tag = node["tag"]?.jsonPrimitive?.content
                // ルビの読み仮名 (Rt) は除外し、本文 (Rb) のみ抽出する
                if (tag == "Rt") return ""
                val children = node["children"]?.jsonArray ?: return ""
                children.joinToString("") { extractText(it) }
            }
            else -> ""
        }
    }
}
