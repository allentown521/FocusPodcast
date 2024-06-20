package allen.town.podcast.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import allen.town.podcast.core.ClientConfig;
import allen.town.podcast.core.util.download.AutoUpdateManager;

/**
 * Refreshes all feeds when it receives an intent
 */
public class FeedsSyncReceiver extends BroadcastReceiver {

    private static final String TAG = "FeedUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        ClientConfig.initialize(context);

        AutoUpdateManager.runOnce(context);
    }

}
