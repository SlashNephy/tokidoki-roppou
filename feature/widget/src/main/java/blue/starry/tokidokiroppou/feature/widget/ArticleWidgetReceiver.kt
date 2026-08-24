package blue.starry.tokidokiroppou.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class ArticleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArticleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        // onEnabled は BroadcastReceiver のメインスレッドで呼ばれるため、
        // DataStore の読み取りを runBlocking で待つと ANR の原因になる。
        // goAsync() で PendingResult を確保し、その生存期間だけ有効な
        // 一時的な CoroutineScope 上でバックグラウンド処理を行う。
        // GlanceAppWidgetReceiver は onUpdate 等では独自に goAsync() を
        // 使うが、onEnabled はオーバーライドしていないため衝突しない。
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val entryPoint = entryPointOf(context)
                val scheduler = entryPoint.articleWidgetScheduler()
                val intervalMinutes =
                    entryPoint.applicationSettingsRepository().get().widgetUpdateIntervalMinutes
                scheduler.schedule(intervalMinutes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule widget update in onEnabled")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Glance の描画は super.onUpdate() が行うため必ず呼ぶ。
        // onUpdate は新規ウィジェット配置のたびに呼ばれる唯一の経路のため
        // (onEnabled は最初の 1 個目が配置されたときしか呼ばれない)、
        // ここで即時更新をリクエストして 2 個目以降が空表示のまま
        // 放置されないようにする。
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        entryPointOf(context).articleWidgetScheduler().requestImmediateUpdate()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        entryPointOf(context).articleWidgetScheduler().cancel()
    }

    private fun entryPointOf(context: Context): ArticleWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        )
}
