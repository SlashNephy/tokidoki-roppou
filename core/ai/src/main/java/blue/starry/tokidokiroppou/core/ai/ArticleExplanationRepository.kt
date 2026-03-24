package blue.starry.tokidokiroppou.core.ai

import blue.starry.tokidokiroppou.core.domain.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 条文の AI 解説を生成するリポジトリ。
 * ストリーミングで解説テキストの断片を返す。
 */
interface ArticleExplanationRepository {
    /**
     * 指定した条文の解説を返す。
     * キャッシュが有効な場合はキャッシュから即座に返し、
     * [forceRefresh] が true の場合はキャッシュを無視して再生成する。
     *
     * @return テキストの断片を逐次返す Flow。キャッシュヒット時は1チャンクで全文を返す。
     */
    /** 使用中のモデル ID */
    val modelName: String

    fun explainArticle(article: Article, forceRefresh: Boolean = false): Flow<String>
}
