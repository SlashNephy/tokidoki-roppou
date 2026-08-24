package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

class ArticleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArticleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val entryPoint = entryPointOf(context)
        val scheduler = entryPoint.articleWidgetScheduler()
        val intervalMinutes = runBlocking {
            entryPoint.applicationSettingsRepository().get().widgetUpdateIntervalMinutes
        }
        scheduler.schedule(intervalMinutes)
        scheduler.requestImmediateUpdate()
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
