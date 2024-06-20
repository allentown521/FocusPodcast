package allen.town.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import allen.town.podcast.core.ClientConfig;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.service.download.DownloadService;
import allen.town.podcast.core.storage.DBTasks;

// modified from http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
// and ConnectivityActionReceiver.java
// Updated based on http://stackoverflow.com/questions/20833241/android-charge-intent-has-no-extra-data
// Since the intent doesn't have the EXTRA_STATUS like the android.com article says it does
// (though it used to)
public class PowerConnectionReceiver extends BroadcastReceiver {
	private static final String TAG = "PowerConnectionReceiver";

	@Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        Log.d(TAG, "onReceive " + action);

        ClientConfig.initialize(context);
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.d(TAG, "charging");
            // we're plugged in, this is a great time to auto-download if everything else is
            // right. So, even if the user allows auto-dl on battery, let's still start
            // downloading now. They shouldn't mind.
            // autodownloadUndownloadedItems will make sure we're on the right wifi networks,
            // etc... so we don't have to worry about it.
            DBTasks.autodownloadUndownloadedItems(context);
        } else {
            // if we're not supposed to be auto-downloading when we're not charging, stop it
            if (!Prefs.isEnableAutodownloadOnBattery()) {
                Log.d(TAG, "not charging");
                DownloadService.cancelAll(context);
            } else {
                Log.d(TAG, "not charging anymore, but the user allows auto-download " +
                           "when on battery so we'll keep going");
            }
        }

    }
}
