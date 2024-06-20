package allen.town.podcast.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.jetradarmobile.snowfall.SnowfallView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

import allen.town.focus_common.extensions.ColorExtensionsUtils;
import allen.town.focus_common.util.MenuIconUtil;
import allen.town.focus_common.util.Timber;
import allen.town.podcast.R;
import allen.town.podcast.activity.DriveModeActivity;
import allen.town.podcast.activity.LockScreenActivity;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.core.feed.util.PlaybackSpeedUtils;
import allen.town.podcast.core.playback.NowPlayingScreen;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.service.playback.PlaybackService;
import allen.town.podcast.core.util.ChapterUtils;
import allen.town.podcast.core.util.Converter;
import allen.town.podcast.core.util.IntentUtils;
import allen.town.podcast.core.util.TimeSpeedConverter;
import allen.town.podcast.core.util.playback.PlaybackController;
import allen.town.podcast.dialog.PlaySpeedDialog;
import allen.town.podcast.dialog.PlaybackControlsDialog;
import allen.town.podcast.dialog.SkipPrefDialog;
import allen.town.podcast.dialog.SleepTimerDialog;
import allen.town.podcast.event.CoverColorChangeEvent;
import allen.town.podcast.event.FavoritesEvent;
import allen.town.podcast.event.FeedItemEvent;
import allen.town.podcast.event.PlayerErrorEvent;
import allen.town.podcast.event.UnreadItemsUpdateEvent;
import allen.town.podcast.event.playback.BufferUpdateEvent;
import allen.town.podcast.event.playback.PlaybackPositionEvent;
import allen.town.podcast.event.playback.PlaybackServiceEvent;
import allen.town.podcast.event.playback.SleepTimerUpdatedEvent;
import allen.town.podcast.event.playback.SpeedChangedEvent;
import allen.town.podcast.menuprocess.FeedItemMenuProcess;
import allen.town.podcast.model.feed.Chapter;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.playback.Playable;
import allen.town.podcast.playback.LibraryViewModel;
import allen.town.podcast.playback.cast.CastEnabledActivity;
import allen.town.podcast.view.DrawableGradient;
import allen.town.podcast.view.PlayButton;
import code.name.monkey.appthemehelper.ThemeStore;
import code.name.monkey.appthemehelper.util.ATHUtil;
import code.name.monkey.appthemehelper.util.ColorUtil;
import code.name.monkey.appthemehelper.util.MaterialValueHelper;
import code.name.monkey.appthemehelper.util.TintHelper;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shows the audio player.
 */
public class AudioPlayerFragment extends Fragment implements
        SeekBar.OnSeekBarChangeListener, Toolbar.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "AudioPlayerFragment";
    public static final int POS_COVER = 0;
    public static final int POS_DESCRIPTION = 1;
    protected static final int NUM_CONTENT_FRAGMENTS = 1;

    TextView txtvPlaybackSpeed;
    protected ViewPager2 pager;
    protected TextView txtvPosition;
    protected TextView txtvLength;
    protected AppCompatSeekBar sbPosition;
    protected ImageButton butRev;
    protected TextView txtvRev;
    protected PlayButton butPlay;
    protected ImageButton butFF;
    protected TextView txtvFF;
    protected ImageButton butSkip;
    protected Toolbar toolbar;
    protected ProgressBar progressIndicator;
    protected CardView cardViewSeek;
    protected TextView txtvSeek;

    protected PlaybackController controller;
    protected Disposable disposable;
    protected boolean showTimeLeft;
    protected boolean seekedToChapterStart = false;
    protected int currentChapterIndex = -1;
    protected int duration;
    protected LibraryViewModel libraryViewModel;
    protected View slideView;
    protected boolean isFromScreenActivity = false;
    protected boolean isDriveMode = false;
    protected ImageView driveCloseIv;
    protected NowPlayingScreen nowPlayingScreen;
    protected View colorGradientBackground;
    protected ImageView playPauseButton;
    protected ImageView swipeUpIv;
    protected TextView swipeUpTipTv;
    protected SnowfallView snowfallView;
    protected ImageView colorBackground;
    protected View mask;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        int layoutResId;
        nowPlayingScreen = Prefs.getNowPlayingScreen();
        if (isDriveMode) {
            layoutResId = R.layout.drive_audioplayer_fragment;
        } else if (nowPlayingScreen == NowPlayingScreen.Adaptive) {
            layoutResId = R.layout.adapter_audioplayer_fragment;
        } else if (nowPlayingScreen == NowPlayingScreen.BlurCard) {
            layoutResId = R.layout.blur_card_audioplayer_fragment;
        } else if (nowPlayingScreen == NowPlayingScreen.Full) {
            layoutResId = R.layout.full_audioplayer_fragment;
        } else if (nowPlayingScreen == NowPlayingScreen.Circle) {
            layoutResId = R.layout.circle_audioplayer_fragment;
        }  else {
            layoutResId = R.layout.audioplayer_fragment;
        }

        View root = inflater.inflate(layoutResId, container, false);
        root.setOnTouchListener((v, event) -> true); // Avoid clicks going through player to fragments below
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationOnClickListener(v ->
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
        toolbar.setOnMenuItemClickListener(this);

        MiniPlayerFragment miniPlayerFragment = new MiniPlayerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.playerFragment, miniPlayerFragment, MiniPlayerFragment.TAG)
                .commit();

        txtvPlaybackSpeed = root.findViewById(R.id.txtvPlaybackSpeed);
        sbPosition = root.findViewById(R.id.sbPosition);
        txtvPosition = root.findViewById(R.id.txtvPosition);
        txtvLength = root.findViewById(R.id.txtvLength);
        butRev = root.findViewById(R.id.butRev);
        txtvRev = root.findViewById(R.id.txtvRev);
        butPlay = root.findViewById(R.id.butPlay);
        butFF = root.findViewById(R.id.butFF);
        txtvFF = root.findViewById(R.id.txtvFF);
        butSkip = root.findViewById(R.id.butSkip);
        progressIndicator = root.findViewById(R.id.progLoading);
        cardViewSeek = root.findViewById(R.id.cardViewSeek);
        txtvSeek = root.findViewById(R.id.txtvSeek);
        slideView = root.findViewById(R.id.slide);
        driveCloseIv = root.findViewById(R.id.close);
        colorGradientBackground = root.findViewById(R.id.colorGradientBackground);
        playPauseButton = root.findViewById(R.id.playPauseButton);
        swipeUpIv = root.findViewById(R.id.swipe_up_iv);
        swipeUpTipTv = root.findViewById(R.id.swipe_up_tip_tv);
        snowfallView = root.findViewById(R.id.snowfall_view);
        colorBackground = root.findViewById(R.id.colorBackground);
        mask = root.findViewById(R.id.mask);

        setupDriveMode();
        setupLengthTextView();
        setupControlButtons();
        txtvPlaybackSpeed.setOnClickListener(v -> new PlaySpeedDialog().show(getChildFragmentManager(), null));
        sbPosition.setOnSeekBarChangeListener(this);

        pager = root.findViewById(R.id.pager);
        pager.setAdapter(new AudioPlayerPagerAdapter(this,isDriveMode));
        // Required for getChildAt(int) in ViewPagerBottomSheetBehavior to return the correct page
        pager.setOffscreenPageLimit((int) NUM_CONTENT_FRAGMENTS);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pager.post(() -> {
                    if (getActivity() != null) {
                        // By the time this is posted, the activity might be closed again.
//                        ((MainActivity) getActivity()).getBottomSheet().updateScrollingChild();
                    }
                });
            }
        });
        libraryViewModel = new ViewModelProvider(getActivity()).get(LibraryViewModel.class);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupScreenLayout();
        startOrStopSnow(Prefs.showSnowFall());
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    protected void setChapterDividers(Playable media) {

        if (media == null) {
            return;
        }

        float[] dividerPos = null;

        if (media.getChapters() != null && !media.getChapters().isEmpty()) {
            List<Chapter> chapters = media.getChapters();
            dividerPos = new float[chapters.size()];

            for (int i = 0; i < chapters.size(); i++) {
                dividerPos[i] = chapters.get(i).getStart() / (float) duration;
            }
        }

    }

    public View getExternalPlayerHolder() {
        return getView().findViewById(R.id.playerFragment);
    }

    protected void setupControlButtons() {
        butRev.setOnClickListener(v -> {
            if (controller != null) {
                int curr = controller.getPosition();
                controller.seekTo(curr - Prefs.getRewindSecs() * 1000);
            }
        });
        butRev.setOnLongClickListener(v -> {
            SkipPrefDialog.showSkipPreference(getContext(),
                    SkipPrefDialog.SkipDirection.SKIP_REWIND, txtvRev);
            return true;
        });
        butPlay.setOnClickListener(v -> {
            if (controller != null) {
                controller.init();
                controller.playPause();
            }
        });
        butFF.setOnClickListener(v -> {
            if (controller != null) {
                int curr = controller.getPosition();
                controller.seekTo(curr + Prefs.getFastForwardSecs() * 1000);
            }
        });
        butFF.setOnLongClickListener(v -> {
            SkipPrefDialog.showSkipPreference(getContext(),
                    SkipPrefDialog.SkipDirection.SKIP_FORWARD, txtvFF);
            return false;
        });
        butSkip.setOnClickListener(v ->
                IntentUtils.sendLocalBroadcast(getActivity(), PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsUpdate(UnreadItemsUpdateEvent event) {
        if (controller == null) {
            return;
        }
        updatePosition(new PlaybackPositionEvent(controller.getPosition(),
                controller.getDuration()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackServiceChanged(PlaybackServiceEvent event) {
        if (event.action == PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN) {
            if(isFromScreenActivity){
                return;
            }
            ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    protected void setupLengthTextView() {
        showTimeLeft = Prefs.shouldShowRemainingTime();
        txtvLength.setOnClickListener(v -> {
            if (controller == null) {
                return;
            }
            showTimeLeft = !showTimeLeft;
            Prefs.setShowRemainTimeSetting(showTimeLeft);
            updatePosition(new PlaybackPositionEvent(controller.getPosition(),
                    controller.getDuration()));
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updatePlaybackSpeedButton(SpeedChangedEvent event) {
        String speedStr = new DecimalFormat("0.0").format(event.getNewSpeed());
        txtvPlaybackSpeed.setText(speedStr + "x");
    }

    protected void loadMediaInfo(boolean includingChapters, boolean forceReloadMedia) {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Playable>create(emitter -> {
            Playable media = controller.getMedia(forceReloadMedia);
            if (media != null) {
                if (includingChapters) {
                    ChapterUtils.loadChapters(media, getContext());
                }
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(media -> {
            updateUi(media);
            if (media.getChapters() == null && !includingChapters) {
                loadMediaInfo(true,forceReloadMedia);
            }
        }, error -> Log.e(TAG, Log.getStackTraceString(error)),
            () -> updateUi(null));
    }

    protected PlaybackController newPlaybackController() {
        return new PlaybackController(getActivity()) {
            @Override
            protected void updatePlayButtonShowsPlay(boolean showPlay) {
                butPlay.setIsShowPlay(showPlay);
            }

            @Override
            public void loadMediaInfo() {
                AudioPlayerFragment.this.loadMediaInfo(false,false);
            }

            @Override
            public void onPlaybackEnd() {
                if(isFromScreenActivity){
                    return;
                }
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        };
    }

    protected void updateUi(Playable media) {
        if (controller == null || media == null) {
            return;
        }
        duration = controller.getDuration();
        updatePosition(new PlaybackPositionEvent(media.getPosition(), media.getDuration()));
        updatePlaybackSpeedButton(new SpeedChangedEvent(PlaybackSpeedUtils.getCurrentPlaybackSpeed(media)));
        setChapterDividers(media);
        setupOptionsMenu(media);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void sleepTimerUpdate(SleepTimerUpdatedEvent event) {
        if (event.isCancelled() || event.wasJustEnabled()) {
            AudioPlayerFragment.this.loadMediaInfo(false,false);
        }
    }

    protected ValueAnimator valueAnimator;
    protected void colorize(int i) {
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }

        valueAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                ColorExtensionsUtils.surfaceColor(this),
                i
        );
        valueAnimator.addUpdateListener(animation -> {
            if (isAdded()) {
                DrawableGradient drawable = new DrawableGradient(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                (int) animation.getAnimatedValue(),
                                ColorExtensionsUtils.surfaceColor(AudioPlayerFragment.this)
                        }, 0
                );
                colorGradientBackground.setBackground(drawable);
            }
        });
        valueAnimator.setDuration(1000).start();
    }

    protected int lastPlaybackControlsColor = 0;

    protected int lastDisabledPlaybackControlsColor = 0;

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void coverColorUpdate(CoverColorChangeEvent event) {
        libraryViewModel.updateColor(event.getColor().getBackgroundColor());

        if(isDriveMode){
            return;
        }

        if (Prefs.isAdapterColor() && (Prefs.getNowPlayingScreen() == NowPlayingScreen.Normal
                || Prefs.getNowPlayingScreen() == NowPlayingScreen.Adaptive
                || Prefs.getNowPlayingScreen() == NowPlayingScreen.Circle)) {
            colorize(event.getColor().getBackgroundColor());
        }

        int colorBg = ATHUtil.resolveColor(requireContext(), android.R.attr.colorBackground);
        if (ColorUtil.isColorLight(colorBg)) {
            lastPlaybackControlsColor =
                    MaterialValueHelper.getSecondaryTextColor(requireContext(), true);
            lastDisabledPlaybackControlsColor =
                    MaterialValueHelper.getSecondaryDisabledTextColor(requireContext(), true);
        } else {
            lastPlaybackControlsColor =
                    MaterialValueHelper.getPrimaryTextColor(requireContext(), false);
            lastDisabledPlaybackControlsColor =
                    MaterialValueHelper.getPrimaryDisabledTextColor(requireContext(), false);
        }

        int colorFinal;
        if(Prefs.isAdapterColor()){
            colorFinal = event.getColor().getPrimaryTextColor();
        }else {
            colorFinal = ThemeStore.accentColor(requireContext());
        }
        colorFinal = ColorExtensionsUtils.ripAlpha(colorFinal);
        //加了这行有个问题必现，播放过程中去设置“自定义颜色”然后回来，播放按钮动画没有完全完成
        TintHelper.setTintAuto(
                butPlay,
                MaterialValueHelper.getPrimaryTextColor(
                        requireContext(),
                        ColorUtil.isColorLight(colorFinal)
                ),
                false
        );
        TintHelper.setTintAuto(playPauseButton, colorFinal, true);
        ColorExtensionsUtils.applyColor(sbPosition,colorFinal);
        butFF.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        butRev.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        butSkip.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        txtvRev.setTextColor(lastPlaybackControlsColor);
        txtvFF.setTextColor(lastPlaybackControlsColor);

        if(isFromScreenActivity){
            swipeUpIv.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
            swipeUpTipTv.setTextColor(lastPlaybackControlsColor);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = newPlaybackController();
        controller.init();
        loadMediaInfo(false,false);
        EventBus.getDefault().register(this);
        txtvRev.setText(NumberFormat.getInstance().format(Prefs.getRewindSecs()));
        txtvFF.setText(NumberFormat.getInstance().format(Prefs.getFastForwardSecs()));
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        progressIndicator.setVisibility(View.GONE); // Controller released; we will not receive buffering updates
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void bufferUpdate(BufferUpdateEvent event) {
        if (event.hasStarted()) {
            progressIndicator.setVisibility(View.VISIBLE);
        } else if (event.hasEnded()) {
            progressIndicator.setVisibility(View.GONE);
        } else if (controller != null && controller.isStreaming()) {
            sbPosition.setSecondaryProgress((int) (event.getProgress() * sbPosition.getMax()));
        } else {
            sbPosition.setSecondaryProgress(0);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updatePosition(PlaybackPositionEvent event) {
        if (controller == null || txtvPosition == null || txtvLength == null || sbPosition == null) {
            return;
        }

        TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
        int currentPosition = converter.convert(event.getPosition());
        int duration = converter.convert(event.getDuration());
        int remainingTime = converter.convert(Math.max(event.getDuration() - event.getPosition(), 0));
        currentChapterIndex = ChapterUtils.getCurrentChapterIndex(controller.getMedia(), currentPosition);
//        Log.d(TAG, "current position  -> " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == PlaybackService.INVALID_TIME || duration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "failed to position observer update because of invalid time");
            return;
        }
        txtvPosition.setText(Converter.getDurationStringLong(currentPosition));
        showTimeLeft = Prefs.shouldShowRemainingTime();
        if (showTimeLeft) {
            txtvLength.setText(((remainingTime > 0) ? "-" : "") + Converter.getDurationStringLong(remainingTime));
        } else {
            txtvLength.setText(Converter.getDurationStringLong(duration));
        }

        if (!sbPosition.isPressed()) {
            float progress = ((float) event.getPosition()) / event.getDuration();
            sbPosition.setProgress((int) (progress * sbPosition.getMax()));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void favoritesChanged(FavoritesEvent event) {
        //收藏状态变化了需要从数据库查询最新的（当前界面操作正常是因为DBWriter直接修改了传过去的item，但是其他界面的修改不行）
        AudioPlayerFragment.this.loadMediaInfo(false,true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void mediaPlayerError(PlayerErrorEvent event) {
        Timber.e("media player error " + event.getMessage());
        if(isFromScreenActivity){
            return;
        }
/*        final AlertDialog.Builder errorDialog =            new AccentMaterialDialog(
                    getContext(),
                    R.style.MaterialAlertDialogTheme
            );
        errorDialog.setTitle(R.string.error_label);
        errorDialog.setMessage(event.getMessage());
        errorDialog.setPositiveButton(android.R.string.ok, (dialog, which) ->
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
        errorDialog.create().show();*/
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (controller == null || txtvLength == null) {
            return;
        }

        if (fromUser) {
            float prog = progress / ((float) seekBar.getMax());
            TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
            int position = converter.convert((int) (prog * controller.getDuration()));
            int newChapterIndex = ChapterUtils.getCurrentChapterIndex(controller.getMedia(), position);
            if (newChapterIndex > -1) {
                if (!sbPosition.isPressed() && currentChapterIndex != newChapterIndex) {
                    currentChapterIndex = newChapterIndex;
                    position = (int) controller.getMedia().getChapters().get(currentChapterIndex).getStart();
                    seekedToChapterStart = true;
                    controller.seekTo(position);
                    updateUi(controller.getMedia());
                }
                txtvSeek.setText(controller.getMedia().getChapters().get(newChapterIndex).getTitle()
                                + "\n" + Converter.getDurationStringLong(position));
            } else {
                txtvSeek.setText(Converter.getDurationStringLong(position));
            }
        } else if (duration != controller.getDuration()) {
            updateUi(controller.getMedia());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // interrupt position Observer, restart later
        cardViewSeek.setScaleX(.8f);
        cardViewSeek.setScaleY(.8f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            if (seekedToChapterStart) {
                seekedToChapterStart = false;
            } else {
                float prog = seekBar.getProgress() / ((float) seekBar.getMax());
                controller.seekTo((int) (prog * controller.getDuration()));
            }
        }
        cardViewSeek.setScaleX(1f);
        cardViewSeek.setScaleY(1f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(0f).scaleX(.8f).scaleY(.8f)
                .setDuration(200)
                .start();
    }

    public void setupOptionsMenu(Playable media) {
        if (toolbar.getMenu().size() == 0) {
            toolbar.inflateMenu(R.menu.mediaplayer);
        }
        if (controller == null) {
            return;
        }
        boolean isFeedMedia = media instanceof FeedMedia;
        toolbar.getMenu().findItem(R.id.open_feed_item).setVisible(isFeedMedia);
        if (isFeedMedia) {
            FeedItemMenuProcess.onPrepareMenu(toolbar.getMenu(), ((FeedMedia) media).getItem());
        }

        toolbar.getMenu().findItem(R.id.set_sleeptimer_item).setVisible(!controller.sleepTimerActive());
        toolbar.getMenu().findItem(R.id.disable_sleeptimer_item).setVisible(controller.sleepTimerActive());
        toolbar.getMenu().findItem(R.id.driver_mode).setVisible(true);
        MenuIconUtil.showToolbarMenuIcon(toolbar);

        ((CastEnabledActivity) getActivity()).requestCastButton(toolbar.getMenu());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
//        if (controller != null) {
//            Log.v(TAG, "FeedItemEvent() called with: " + "event = [" + event + "]");
//            for (FeedItem item : event.items) {
//                if (Objects.equals(this.controller.getMedia().getIdentifier(), item.getMedia().getIdentifier())) {
//                    Log.d(TAG, "re - setupOptionsMenu");
//                    setupOptionsMenu(this.controller.getMedia());
//                    return;
//                }
//            }
//        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (media == null) {
            return false;
        }

        final @Nullable FeedItem feedItem = (media instanceof FeedMedia) ? ((FeedMedia) media).getItem() : null;
        if (feedItem != null && FeedItemMenuProcess.onMenuItemClicked(this, item.getItemId(), feedItem)) {
            return true;
        }

        final int itemId = item.getItemId();
        if (itemId == R.id.disable_sleeptimer_item || itemId == R.id.set_sleeptimer_item) {
            new SleepTimerDialog().show(getChildFragmentManager(), "SleepTimerDialog");
            return true;
        } else if (itemId == R.id.audio_controls) {
            PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance(feedItem.getFeedId());
            dialog.show(getChildFragmentManager(), "playback_controls");
            return true;
        } else if (itemId == R.id.open_feed_item) {
            if (feedItem != null) {
                Intent intent = MainActivity.getIntentToOpenFeedWithId(getContext(), feedItem.getFeedId());
                startActivity(intent);
            }
            return true;
        } else if (itemId == R.id.driver_mode) {
            if (feedItem != null) {
                Intent intent = new Intent(getActivity(), DriveModeActivity.class);
                startActivity(intent);
            }
            return true;
        }
        return false;
    }

    protected void startOrStopSnow(boolean isSnowFalling) {

        if(isDriveMode){
            return;
        }
        if (isSnowFalling && !ColorExtensionsUtils.isColorLight(ColorExtensionsUtils.surfaceColor(this))) {
            snowfallView.setVisibility(View.VISIBLE);
            snowfallView.restartFalling();
        } else {
            snowfallView.setVisibility(View.GONE);
            snowfallView.stopFalling();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Objects.equals(key, Prefs.PREF_SNOWFALL)) {
            startOrStopSnow(Prefs.showSnowFall());
        }
    }

    protected static class AudioPlayerPagerAdapter extends FragmentStateAdapter {
        protected static final String TAG = "AudioPlayerPagerAdapter";
        protected boolean isDriveMode = false;

        public AudioPlayerPagerAdapter(@NonNull Fragment fragment,boolean isDriveMode) {
            super(fragment);
            this.isDriveMode = isDriveMode;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                default:
                    return new CoverFragment(isDriveMode);
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CONTENT_FRAGMENTS;
        }
    }

    public void scrollToPage(int page, boolean smoothScroll) {
        if (pager == null) {
            return;
        }

        pager.setCurrentItem(page, smoothScroll);

        Fragment visibleChild = getChildFragmentManager().findFragmentByTag("f" + POS_DESCRIPTION);
    }

    public void scrollToPage(int page) {
        scrollToPage(page, false);
    }

    protected void setupScreenLayout(){
        if(getActivity() instanceof LockScreenActivity){
            isFromScreenActivity = true;
            toolbar.setVisibility(View.GONE);
            slideView.setVisibility(View.VISIBLE);
            slideView.setTranslationY(250f);
            slideView.setAlpha(0f);
            slideView.animate().translationY(0f).alpha(1f).setDuration(1500).start();

            getExternalPlayerHolder().setVisibility(View.GONE);

        }
    }

    public AudioPlayerFragment(boolean isDriveMode){
        this.isDriveMode = isDriveMode;
    }

    //必须要有，别问为什么
    public AudioPlayerFragment(){
    }

    protected void setupDriveMode(){
        if(isDriveMode){
            driveCloseIv.setOnClickListener(v -> getActivity().onBackPressed());
        }
    }
}
