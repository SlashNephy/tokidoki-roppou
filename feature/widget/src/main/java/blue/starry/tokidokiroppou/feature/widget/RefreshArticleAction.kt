package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import timber.log.Timber

/**
 * リロードアイコンのタップを受け取る仮実装。
 * 実際の条文再抽選と Worker 起動は Task 5 で実装する。
 */
class RefreshArticleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("Widget refresh requested")
    }
}
