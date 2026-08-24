package blue.starry.tokidokiroppou.feature.widget

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * ウィジェットが表示中の条文を特定するためのキー。
 * 本文は保存せず、描画のたびに DB から解決する。
 */
object ArticleWidgetStateKeys {
    val LAW_ID = stringPreferencesKey("widget_law_id")
    val ARTICLE_NUMBER = stringPreferencesKey("widget_article_number")
    val SUPPLEMENTARY_PROVISION_LABEL = stringPreferencesKey("widget_supplementary_provision_label")
}
