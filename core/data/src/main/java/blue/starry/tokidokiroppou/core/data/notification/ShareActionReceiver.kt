package blue.starry.tokidokiroppou.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * 通知の「共有」アクションから呼び出される BroadcastReceiver
 */
class ShareActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHARE) {
            Timber.w("想定外の Intent action を受信: %s", intent.action)
            return
        }

        val text = intent.getStringExtra(EXTRA_TEXT) ?: return

        // Android の共有シートを起動
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooserIntent = Intent.createChooser(shareIntent, null).apply {
            // BroadcastReceiver から Activity を起動するため FLAG_ACTIVITY_NEW_TASK が必要
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        Timber.d("共有シートを起動")
    }

    companion object {
        const val ACTION_SHARE = "blue.starry.tokidokiroppou.ACTION_SHARE"
        const val EXTRA_TEXT = "extra_share_text"
    }
}
