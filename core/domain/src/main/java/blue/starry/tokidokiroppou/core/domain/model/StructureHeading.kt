package blue.starry.tokidokiroppou.core.domain.model

/**
 * 法令の構造的な見出し（編・章・節・款・目など）を表すモデル。
 * e-Gov API の PartTitle, ChapterTitle 等に対応する。
 */
data class StructureHeading(
    val lawCode: LawCode,
    val title: String,
    val level: Level,
    val orderIndex: Int,
) {
    /** 法令構造の階層レベル */
    enum class Level(val displayPrefix: String, val depth: Int) {
        /** 編 */
        Part("編", 0),
        /** 章 */
        Chapter("章", 1),
        /** 節 */
        Section("節", 2),
        /** 款 */
        Subsection("款", 3),
        /** 目 */
        Division("目", 4),
        /** 附則 */
        SupplementaryProvision("附則", 0),
    }
}
