package blue.starry.tokidokiroppou.core.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import blue.starry.tokidokiroppou.core.data.R
import blue.starry.tokidokiroppou.core.domain.model.Article
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleNotificationSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "条文通知",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "六法の条文を定期的にお届けします"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun sendArticleNotification(article: Article) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Notification permission not granted")
                return
            }
        }

        val displayText = article.fullText.let {
            if (it.length > 300) it.take(300) + "…" else it
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(article.displayTitle)
            .setContentText(displayText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(article.fullText)
                    .setBigContentTitle(article.displayTitle)
                    .setSummaryText(article.lawCode.displayName)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            article.hashCode(),
            notification,
        )

        Timber.d("Sent notification: %s", article.displayTitle)
    }

    companion object {
        const val CHANNEL_ID = "article_notification"
    }
}
