package blue.starry.tokidokiroppou.core.domain.model

/**
 * 法令一覧で表示するコンテンツの1要素。
 * 構造見出し（編・章・節など）または条文のいずれかを表す。
 */
sealed interface LawContentItem {
    val orderIndex: Int

    /** 構造見出し（「第一編　総則」など） */
    data class Heading(
        val heading: StructureHeading,
    ) : LawContentItem {
        override val orderIndex: Int get() = heading.orderIndex
    }

    /** 条文 */
    data class ArticleItem(
        val article: Article,
        override val orderIndex: Int,
    ) : LawContentItem
}
