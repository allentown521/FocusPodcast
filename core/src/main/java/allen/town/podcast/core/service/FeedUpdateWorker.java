package allen.town.podcast.core.service;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import allen.town.podcast.core.ClientConfig;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.storage.DBTasks;
import allen.town.podcast.core.util.NetworkUtils;
import allen.town.podcast.core.util.download.AutoUpdateManager;

public class FeedUpdateWorker extends Worker {

    private static final String TAG = "FeedUpdateWorker";

    public static final String PARAM_RUN_ONCE = "runOnce";

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        final boolean isRunOnce = getInputData().getBoolean(PARAM_RUN_ONCE, false);
        Log.d(TAG, "syncing " + isRunOnce);
        ClientConfig.initialize(getApplicationContext());

        if (NetworkUtils.networkAvailable() && NetworkUtils.isFeedRefreshAllowed()) {
            DBTasks.refreshAllFeeds(getApplicationContext(), false);
        } else {
            Log.d(TAG, "not to auto update");
        }

        if (!isRunOnce && Prefs.isAutoUpdateTimeOfDay()) {
            // WorkManager does not allow to set specific time for repeated tasks.
            // We repeatedly schedule a OneTimeWorkRequest instead.
            AutoUpdateManager.restartUpdateAlarm(getApplicationContext());
        }

        return Result.success();
    }
}
