package allen.town.podcast.activity

import allen.town.core.service.PayService
import allen.town.focus_common.crash.Crashlytics
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.MyApp
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.storage.db.Db
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wyjson.router.GoRouter
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Shows the logo while waiting for the main activity to start.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)
        Completable.create { subscriber: CompletableEmitter ->
            // Trigger schema updates
            Db.getInstance()
            subscriber.onComplete()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    MyApp.instance.openAdManager.fetchAd(
                        findViewById(R.id.container)
                    ) {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        overridePendingTransition(0, 0)

                        if(!GoRouter.getInstance().getService(PayService::class.java)!!.isAliPay()){
                            Handler().postDelayed({
                                try {
                                    finish()
                                } catch (e: Exception) {
                                    //如果play版马上finish了会看到桌面然后才是首页，体验不好
                                    Timber.w("splash error $e")
                                }
                            }, 200)
                        }

                    }

                }) { error: Throwable ->
                Timber.e(error, "init")
                Crashlytics.getInstance().recordException(error)
                showSnack(this, error.localizedMessage, Toast.LENGTH_LONG)
                finish()
            }
    }

    /**
     * 开屏页一定要禁止用户对返回按钮的控制，否则将可能导致用户手动退出了App而广告无法正常曝光和计费
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            true
        } else super.onKeyDown(keyCode, event)
    }
}