package allen.town.podcast.activity

import allen.town.focus_common.util.MenuIconUtil.showMenuIcon
import allen.town.focus_common.util.Timber
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity.Companion.getIntentToOpenFeedWithId
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.*
import allen.town.podcast.core.util.IntentUtils.openInBrowser
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.core.util.ui.PictureInPictureUtil
import allen.town.podcast.databinding.VideoplayerActivityBinding
import allen.town.podcast.dialog.PlaySpeedDialog
import allen.town.podcast.dialog.PlaybackControlsDialog.Companion.newInstance
import allen.town.podcast.dialog.ShareDialog
import allen.town.podcast.dialog.SkipPrefDialog
import allen.town.podcast.dialog.SleepTimerDialog
import allen.town.podcast.event.PlayerErrorEvent
import allen.town.podcast.event.playback.BufferUpdateEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.event.playback.PlaybackServiceEvent
import allen.town.podcast.event.playback.SleepTimerUpdatedEvent
import allen.town.podcast.fragment.FeedItemExtraInfoFragment
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.model.playback.Playable
import allen.town.podcast.playback.base.PlayerStatus
import allen.town.podcast.playback.cast.CastEnabledActivity
import allen.town.podcast.ui.startintent.MainActivityStarter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.animation.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.view.WindowCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.Glide
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Activity for playing video files.
 */
class VideoPlayerActivity : CastEnabledActivity(), OnSeekBarChangeListener {
    /**
     * True if video controls are currently visible.
     */
    private var videoControlsShowing = true
    private var videoSurfaceCreated = false
    private var destroyingDueToReload = false
    private var lastScreenTap: Long = 0
    private val videoControlsHider = Handler(Looper.getMainLooper())
    private var viewBinding: VideoplayerActivityBinding? = null
    private var controller: PlaybackController? = null
    private var showTimeLeft = false
    private var isFavorite = false
    private var switchToAudioOnly = false
    private var disposable: Disposable? = null
    private var prog = 0f
    @SuppressLint("AppCompatMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        // has to be called before setting layout content
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY)
        setTheme(R.style.Theme_FocusPodcast_VideoPlayer)
        super.onCreate(savedInstanceState)
        StorageUtils.checkStorageAvailability(this)
        window.setFormat(PixelFormat.TRANSPARENT)
        viewBinding = VideoplayerActivityBinding.inflate(LayoutInflater.from(this))
        setSupportActionBar(viewBinding!!.toolbar)
        setContentView(viewBinding!!.root)
        setupView()
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(0x20000000))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        StorageUtils.checkStorageAvailability(this)
        switchToAudioOnly = false
        if (PlaybackService.isCasting()) {
            val intent = PlaybackService.getPlayerActivityIntent(this)
            if (intent.component!!.className != VideoPlayerActivity::class.java.name) {
                destroyingDueToReload = true
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onStop() {
        if (controller != null) {
            controller!!.release()
            controller = null // prevent leak
        }
        if (disposable != null) {
            disposable!!.dispose()
        }
        EventBus.getDefault().unregister(this)
        super.onStop()
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            videoControlsHider.removeCallbacks(hideVideoControls)
        }
        // Controller released; we will not receive buffering updates
        viewBinding!!.progressBar.visibility = View.GONE
    }

    public override fun onUserLeaveHint() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            compatEnterPictureInPicture()
        }
    }

    override fun onStart() {
        super.onStart()
        controller = newPlaybackController()
        controller!!.init()
        loadMediaInfo()
        onPositionObserverUpdate()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            if (controller != null && controller!!.status == PlayerStatus.PLAYING) {
                controller!!.pause()
            }
        }
        super.onPause()
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    private fun newPlaybackController(): PlaybackController {
        return object : PlaybackController(this) {
            override fun onPositionObserverUpdate() {
                this@VideoPlayerActivity.onPositionObserverUpdate()
            }

            override fun onReloadNotification(code: Int) {
                this@VideoPlayerActivity.onReloadNotification(code)
            }

            override fun updatePlayButtonShowsPlay(showPlay: Boolean) {
                viewBinding!!.playButton.setIsShowPlay(showPlay)
            }

            override fun loadMediaInfo() {
                this@VideoPlayerActivity.loadMediaInfo()
            }

            override fun onAwaitingVideoSurface() {
                setupVideoAspectRatio()
                if (videoSurfaceCreated && controller != null) {
                    Log.d(TAG, "video created")
                    controller!!.setVideoSurface(viewBinding!!.videoView.holder)
                }
            }

            override fun onPlaybackEnd() {
                finish()
            }

            override fun setScreenOn(enable: Boolean) {
                if (enable) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun bufferUpdate(event: BufferUpdateEvent) {
        if (event.hasStarted()) {
            viewBinding!!.progressBar.visibility = View.VISIBLE
        } else if (event.hasEnded()) {
            viewBinding!!.progressBar.visibility = View.INVISIBLE
        } else {
            viewBinding!!.sbPosition.secondaryProgress =
                (event.progress * viewBinding!!.sbPosition.max).toInt()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun sleepTimerUpdate(event: SleepTimerUpdatedEvent) {
        if (event.isCancelled || event.wasJustEnabled()) {
            supportInvalidateOptionsMenu()
        }
    }

    protected fun loadMediaInfo() {
        if (controller == null || controller!!.media == null) {
            return
        }

        if (controller!!.status === PlayerStatus.PLAYING && !controller!!.isPlayingVideoLocally) {
            Timber.d( "Closing, no longer video")
            destroyingDueToReload = true
            finish()
            MainActivityStarter(this).withOpenPlayer().start()
            return
        }

        showTimeLeft = Prefs.shouldShowRemainingTime()
        onPositionObserverUpdate()
        checkFavorite()
        val media = controller!!.media
        if (media != null) {
            supportActionBar!!.subtitle = media.episodeTitle
            supportActionBar!!.title = media.feedTitle
        }
    }

    protected fun setupView() {
        showTimeLeft = Prefs.shouldShowRemainingTime()
        viewBinding!!.durationLabel.setOnClickListener { v: View? ->
            showTimeLeft = !showTimeLeft
            val media = controller!!.media ?: return@setOnClickListener
            val converter = TimeSpeedConverter(controller!!.currentPlaybackSpeedMultiplier)
            val length: String
            length = if (showTimeLeft) {
                val remainingTime = converter.convert(media.duration - media.position)
                "-" + Converter.getDurationStringLong(remainingTime)
            } else {
                val duration = converter.convert(media.duration)
                Converter.getDurationStringLong(duration)
            }
            viewBinding!!.durationLabel.text = length
            Prefs.setShowRemainTimeSetting(showTimeLeft)
        }
        viewBinding!!.sbPosition.setOnSeekBarChangeListener(this)
        viewBinding!!.rewindButton.setOnClickListener { v: View? -> onRewind() }
        viewBinding!!.rewindButton.setOnLongClickListener { v: View? ->
            SkipPrefDialog.showSkipPreference(
                this@VideoPlayerActivity,
                SkipPrefDialog.SkipDirection.SKIP_REWIND, null
            )
            true
        }
        viewBinding!!.playButton.setIsVideoScreen(true)
        viewBinding!!.playButton.setOnClickListener { v: View? -> onPlayPause() }
        viewBinding!!.fastForwardButton.setOnClickListener { v: View? -> onFastForward() }
        viewBinding!!.fastForwardButton.setOnLongClickListener { v: View? ->
            SkipPrefDialog.showSkipPreference(
                this@VideoPlayerActivity,
                SkipPrefDialog.SkipDirection.SKIP_FORWARD, null
            )
            false
        }
        // To suppress touches directly below the slider
        viewBinding!!.bottomControlsContainer.setOnTouchListener { view: View?, motionEvent: MotionEvent? -> true }
        viewBinding!!.bottomControlsContainer.fitsSystemWindows = true
        viewBinding!!.videoView.holder.addCallback(surfaceHolderCallback)
        viewBinding!!.videoView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setupVideoControlsToggler()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        viewBinding!!.videoPlayerContainer.setOnTouchListener(onVideoviewTouched)
        viewBinding!!.videoPlayerContainer.viewTreeObserver.addOnGlobalLayoutListener {
            viewBinding!!.videoView.setAvailableSize(
                viewBinding!!.videoPlayerContainer.width.toFloat(),
                viewBinding!!.videoPlayerContainer.height.toFloat()
            )
        }
    }

    private val hideVideoControls = Runnable {
        if (videoControlsShowing) {
            supportActionBar!!.hide()
            hideVideoControls(true)
            videoControlsShowing = false
        }
    }
    private val onVideoviewTouched = OnTouchListener { v: View, event: MotionEvent ->
        if (event.action != MotionEvent.ACTION_DOWN) {
            return@OnTouchListener false
        }
        if (PictureInPictureUtil.isInPictureInPictureMode(this)) {
            return@OnTouchListener true
        }
        videoControlsHider.removeCallbacks(hideVideoControls)
        if (System.currentTimeMillis() - lastScreenTap < 300) {
            if (event.x > v.measuredWidth / 2.0f) {
                onFastForward()
                showSkipAnimation(true)
            } else {
                onRewind()
                showSkipAnimation(false)
            }
            if (videoControlsShowing) {
                supportActionBar!!.hide()
                hideVideoControls(false)
                videoControlsShowing = false
            }
            return@OnTouchListener true
        }
        toggleVideoControlsVisibility()
        if (videoControlsShowing) {
            setupVideoControlsToggler()
        }
        lastScreenTap = System.currentTimeMillis()
        true
    }

    private fun showSkipAnimation(isForward: Boolean) {
        val skipAnimation = AnimationSet(true)
        skipAnimation.addAnimation(
            ScaleAnimation(
                1f, 2f, 1f, 2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            )
        )
        skipAnimation.addAnimation(AlphaAnimation(1f, 0f))
        skipAnimation.fillAfter = false
        skipAnimation.duration = 800
        val params = viewBinding!!.skipAnimationImage.layoutParams as FrameLayout.LayoutParams
        if (isForward) {
            viewBinding!!.skipAnimationImage.setImageResource(R.drawable.ic_fast_forward_video_white)
            params.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        } else {
            viewBinding!!.skipAnimationImage.setImageResource(R.drawable.ic_fast_rewind_video_white)
            params.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        }
        viewBinding!!.skipAnimationImage.visibility = View.VISIBLE
        viewBinding!!.skipAnimationImage.layoutParams = params
        viewBinding!!.skipAnimationImage.startAnimation(skipAnimation)
        skipAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                viewBinding!!.skipAnimationImage.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun setupVideoControlsToggler() {
        videoControlsHider.removeCallbacks(hideVideoControls)
        videoControlsHider.postDelayed(hideVideoControls, 2500)
    }

    private fun setupVideoAspectRatio() {
        if (videoSurfaceCreated && controller != null) {
            val videoSize = controller!!.videoSize
            if (videoSize != null && videoSize.first > 0 && videoSize.second > 0) {
                viewBinding!!.videoView.setVideoSize(videoSize.first, videoSize.second)
            } else {
                Log.e(TAG, "Could not determine video size")
            }
        }
    }

    private fun toggleVideoControlsVisibility() {
        if (videoControlsShowing) {
            supportActionBar!!.hide()
            hideVideoControls(true)
        } else {
            supportActionBar!!.show()
            showVideoControls()
        }
        videoControlsShowing = !videoControlsShowing
    }

    fun onRewind() {
        if (controller == null) {
            return
        }
        val curr = controller!!.position
        controller!!.seekTo(curr - Prefs.rewindSecs * 1000)
        setupVideoControlsToggler()
    }

    fun onPlayPause() {
        if (controller == null) {
            return
        }
        controller!!.playPause()
        setupVideoControlsToggler()
    }

    fun onFastForward() {
        if (controller == null) {
            return
        }
        val curr = controller!!.position
        controller!!.seekTo(curr + Prefs.fastForwardSecs * 1000)
        setupVideoControlsToggler()
    }

    private val surfaceHolderCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            holder.setFixedSize(width, height)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            videoSurfaceCreated = true
            if (controller != null && controller!!.status == PlayerStatus.PLAYING) {
                controller!!.setVideoSurface(holder)
            }
            setupVideoAspectRatio()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            videoSurfaceCreated = false
            if (controller != null && !destroyingDueToReload && !switchToAudioOnly) {
                controller!!.notifyVideoSurfaceAbandoned()
            }
        }
    }

    protected fun onReloadNotification(notificationCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && PictureInPictureUtil.isInPictureInPictureMode(
                this
            )
        ) {
            if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO
                || notificationCode == PlaybackService.EXTRA_CODE_CAST
            ) {
                finish()
            }
            return
        }
        if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "switch to cast player")
            destroyingDueToReload = true
            finish()
            MainActivityStarter(this).withOpenPlayer().start()
        }
    }

    private fun showVideoControls() {
        viewBinding!!.bottomControlsContainer.visibility = View.VISIBLE
        viewBinding!!.controlsContainer.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        if (animation != null) {
            viewBinding!!.bottomControlsContainer.startAnimation(animation)
            viewBinding!!.controlsContainer.startAnimation(animation)
        }
        viewBinding!!.videoView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun hideVideoControls(showAnimation: Boolean) {
        if (showAnimation) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            if (animation != null) {
                viewBinding!!.bottomControlsContainer.startAnimation(animation)
                viewBinding!!.controlsContainer.startAnimation(animation)
            }
        }
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        viewBinding!!.bottomControlsContainer.fitsSystemWindows = true
        viewBinding!!.bottomControlsContainer.visibility = View.GONE
        viewBinding!!.controlsContainer.visibility = View.GONE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent?) {
        onPositionObserverUpdate()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlaybackServiceChanged(event: PlaybackServiceEvent) {
        if (event.action == PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN) {
            finish()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaPlayerError(event: PlayerErrorEvent) {
        Timber.e("media player error " + event.message)
/*        val errorDialog: AlertDialog.Builder = AccentMaterialDialog(
            this,
            R.style.MaterialAlertDialogTheme
        )
        errorDialog.setTitle(R.string.error_label)
        errorDialog.setMessage(event.message)
        errorDialog.setNeutralButton(android.R.string.ok) { dialog: DialogInterface?, which: Int -> finish() }
        errorDialog.show()*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        requestCastButton(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.mediaplayer, menu)
        //        menu.findItem(R.id.audio_controls).setVisible(false);
        showMenuIcon(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (controller == null) {
            return false
        }
        val media = controller!!.media
        val isFeedMedia = media is FeedMedia
        menu.findItem(R.id.open_feed_item).isVisible =
            isFeedMedia // FeedMedia implies it belongs to a Feed
        val hasWebsiteLink = getWebsiteLinkWithFallback(media) != null
        menu.findItem(R.id.visit_website_item).isVisible = hasWebsiteLink
        val isItemAndHasLink = isFeedMedia && ShareUtils.hasLinkToShare((media as FeedMedia).item)
        val isItemHasDownloadLink = isFeedMedia && (media as FeedMedia).download_url != null
        menu.findItem(R.id.share_item).isVisible =
            hasWebsiteLink || isItemAndHasLink || isItemHasDownloadLink
        menu.findItem(R.id.add_to_favorites_item).isVisible = false
        menu.findItem(R.id.remove_from_favorites_item).isVisible = false
        if (isFeedMedia) {
            menu.findItem(R.id.add_to_favorites_item).isVisible = !isFavorite
            menu.findItem(R.id.remove_from_favorites_item).isVisible = isFavorite
        }
        menu.findItem(R.id.set_sleeptimer_item).isVisible = !controller!!.sleepTimerActive()
        menu.findItem(R.id.disable_sleeptimer_item).isVisible = controller!!.sleepTimerActive()
        menu.findItem(R.id.player_switch_to_audio_only).isVisible = true
        menu.findItem(R.id.audio_controls).setIcon(R.drawable.ic_sliders)
        menu.findItem(R.id.playback_speed).isVisible = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.player_switch_to_audio_only) {
            switchToAudioOnly = true
            finish()
            return true
        }
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this@VideoPlayerActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return true
        }
        if (controller == null) {
            return false
        }
        val media = controller!!.media ?: return false
        val feedItem = getFeedItem(media) // some options option requires FeedItem
        if (item.itemId == R.id.add_to_favorites_item && feedItem != null) {
            DBWriter.addFavoriteItem(feedItem)
            isFavorite = true
            invalidateOptionsMenu()
        } else if (item.itemId == R.id.remove_from_favorites_item && feedItem != null) {
            DBWriter.removeFavoriteItem(feedItem)
            isFavorite = false
            invalidateOptionsMenu()
        } else if (item.itemId == R.id.disable_sleeptimer_item
            || item.itemId == R.id.set_sleeptimer_item
        ) {
            SleepTimerDialog().show(supportFragmentManager, "SleepTimerDialog")
        } else if (item.itemId == R.id.audio_controls && feedItem != null) {
            val dialog = newInstance(feedItem.feedId)
            dialog.show(supportFragmentManager, "playback_controls")
        } else if (item.itemId == R.id.open_feed_item && feedItem != null) {
            val intent = getIntentToOpenFeedWithId(this, feedItem.feedId)
            startActivity(intent)
        } else if (item.itemId == R.id.visit_website_item) {
            openInBrowser(this@VideoPlayerActivity, getWebsiteLinkWithFallback(media))
        } else if (item.itemId == R.id.share_item && feedItem != null) {
            val shareDialog = ShareDialog.newInstance(feedItem)
            shareDialog.show(supportFragmentManager, "ShareEpisodeDialog")
        } else if (item.itemId == R.id.playback_speed) {
            PlaySpeedDialog().show(supportFragmentManager, null)
        } else if (item.itemId == R.id.extra_info_item) {
            FeedItemExtraInfoFragment().show(supportFragmentManager, "FeedItemExtraInfoFragment")
        } else {
            return false
        }
        return true
    }

    fun onPositionObserverUpdate() {
        if (controller == null) {
            return
        }
        val converter = TimeSpeedConverter(controller!!.currentPlaybackSpeedMultiplier)
        val currentPosition = converter.convert(controller!!.position)
        val duration = converter.convert(controller!!.duration)
        val remainingTime = converter.convert(
            controller!!.duration - controller!!.position
        )
        Log.d(TAG, "currentPosition  ->  " + Converter.getDurationStringLong(currentPosition))
        if (currentPosition == PlaybackService.INVALID_TIME
            || duration == PlaybackService.INVALID_TIME
        ) {
            Log.w(TAG, "failed to position observer invalid time")
            return
        }
        viewBinding!!.positionLabel.text =
            Converter.getDurationStringLong(currentPosition)
        if (showTimeLeft) {
            viewBinding!!.durationLabel.text = "-" + Converter.getDurationStringLong(remainingTime)
        } else {
            viewBinding!!.durationLabel.text = Converter.getDurationStringLong(duration)
        }
        updateProgressbarPosition(currentPosition, duration)
    }

    private fun updateProgressbarPosition(position: Int, duration: Int) {
        val progress = position.toFloat() / duration
        viewBinding!!.sbPosition.progress = (progress * viewBinding!!.sbPosition.max).toInt()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (controller == null) {
            return
        }
        if (fromUser) {
            prog = progress / seekBar.max.toFloat()
            val converter = TimeSpeedConverter(controller!!.currentPlaybackSpeedMultiplier)
            val position = converter.convert((prog * controller!!.duration).toInt())
            viewBinding!!.seekPositionLabel.text = Converter.getDurationStringLong(position)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        viewBinding!!.seekCardView.scaleX = .8f
        viewBinding!!.seekCardView.scaleY = .8f
        viewBinding!!.seekCardView.animate()
            .setInterpolator(FastOutSlowInInterpolator())
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(200)
            .start()
        videoControlsHider.removeCallbacks(hideVideoControls)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (controller != null) {
            controller!!.seekTo((prog * controller!!.duration).toInt())
        }
        viewBinding!!.seekCardView.scaleX = 1f
        viewBinding!!.seekCardView.scaleY = 1f
        viewBinding!!.seekCardView.animate()
            .setInterpolator(FastOutSlowInInterpolator())
            .alpha(0f).scaleX(.8f).scaleY(.8f)
            .setDuration(200)
            .start()
        setupVideoControlsToggler()
    }

    private fun checkFavorite() {
        val feedItem = getFeedItem(controller!!.media) ?: return
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable { DBReader.getFeedItem(feedItem.id) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { item: FeedItem? ->
                    val isFav = item!!.isTagged(FeedItem.TAG_FAVORITE)
                    if (isFavorite != isFav) {
                        isFavorite = isFav
                        invalidateOptionsMenu()
                    }
                }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    private fun compatEnterPictureInPicture() {
        if (PictureInPictureUtil.supportsPictureInPicture(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            supportActionBar!!.hide()
            hideVideoControls(false)
            enterPictureInPictureMode()
        }
    }

    //Hardware keyboard support
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val currentFocus = currentFocus
        if (currentFocus is EditText) {
            return super.onKeyUp(keyCode, event)
        }
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        when (keyCode) {
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_SPACE -> {
                onPlayPause()
                toggleVideoControlsVisibility()
                return true
            }
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_COMMA -> {
                onRewind()
                showSkipAnimation(false)
                return true
            }
            KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_PERIOD -> {
                onFastForward()
                showSkipAnimation(true)
                return true
            }
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_ESCAPE -> {
                //Exit fullscreen mode
                onBackPressed()
                return true
            }
            KeyEvent.KEYCODE_I -> {
                compatEnterPictureInPicture()
                return true
            }
            KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_W -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
                )
                return true
            }
            KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_S -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
                )
                return true
            }
            KeyEvent.KEYCODE_M -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI
                )
                return true
            }
        }

        //Go to x% of video:
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            controller!!.seekTo((0.1f * (keyCode - KeyEvent.KEYCODE_0) * controller!!.duration).toInt())
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        private const val TAG = "VideoplayerActivity"
        private fun getWebsiteLinkWithFallback(media: Playable?): String? {
            if (media == null) {
                return null
            } else if (StringUtils.isNotBlank(media.websiteLink)) {
                return media.websiteLink
            } else if (media is FeedMedia) {
                return FeedItemUtil.getLinkWithFallback(media.item)
            }
            return null
        }

        private fun getFeedItem(playable: Playable?): FeedItem? {
            return if (playable is FeedMedia) {
                playable.item
            } else {
                null
            }
        }
    }
}