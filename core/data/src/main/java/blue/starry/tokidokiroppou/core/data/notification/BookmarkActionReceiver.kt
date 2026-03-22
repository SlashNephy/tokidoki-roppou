package blue.starry.tokidokiroppou.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * 通知の「保存」アクションから呼び出される BroadcastReceiver
 */
@AndroidEntryPoint
class BookmarkActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BOOKMARK) {
            Timber.w("想定外の Intent action を受信: %s", intent.action)
            return
        }

        val lawCode = intent.getStringExtra(EXTRA_LAW_CODE) ?: return
        val articleNumber = intent.getStringExtra(EXTRA_ARTICLE_NUMBER) ?: return
        val supplementaryProvisionLabel = intent.getStringExtra(EXTRA_SUPPLEMENTARY_PROVISION_LABEL) ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bookmarkRepository.add(
                    LawCode.valueOf(lawCode),
                    articleNumber,
                    supplementaryProvisionLabel.ifEmpty { null },
                )
                Timber.d("条文をブックマークに保存: %s %s", lawCode, articleNumber)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "条文を保存しました", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "ブックマーク保存に失敗")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_BOOKMARK = "blue.starry.tokidokiroppou.ACTION_BOOKMARK"
        const val EXTRA_LAW_CODE = "extra_bookmark_law_code"
        const val EXTRA_ARTICLE_NUMBER = "extra_bookmark_article_number"
        const val EXTRA_SUPPLEMENTARY_PROVISION_LABEL = "extra_bookmark_supplementary_provision_label"
    }
}
