package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlinx.coroutines.flow.Flow

/**
 * ブックマーク（保存済み条文）のリポジトリ
 */
interface BookmarkRepository {
    /**
     * 全ブックマークを保存日時の降順で取得する
     */
    fun observeAll(): Flow<List<Article>>

    /**
     * 条文がブックマーク済みかどうかを監視する
     */
    fun observeIsBookmarked(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String? = null): Flow<Boolean>

    /**
     * 条文をブックマークに追加する
     */
    suspend fun add(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String? = null)

    /**
     * 条文をブックマークから削除する
     */
    suspend fun remove(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String? = null)

    /**
     * ブックマークの追加/削除をトグルする
     */
    suspend fun toggle(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String? = null)
}
