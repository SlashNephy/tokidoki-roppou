package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleEntityMapperTest {
    @Test
    fun toDomainMapsLawIdStringBackToLawCode() {
        val entity = ArticleEntity(
            lawCode = "129AC0000000089",
            articleNumber = "1",
            articleTitle = "第1条",
            articleCaption = "",
            paragraphsJson = """[{"number":1,"text":"本文"}]""",
        )

        assertEquals(LawCode.CIVIL_CODE, entity.toDomain()?.lawCode)
    }

    @Test
    fun toEntityStoresLawIdString() {
        val article = Article(
            lawCode = LawCode.CIVIL_CODE,
            articleNumber = "1",
            articleTitle = "第1条",
            articleCaption = "",
            paragraphs = listOf(Article.Paragraph(number = 1, text = "本文")),
        )

        assertEquals("129AC0000000089", article.toEntity().lawCode)
    }
}
