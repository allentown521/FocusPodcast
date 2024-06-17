package allen.town.podcast.core.service.playback;

import android.app.Notification;
import android.util.Log;

import androidx.core.app.ServiceCompat;

import allen.town.focus_common.util.Timber;

class PlaybackServiceStateManager {
    private final PlaybackService playbackService;

    private volatile boolean isInForeground = false;
    private volatile boolean hasReceivedValidStartCommand = false;

    PlaybackServiceStateManager(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    void startForeground(int notificationId, Notification notification) {
        Timber.d( "startForeground");
        playbackService.startForeground(notificationId, notification);
        isInForeground = true;
    }

    void stopService() {
        Timber.d( "stopService");
        stopForeground(true);
        playbackService.stopSelf();
        hasReceivedValidStartCommand = false;
    }

    void stopForeground(boolean removeNotification) {
        Timber.d( "stopForeground");
        if (isInForeground) {
            if (removeNotification) {
                ServiceCompat.stopForeground(playbackService, ServiceCompat.STOP_FOREGROUND_REMOVE);
            } else {
                ServiceCompat.stopForeground(playbackService, ServiceCompat.STOP_FOREGROUND_DETACH);
            }
        }
        isInForeground = false;
    }

    boolean hasReceivedValidStartCommand() {
        return hasReceivedValidStartCommand;
    }

    void validStartCommandWasReceived() {
        this.hasReceivedValidStartCommand = true;
    }
}
