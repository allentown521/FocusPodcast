package allen.town.podcast.playback.cast

import allen.town.core.service.PayService
import allen.town.focus_common.activity.ToolbarBaseActivity
import allen.town.focus_common.util.Timber
import android.os.Bundle
import android.view.Menu
import androidx.core.view.MenuItemCompat
import androidx.mediarouter.app.MediaRouteActionProvider
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.wyjson.router.GoRouter

/**
 * chromecast投屏，play版本有
 */
abstract class CastEnabledActivity : ToolbarBaseActivity() {
    private var canCast = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(GoRouter.getInstance().getService(PayService::class.java)!!.isAliPay()){
            Timber.i("chromecast is not for china")
            return
        }
        canCast = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        if (canCast) {
            try {
                CastContext.getSharedInstance(this)
            } catch (e: Exception) {
                e.printStackTrace()
                canCast = false
            }
        }
    }


    fun requestCastButton(menu: Menu) {
        if (!canCast) {
            return
        }
        menuInflater.inflate(R.menu.cast_button, menu)
        val mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item)
        val mediaRouteActionProvider =
            MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider?
        //总是显示chromecast图标,否则只会在有chromecast设备时才会显示
        if (BuildConfig.DEBUG) {
            mediaRouteActionProvider!!.setAlwaysVisible(true)
        }

        CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu,
            R.id.media_route_menu_item
        )
    }
}