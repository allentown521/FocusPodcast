package allen.town.podcast.fragment

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity.Companion.getIntentToOpenFeedWithId
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.glide.palette.BitmapPaletteWrapper
import allen.town.podcast.core.playback.AlbumCoverStyle
import allen.town.podcast.core.pref.Prefs.albumCoverStyle
import allen.town.podcast.core.util.ChapterUtils
import allen.town.podcast.core.util.DateFormatter
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.event.CoverColorChangeEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.glide.RetroMusicColoredTarget
import allen.town.podcast.model.feed.Chapter
import allen.town.podcast.model.feed.EmbeddedChapterImage
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.model.playback.Playable
import allen.town.podcast.playback.VinylAlbumCoverView
import allen.town.podcast.util.MediaNotificationProcessor
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import code.name.monkey.appthemehelper.util.MaterialValueHelper.getPrimaryTextColor
import allen.town.focus_common.extensions.accentColor
import allen.town.focus_common.extensions.isColorLight
import android.os.Build
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.MaybeOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays the cover and the title of a FeedItem.
 */
class CoverFragment : Fragment {
    private var root: View ? = null
    private lateinit var txtvPodcastTitle: TextView
    private lateinit var txtvEpisodeTitle: TextView
    private lateinit var imgvCover: ImageView
    private lateinit var butPrevChapter: ImageButton
    private lateinit var butNextChapter: ImageButton
    private lateinit var episodeDetails: LinearLayout
    private lateinit var chapterControl: LinearLayout
    private var controller: PlaybackController? = null
    private var disposable: Disposable? = null
    private var displayedChapterIndex: Int = -1
    private var media: Playable? = null
    private lateinit var chapterTitleTv: TextView
    private var albumCoverOverlay: ImageView? = null
    private lateinit var rotateAnimator: ObjectAnimator
    private var imgvCoverVinyl: VinylAlbumCoverView? = null
    private var isDriveMode: Boolean = false
    private lateinit var coverViewHolder: CoverViewHolder
    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        setRetainInstance(true);
        root = inflater.inflate(layoutWithPlayerTheme, container, false)
        txtvPodcastTitle = root!!.findViewById(R.id.txtvPodcastTitle)
        txtvEpisodeTitle = root!!.findViewById(R.id.txtvEpisodeTitle)
        imgvCover = root!!.findViewById(R.id.imgvCover)
        episodeDetails = root!!.findViewById(R.id.episode_details)
        chapterControl = root!!.findViewById(R.id.chapterButton)
        butPrevChapter = root!!.findViewById(R.id.butPrevChapter)
        butNextChapter = root!!.findViewById(R.id.butNextChapter)
        chapterTitleTv = root!!.findViewById(R.id.chapters_label)
        coverViewHolder = CoverViewHolder()
        coverViewHolder.chapterTv = chapterTitleTv
        coverViewHolder.preChapterIv = butPrevChapter
        coverViewHolder.nextChapterIv = butNextChapter
        coverViewHolder.episodeTv = txtvEpisodeTitle
        coverViewHolder.podcastTv = txtvPodcastTitle
        imgvCover.setOnClickListener({ v: View? -> onPlayPause() })
        val scrollToDesc: View.OnClickListener = View.OnClickListener { view: View? ->
            FeedItemExtraInfoFragment().scrollToPage(FeedItemExtraInfoFragment.CHAPTERS_POS)
                .show(getParentFragmentManager(), "FeedItemExtraInfoFragment")
        }
        chapterTitleTv.setOnClickListener(scrollToDesc)
        val colorFilter: ColorFilter? = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            txtvPodcastTitle.getCurrentTextColor(), BlendModeCompat.SRC_IN
        )
        butNextChapter.setColorFilter(colorFilter)
        butPrevChapter.setColorFilter(colorFilter)
        butPrevChapter.setOnClickListener(View.OnClickListener({ v: View? -> seekToPrevChapter() }))
        butNextChapter.setOnClickListener(View.OnClickListener({ v: View? -> seekToNextChapter() }))
        albumCoverOverlay = root!!.findViewById(R.id.album_cover_overlay)
        imgvCoverVinyl = root!!.findViewById(R.id.imgvCoverVinyl)
        return root!!
    }

    public override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (layoutWithPlayerTheme == R.layout.circle_cover_fragment) {
            albumCoverOverlay!!.setBackground(
                ColorDrawable(
                    getPrimaryTextColor(
                        requireContext(),
                        this.accentColor().isColorLight
                    )
                )
            )
            setupRotateAnimation()
            //            ColorExtensionsUtils.accentColor(circularProgressIndicator);

//            binding.volumeSeekBar.circleProgressColor = accentColor()
//            binding.volumeSeekBar.circleColor = ColorUtil.withAlpha(accentColor(), 0.25f)
        }
    }

    private fun setupRotateAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(imgvCover, View.ROTATION, 360f)
        rotateAnimator.setInterpolator(LinearInterpolator())
        rotateAnimator.setRepeatCount(Animation.INFINITE)
        rotateAnimator.setDuration(20000)
    }

    private fun loadMediaInfo(includingChapters: Boolean) {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Maybe.create(MaybeOnSubscribe({ emitter: MaybeEmitter<Playable> ->
            val media: Playable? = controller!!.getMedia()
            if (media != null) {
                if (includingChapters) {
                    ChapterUtils.loadChapters(media, getContext())
                }
                emitter.onSuccess(media)
            } else {
                emitter.onComplete()
            }
        })).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer({ media: Playable ->
                this.media = media
                displayMediaInfo(media)
                if (media.getChapters() == null && !includingChapters) {
                    loadMediaInfo(true)
                }
            }), Consumer({ error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }))
    }

    private fun displayMediaInfo(media: Playable) {
        val pubDateStr: String = DateFormatter.formatAbbrev(getActivity(), media.getPubDate())
        txtvPodcastTitle!!.setText(
            StringUtils.stripToEmpty(media.getFeedTitle()) /*+ "\u00A0"
                + "・"
                + "\u00A0"
                + StringUtils.replace(StringUtils.stripToEmpty(pubDateStr), " ", "\u00A0")*/
        )
        if (media is FeedMedia) {
            val openFeed: Intent = getIntentToOpenFeedWithId(
                requireContext(),
                media.getItem()!!.getFeedId()
            )
            txtvPodcastTitle!!.setOnClickListener(View.OnClickListener({ v: View? ->
                startActivity(
                    openFeed
                )
            }))
        } else {
            txtvPodcastTitle!!.setOnClickListener(null)
        }
        txtvPodcastTitle!!.setOnLongClickListener(OnLongClickListener({ v: View? -> copyText(media.getFeedTitle()) }))
        txtvEpisodeTitle!!.setText(media.getEpisodeTitle())
        txtvEpisodeTitle!!.setOnLongClickListener(OnLongClickListener({ v: View? -> copyText(media.getEpisodeTitle()) }))
        txtvEpisodeTitle!!.setOnClickListener(View.OnClickListener({ v: View? ->
            val lines: Int = txtvEpisodeTitle!!.getLineCount()
            val animUnit: Int = 1000
            if (lines > txtvEpisodeTitle!!.getMaxLines()) {
                val verticalMarquee: ObjectAnimator = ObjectAnimator.ofInt(
                    txtvEpisodeTitle,
                    "scrollY",
                    0,
                    (lines - txtvEpisodeTitle!!.getMaxLines()) * (((txtvEpisodeTitle!!.getHeight() - txtvEpisodeTitle!!.getPaddingTop()
                            - txtvEpisodeTitle!!.getPaddingBottom())) / txtvEpisodeTitle!!.getMaxLines())
                )
                    .setDuration((lines * animUnit).toLong())
                val fadeOut: ObjectAnimator = ObjectAnimator.ofFloat(
                    txtvEpisodeTitle, "alpha", 0f
                )
                fadeOut.setStartDelay(animUnit.toLong())
                fadeOut.addListener(object : AnimatorListenerAdapter() {
                    public override fun onAnimationEnd(animation: Animator) {
                        txtvEpisodeTitle!!.scrollTo(0, 0)
                    }
                })
                val fadeBackIn: ObjectAnimator = ObjectAnimator.ofFloat(
                    txtvEpisodeTitle, "alpha", 1f
                )
                val set: AnimatorSet = AnimatorSet()
                set.playSequentially(verticalMarquee, fadeOut, fadeBackIn)
                set.start()
            }
        }))
        displayedChapterIndex = -1
        refreshChapterData(
            ChapterUtils.getCurrentChapterIndex(
                media,
                media.getPosition()
            )
        ) //calls displayCoverImage
        updateChapterControlVisibility()
    }

    private fun updateChapterControlVisibility() {
        if (isDriveMode) {
            return
        }
        var chapterControlVisible: Boolean = false
        if (media!!.getChapters() != null) {
            chapterControlVisible = media!!.getChapters().size > 0
        } else if (media is FeedMedia) {
            val fm: FeedMedia = (media as FeedMedia)
            // If an item has chapters but they are not loaded yet, still display the button.
            chapterControlVisible = fm.getItem() != null && fm.getItem()!!.hasChapters()
        }
        val newVisibility: Int = if (chapterControlVisible) View.VISIBLE else View.GONE
        if (chapterControl.getVisibility() != newVisibility) {
            chapterControl.setVisibility(newVisibility)
            ObjectAnimator.ofFloat(
                chapterControl,
                "alpha",
                if (chapterControlVisible) 0f else 1f,
                if (chapterControlVisible) 1f else 0f
            )
                .start()
        }
    }

    private fun refreshChapterData(chapterIndex: Int) {
        if (chapterIndex > -1) {
            if (media!!.getPosition() > media!!.getDuration() || chapterIndex >= media!!.getChapters().size - 1) {
                displayedChapterIndex = media!!.getChapters().size - 1
                butNextChapter.setVisibility(View.INVISIBLE)
            } else {
                displayedChapterIndex = chapterIndex
                butNextChapter.setVisibility(View.VISIBLE)
            }
            val chapterTitle: String = media!!.getChapters().get(displayedChapterIndex).getTitle()
            if (!TextUtils.isEmpty(chapterTitle)) {
                chapterTitleTv.setText(chapterTitle)
            }
        }
        displayCoverImage()
    }

    private val currentChapter: Chapter?
        private get() {
            if ((media == null) || (media!!.getChapters() == null) || (displayedChapterIndex == -1)) {
                return null
            }
            return media!!.getChapters().get(displayedChapterIndex)
        }

    private fun seekToPrevChapter() {
        val curr: Chapter? = currentChapter
        if ((controller == null) || (curr == null) || (displayedChapterIndex == -1)) {
            return
        }
        if (displayedChapterIndex < 1) {
            controller!!.seekTo(0)
        } else if (((controller!!.getPosition() - 10000 * controller!!.getCurrentPlaybackSpeedMultiplier())
                    < curr.getStart())
        ) {
            refreshChapterData(displayedChapterIndex - 1)
            controller!!.seekTo(media!!.getChapters().get(displayedChapterIndex).getStart().toInt())
        } else {
            controller!!.seekTo(curr.getStart().toInt())
        }
    }

    private fun seekToNextChapter() {
        if ((controller == null) || (media == null) || (media!!.getChapters() == null
                    ) || (displayedChapterIndex == -1) || (displayedChapterIndex + 1 >= media!!.getChapters().size)
        ) {
            return
        }
        refreshChapterData(displayedChapterIndex + 1)
        controller!!.seekTo(media!!.getChapters().get(displayedChapterIndex).getStart().toInt())
    }

    public override fun onDestroy() {
        super.onDestroy()
        // prevent memory leaks
        root = null
    }

    public override fun onStart() {
        super.onStart()
        controller = object : PlaybackController((getActivity())!!) {
            public override fun loadMediaInfo() {
                this@CoverFragment.loadMediaInfo(false)
            }

            override fun updatePlayButtonShowsPlay(showPlay: Boolean) {
                if (layoutWithPlayerTheme == R.layout.circle_cover_fragment) {
                    if (rotateAnimator != null) {
                        if (!showPlay) {
                            if (rotateAnimator!!.isStarted()) rotateAnimator!!.resume() else rotateAnimator!!.start()
                        } else {
                            rotateAnimator!!.pause()
                        }
                    }
                } else if (layoutWithPlayerTheme == R.layout.vinyl_cover_fragment) {
                    if (!showPlay) {
                        imgvCoverVinyl!!.start()
                    } else {
                        imgvCoverVinyl!!.pause()
                    }
                }
            }
        }
        controller!!.init()
        loadMediaInfo(false)
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        super.onStop()
        if (disposable != null) {
            disposable!!.dispose()
        }
        controller!!.release()
        controller = null
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent) {
        val newChapterIndex: Int = ChapterUtils.getCurrentChapterIndex(media, event.getPosition())
        if (newChapterIndex > -1 && newChapterIndex != displayedChapterIndex) {
            refreshChapterData(newChapterIndex)
        }
    }

    private fun displayCoverImage() {
        val options: RequestOptions
        if (layoutWithPlayerTheme == R.layout.full_cover_fragment) {
            //不需要rect round
            options = RequestOptions()
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .centerCrop()
                .dontAnimate()
        } else {
            options = RequestOptions()
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .dontAnimate()
                .transforms(
                    CenterCrop(),
                    RoundedCorners((16 * getResources().getDisplayMetrics().density).toInt())
                )
        }
        val cover: RequestBuilder<BitmapPaletteWrapper> = Glide.with(this).`as`(
            BitmapPaletteWrapper::class.java
        )
            .load(media!!.getImageLocation())
            .error(
                Glide.with(this).`as`(BitmapPaletteWrapper::class.java)
                    .load(ImageResourceUtils.getFallbackImageLocation((media)!!))
                    .apply(options)
            )
            .apply(options)
        if ((displayedChapterIndex == -1) || (media == null) || (media!!.getChapters() == null
                    ) || TextUtils.isEmpty(
                media!!.getChapters().get(displayedChapterIndex).getImageUrl()
            )
        ) {
            cover.into(object : RetroMusicColoredTarget((imgvCover)!!) {
                public override fun onColorReady(
                    colors: MediaNotificationProcessor,
                    bitmap: Bitmap?
                ) {
                    //drive mode不需要针对cover ui的颜色做调整，否则其他界面监听到event会改变cover的颜色
                    EventBus.getDefault().post(
                        CoverColorChangeEvent(
                            colors,
                            bitmap,
                            if (isDriveMode) null else coverViewHolder
                        )
                    )
                    if (layoutWithPlayerTheme == R.layout.vinyl_cover_fragment) {
                        imgvCoverVinyl!!.setCoverBitmap(bitmap)
                    }
                }
            })
        } else {
            Glide.with(this).`as`(BitmapPaletteWrapper::class.java)
                .load(EmbeddedChapterImage.getModelFor(media, displayedChapterIndex))
                .apply(options)
                .thumbnail(cover)
                .error(cover)
                .into(object : RetroMusicColoredTarget((imgvCover)!!) {
                    public override fun onColorReady(
                        colors: MediaNotificationProcessor,
                        bitmap: Bitmap?
                    ) {
                        EventBus.getDefault().post(
                            CoverColorChangeEvent(
                                colors,
                                bitmap,
                                if (isDriveMode) null else coverViewHolder
                            )
                        )
                        if (layoutWithPlayerTheme == R.layout.vinyl_cover_fragment) {
                            imgvCoverVinyl!!.setCoverBitmap(bitmap)
                        }
                    }
                })
        }
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    fun convertDpToPixel(dp: Float): Float {
        val context: Context = requireActivity().getApplicationContext()
        return dp * (context.getResources()
            .getDisplayMetrics().densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun onPlayPause() {
        if (controller == null) {
            return
        }
        controller!!.playPause()
    }

    private fun copyText(text: String): Boolean {
        val clipboardManager: ClipboardManager? =
            ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("FocusPodcast", text))
        }
        if (Build.VERSION.SDK_INT <= 32) {
            showSnack(
                requireActivity(),
                getResources().getString(R.string.copied_to_clipboard),
                Toast.LENGTH_SHORT
            )
        }
        return true
    }

    private val layoutWithPlayerTheme: Int
        private get() {
            if (isDriveMode) {
                return R.layout.drive_cover_fragment
            }
            when (albumCoverStyle) {
                AlbumCoverStyle.Normal -> return R.layout.cover_fragment
                AlbumCoverStyle.Circle -> return R.layout.circle_cover_fragment
                AlbumCoverStyle.Vinyl -> return R.layout.vinyl_cover_fragment
                AlbumCoverStyle.FullCard -> return R.layout.fullcard_cover_fragment
                AlbumCoverStyle.BlurCard -> return R.layout.blur_card_cover_fragment
                AlbumCoverStyle.Full -> return R.layout.full_cover_fragment
                else -> throw IllegalStateException("Unexpected value: " + albumCoverStyle)
            }
        }

    constructor(isDriveMode: Boolean) {
        this.isDriveMode = isDriveMode
    }

    //必须要有，别问为什么
    constructor() {}

    class CoverViewHolder constructor() {
        var podcastTv: TextView? = null
        var episodeTv: TextView? = null
        var chapterTv: TextView? = null
        var preChapterIv: ImageView? = null
        var nextChapterIv: ImageView? = null
    }

    companion object {
        private val TAG: String = "CoverFragment"
        val SIXTEEN_BY_NINE: Double = 1.7
    }
}