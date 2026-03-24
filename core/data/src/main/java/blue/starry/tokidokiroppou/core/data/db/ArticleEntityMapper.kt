package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@kotlinx.serialization.Serializable
private data class ParagraphJson(
    val number: Int,
    val text: String,
)

fun Article.toEntity(orderIndex: Int = 0): ArticleEntity {
    val paragraphsJson = json.encodeToString(
        paragraphs.map { ParagraphJson(it.number, it.text) }
    )
    return ArticleEntity(
        lawCode = lawCode.name,
        articleNumber = articleNumber,
        articleTitle = articleTitle,
        articleCaption = articleCaption,
        paragraphsJson = paragraphsJson,
        supplementaryProvisionLabel = supplementaryProvisionLabel,
        orderIndex = orderIndex,
    )
}

fun ArticleEntity.toDomain(): Article? {
    val lawCode = runCatching { LawCode.valueOf(lawCode) }.getOrNull() ?: return null
    val paragraphs = runCatching {
        json.decodeFromString<List<ParagraphJson>>(paragraphsJson)
    }.getOrNull() ?: return null

    return Article(
        lawCode = lawCode,
        articleNumber = articleNumber,
        articleTitle = articleTitle,
        articleCaption = articleCaption,
        paragraphs = paragraphs.map { Article.Paragraph(it.number, it.text) },
        supplementaryProvisionLabel = supplementaryProvisionLabel,
    )
}
