package blue.starry.tokidokiroppou.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import blue.starry.tokidokiroppou.core.data.repository.LawRepositoryImpl
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class CacheRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lawRepository: LawRepositoryImpl,
    private val settingsRepository: ApplicationSettingsRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("CacheRefreshWorker started")

        val settings = settingsRepository.get()
        val lawCodes = settings.enabledLawCodes.ifEmpty { LawCode.entries.toSet() }

        var allSuccess = true
        for (lawCode in lawCodes) {
            if (!lawRepository.refreshLawCode(lawCode)) {
                allSuccess = false
            }
        }

        return if (allSuccess) {
            Timber.d("Cache refresh completed successfully")
            Result.success()
        } else {
            Timber.w("Some law codes failed to refresh")
            Result.success() // 一部失敗しても次回に期待
        }
    }

    companion object {
        const val WORK_NAME = "cache_refresh"
    }
}
