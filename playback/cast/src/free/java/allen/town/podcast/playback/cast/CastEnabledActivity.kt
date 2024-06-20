package allen.town.podcast.playback.cast

import allen.town.focus_common.activity.ToolbarBaseActivity
import android.view.Menu

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
abstract class CastEnabledActivity : ToolbarBaseActivity() {
    fun requestCastButton(menu: Menu?) {
        // no-op
    }

    companion object {
        const val TAG: String = "CastEnabledActivity"
    }
}
