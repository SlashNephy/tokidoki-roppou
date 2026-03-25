package blue.starry.tokidokiroppou

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

/**
 * Timber の Tree 実装で、ログを Firebase Crashlytics に転送する。
 * リリースビルドで plant することで、クラッシュレポートにログコンテキストを付与できる。
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // DEBUG / VERBOSE レベルのログは Crashlytics に送らない
        if (priority < Log.INFO) return

        Firebase.crashlytics.log("${tag ?: "---"}: $message")

        // 例外がある場合は非致命的エラーとして記録
        if (t != null) {
            Firebase.crashlytics.recordException(t)
        }
    }
}
