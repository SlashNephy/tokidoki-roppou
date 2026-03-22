package blue.starry.tokidokiroppou.core.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "六法の条文を定期的にお届けします"
            setSound(null, null)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun sendArticleNotification(article: Article, useHalfWidthParentheses: Boolean = false) {
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

        val title = article.displayTitle(useHalfWidthParentheses)
        val fullText = article.fullText(useHalfWidthParentheses)
        val displayText = if (fullText.length > 300) fullText.take(300) + "…" else fullText

        val contentIntent = createContentIntent(article)

        val copyIntent = createCopyIntent(article, useHalfWidthParentheses)
        val bookmarkIntent = createBookmarkIntent(article)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(displayText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullText)
                    .setBigContentTitle(title)
                    .setSummaryText(article.lawCode.displayName)
            )
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_notification,
                "コピー",
                copyIntent,
            )
            .addAction(
                R.drawable.ic_notification,
                "保存",
                bookmarkIntent,
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

    private fun createContentIntent(article: Article): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply {
                setClassName(context, "blue.starry.tokidokiroppou.MainActivity")
            }
        launchIntent.apply {
            putExtra(EXTRA_LAW_CODE, article.lawCode.name)
            putExtra(EXTRA_ARTICLE_NUMBER, article.articleNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            article.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createCopyIntent(article: Article, useHalfWidthParentheses: Boolean): PendingIntent {
        val fullText = "${article.displayTitle(useHalfWidthParentheses)}\n${article.fullText(useHalfWidthParentheses)}"
        val intent = Intent(context, CopyActionReceiver::class.java).apply {
            action = CopyActionReceiver.ACTION_COPY
            putExtra(CopyActionReceiver.EXTRA_TEXT, fullText)
        }

        return PendingIntent.getBroadcast(
            context,
            article.hashCode() xor 0x434F50, // "COP" (Copy)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createBookmarkIntent(article: Article): PendingIntent {
        val intent = Intent(context, BookmarkActionReceiver::class.java).apply {
            action = BookmarkActionReceiver.ACTION_BOOKMARK
            putExtra(BookmarkActionReceiver.EXTRA_LAW_CODE, article.lawCode.name)
            putExtra(BookmarkActionReceiver.EXTRA_ARTICLE_NUMBER, article.articleNumber)
            putExtra(BookmarkActionReceiver.EXTRA_SUPPLEMENTARY_PROVISION_LABEL, article.supplementaryProvisionLabel ?: "")
        }

        return PendingIntent.getBroadcast(
            context,
            article.hashCode() xor 0x424D4B, // "BMK" (Bookmark)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "article_notification"
        const val EXTRA_LAW_CODE = "extra_law_code"
        const val EXTRA_ARTICLE_NUMBER = "extra_article_number"
    }
}
