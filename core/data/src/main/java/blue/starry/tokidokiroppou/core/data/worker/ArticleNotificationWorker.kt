package blue.starry.tokidokiroppou.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ArticleNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
    private val notificationSender: ArticleNotificationSender,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("ArticleNotificationWorker started")

        val settings = settingsRepository.get()
        if (!settings.isNotificationEnabled) {
            Timber.d("Notifications disabled, skipping")
            return Result.success()
        }

        val article = lawRepository.getRandomArticle(settings.enabledLawCodes)
        if (article == null) {
            Timber.w("No article found")
            return Result.retry()
        }

        notificationSender.sendArticleNotification(article)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "article_notification"
    }
}
