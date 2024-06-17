package allen.town.podcast.fragment

import allen.town.focus_common.extensions.accentColor
import allen.town.focus_common.extensions.tint
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.R
import allen.town.podcast.actionbuttons.*
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.service.download.Downloader
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.Converter
import allen.town.podcast.core.util.DateFormatter
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.core.util.playback.Timeline
import allen.town.podcast.databinding.FeeditemFragmentBinding
import allen.town.podcast.event.FeedItemEvent
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.view.PodWebView
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import code.name.monkey.appthemehelper.util.VersionUtils.hasMarshmallow
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.faltenreich.skeletonlayout.SkeletonLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.ArrayUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * Displays information about a FeedItem and actions.
 */
class FeedItemFragment : Fragment() {
    private var itemsLoaded = false
    private var itemId: Long = 0
    private var item: FeedItem? = null
    private var webviewData: String? = null
    private var downloaderList: List<Downloader>? = null
    private var root: ViewGroup? = null
    private var webvDescription: PodWebView? = null
    private var txtvPodcast: TextView? = null
    private var txtvTitle: TextView? = null
    private var tvSize: TextView? = null
    private var txtvPublished: TextView? = null
    private var imgvCover: ImageView? = null
    private var progbarDownload: CircularProgressIndicator? = null
    private var progbarPlayed: LinearProgressIndicator? = null
    private var downloadIcon: ImageView? = null
    private var actionButton1: ItemActionButton? = null
    private var actionButton2: ItemActionButton? = null
    private var disposable: Disposable? = null
    private var controller: PlaybackController? = null
    private var floatingPlayActionButton: ExtendedFloatingActionButton? = null
    private var skeletonLayout: SkeletonLayout? = null
    private lateinit var feedItemListFragmentBinding: FeeditemFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = requireArguments().getLong(ARG_FEEDITEM)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val layout = inflater.inflate(R.layout.feeditem_fragment, container, false)
        feedItemListFragmentBinding = FeeditemFragmentBinding.bind(layout)
        root = layout.findViewById(R.id.content_root)
        txtvPodcast = layout.findViewById(R.id.txtvPodcast)
        layout.findViewById<View>(R.id.txtvPodcast_l)
            .setOnClickListener { v: View? -> openPodcast() }
        txtvTitle = layout.findViewById(R.id.txtvTitle)
        if (Build.VERSION.SDK_INT >= 23) {
            txtvTitle!!.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
        }
        tvSize = layout.findViewById(R.id.tv_item_size)
        txtvPublished = layout.findViewById(R.id.txtvPublished)
        val scrollView = layout.findViewById<ScrollView>(R.id.scroll_view)
        ThemedFastScroller.create(scrollView)
        floatingPlayActionButton =
            (parentFragment as FeedItemsViewPagerFragment).extendedFloatingActionButton
        if (hasMarshmallow()) {
            scrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY > 0) {
                    floatingPlayActionButton!!.shrink()
                } else if (scrollY < 0) {
                    floatingPlayActionButton!!.extend()
                }
            }
        }
        txtvTitle!!.setEllipsize(TextUtils.TruncateAt.END)
        webvDescription = layout.findViewById(R.id.webvDescription)
        webvDescription!!.setTimecodeSelectedListener(Consumer { time: Int? ->
            if (controller != null && item!!.media != null && controller!!.media != null && item!!.media!!.identifier == controller!!.media.identifier) {
                controller!!.seekTo(time!!)
            } else {
                showSnack(activity, R.string.play_this_to_seek_position, Toast.LENGTH_LONG)
            }
        })
        registerForContextMenu(webvDescription!!)
        imgvCover = layout.findViewById(R.id.imgvCover)
        imgvCover!!.setOnClickListener(View.OnClickListener { v: View? -> openPodcast() })
        progbarDownload = layout.findViewById(R.id.progbarDownload)
        progbarDownload!!.accentColor()

        progbarPlayed = layout.findViewById(R.id.progbarPlayed)
        progbarPlayed!!.accentColor()

        downloadIcon = layout.findViewById(R.id.downloadIcon)

        downloadIcon!!.setOnClickListener(View.OnClickListener { v: View? ->
            actionButton2?.onClick(
                activity
            )
        })
        skeletonLayout = layout.findViewById(R.id.skeletonLayout)
        skeletonLayout!!.showSkeleton()

        webvDescription!!.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    //ScrollView嵌套了其他布局导致 webview 实际展现到页面的时候比较慢
                    skeletonLayout!!.postDelayed({skeletonLayout!!.visibility = View.GONE},350)
                }
            }
        }
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedItemListFragmentBinding.addToFavoritesItemIcon.setOnClickListener {
            DBWriter.addFavoriteItem(item)
        }
        feedItemListFragmentBinding.removeFromFavoritesItemIcon.setOnClickListener {
            DBWriter.removeFavoriteItem(item)
        }
        feedItemListFragmentBinding.addToQueueItemIcon.setOnClickListener {
            DBWriter.addQueueItem(context, item)
        }
        feedItemListFragmentBinding.removeFromQueueItemIcon.setOnClickListener {
            DBWriter.removeQueueItem(context, true, item)
        }

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        controller = object : PlaybackController(requireActivity()) {
            override fun loadMediaInfo() {
                // Do nothing
            }
        }
        controller!!.init()
        load()
    }

    override fun onResume() {
        super.onResume()
        if (itemsLoaded) {
//            progbarLoading!!.visibility = View.GONE
            skeletonLayout!!.visibility = View.GONE
            updateAppearance()
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        controller!!.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (webvDescription != null && root != null) {
            root!!.removeView(webvDescription)
            webvDescription!!.destroy()
        }
    }

    private fun onFragmentLoaded() {
        if (webviewData != null && !itemsLoaded) {
            webvDescription!!.loadDataWithBaseURL(
                "https://127.0.0.1",
                webviewData!!,
                "text/html",
                "utf-8",
                "about:blank"
            )
        }
        updateAppearance()
    }

    private fun updateAppearance() {
        if (item == null) {
            Log.d(TAG, "update appearance item is null")
            return
        }
        txtvPodcast!!.text = item!!.feed.title
        txtvTitle!!.text = item!!.title
        if (item!!.pubDate != null) {
            val pubDateStr = DateFormatter.formatAbbrev(activity, item!!.pubDate)
            txtvPublished!!.text = pubDateStr
            txtvPublished!!.contentDescription = DateFormatter.formatForAccessibility(
                item!!.pubDate
            )
        }
        val options = RequestOptions()
            .error(R.drawable.ic_podcast_background_round)
            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
            .dontAnimate()
        Glide.with(this)
            .load(item!!.imageLocation)
            .error(
                Glide.with(this)
                    .load(ImageResourceUtils.getFallbackImageLocation(item!!))
                    .apply(options)
            )
            .apply(options)
            .centerCrop()
            .into(imgvCover!!)
        updateButtons()
    }

    private fun updatePlayButton() {
        //floatbutton 是公用的，viewpager2默认屏幕外的不会走onResume方法，所以可以根据这个来控制是否更新 floatbutton
        if (isResumed) {
            val media = item!!.media
            floatingPlayActionButton!!.icon =
                ContextCompat.getDrawable(requireContext(), actionButton1!!.drawable)
            floatingPlayActionButton!!.visibility = actionButton1!!.isVisibility

            floatingPlayActionButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                actionButton1?.onClick(
                    activity
                )
            })
            if (media != null) {
                if (media.duration > 0) {
                    floatingPlayActionButton!!.text =
                        Converter.getDurationStringLong(media.duration)
                    floatingPlayActionButton!!.contentDescription =
                        Converter.getDurationStringLocalized(
                            context, media.duration.toLong()
                        )
                    floatingPlayActionButton!!.extend()
                } else {
                    floatingPlayActionButton!!.setText(actionButton1!!.label)
                }
            } else {
                //没有媒体信息不显示时长,text赋值在前，shrink在后没有效果
                floatingPlayActionButton!!.shrink()
                floatingPlayActionButton!!.text = ""
            }
        }
    }

    private var lastPosition: Int = 0

    private fun updateButtons() {
        progbarDownload!!.visibility = View.INVISIBLE
        if (item!!.hasMedia() && downloaderList != null) {
            for (downloader in downloaderList!!) {
                if (downloader.downloadRequest.feedfileType == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                    && downloader.downloadRequest.feedfileId == item!!.media!!.id
                ) {
                    progbarDownload!!.visibility = View.VISIBLE
                    progbarDownload!!.progress = downloader.downloadRequest.progressPercent
                }
            }
        }

        val media = item!!.media
        if (media == null) {
            actionButton1 = MarkAsPlayedActionButton(item!!)
            actionButton2 = VisitWebsiteActionButton(item!!)
            feedItemListFragmentBinding.downloadLayout.visibility = View.GONE
            progbarPlayed!!.setVisibility(View.GONE)
        } else {
            EpisodeItemViewHolder.setSizeTextView(media, context, tvSize!!, null)
            actionButton1 = if (FeedItemUtil.isCurrentlyPlaying(media)) {
                PauseActionButton(item!!)
            } else if (item!!.feed.isLocalFeed) {
                PlayLocalActionButton(item!!)
            } else if (media.isDownloaded) {
                PlayActionButton(item!!)
            } else {
                StreamActionButton(item!!)
            }
            actionButton2 = if (DownloadService.isDownloadingFile(media.download_url)) {
                CancelDownloadActionButton(item!!)
            } else if (!media.isDownloaded) {
                DownloadActionButton(item!!)
            } else {
                DeleteActionButton(item!!)
            }

            if (FeedItemUtil.isPlaying(item!!.media) || item!!.isInProgress) {
                if (lastPosition == 0) {
                    //使用最近的position，否则暂停后重新从item获取的是旧的position
                    lastPosition = media.getPosition()
                }
                val progress: Int = (100.0 * lastPosition / media.getDuration()).toInt()
                progbarPlayed!!.setProgress(progress)
                progbarPlayed!!.setVisibility(View.VISIBLE)
            } else {
                progbarPlayed!!.setVisibility(View.GONE)
            }
        }

        //设置了强调色
        if (actionButton2!!.getDrawableTintColor(context) != -1) {
            downloadIcon!!.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    actionButton2!!.drawable
                )!!.tint(actionButton2!!.getDrawableTintColor(context))
            )
        } else {
            downloadIcon!!.setImageResource(actionButton2!!.drawable)
        }

        feedItemListFragmentBinding.downloadLayout.visibility = actionButton2!!.isVisibility


        val hasMedia = item!!.getMedia() != null
        val isPlaying = hasMedia && FeedItemUtil.isPlaying(item!!.getMedia())
        val isInQueue: Boolean = item!!.isTagged(FeedItem.TAG_QUEUE)
        val fileDownloaded = hasMedia && item!!.getMedia()?.fileExists() ?: false
        val isFavorite: Boolean = item!!.isTagged(FeedItem.TAG_FAVORITE)

        feedItemListFragmentBinding.addToQueueItem.visibility =
            if (!isInQueue && item!!.getMedia() != null)
                View.VISIBLE
            else
                View.GONE

        feedItemListFragmentBinding.removeFromQueueItem.visibility =
            if (isInQueue)
                View.VISIBLE
            else
                View.GONE
        feedItemListFragmentBinding.addToFavoritesItem.visibility =
            if (!isFavorite)
                View.VISIBLE
            else
                View.GONE
        feedItemListFragmentBinding.removeFromFavoritesItem.visibility =
            if (isFavorite)
                View.VISIBLE
            else
                View.GONE

        updatePlayButton()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return webvDescription!!.onContextItemSelected(item)
    }

    private fun openPodcast() {
        if (item == null) {
            return
        }

        val fragment: Fragment = FeedItemlistFragment.newInstance(item!!.feedId)
        (activity as MainActivity?)!!.loadChildFragment(fragment)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        for (item in event.items) {
            if (this.item!!.id == item.id) {
                load()
                return
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent?) {
        if (FeedItemUtil.isCurrentlyPlaying(item?.getMedia())) {
            lastPosition = event!!.position
            progbarPlayed!!.setProgress((100.0 * lastPosition / event.duration).toInt())
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        val update = event.update
        downloaderList = update.downloaders
        if (item == null || item!!.media == null) {
            return
        }
        val mediaId = item!!.media!!.id
        if (ArrayUtils.contains(update.mediaIds, mediaId)) {
            if (itemsLoaded && activity != null) {
                updateButtons()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerStatusChanged(event: PlayerStatusEvent?) {
        updateButtons()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        load()
    }

    private fun load() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (!itemsLoaded) {
//            progbarLoading!!.visibility = View.VISIBLE
            skeletonLayout!!.showSkeleton()
        }
        disposable = Observable.fromCallable { loadInBackground() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result: FeedItem? ->
//                progbarLoading!!.visibility = View.GONE
//                skeletonLayout!!.showOriginal()
                item = result
                onFragmentLoaded()
                itemsLoaded = true
            }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    private fun loadInBackground(): FeedItem? {
        val feedItem = DBReader.getFeedItem(itemId)
        val context = context
        if (feedItem != null && context != null) {
            val duration = if (feedItem.media != null) feedItem.media!!.duration else Int.MAX_VALUE
            DBReader.loadDescriptionOfFeedItem(feedItem)
            val t = Timeline(context, feedItem.description, duration)
            webviewData = t.processShownotes()
        }
        return feedItem
    }

    companion object {
        private const val TAG = "ItemFragment"
        private const val ARG_FEEDITEM = "feeditem"

        /**
         * Creates a new instance of an ItemFragment
         *
         * @param feeditem The ID of the FeedItem to show
         * @return The ItemFragment instance
         */
        @JvmStatic
        fun newInstance(feeditem: Long): FeedItemFragment {
            val fragment = FeedItemFragment()
            val args = Bundle()
            args.putLong(ARG_FEEDITEM, feeditem)
            fragment.arguments = args
            return fragment
        }
    }
}