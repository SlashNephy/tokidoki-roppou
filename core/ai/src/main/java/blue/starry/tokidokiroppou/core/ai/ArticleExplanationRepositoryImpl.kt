package blue.starry.tokidokiroppou.core.ai

import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheDao
import blue.starry.tokidokiroppou.core.ai.di.AiConstants
import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheEntity
import blue.starry.tokidokiroppou.core.domain.model.Article
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleExplanationRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val cacheDao: ExplanationCacheDao,
) : ArticleExplanationRepository {

    override val modelName: String = AiConstants.MODEL_NAME

    override fun explainArticle(article: Article, forceRefresh: Boolean): Flow<String> = flow {
        // キャッシュを確認 (7日間有効)
        if (!forceRefresh) {
            val cached = cacheDao.get(
                lawCode = article.lawCode.name,
                articleNumber = article.articleNumber,
                supplementaryProvisionLabel = article.supplementaryProvisionLabel ?: "",
                modelName = modelName,
                minTimestamp = System.currentTimeMillis() - CACHE_TTL_MS,
            )
            if (cached != null) {
                emit(cached.explanation)
                return@flow
            }
        }

        // キャッシュミスまたは強制リフレッシュ: API で生成
        val accumulated = StringBuilder()
        generativeModel.generateContentStream(buildPrompt(article)).collect { response ->
            val text = response.text
            if (text != null) {
                accumulated.append(text)
                emit(text)
            }
        }

        // 生成結果をキャッシュに保存
        if (accumulated.isNotEmpty()) {
            cacheDao.upsert(
                ExplanationCacheEntity(
                    lawCode = article.lawCode.name,
                    articleNumber = article.articleNumber,
                    supplementaryProvisionLabel = article.supplementaryProvisionLabel ?: "",
                    modelName = modelName,
                    explanation = accumulated.toString(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
            // 期限切れのキャッシュを掃除
            cacheDao.deleteExpired(System.currentTimeMillis() - CACHE_TTL_MS)
        }
    }

    private fun buildPrompt(article: Article): String = buildString {
        appendLine("あなたは日本の法律に精通した専門家です。")
        appendLine("次の【条文】について、一般の人にも理解できるよう「概要」「条文の意図・ポイント」「日常生活との関連性」のセクションごとにそれぞれ300文字程度で解説してください。")
        appendLine("セクションの見出し (h1) とその内容のみが出力されるようにしてください。")
        appendLine("「日常生活との関連性」では具体例をあげ、仮想的な人物を用いて説明することも効果的です。")
        appendLine("テキストは Markdown 記法で出力し、重要な箇所には太字や斜体を使用してください。太字マーカー (**) の前後には必ず半角スペースを入れてください。")
        appendLine()
        appendLine("【条文】")
        appendLine("${article.lawCode.displayName} ${article.displayTitle}")
        append(article.fullText)
    }

    companion object {
        // キャッシュの有効期限: 7日間
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
