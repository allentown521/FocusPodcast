package allen.town.podcast.core.feed.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.model.playback.Playable;

/**
 * Utility class to use the appropriate image resource based on {@link Prefs}.
 */
public final class ImageResourceUtils {

    private ImageResourceUtils() {
    }

    /**
     * returns the image location, does prefer the episode cover if available and enabled in settings.
     */
    @Nullable
    public static String getEpisodeListImageLocation(@NonNull Playable playable) {
        if (Prefs.getUseEpisodeCoverSetting()) {
            return playable.getImageLocation();
        } else {
            return getFallbackImageLocation(playable);
        }
    }

    /**
     * returns the image location, does prefer the episode cover if available and enabled in settings.
     */
    @Nullable
    public static String getEpisodeListImageLocation(@NonNull FeedItem feedItem) {
        if (Prefs.getUseEpisodeCoverSetting()) {
            return feedItem.getImageLocation();
        } else {
            return getFallbackImageLocation(feedItem);
        }
    }

    @Nullable
    public static String getFallbackImageLocation(@NonNull Playable playable) {
        if (playable instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) playable;
            FeedItem item = media.getItem();
            if (item != null && item.getFeed() != null) {
                return item.getFeed().getImageUrl();
            } else {
                return null;
            }
        } else {
            return playable.getImageLocation();
        }
    }

    @Nullable
    public static String getFallbackImageLocation(@NonNull FeedItem feedItem) {
        if (feedItem.getFeed() != null) {
            return feedItem.getFeed().getImageUrl();
        } else {
            return null;
        }
    }
}
