package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleEntityMapperTest {
    @Test
    fun toDomainPreservesLegacyPresetLawIdString() {
        val entity = ArticleEntity(
            lawCode = "129AC0000000089",
            articleNumber = "1",
            articleTitle = "第1条",
            articleCaption = "",
            paragraphsJson = """[{"number":1,"text":"本文"}]""",
        )

        assertEquals(LawId("129AC0000000089"), entity.toDomain()?.lawId)
    }

    @Test
    fun roundTripPreservesLawIdAndOrderIndex() {
        val article = Article(
            lawId = LawId("999AC0000000001"),
            articleNumber = "1",
            articleTitle = "第1条",
            articleCaption = "",
            paragraphs = listOf(Article.Paragraph(number = 1, text = "本文")),
        )

        val entity = article.toEntity(orderIndex = 42)
        val restored = entity.toDomain()

        assertEquals("999AC0000000001", entity.lawCode)
        assertEquals(42, entity.orderIndex)
        assertEquals(LawId("999AC0000000001"), restored?.lawId)
    }
}
