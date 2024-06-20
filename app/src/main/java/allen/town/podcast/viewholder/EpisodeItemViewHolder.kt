package allen.town.podcast.viewholder

import allen.town.focus_common.extensions.accentColor
import allen.town.focus_common.util.Timber
import allen.town.podcast.R
import allen.town.podcast.actionbuttons.CancelDownloadActionButton
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.CoverLoader
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.core.util.Converter
import allen.town.podcast.core.util.DateFormatter
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.NetworkUtils
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.model.playback.MediaType
import allen.town.podcast.view.PlayPauseProgressButton
import android.content.Context
import android.os.Build
import android.text.Layout
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.joanzapata.iconify.Iconify

/**
 * Holds the view which shows FeedItems.
 */
class EpisodeItemViewHolder(private val activity: MainActivity, parent: ViewGroup?) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(
            activity
        ).inflate(R.layout.feeditemlist_item, parent, false)
    ) {
    private val container: View
    val dragHandle: ImageView
    private val placeholder: TextView
    private val cover: ImageView
    private val title: TextView
    private val pubDate: TextView
    private val duration: TextView
    val size: TextView
    val isInQueue: ImageView
    private val isVideo: ImageView
    val isFavorite: ImageView

    //    private final CircularProgressBar secondaryActionProgress;
    private val separatorIcons: TextView
    val separatorSize: TextView
    private val leftPadding: View
    @JvmField
    val coverHolder: CardView
    val selectCheckBox: CheckBox
    val playPauseProgressButton: PlayPauseProgressButton
    private val progressLayout: View
    var feedItem: FeedItem? = null
        private set
    private val cancelDownloadButtons: View
    @JvmField
    val downloadedButton: ImageView
    private val downloadProgress: CircularProgressIndicator
    private val playingLottieView: LottieAnimationView
    fun bind(item: FeedItem) {
        feedItem = item
        placeholder.text = item.feed.title
        title.text = item.title
        leftPadding.contentDescription = item.title
        pubDate.text = DateFormatter.formatAbbrev(activity, item.pubDate)
        pubDate.contentDescription = DateFormatter.formatForAccessibility(item.pubDate)
        isFavorite.visibility =
            if (item.isTagged(FeedItem.TAG_FAVORITE)) View.VISIBLE else View.GONE
        isInQueue.visibility = if (item.isTagged(FeedItem.TAG_QUEUE)) View.VISIBLE else View.GONE
        container.alpha = if (item.isPlayed) 0.5f else 1.0f
        playPauseProgressButton.setFeedItem(item)
        if (FeedItemUtil.isCurrentlyPlaying(item.media)) {
            playPauseProgressButton.setPlayingAndPlayed(true, false, false)
        } else {
            playPauseProgressButton.setPlayingAndPlayed(false, false, false)
        }
        playPauseProgressButton.progress = 0
        playPauseProgressButton.configure(playPauseProgressButton, null, activity)
        playPauseProgressButton.setAccentDefaultTheme()
        cancelDownloadButtons.setOnClickListener { v: View? ->
            CancelDownloadActionButton(item).onClick(
                activity
            )
        }
        setSizeTextView(item.media, activity, size, separatorSize)
        //        itemView.setBackgroundResource(ThemeUtils.getDrawableFromAttr(activity, R.attr.rectSelector));
//        itemView.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, R.attr.colorSurface));
//        itemView.setSelected(false);
        (itemView as MaterialCardView).isChecked = false
        playingLottieView.visibility = View.GONE
        if (item.media != null) {
            bind(item.media)
        } else {
            progressLayout.visibility = View.GONE
            cancelDownloadButtons.visibility = View.GONE
            downloadedButton.visibility = View.GONE
            isVideo.visibility = View.GONE
            duration.visibility = View.GONE
        }
        if (coverHolder.visibility == View.VISIBLE) {
            CoverLoader(activity)
                .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                .withFallbackUri(item.feed.imageUrl)
                .withPlaceholderView(placeholder)
                .withCoverView(cover)
                .load()
        }
    }

    private fun bind(media: FeedMedia?) {
        isVideo.visibility = if (media!!.mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        duration.visibility = if (media.duration > 0) View.VISIBLE else View.GONE
        progressLayout.visibility = View.VISIBLE
        if (FeedItemUtil.isCurrentlyPlaying(media)) {
//            itemView.setSelected(true);
            (itemView as MaterialCardView).isChecked = true
            playingLottieView.visibility = View.VISIBLE
        }
        if (DownloadService.isDownloadingFile(media.download_url)) {
            val downloadRequest = DownloadService.findRequest(
                media.download_url
            )
            val percent = 0.01f * downloadRequest.progressPercent
            //            secondaryActionProgress.setPercentage(Math.max(percent, 0.01f), item);
            cancelDownloadButtons.visibility = View.VISIBLE
            downloadedButton.visibility = View.GONE
            downloadProgress.progress = downloadRequest.progressPercent
        } else if (media.isDownloaded) {
            downloadedButton.visibility = View.VISIBLE
            cancelDownloadButtons.visibility = View.GONE
            //            secondaryActionProgress.setPercentage(1, item); // Do not animate 100% -> 0%
        } else {
            downloadedButton.visibility = View.GONE
            cancelDownloadButtons.visibility = View.GONE
            //            secondaryActionProgress.setPercentage(0, item); // Animate X% -> 0%
        }
        duration.text = Converter.getDurationStringLong(media.duration)
        duration.contentDescription = activity.getString(
            R.string.chapter_duration,
            Converter.getDurationStringLocalized(activity, media.duration.toLong())
        )
        if (FeedItemUtil.isPlaying(feedItem!!.media) || feedItem!!.isInProgress) {
            val progress = (100.0 * media.position / media.duration).toInt()
            val remainingTime = Math.max(media.duration - media.position, 0)
            Timber.d(feedItem!!.title + " : " + progress + " " + feedItem.toString())
            playPauseProgressButton.progress = progress
            if (Prefs.shouldShowRemainingTime()) {
                duration.text =
                    (if (remainingTime > 0) "-" else "") + Converter.getDurationStringLong(
                        remainingTime
                    )
                duration.contentDescription = activity.getString(
                    R.string.chapter_duration,
                    Converter.getDurationStringLocalized(
                        activity,
                        (media.duration - media.position).toLong()
                    )
                )
            }
        } else {
        }
    }

    private fun updateDuration(event: PlaybackPositionEvent) {
        feedItem!!.media?.run{
            position = event.position
            duration = event.duration
        }
        val currentPosition = event.position
        val timeDuration = event.duration
        val remainingTime = Math.max(timeDuration - currentPosition, 0)
        Log.v(TAG, "current position -> " + Converter.getDurationStringLong(currentPosition))
        if (currentPosition == PlaybackService.INVALID_TIME || timeDuration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "failed to position because of invalid time")
            return
        }
        if (Prefs.shouldShowRemainingTime()) {
            duration.text =
                (if (remainingTime > 0) "-" else "") + Converter.getDurationStringLong(
                    remainingTime
                )
        } else {
            duration.text = Converter.getDurationStringLong(timeDuration)
        }
    }

    val isCurrentlyPlayingItem: Boolean
        get() = feedItem!!.media != null && FeedItemUtil.isCurrentlyPlaying(feedItem!!.media)

    fun notifyPlaybackPositionUpdated(event: PlaybackPositionEvent) {
        playPauseProgressButton.progress = (100.0 * event.position / event.duration).toInt()
        updateDuration(event)
        duration.visibility =
            View.VISIBLE // Even if the duration was previously unknown, it is now known
    }

    /**
     * Hides the separator dot between icons and text if there are no icons.
     */
    fun hideSeparatorIfNecessary() {
        val hasIcons =
            isInQueue.visibility == View.VISIBLE || isVideo.visibility == View.VISIBLE || isFavorite.visibility == View.VISIBLE
        separatorIcons.visibility = View.GONE
    }

    companion object {
        private const val TAG = "EpisodeItemViewHolder"
        fun setSizeTextView(
            media: FeedMedia?,
            context: Context?,
            size: TextView,
            separatorSize: TextView?
        ) {
            if (media == null) {
                size.visibility = View.GONE
                if (separatorSize != null) {
                    separatorSize.visibility = View.GONE
                }
                return
            }
            size.visibility = if (media.size > 0) View.VISIBLE else View.GONE
            if (separatorSize != null) {
                separatorSize.visibility = if (media.size > 0) View.VISIBLE else View.GONE
            }
            if (media.size > 0) {
                size.text = Formatter.formatShortFileSize(context, media.size)
            } else if (NetworkUtils.isEpisodeHeadDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
                size.text = "{fa-spinner}"
                Iconify.addIcons(size)
                NetworkUtils.getFeedMediaSizeObservable(media).subscribe(
                    { sizeValue: Long ->
                        if (sizeValue > 0) {
                            size.text = Formatter.formatShortFileSize(context, sizeValue)
                            size.visibility = View.VISIBLE
                            if (separatorSize != null) {
                                separatorSize.visibility = View.VISIBLE
                            }
                        } else {
                            size.text = ""
                        }
                    }) { error: Throwable? ->
                    size.text = ""
                    Log.e(TAG, Log.getStackTraceString(error))
                }
            } else {
                size.text = ""
            }
        }
    }

    init {
        container = itemView.findViewById(R.id.container)
        dragHandle = itemView.findViewById(R.id.drag_handle)
        placeholder = itemView.findViewById(R.id.txtvPlaceholder)
        cover = itemView.findViewById(R.id.imgvCover)
        title = itemView.findViewById(R.id.txtvTitle)
        if (Build.VERSION.SDK_INT >= 23) {
            title.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_FULL
        }
        pubDate = itemView.findViewById(R.id.txtvPubDate)
        duration = itemView.findViewById(R.id.txtvDuration)
        isInQueue = itemView.findViewById(R.id.ivInPlaylist)
        isVideo = itemView.findViewById(R.id.ivIsVideo)
        isFavorite = itemView.findViewById(R.id.isFavorite)
        size = itemView.findViewById(R.id.size)
        separatorIcons = itemView.findViewById(R.id.separatorIcons)
        coverHolder = itemView.findViewById(R.id.coverHolder)
        leftPadding = itemView.findViewById(R.id.left_padding)
        itemView.tag = this
        selectCheckBox = itemView.findViewById(R.id.selectCheckBox)
        playPauseProgressButton = itemView.findViewById(R.id.play_pause_button)
        cancelDownloadButtons = itemView.findViewById(R.id.cancel_download_buttons)
        downloadedButton = itemView.findViewById(R.id.downloaded_button)
        downloadProgress = itemView.findViewById(R.id.downloadProgress)
        downloadProgress.accentColor()
        separatorSize = itemView.findViewById(R.id.size_separator)
        progressLayout = itemView.findViewById(R.id.progress)
        playingLottieView = itemView.findViewById(R.id.playing_lottie)
    }
}