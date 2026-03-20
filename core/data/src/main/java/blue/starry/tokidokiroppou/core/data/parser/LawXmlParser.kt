package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

data class LawParseResult(
    val lawNum: String?,
    val promulgationDate: String?,
    val articles: List<Article>,
)

@Singleton
class LawXmlParser @Inject constructor() {

    fun parseLaw(xml: String, lawCode: LawCode): LawParseResult {
        val articles = mutableListOf<Article>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var lawNum: String? = null
        var promulgationDate: String? = null
        var insideLawNum = false
        var lawNumText = StringBuilder()

        var currentArticleNumber: String? = null
        var currentArticleTitle: String? = null
        var currentArticleCaption: String? = null
        var currentParagraphs = mutableListOf<Article.Paragraph>()
        var currentParagraphNum = 0
        var currentText = StringBuilder()
        var insideArticle = false
        var insideParagraphSentence = false
        var insideArticleTitle = false
        var insideArticleCaption = false
        var insideItemSentence = false
        var insideItemTitle = false
        var currentItemTitle = ""
        var currentItemText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Law" -> {
                            if (promulgationDate == null) {
                                val era = parser.getAttributeValue(null, "Era")
                                val year = parser.getAttributeValue(null, "Year")
                                val month = parser.getAttributeValue(null, "PromulgateMonth")
                                val day = parser.getAttributeValue(null, "PromulgateDay")
                                if (era != null && year != null && month != null && day != null) {
                                    promulgationDate = formatPromulgationDate(era, year, month, day)
                                }
                            }
                        }
                        "LawNum" -> {
                            if (lawNum == null) {
                                insideLawNum = true
                                lawNumText = StringBuilder()
                            }
                        }
                        "Article" -> {
                            insideArticle = true
                            currentArticleNumber = parser.getAttributeValue(null, "Num") ?: ""
                            currentArticleTitle = null
                            currentArticleCaption = null
                            currentParagraphs = mutableListOf()
                            currentParagraphNum = 0
                        }
                        "ArticleTitle" -> {
                            if (insideArticle) {
                                insideArticleTitle = true
                                currentText = StringBuilder()
                            }
                        }
                        "ArticleCaption" -> {
                            if (insideArticle) {
                                insideArticleCaption = true
                                currentText = StringBuilder()
                            }
                        }
                        "Paragraph" -> {
                            if (insideArticle) {
                                currentParagraphNum++
                                currentText = StringBuilder()
                            }
                        }
                        "ParagraphSentence" -> {
                            if (insideArticle) {
                                insideParagraphSentence = true
                                currentText = StringBuilder()
                            }
                        }
                        "Item" -> {
                            if (insideArticle) {
                                currentItemTitle = ""
                                currentItemText = StringBuilder()
                            }
                        }
                        "ItemTitle" -> {
                            if (insideArticle) {
                                insideItemTitle = true
                                currentText = StringBuilder()
                            }
                        }
                        "ItemSentence" -> {
                            if (insideArticle) {
                                insideItemSentence = true
                                currentText = StringBuilder()
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when {
                            insideLawNum -> lawNumText.append(text)
                            insideArticleTitle -> currentText.append(text)
                            insideArticleCaption -> currentText.append(text)
                            insideParagraphSentence -> currentText.append(text)
                            insideItemTitle -> currentText.append(text)
                            insideItemSentence -> currentItemText.append(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "LawNum" -> {
                            if (insideLawNum) {
                                lawNum = lawNumText.toString().ifEmpty { null }
                                insideLawNum = false
                            }
                        }
                        "Article" -> {
                            if (insideArticle && currentArticleTitle != null && currentParagraphs.isNotEmpty()) {
                                // 「削除」のみの条文を除外
                                val isDeleted = currentParagraphs.size == 1 &&
                                    currentParagraphs.first().text.trim() == "削除"
                                if (!isDeleted) {
                                    articles.add(
                                        Article(
                                            lawCode = lawCode,
                                            articleNumber = currentArticleNumber ?: "",
                                            articleTitle = currentArticleTitle ?: "",
                                            articleCaption = currentArticleCaption ?: "",
                                            paragraphs = currentParagraphs.toList(),
                                        )
                                    )
                                }
                            }
                            insideArticle = false
                        }
                        "ArticleTitle" -> {
                            if (insideArticleTitle) {
                                currentArticleTitle = currentText.toString()
                                insideArticleTitle = false
                            }
                        }
                        "ArticleCaption" -> {
                            if (insideArticleCaption) {
                                currentArticleCaption = currentText.toString()
                                insideArticleCaption = false
                            }
                        }
                        "ParagraphSentence" -> {
                            if (insideParagraphSentence) {
                                val text = currentText.toString()
                                if (text.isNotEmpty() && text.trim() != "略") {
                                    currentParagraphs.add(
                                        Article.Paragraph(
                                            number = currentParagraphNum,
                                            text = text,
                                        )
                                    )
                                }
                                insideParagraphSentence = false
                            }
                        }
                        "ItemTitle" -> {
                            if (insideItemTitle) {
                                currentItemTitle = currentText.toString()
                                insideItemTitle = false
                            }
                        }
                        "ItemSentence" -> {
                            if (insideItemSentence) {
                                val text = currentItemText.toString()
                                if (text.isNotEmpty() && text.trim() != "略") {
                                    val lastParagraph = currentParagraphs.lastOrNull()
                                    if (lastParagraph != null) {
                                        currentParagraphs[currentParagraphs.lastIndex] =
                                            lastParagraph.copy(
                                                text = "${lastParagraph.text}\n　$currentItemTitle　$text"
                                            )
                                    }
                                }
                                insideItemSentence = false
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return LawParseResult(
            lawNum = lawNum,
            promulgationDate = promulgationDate,
            articles = articles,
        )
    }

    private fun formatPromulgationDate(era: String, year: String, month: String, day: String): String {
        val eraName = when (era) {
            "Meiji" -> "明治"
            "Taisho" -> "大正"
            "Showa" -> "昭和"
            "Heisei" -> "平成"
            "Reiwa" -> "令和"
            else -> era
        }
        val y = year.toIntOrNull() ?: return ""
        val m = month.toIntOrNull() ?: return ""
        val d = day.toIntOrNull() ?: return ""
        return "${eraName}${y}年${m}月${d}日"
    }
}
