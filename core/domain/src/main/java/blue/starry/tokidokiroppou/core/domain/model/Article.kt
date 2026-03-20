package blue.starry.tokidokiroppou.core.domain.model

data class Article(
    val lawCode: LawCode,
    val articleNumber: String,
    val articleTitle: String,
    val articleCaption: String,
    val paragraphs: List<Paragraph>,
) {
    data class Paragraph(
        val number: Int,
        val text: String,
    )

    val displayTitle: String
        get() = if (articleCaption.isNotEmpty()) {
            "$articleTitle $articleCaption"
        } else {
            "$articleTitle"
        }

    val fullText: String
        get() = paragraphs.joinToString("\n") { paragraph ->
            if (paragraph.number <= 1) {
                paragraph.text
            } else {
                "${paragraph.number}　${paragraph.text}"
            }
        }
}
