package blue.starry.tokidokiroppou

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.core.data.worker.ArticleNotificationScheduler
import blue.starry.tokidokiroppou.core.data.worker.CacheRefreshScheduler
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TokidokiRoppouApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationSender: ArticleNotificationSender

    @Inject
    lateinit var notificationScheduler: ArticleNotificationScheduler

    @Inject
    lateinit var cacheRefreshScheduler: CacheRefreshScheduler

    @Inject
    lateinit var appCheckProviderFactory: AppCheckProviderFactory

    @Inject
    lateinit var settingsRepository: ApplicationSettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Firebase の初期化
        Firebase.initialize(this)

        // App Check の初期化 (API キーの不正利用を防止)
        // フレーバーごとに DI で適切なプロバイダーが注入される
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        notificationSender.createNotificationChannel()

        // キャッシュの定期更新をスケジュール (24時間ごと、ネットワーク接続時)
        cacheRefreshScheduler.schedulePeriodicRefresh()
        // 初回起動時は即座にダウンロードを開始
        cacheRefreshScheduler.requestImmediateRefresh()

        val settings = runBlocking { settingsRepository.get() }
        if (settings.isNotificationEnabled) {
            notificationScheduler.schedule(settings.notificationIntervalMinutes)
        }
    }
}
