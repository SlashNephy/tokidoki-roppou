package blue.starry.tokidokiroppou.core.data.notification

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

/**
 * 通知の「コピー」アクションから呼び出される BroadcastReceiver
 */
class CopyActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COPY) {
            Timber.w("想定外の Intent action を受信: %s", intent.action)
            return
        }

        val text = intent.getStringExtra(EXTRA_TEXT) ?: return

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("条文", text)
        clipboardManager.setPrimaryClip(clip)

        Timber.d("条文をクリップボードにコピー")
        Toast.makeText(context, "条文をコピーしました", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_COPY = "blue.starry.tokidokiroppou.ACTION_COPY"
        const val EXTRA_TEXT = "extra_copy_text"
    }
}
