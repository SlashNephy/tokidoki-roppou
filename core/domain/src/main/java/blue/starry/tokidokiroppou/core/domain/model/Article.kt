package blue.starry.tokidokiroppou.core.domain.model

data class Article(
    val lawCode: LawCode,
    val articleNumber: String,
    val articleTitle: String,
    val articleCaption: String,
    val paragraphs: List<Paragraph>,
    val supplementaryProvisionLabel: String? = null,
) {
    data class Paragraph(
        val number: Int,
        val text: String,
    )

    val displayTitle: String
        get() {
            val title = if (articleCaption.isNotEmpty()) {
                "$articleTitle $articleCaption"
            } else {
                articleTitle
            }
            return if (supplementaryProvisionLabel != null) {
                "$supplementaryProvisionLabel 附則$title"
            } else {
                title
            }
        }

    val fullText: String
        get() = paragraphs.joinToString("\n") { paragraph ->
            if (paragraph.number <= 1) {
                paragraph.text
            } else {
                "${paragraph.number}　${paragraph.text}"
            }
        }

    fun displayTitle(useHalfWidthParentheses: Boolean): String {
        val title = displayTitle
        return if (useHalfWidthParentheses) title.normalizeDisplay() else title
    }

    fun fullText(useHalfWidthParentheses: Boolean): String {
        val text = fullText
        return if (useHalfWidthParentheses) text.normalizeDisplay() else text
    }
}
