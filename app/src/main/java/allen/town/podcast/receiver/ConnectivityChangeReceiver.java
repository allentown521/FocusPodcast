package allen.town.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import allen.town.podcast.core.ClientConfig;
import allen.town.podcast.core.util.NetworkUtils;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityActionRecvr";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(TAG, "onReceive");

            ClientConfig.initialize(context);
            NetworkUtils.networkChangedDetected();
        }
    }
}
