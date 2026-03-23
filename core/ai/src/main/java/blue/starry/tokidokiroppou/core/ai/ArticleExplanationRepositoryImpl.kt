package blue.starry.tokidokiroppou.core.ai

import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheDao
import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheEntity
import blue.starry.tokidokiroppou.core.ai.di.Grounded
import blue.starry.tokidokiroppou.core.ai.di.Plain
import blue.starry.tokidokiroppou.core.domain.model.Article
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleExplanationRepositoryImpl @Inject constructor(
    @Grounded private val groundedModel: GenerativeModel,
    @Plain private val plainModel: GenerativeModel,
    private val cacheDao: ExplanationCacheDao,
) : ArticleExplanationRepository {

    override fun explainArticle(article: Article, forceRefresh: Boolean): Flow<String> = flow {
        // キャッシュを確認 (7日間有効)
        if (!forceRefresh) {
            val cached = cacheDao.get(
                lawCode = article.lawCode.name,
                articleNumber = article.articleNumber,
                supplementaryProvisionLabel = article.supplementaryProvisionLabel ?: "",
                minTimestamp = System.currentTimeMillis() - CACHE_TTL_MS,
            )
            if (cached != null) {
                emit(cached.explanation)
                return@flow
            }
        }

        // グラウンディング付きで生成を試みる
        val prompt = buildPrompt(article)
        var accumulated = generateWithModel(groundedModel, prompt)

        // グラウンディング付きで空応答の場合、グラウンディングなしでフォールバック
        if (accumulated.isEmpty()) {
            Timber.w("Grounded model returned empty response, falling back to plain model")
            accumulated = generateWithModel(plainModel, prompt)
        }

        // ストリーミング結果を emit
        if (accumulated.isNotEmpty()) {
            emit(accumulated)
            saveToCache(article, accumulated)
        }
    }

    /**
     * 指定したモデルでストリーミング生成を実行し、全文を返す。
     * エラー時は空文字列を返す。
     */
    private suspend fun generateWithModel(model: GenerativeModel, prompt: String): String {
        return try {
            val result = StringBuilder()
            model.generateContentStream(prompt).collect { response ->
                response.text?.let { result.append(it) }
            }
            result.toString()
        } catch (e: Exception) {
            Timber.w(e, "GenerativeModel failed")
            ""
        }
    }

    private suspend fun saveToCache(article: Article, explanation: String) {
        cacheDao.upsert(
            ExplanationCacheEntity(
                lawCode = article.lawCode.name,
                articleNumber = article.articleNumber,
                supplementaryProvisionLabel = article.supplementaryProvisionLabel ?: "",
                explanation = explanation,
                createdAt = System.currentTimeMillis(),
            ),
        )
        cacheDao.deleteExpired(System.currentTimeMillis() - CACHE_TTL_MS)
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

    companion object {
        // キャッシュの有効期限: 7日間
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
