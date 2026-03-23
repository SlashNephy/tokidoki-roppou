package blue.starry.tokidokiroppou.core.ai

import blue.starry.tokidokiroppou.core.domain.model.Article
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleExplanationRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
) : ArticleExplanationRepository {

    override fun explainArticle(article: Article): Flow<String> = flow {
        val prompt = buildPrompt(article)
        generativeModel.generateContentStream(prompt).collect { response ->
            val text = response.text
            if (text != null) {
                emit(text)
            }
        }
    }

    private fun buildPrompt(article: Article): String = buildString {
        appendLine("あなたは日本の法律に精通した専門家です。")
        appendLine("次の【条文】について、一般の人にも理解できるよう「概要」「条文の意図・ポイント」「日常生活との関連性」のセクションごとにそれぞれ300文字程度で解説してください。")
        appendLine("セクションの見出しとその内容のみが出力されるようにしてください。")
        appendLine()
        appendLine("【条文】")
        appendLine("${article.lawCode.displayName} ${article.displayTitle}")
        append(article.fullText)
    }
}
