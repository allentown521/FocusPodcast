package allen.town.podcast.playback.cast;


import android.view.Menu;

import allen.town.focus_common.activity.ToolbarBaseActivity;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends ToolbarBaseActivity {
    public static final String TAG = "CastEnabledActivity";

    public final void requestCastButton(Menu menu) {
        // no-op
    }
}
