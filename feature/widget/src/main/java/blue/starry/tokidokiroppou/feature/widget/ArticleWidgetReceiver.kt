package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

class ArticleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArticleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val scheduler = schedulerOf(context)
        val intervalMinutes = runBlocking {
            settingsRepositoryOf(context).get().widgetUpdateIntervalMinutes
        }
        scheduler.schedule(intervalMinutes)
        scheduler.requestImmediateUpdate()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        schedulerOf(context).cancel()
    }

    private fun schedulerOf(context: Context): ArticleWidgetScheduler =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetSchedulerEntryPoint::class.java,
        ).articleWidgetScheduler()

    private fun settingsRepositoryOf(context: Context) =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        ).applicationSettingsRepository()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArticleWidgetSchedulerEntryPoint {
        fun articleWidgetScheduler(): ArticleWidgetScheduler
    }
}
