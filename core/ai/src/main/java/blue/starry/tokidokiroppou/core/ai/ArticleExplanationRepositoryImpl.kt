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
        appendLine("以下の条文を、法律の専門家ではない一般の方にも理解できるよう、わかりやすく解説してください。")
        appendLine("条文の目的、重要なポイント、日常生活への関連性を含めて説明してください。")
        appendLine()
        appendLine("【法令名】${article.lawCode.displayName}")
        appendLine("【条文】${article.displayTitle}")
        appendLine("【本文】")
        append(article.fullText)
    }
}
