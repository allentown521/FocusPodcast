package allen.town.podcast.appshortcuts;

import static allen.town.podcast.activity.MainActivity.EXTRA_FEED_ID;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import allen.town.focus_common.util.ImageUtils;
import allen.town.focus_common.util.ShortCutUtils;
import allen.town.podcast.R;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.model.feed.Feed;

/**
 * Launches the main activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class SubscriptionActivityStarter {


    public static Intent getIntentStartFromLauncher(Context context, Feed feed) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_FEED_ID, feed.getId());
        return intent;
    }

    public static void getBitmapFromUrl(Context context, Feed feed, FeedShortcutBitmapCallback feedShortcutBitmapCallback) {
        int iconSize = (int) (128 * context.getResources().getDisplayMetrics().density);
        Glide.with(context)
                .asBitmap()
                .load(feed.getImageUrl())
                .apply(new RequestOptions().override(iconSize, iconSize))
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Bitmap> target, boolean isFirstResource) {

                        feedShortcutBitmapCallback.onGetBitmap(feed, ImageUtils.drawableToBitmap(context.getDrawable(R.drawable.ic_podcast)));
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model,
                                                   Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        feedShortcutBitmapCallback.onGetBitmap(feed, resource);
                        return true;
                    }
                }).submit();
    }

    /**
     * 主动在app中创建快捷方式，区别于直接在launcher创建
     *
     * @param context
     * @param feed
     */
    public static void createShortcut(Context context, Feed feed) {
        getBitmapFromUrl(context, feed, new FeedShortcutBitmapCallback() {
            @Override
            public void onGetBitmap(Feed feed, Bitmap bitmap) {
                ShortCutUtils.install(context, feed.getFeedTitle(), bitmap, getIntentStartFromLauncher(context, feed), true);
            }
        });
    }

    public interface FeedShortcutBitmapCallback {
        void onGetBitmap(Feed feed, Bitmap bitmap);
    }
}
