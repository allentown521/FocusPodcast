package allen.town.podcast.config;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import allen.town.podcast.R;
import allen.town.podcast.activity.FeedAuthenticationActivity;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.core.DownloadServiceCallbacks;
import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.fragment.DownloadPagerFragment;
import allen.town.podcast.fragment.PlaylistFragment;


public class DownloadServiceCallbacksImpl implements DownloadServiceCallbacks {

    @Override
    public PendingIntent getNotificationContentIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, DownloadPagerFragment.TAG);
        Bundle args = new Bundle();
        args.putInt(DownloadPagerFragment.ARG_SELECTED_TAB, DownloadPagerFragment.POS_LOG);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_ARGS, args);
        return PendingIntent.getActivity(context,
                R.id.pending_intent_download_service_notification, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    @Override
    public PendingIntent getAuthentificationNotificationContentIntent(Context context, DownloadRequest request) {
        final Intent activityIntent = new Intent(context.getApplicationContext(), FeedAuthenticationActivity.class);
        activityIntent.setAction("request" + request.getFeedfileId());
        activityIntent.putExtra(FeedAuthenticationActivity.ARG_DOWNLOAD_REQUEST, request);
        return PendingIntent.getActivity(context.getApplicationContext(),
                request.getSource().hashCode(), activityIntent,
                PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    @Override
    public PendingIntent getReportNotificationContentIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, DownloadPagerFragment.TAG);
        Bundle args = new Bundle();
        args.putInt(DownloadPagerFragment.ARG_SELECTED_TAB, DownloadPagerFragment.POS_LOG);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_ARGS, args);
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    @Override
    public PendingIntent getAutoDownloadReportNotificationContentIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, PlaylistFragment.TAG);
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_autodownload_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
}
