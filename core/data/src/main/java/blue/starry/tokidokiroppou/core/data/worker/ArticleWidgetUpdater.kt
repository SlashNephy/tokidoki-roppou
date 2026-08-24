package blue.starry.tokidokiroppou.core.data.worker

import blue.starry.tokidokiroppou.core.domain.model.Article

/**
 * ウィジェットの表示内容を更新する。
 * Glance の実装は feature:widget にあるため、core:data からはこのインターフェース経由で呼ぶ。
 */
interface ArticleWidgetUpdater {
    /** 全てのウィジェットに条文を反映する */
    suspend fun updateAll(article: Article)

    /** ウィジェットが 1 つ以上配置されているか */
    suspend fun hasPlacedWidget(): Boolean
}
