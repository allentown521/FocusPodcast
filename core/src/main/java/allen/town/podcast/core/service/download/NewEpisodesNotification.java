package allen.town.podcast.core.service.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import allen.town.podcast.core.R;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedCounter;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.core.glide.ApGlideSettings;
import allen.town.podcast.storage.db.LongIntMap;
import allen.town.podcast.core.util.ui.NotificationUtils;
import allen.town.podcast.storage.db.Db;

public class NewEpisodesNotification {
    private static final String TAG = "NewEpisodesNotification";
    private static final String GROUP_KEY = "allen.town.podcast.EPISODES";

    private LongIntMap countersBefore;

    public NewEpisodesNotification() {
    }

    public void loadCountersBeforeRefresh() {
        Db adapter = Db.getInstance();
        adapter.open();
        countersBefore = adapter.getFeedCounters(FeedCounter.SHOW_NEW);
        adapter.close();
    }

    public void showIfNeeded(Context context, Feed feed) {
        FeedPreferences prefs = feed.getPreferences();
        if (!prefs.getKeepUpdated() || !prefs.getShowEpisodeNotification()) {
            return;
        }

        int newEpisodesBefore = countersBefore.get(feed.getId());
        int newEpisodesAfter = getNewEpisodeCount(feed.getId());

        if (newEpisodesAfter > newEpisodesBefore) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            showNotification(newEpisodesAfter, feed, context, notificationManager);
        }
    }

    private static void showNotification(int newEpisodes, Feed feed, Context context,
                                         NotificationManagerCompat notificationManager) {
        Resources res = context.getResources();
        String text = res.getQuantityString(
                R.plurals.new_episode_notification_message, newEpisodes, newEpisodes, feed.getTitle()
        );
        String title = res.getQuantityString(R.plurals.new_episode_notification_title, newEpisodes);

        Intent intent = new Intent();
        intent.setAction("NewEpisodes" + feed.getId());
        intent.setComponent(new ComponentName(context, "allen.town.podcast.activity.MainActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment_feed_id", feed.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification notification = new NotificationCompat.Builder(
                context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS)
                .setSmallIcon(R.drawable.ic_notification_new)
                .setContentTitle(title)
                .setLargeIcon(loadIcon(context, feed))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS, feed.hashCode(), notification);
        showGroupSummaryNotification(context, notificationManager);
    }

    private static void showGroupSummaryNotification(Context context, NotificationManagerCompat notificationManager) {
        Intent intent = new Intent();
        intent.setAction("NewEpisodes");
        intent.setComponent(new ComponentName(context, "allen.town.podcast.activity.MainActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment_tag", "EpisodesFragment");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification notificationGroupSummary = new NotificationCompat.Builder(
                context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS)
                .setSmallIcon(R.drawable.ic_notification_new)
                .setContentTitle(context.getString(R.string.new_episode_notification_group_text))
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS, 0, notificationGroupSummary);
    }

    private static Bitmap loadIcon(Context context, Feed feed) {
        int iconSize = (int) (128 * context.getResources().getDisplayMetrics().density);
        try {
            return Glide.with(context)
                    .asBitmap()
                    .load(feed.getImageUrl())
                    .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                    .apply(new RequestOptions().centerCrop())
                    .submit(iconSize, iconSize)
                    .get();
        } catch (Throwable tr) {
            return null;
        }
    }

    private static int getNewEpisodeCount(long feedId) {
        Db adapter = Db.getInstance();
        adapter.open();
        int episodeCount = adapter.getFeedCounters(FeedCounter.SHOW_NEW, feedId).get(feedId);
        adapter.close();
        return episodeCount;
    }
}
