package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LawXmlParser @Inject constructor() {

    fun parseArticles(xml: String, lawCode: LawCode): List<Article> {
        val articles = mutableListOf<Article>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

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
        var depth = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
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
                        "Article" -> {
                            if (insideArticle && currentArticleTitle != null && currentParagraphs.isNotEmpty()) {
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
                                if (text.isNotEmpty()) {
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
                                if (text.isNotEmpty()) {
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

        return articles
    }
}
