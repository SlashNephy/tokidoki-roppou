package blue.starry.tokidokiroppou.core.ai

import blue.starry.tokidokiroppou.core.domain.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 条文の AI 解説を生成するリポジトリ。
 * ストリーミングで解説テキストの断片を返す。
 */
interface ArticleExplanationRepository {
    /**
     * 指定した条文の解説を生成し、テキストの断片を逐次返す。
     * エラー時は Flow 内で例外をスローする。
     */
    fun explainArticle(article: Article): Flow<String>
}
