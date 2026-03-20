package blue.starry.tokidokiroppou.core.data.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRefreshScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicRefresh() {
        val workRequest = PeriodicWorkRequestBuilder<CacheRefreshWorker>(
            REFRESH_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CacheRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )

        Timber.d("Scheduled periodic cache refresh every %d hours", REFRESH_INTERVAL_HOURS)
    }

    fun requestImmediateRefresh() {
        val workRequest = OneTimeWorkRequestBuilder<CacheRefreshWorker>()
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniqueWork(
            "${CacheRefreshWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.KEEP,
            workRequest,
        )

        Timber.d("Requested immediate cache refresh")
    }

    companion object {
        private const val REFRESH_INTERVAL_HOURS = 24L
    }
}
