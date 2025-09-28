package allen.town.podcast.core.feed.util;

import static allen.town.podcast.model.feed.FeedPreferences.SPEED_USE_GLOBAL;

import android.util.Log;

import allen.town.podcast.core.pref.PlaybackPreferences;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.model.playback.MediaType;
import allen.town.podcast.model.playback.Playable;

/**
 * Utility class to use the appropriate playback speed based on {@link PlaybackPreferences}
 */
public final class PlaybackSpeedUtils {
    private static final String TAG = "PlaybackSpeedUtils";

    private PlaybackSpeedUtils() {
    }

    /**
     * Returns the currently configured playback speed for the specified media.
     */
    public static float getCurrentPlaybackSpeed(Playable media) {
        float playbackSpeed = SPEED_USE_GLOBAL;
        MediaType mediaType = null;

        if (media != null) {
            mediaType = media.getMediaType();
            playbackSpeed = PlaybackPreferences.getCurrentlyPlayingTemporaryPlaybackSpeed();

            if (playbackSpeed == SPEED_USE_GLOBAL && media instanceof FeedMedia) {
                FeedItem item = ((FeedMedia) media).getItem();
                if (item != null) {
                    Feed feed = item.getFeed();
                    if (feed != null && feed.getPreferences() != null) {
                        playbackSpeed = feed.getPreferences().getFeedPlaybackSpeed();
                    } else {
                        Log.d(TAG, "can not get feed playback speed -> " + feed);
                    }
                }
            }
        }

        if (playbackSpeed == SPEED_USE_GLOBAL) {
            playbackSpeed = Prefs.getPlaybackSpeed(mediaType);
        }

        return playbackSpeed;
    }

    /**
     * Returns the currently configured skip silence for the specified media.
     */
    public static FeedPreferences.SkipSilence getCurrentSkipSilencePreference(Playable media) {
        FeedPreferences.SkipSilence skipSilence = FeedPreferences.SkipSilence.GLOBAL;
        if (media instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) media;
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == feedMedia.getId()) {
                skipSilence = PlaybackPreferences.getCurrentlyPlayingTemporarySkipSilence();
            }
            if (skipSilence == FeedPreferences.SkipSilence.GLOBAL && feedMedia.getItem() != null) {
                Feed feed = feedMedia.getItem().getFeed();
                if (feed != null && feed.getPreferences() != null) {
                    skipSilence = feed.getPreferences().getFeedSkipSilence();
                }
            }
        }
        if (skipSilence == FeedPreferences.SkipSilence.GLOBAL) {
            skipSilence = Prefs.isSkipSilence()
                    ? FeedPreferences.SkipSilence.AGGRESSIVE : FeedPreferences.SkipSilence.OFF;
        }
        return skipSilence;
    }
}
