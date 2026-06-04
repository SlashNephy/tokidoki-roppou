package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlin.test.Test
import kotlin.test.assertEquals

class LawJsonParserTest {
    @Test
    fun parseAssignsLawIdToArticles() {
        val json = """
            {
              "law_full_text": {
                "tag": "Law",
                "children": [
                  {
                    "tag": "Article",
                    "attr": { "Num": "1" },
                    "children": [
                      { "tag": "ArticleTitle", "children": ["第一条"] },
                      {
                        "tag": "Paragraph",
                        "children": [
                          {
                            "tag": "ParagraphSentence",
                            "children": [
                              { "tag": "Sentence", "children": ["本文"] }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val result = LawJsonParser().parse(json, LawId("999AC0000000001"))

        assertEquals(LawId("999AC0000000001"), result.articles.single().lawId)
    }
}
