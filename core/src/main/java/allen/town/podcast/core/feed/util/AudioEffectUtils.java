package allen.town.podcast.core.feed.util;

import android.util.Log;

import allen.town.podcast.core.pref.PlaybackPreferences;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.playback.MediaType;
import allen.town.podcast.model.playback.Playable;

/**
 * Utility class to use the appropriate playback speed based on {@link PlaybackPreferences}
 */
public final class AudioEffectUtils {
    private static final String TAG = "AudioEffectUtils";

    private AudioEffectUtils() {
    }

    /**
     * Returns the currently configured playback speed for the specified media.
     */
    public static boolean isSkipEnable(Playable media) {
        MediaType mediaType = null;
        boolean isUseFeedEffect;

        if (media != null) {
            mediaType = media.getMediaType();
            if (media instanceof FeedMedia) {
                FeedItem item = ((FeedMedia) media).getItem();
                if (item != null) {
                    Feed feed = item.getFeed();
                    if (feed != null && feed.getPreferences() != null) {
                        isUseFeedEffect = feed.getPreferences().isUseFeedEffect();
                        if (isUseFeedEffect) {
                            return feed.getPreferences().isSkipSilence();
                        } else {
                            return Prefs.isSkipSilence();
                        }
                    } else {
                        Log.w(TAG, "can not get feed skip status -> " + feed);
                    }
                }
            }
        }
        return Prefs.isSkipSilence();
    }

    public static boolean isMonoEnable(Playable media) {
        MediaType mediaType = null;
        boolean isUseFeedEffect;

        if (media != null) {
            mediaType = media.getMediaType();
            if (media instanceof FeedMedia) {
                FeedItem item = ((FeedMedia) media).getItem();
                if (item != null) {
                    Feed feed = item.getFeed();
                    if (feed != null && feed.getPreferences() != null) {
                        isUseFeedEffect = feed.getPreferences().isUseFeedEffect();
                        if (isUseFeedEffect) {
                            return feed.getPreferences().isMono();
                        } else {
                            return Prefs.stereoToMono();
                        }
                    } else {
                        Log.w(TAG, "can not get feed mono status ->" + feed);
                    }
                }
            }
        }
        return Prefs.stereoToMono();
    }

    public static boolean isLoudnessEnable(Playable media) {
        MediaType mediaType = null;
        boolean isUseFeedEffect;

        if (media != null) {
            mediaType = media.getMediaType();
            if (media instanceof FeedMedia) {
                FeedItem item = ((FeedMedia) media).getItem();
                if (item != null) {
                    Feed feed = item.getFeed();
                    if (feed != null && feed.getPreferences() != null) {
                        isUseFeedEffect = feed.getPreferences().isUseFeedEffect();
                        if (isUseFeedEffect) {
                            return feed.getPreferences().isLoudness();
                        } else {
                            return Prefs.audioLoudness();
                        }
                    } else {
                        Log.d(TAG, "can not get feed loudness status -> " + feed);
                    }
                }
            }
        }
        return Prefs.audioLoudness();
    }
}
