package blue.starry.tokidokiroppou.core.data.worker

import blue.starry.tokidokiroppou.core.domain.model.Article

/**
 * ウィジェットの表示内容を更新する。
 * Glance の実装は feature:widget にあるため、core:data からはこのインターフェース経由で呼ぶ。
 */
interface ArticleWidgetUpdater {
    /** 全てのウィジェットに条文を反映する */
    suspend fun updateAll(article: Article)

    /**
     * 条文は変えずに、全てのウィジェットを再描画する。
     * 表示設定 (半角括弧表記など) の変更を、次回の定期更新を待たずに反映するために使う。
     */
    suspend fun rerenderAll()

    /** ウィジェットが 1 つ以上配置されているか */
    suspend fun hasPlacedWidget(): Boolean
}
