package allen.town.podcast.core.util.playback;

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import allen.town.focus_common.ad.InterstitialAdManager;
import allen.town.podcast.core.feed.util.PlaybackSpeedUtils;
import allen.town.podcast.core.pref.PlaybackPreferences;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.service.playback.PlaybackService;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.event.playback.PlaybackPositionEvent;
import allen.town.podcast.event.playback.PlaybackServiceEvent;
import allen.town.podcast.event.playback.SpeedChangedEvent;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.playback.MediaType;
import allen.town.podcast.model.playback.Playable;
import allen.town.podcast.playback.base.PlaybackServiceMediaPlayer;
import allen.town.podcast.playback.base.PlayerStatus;

/**
 * 界面使用它来控制 PlaybackService
 */
public abstract class PlaybackController {

    private static final String TAG = "PlaybackController";
    private static final int INVALID_TIME = -1;

    private final Activity activity;
    private PlaybackService playbackService;
    private Playable media;
    private PlayerStatus status = PlayerStatus.STOPPED;

    private boolean mediaInfoLoaded = false;
    private boolean released = false;
    private boolean initialized = false;
    private boolean eventsRegistered = false;
    private long loadedFeedMedia = -1;

    public PlaybackController(@NonNull Activity activity) {
        this.activity = activity;
    }

    /**
     * Creates a new connection to the playbackService.
     */
    public synchronized void init() {
        if (!eventsRegistered) {
            EventBus.getDefault().register(this);
            eventsRegistered = true;
        }
        if (PlaybackService.isRunning) {
            initServiceRunning();
        } else {
            updatePlayButtonShowsPlay(true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackServiceEvent event) {
        if (event.action == PlaybackServiceEvent.Action.SERVICE_STARTED) {
            init();
        }
    }

    private synchronized void initServiceRunning() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(statusUpdate, new IntentFilter(
                PlaybackService.ACTION_PLAYER_STATUS_CHANGED),RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(statusUpdate, new IntentFilter(
                PlaybackService.ACTION_PLAYER_STATUS_CHANGED));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(notificationReceiver, new IntentFilter(
                PlaybackService.ACTION_PLAYER_NOTIFICATION),RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(notificationReceiver, new IntentFilter(
                PlaybackService.ACTION_PLAYER_NOTIFICATION));
        }

        if (!released) {
            bindToService();
        } else {
            throw new IllegalStateException("Can't call init() after release() has been called");
        }
        checkMediaInfoLoaded();
    }

    /**
     * Should be called if the PlaybackController is no longer needed, for
     * example in the activity's onStop() method.
     */
    public void release() {
        Log.d(TAG, "Releasing PlaybackController");

        try {
            activity.unregisterReceiver(statusUpdate);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            activity.unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        unbind();
        media = null;
        released = true;

        if (eventsRegistered) {
            EventBus.getDefault().unregister(this);
            eventsRegistered = false;
        }
    }

    private void unbind() {
        try {
            activity.unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        initialized = false;
    }

    /**
     * Should be called in the activity's onPause() method.
     */
    public void pause() {
        mediaInfoLoaded = false;
    }

    /**
     * Tries to establish a connection to the PlaybackService. If it isn't
     * running, the PlaybackService will be started with the last played media
     * as the arguments of the launch intent.
     */
    private void bindToService() {
        Log.d(TAG, "Trying to connect to service");
        if (!PlaybackService.isRunning) {
            throw new IllegalStateException("Trying to bind but service is not running");
        }
        boolean bound = activity.bindService(new Intent(activity, PlaybackService.class), mConnection, 0);
        Log.d(TAG, "Result for service binding: " + bound);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if(service instanceof PlaybackService.LocalBinder) {
                playbackService = ((PlaybackService.LocalBinder) service).getService();
                if (!released) {
                    queryService();
                    Log.d(TAG, "Connection to Service established");
                } else {
                    Log.i(TAG, "Connection to playback service has been established, " +
                            "but controller has already been released");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            initialized = false;
            Log.d(TAG, "Disconnected from Service");
        }
    };

    private final BroadcastReceiver statusUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "Received statusUpdate Intent.");
            if (playbackService != null) {
                PlaybackServiceMediaPlayer.PSMPInfo info = playbackService.getPSMPInfo();
                status = info.getPlayerStatus();
                media = info.getPlayable();
                handleStatus();
            } else {
//                Log.w(TAG, "Couldn't receive status update: playbackService was null");
                if (PlaybackService.isRunning) {
                    bindToService();
                } else {
                    status = PlayerStatus.STOPPED;
                    handleStatus();
                }
            }
        }
    };

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(PlaybackService.EXTRA_NOTIFICATION_TYPE, -1);
            int code = intent.getIntExtra(PlaybackService.EXTRA_NOTIFICATION_CODE, -1);
            if (code == -1 || type == -1) {
                Log.d(TAG, "Bad arguments. Won't handle intent");
                return;
            }
            switch (type) {
                case PlaybackService.NOTIFICATION_TYPE_RELOAD:
                    if (playbackService == null && PlaybackService.isRunning) {
                        bindToService();
                        return;
                    }
                    mediaInfoLoaded = false;
                    queryService();
                    onReloadNotification(intent.getIntExtra(
                            PlaybackService.EXTRA_NOTIFICATION_CODE, -1));
                    break;
                case PlaybackService.NOTIFICATION_TYPE_PLAYBACK_END:
                    onPlaybackEnd();
                    break;
            }
        }

    };

    public void onPositionObserverUpdate() {}

    /**
     * Called when the currently displayed information should be refreshed.
     */
    public void onReloadNotification(int code) {}

    public void onPlaybackEnd() {}

    /**
     * Is called whenever the PlaybackService changes its status. This method
     * should be used to update the GUI or start/cancel background threads.
     */
    private void handleStatus() {
//        Log.d(TAG, "status: " + status.toString());
        checkMediaInfoLoaded();
        switch (status) {
            case PAUSED:
                onPositionObserverUpdate();
                updatePlayButtonShowsPlay(true);
                if (!PlaybackService.isCasting() && PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    setScreenOn(false);
                }
                break;
            case PLAYING:
                if (!PlaybackService.isCasting() && PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    onAwaitingVideoSurface();
                    setScreenOn(true);
                }
                updatePlayButtonShowsPlay(false);
                break;
            case PREPARING:
                if (playbackService != null) {
                    updatePlayButtonShowsPlay(!playbackService.isStartWhenPrepared());
                }
                break;
            case PREPARED:
                updatePlayButtonShowsPlay(true);
                onPositionObserverUpdate();
                break;
            case SEEKING:
                onPositionObserverUpdate();
                break;
            case STOPPED: // Fall-through
            case INITIALIZED:
                updatePlayButtonShowsPlay(true);
                break;
            default:
                break;
        }
    }

    private void checkMediaInfoLoaded() {
        if (!mediaInfoLoaded || loadedFeedMedia != PlaybackPreferences.getCurrentlyPlayingFeedMediaId()) {
            loadedFeedMedia = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
            loadMediaInfo();
        }
        mediaInfoLoaded = true;
    }

    protected void updatePlayButtonShowsPlay(boolean showPlay) {

    }

    public abstract void loadMediaInfo();

    public  void onAwaitingVideoSurface()  {}

    /**
     * Called when connection to playback service has been established or
     * information has to be refreshed
     */
    private void queryService() {
//        Log.d(TAG, "Querying service info");
        if (playbackService != null) {
            PlaybackServiceMediaPlayer.PSMPInfo info = playbackService.getPSMPInfo();
            status = info.getPlayerStatus();
            media = info.getPlayable();

            // make sure that new media is loaded if it's available
            mediaInfoLoaded = false;
            handleStatus();

        } else {
            Log.e(TAG,
                    "queryService() was called without an existing connection to playbackservice");
        }
    }

    /**
     * Should be implemented by classes that show a video. The default implementation
     * does nothing
     *
     * @param enable True if the screen should be kept on, false otherwise
     */
    protected void setScreenOn(boolean enable) {

    }

    public void playPause() {
        if (playbackService == null) {
            new PlaybackServiceStarter(activity, media).start();
            Log.w(TAG, "Play/Pause button was pressed, but playbackservice was null!");
            return;
        }
        switch (status) {
            case PLAYING:
                playbackService.pause(true, false);
                InterstitialAdManager.loadAd(activity,true);
                break;
            case PAUSED:
            case PREPARED:
                InterstitialAdManager.loadAd(activity,true);
                playbackService.resume();
                break;
            case PREPARING:
                playbackService.setStartWhenPrepared(!playbackService.isStartWhenPrepared());
                break;
            case INITIALIZED:
                playbackService.setStartWhenPrepared(true);
                playbackService.prepare();
                break;
            default:
                new PlaybackServiceStarter(activity, media)
                        .callEvenIfRunning(true)
                        .start();
                Log.w(TAG, "Play/Pause button was pressed and PlaybackService state was unknown");
                break;
        }
    }

    public int getPosition() {
        if (playbackService != null) {
            return playbackService.getCurrentPosition();
        } else if (getMedia() != null) {
            return getMedia().getPosition();
        } else {
            return PlaybackService.INVALID_TIME;
        }
    }

    public int getDuration() {
        if (playbackService != null) {
            return playbackService.getDuration();
        } else if (getMedia() != null) {
            return getMedia().getDuration();
        } else {
            return PlaybackService.INVALID_TIME;
        }
    }

    public Playable getMedia() {
        if (media == null) {
            media = PlayableUtils.createInstanceFromPreferences(activity);
        }
        return media;
    }

    /**
     * 强制从数据库中获取最新的
     * @param forceReloadMedia
     * @return
     */
    public Playable getMedia(boolean forceReloadMedia) {
        if (forceReloadMedia) {
            media = PlayableUtils.createInstanceFromPreferences(activity);
        } else {
            return getMedia();
        }
        return media;
    }

    public boolean sleepTimerActive() {
        return playbackService != null && playbackService.sleepTimerActive();
    }

    public void disableSleepTimer() {
        if (playbackService != null) {
            playbackService.disableSleepTimer();
        }
    }

    public long getSleepTimerTimeLeft() {
        if (playbackService != null) {
            return playbackService.getSleepTimerTimeLeft();
        } else {
            return INVALID_TIME;
        }
    }

    public void extendSleepTimer(long extendTime) {
        long timeLeft = getSleepTimerTimeLeft();
        if (playbackService != null && timeLeft != INVALID_TIME) {
            setSleepTimer(timeLeft + extendTime);
        }
    }

    public void setSleepTimer(long time) {
        if (playbackService != null) {
            playbackService.setSleepTimer(time);
        }
    }

    public void seekTo(int time) {
        if (playbackService != null) {
            playbackService.seekTo(time);
        } else if (getMedia() instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) getMedia();
            media.setPosition(time);
            DBWriter.setFeedItem(media.getItem());
            EventBus.getDefault().post(new PlaybackPositionEvent(time, getMedia().getDuration()));
        }
    }

    public void setVideoSurface(SurfaceHolder holder) {
        if (playbackService != null) {
            playbackService.setVideoSurface(holder);
        }
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setPlaybackSpeed(float speed) {
        PlaybackPreferences.setCurrentlyPlayingTemporaryPlaybackSpeed(speed);
        if (getMedia() != null && getMedia().getMediaType() == MediaType.VIDEO) {
            Prefs.setVideoPlaybackSpeed(speed);
        } else {
            Prefs.setPlaybackSpeed(speed);
        }

        if (playbackService != null) {
            playbackService.setSpeed(speed);
        } else {
            EventBus.getDefault().post(new SpeedChangedEvent(speed));
        }
    }

    public void setSkipSilence(boolean skipSilence) {
        if (playbackService != null) {
            playbackService.skipSilence(skipSilence);
        }
    }

    /**
     * 获取当前正在播放的音频的播放速度
     * @return
     */
    public float getCurrentPlaybackSpeedMultiplier() {
        if (playbackService != null) {
            return playbackService.getCurrentPlaybackSpeed();
        } else {
            return PlaybackSpeedUtils.getCurrentPlaybackSpeed(getMedia());
        }
    }

    public boolean canDownmix() {
        return (playbackService != null && playbackService.canDownmix());
    }

    public void setDownmix(boolean enable) {
        if (playbackService != null) {
            playbackService.setDownmix(enable);
        }
    }

    public void setLoudness(boolean enable) {
        if (playbackService != null) {
            playbackService.setLoudness(enable);
        }
    }

    public List<String> getAudioTracks() {
        if (playbackService == null) {
            return Collections.emptyList();
        }
        return playbackService.getAudioTracks();
    }

    public int getSelectedAudioTrack() {
        if (playbackService == null) {
            return -1;
        }
        return playbackService.getSelectedAudioTrack();
    }

    public void setAudioTrack(int track) {
        if (playbackService != null) {
            playbackService.setAudioTrack(track);
        }
    }

    public boolean isPlayingVideoLocally() {
        if (PlaybackService.isCasting()) {
            return false;
        } else if (playbackService != null) {
            return PlaybackService.getCurrentMediaType() == MediaType.VIDEO;
        } else {
            return getMedia() != null && getMedia().getMediaType() == MediaType.VIDEO;
        }
    }

    public Pair<Integer, Integer> getVideoSize() {
        if (playbackService != null) {
            return playbackService.getVideoSize();
        } else {
            return null;
        }
    }

    public void notifyVideoSurfaceAbandoned() {
        if (playbackService != null) {
            playbackService.notifyVideoSurfaceAbandoned();
        }
    }

    public boolean isStreaming() {
        return playbackService != null && playbackService.isStreaming();
    }
}
