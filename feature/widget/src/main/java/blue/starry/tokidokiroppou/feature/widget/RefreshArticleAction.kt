package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

/**
 * リロードボタン。
 * ActionCallback は Hilt 非対応のため、抽選ロジックは持たず Worker の起動だけを行う。
 */
class RefreshArticleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("Widget refresh requested")

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        ).articleWidgetScheduler().requestImmediateUpdate()
    }
}
