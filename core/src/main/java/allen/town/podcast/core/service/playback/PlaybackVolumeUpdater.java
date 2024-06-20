package allen.town.podcast.core.service.playback;

import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.model.feed.VolumeAdaptionSetting;
import allen.town.podcast.model.playback.Playable;
import allen.town.podcast.playback.base.PlaybackServiceMediaPlayer;
import allen.town.podcast.playback.base.PlayerStatus;

class PlaybackVolumeUpdater {

    public void updateVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, long feedId,
                                        VolumeAdaptionSetting volumeAdaptionSetting) {
        Playable playable = mediaPlayer.getPlayable();

        if (playable instanceof FeedMedia) {
            updateFeedMediaVolumeIfNecessary(mediaPlayer, feedId, volumeAdaptionSetting, (FeedMedia) playable);
        }
    }

    private void updateFeedMediaVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, long feedId,
                                                  VolumeAdaptionSetting volumeAdaptionSetting, FeedMedia feedMedia) {
        if (feedMedia.getItem().getFeed().getId() == feedId) {
            FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
            preferences.setVolumeAdaptionSetting(volumeAdaptionSetting);

            if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
                forceUpdateVolume(mediaPlayer);
            }
        }
    }

    private void forceUpdateVolume(PlaybackServiceMediaPlayer mediaPlayer) {
        mediaPlayer.pause(false, false);
        mediaPlayer.resume();
    }

}
