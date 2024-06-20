package allen.town.podcast.fragment

import allen.town.focus_common.util.DoubleClickBackToContentTopListener
import allen.town.focus_common.util.ImageUtils.getColoredDrawable
import allen.town.focus_common.util.MenuIconUtil.showToolbarMenuIcon
import allen.town.focus_common.util.StatusBarUtils.setPaddingStatusBarTop
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.util.Util.dp2Px
import allen.town.podcast.MyApp.Companion.runOnUiThread
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.activity.MainActivity.Companion.onPanelCollapsed
import allen.town.podcast.activity.RssSearchActivity.Companion.feedInFeedlist
import allen.town.podcast.activity.RssSearchActivity.Companion.getFeedId
import allen.town.podcast.adapter.EpisodeItemListAdapter
import allen.town.podcast.adapter.MultiSelectAdapter
import allen.town.podcast.adapter.MultiSelectAdapter.OnPrepareActionModeListener
import allen.town.podcast.adapter.MultiSelectAdapter.OnSelectModeListener
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.feed.FeedEvent
import allen.town.podcast.core.feed.FeedUrlNotFoundException
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.glide.FastBlurTransformation
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.download.DownloadRequestCreator
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBTasks
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.FeedItemPermutors
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.core.util.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import allen.town.podcast.core.util.ui.ListFooterUtil
import allen.town.podcast.dialog.RemoveFeedDialog.OnFeedRemovedListener
import allen.town.podcast.dialog.RemoveFeedDialog.show
import allen.town.podcast.dialog.RenameItemDialog
import allen.town.podcast.discovery.PodcastSearcherRegistry
import allen.town.podcast.discovery.RetrieveFeedUtil.tryToRetrieveFeedUrlBySearch
import allen.town.podcast.event.*
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.fragment.actions.EpisodeMultiSelectActionHandler
import allen.town.podcast.fragment.swipeactions.SwipeActions
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.menuprocess.FeedMenuProcess
import allen.town.podcast.model.feed.Feed
import allen.town.podcast.util.SkeletonRecyclerDelay
import allen.town.podcast.view.FeedItemListToolbarIconTintHelper
import allen.town.podcast.view.SeriesDetailInfoView
import allen.town.podcast.view.StorePositionRecyclerView
import allen.town.podcast.view.SubscribeButton
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.res.Configuration
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.appthemehelper.util.ATHUtil.resolveColor
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.joanzapata.iconify.Iconify
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.Validate
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays a list of FeedItems.
 */
class FeedItemlistFragment() : Fragment(), OnItemClickListener, Toolbar.OnMenuItemClickListener,
    OnSelectModeListener , DoubleClickBackToContentTopListener.IBackToContentTopView {
    private var adapter: FeedItemListAdapter? = null
    private var swipeActions: SwipeActions? = null
    private var nextPageLoader: ListFooterUtil? = null
    private lateinit var recyclerView: StorePositionRecyclerView
    private lateinit var txtvTitle: TextView
    private lateinit var txtvFailure: TextView
    private lateinit var imgvBackground: ImageView
    private lateinit var imgvCover: ImageView
    private lateinit var txtvInformation: TextView
    private lateinit var txtvAuthor: TextView
    private lateinit var txtvUpdatesDisabled: TextView
    private lateinit var header: View
    private lateinit var toolbar: Toolbar
    private var displayUpArrow = false
    private var feedID: Long = 0
    private var feed: Feed? = null
    private var headerCreated = false
    private var isUpdatingFeed = false
    private var disposable: Disposable? = null
    private lateinit var detailInfoView: SeriesDetailInfoView
    private lateinit var skeleton: Skeleton
    private lateinit var mInfoViewToggleButton: View
    private lateinit var detailInfoViewContainer: FrameLayout
    private lateinit var subscribe_button: SubscribeButton
    private var isDownloadingFeed = false
    private var updateDownloadStatus: Disposable? = null
    private var iconTintManager: FeedItemListToolbarIconTintHelper? = null
    private lateinit var appBar: AppBarLayout
    private lateinit var skeletonRecyclerDelay: SkeletonRecyclerDelay

    @BindView(R.id.filter_items)
    public lateinit var filterImage:ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //保留这行当前界面切换暗黑模式item不显示
//        setRetainInstance(true);
        val args = arguments
        Validate.notNull(args)
        feedID = args!!.getLong(ARGUMENT_FEED_ID)
        feed = args.getParcelable(ARGUMENT_FEED)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.feed_item_list_fragment, container, false)
        ButterKnife.bind(this, root)
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.feedlist)
        showToolbarMenuIcon(toolbar)
        toolbar.setOnMenuItemClickListener(this)
        toolbar.setOnClickListener(DoubleClickBackToContentTopListener(this))
        displayUpArrow = parentFragmentManager.backStackEntryCount != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (activity as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        refreshToolbarState()
        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.setRecycledViewPool((activity as MainActivity?)!!.recycledViewPool)
        create(recyclerView)
        skeleton = recyclerView.applySkeleton(R.layout.item_small_recyclerview_skeleton, 15)
        skeletonRecyclerDelay = SkeletonRecyclerDelay(skeleton, recyclerView)
        skeletonRecyclerDelay.showSkeleton()
        txtvTitle = root.findViewById(R.id.txtvTitle)
        txtvAuthor = root.findViewById(R.id.txtvAuthor)
        imgvBackground = root.findViewById(R.id.imgvBackground)
        imgvCover = root.findViewById(R.id.imgvCover)
        txtvInformation = root.findViewById(R.id.txtvInformation)
        txtvFailure = root.findViewById(R.id.txtvFailure)
        txtvUpdatesDisabled = root.findViewById(R.id.txtvUpdatesDisabled)
        header = root.findViewById(R.id.headerContainer)
        appBar = root.findViewById<AppBarLayout>(R.id.appBar)
        val collapsingToolbar = root.findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        detailInfoView = root.findViewById(R.id.detailInfoView)
        mInfoViewToggleButton = root.findViewById(R.id.info_view_toggle_button)
        imgvBackground.setOnClickListener(View.OnClickListener { v: View? -> openAbout() })
        detailInfoViewContainer = root.findViewById(R.id.series_info_view_container)
        subscribe_button = root.findViewById(R.id.subscribe_button)
        subscribe_button.setTickSize(dp2Px((context)!!, 18.0f))
        subscribe_button.setOnClickListener(View.OnClickListener { v: View? ->
            if (subscribe_button.isSubscribed()) {
                show((getContext())!!, (feed)!!, object : OnFeedRemovedListener {
                    override fun onFeedRemoved() {
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack()
                        } else {
                            (getActivity() as MainActivity?)!!.loadFragment(
                                SubFeedsFragment.TAG,
                                null
                            )
                        }
                    }
                })
            } else {
                DBWriter.subscribeFeed(feed, getContext())
            }
        })

        //需要设置paddingtop
        setPaddingStatusBarTop((activity)!!, toolbar)
        setPaddingStatusBarTop((activity)!!, header)
        iconTintManager = FeedItemListToolbarIconTintHelper(context, toolbar, collapsingToolbar)
        iconTintManager!!.updateTint()
        appBar.addOnOffsetChangedListener(iconTintManager)
        nextPageLoader = ListFooterUtil(root.findViewById(R.id.more_content_list_footer))
        nextPageLoader!!.setClickListener({
            if (feed != null) {
                DBTasks.loadNextPageOfFeed(getActivity(), feed, false)
            }
        })
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, deltaX: Int, deltaY: Int) {
                super.onScrolled(view, deltaX, deltaY)
                val hasMorePages = (feed != null) && feed!!.isPaged && (feed!!.nextPageLink != null)
                val pageLoaderVisible = recyclerView.isScrolledToBottom && hasMorePages
                nextPageLoader!!.root.visibility =
                    if (pageLoaderVisible) View.VISIBLE else View.GONE
                recyclerView.setPadding(
                    recyclerView.getPaddingLeft(), 0, recyclerView.getPaddingRight(),
                    if (pageLoaderVisible) nextPageLoader!!.root.measuredHeight else 0
                )
            }
        })
        EventBus.getDefault().register(this)
        val swipeRefreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getInteger(R.integer.swipe_refresh_distance))
        swipeRefreshLayout.setOnRefreshListener {
            if (feed != null) {
                DBTasks.forceRefreshFeed(requireContext(), feed, true)
            } else {
                Timber.e("not going to refresh feed becasue is null")
            }

            Handler(Looper.getMainLooper()).postDelayed(
                Runnable { swipeRefreshLayout.setRefreshing(false) },
                getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms).toLong()
            )
        }
        loadItems()
//        loadAd(requireActivity(), true)
        return root
    }

    fun updateTint() {
        if (iconTintManager != null) {
            iconTintManager!!.updateTint()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        //新的fragment add 才会isHidden=false
        Log.d(
            TAG,
            "==>onHiddenChanged,isHidden=" + hidden + ",getUserVisibleHint=" + userVisibleHint
        )
        if (hidden) {
//            if (isPageResume && getUserVisibleHint()) {
//                onPagePause();
//            }
            //离开界面statusbar颜色就要还原
            onPanelCollapsed((activity as AppCompatActivity?)!!)
        } else {
//            if (!isPageResume && getUserVisibleHint()) {
//                onPageResume();
//            }
            //进入界面statusbar颜色就要重置
            if (iconTintManager != null) {
                iconTintManager!!.updateTint()
            }
        }
        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //界面退出还原
        onPanelCollapsed((activity as AppCompatActivity?)!!)
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (adapter != null) {
            adapter!!.endSelectMode()
        }
        if (updateDownloadStatus != null) {
            updateDownloadStatus!!.dispose()
        }
//        loadAd(requireActivity(), true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    private val updateRefreshMenuItemChecker = UpdateRefreshMenuItemChecker {
        DownloadService.isRunning && DownloadService.isDownloadingFile(
            feed!!.getDownload_url()
        )
    }

    private fun refreshToolbarState() {
        if (feed == null) {
            return
        }
        toolbar.menu.findItem(R.id.share_link_item).isVisible = feed!!.link != null
        toolbar.menu.findItem(R.id.visit_website_item).isVisible = feed!!.link != null
        toolbar.menu.findItem(R.id.feed_setting).isVisible = feed!!.isSubscribed
        toolbar.menu.findItem(R.id.rename_item).isVisible = feed!!.isSubscribed
        isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(
            toolbar.menu,
            R.id.refresh_item, updateRefreshMenuItemChecker
        )
        FeedMenuProcess.onPrepareOptionsMenu(toolbar.menu, feed)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val horizontalSpacing =
            resources.getDimension(R.dimen.additional_horizontal_spacing).toInt()
        header!!.setPadding(
            horizontalSpacing,
            header!!.paddingTop,
            horizontalSpacing,
            header!!.paddingBottom
        )
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (feed == null) {
            showSnack(activity, R.string.please_wait_for_data, Toast.LENGTH_LONG)
            return true
        }
        val feedMenuHandled = FeedMenuProcess.onOptionsItemClicked(activity, item, feed)
        if (feedMenuHandled) {
            return true
        }
        val itemId = item.itemId
        if (itemId == R.id.rename_item) {
            RenameItemDialog((activity)!!, feed).show()
            return true
        } else if (itemId == R.id.feed_setting) {
            if (feed != null) {
                val fragment: FeedSettingsFragment =
                    FeedSettingsFragment.newInstance(feed!!)
                (getActivity() as MainActivity?)!!.loadChildFragment(fragment)
            }
            return true
        }
        return false
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val selectedItem = adapter!!.longPressedItem
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection")
            return super.onContextItemSelected(item)
        }
        return if (adapter!!.onContextItemSelected(item)) {
            true
        } else FeedItemMenuProcess.onMenuItemClicked(this, item.itemId, selectedItem)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (adapter == null) {
            return
        }
        val activity = activity as MainActivity?
        val ids = FeedItemUtil.getIds(feed!!.items)
        activity!!.loadChildFragment(
            FeedItemsViewPagerFragment.Companion.newInstance(
                ids,
                position
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: FeedEvent) {
        if (event.feedId == feedID) {
            loadItems()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        if (feed == null || feed!!.items == null) {
            return
        } else if (adapter == null) {
            loadItems()
            return
        }
        var i = 0
        val size = event.items.size
        while (i < size) {
            val item = event.items[i]
            val pos = FeedItemUtil.indexOfItemWithId(feed!!.items, item.id)
            if (pos >= 0) {
                feed!!.items.removeAt(pos)
                feed!!.items.add(pos, item)
                adapter!!.notifyItemChangedCompat(pos)
            }
            i++
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        val update = event.update
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeed)) {
            updateSyncProgressBarVisibility()
        }
        if ((adapter != null) && (update.mediaIds.size > 0) && (feed != null)) {
            for (mediaId: Long in update.mediaIds) {
                val pos = FeedItemUtil.indexOfItemWithMediaId(feed!!.items, mediaId)
                if (pos >= 0) {
                    adapter!!.notifyItemChangedCompat(pos)
                }
            }
        }
        if ((feed != null) && !DownloadService.isDownloadingFile(
                feed!!.download_url
            ) && isDownloadingFeed
        ) {
            updateDownloadStatus = Observable.fromCallable(
                { DBReader.getAllFeedList() })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { feeds: List<Feed>? ->
                        this@FeedItemlistFragment.feeds = feeds
                        if (feedInFeedlist(feeds, feed)) {
                            //下载完成了，数据库中已有该feed了
                            isDownloadingFeed = false
                            Log.i(TAG, "set isDownloadingFeed = false ")
                            //从数据库中查找feedId(通过下载url)
                            feedID = getFeedId(feeds, feed)
                            updateUi()
                        }
                    }, { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
                )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent?) {
        if (adapter != null) {
            for (i in 0 until adapter!!.itemCount) {
                val holder =
                    recyclerView!!.findViewHolderForAdapterPosition(i) as EpisodeItemViewHolder?
                if (holder != null && holder.isCurrentlyPlayingItem) {
                    holder.notifyPlaybackPositionUpdated((event)!!)
                    break
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun favoritesChanged(event: FavoritesEvent?) {
        updateUi()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onQueueChanged(event: QueueEvent?) {
        updateUi()
    }

    override fun onStartSelectMode() {
        swipeActions!!.detach()
        toolbar!!.visibility = View.GONE
        refreshToolbarState()
    }

    override fun onEndSelectMode() {
        swipeActions!!.attachTo(recyclerView)
        toolbar!!.visibility = View.VISIBLE
    }

    private fun updateUi() {
        loadItems()
        updateSyncProgressBarVisibility()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerStatusChanged(event: PlayerStatusEvent?) {
        updateUi()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        updateUi()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFeedListChanged(event: FeedListUpdateEvent) {
        if (feed != null && event.contains(feed)) {
            updateUi()
        }
    }

    private var feeds: List<Feed>? = null
    private fun updateSyncProgressBarVisibility() {
        if (isUpdatingFeed != updateRefreshMenuItemChecker.isRefreshing) {
            refreshToolbarState()
        }
        if (!DownloadService.isDownloadingFeeds()) {
            nextPageLoader!!.root.visibility = View.GONE
        }
        nextPageLoader!!.setLoadingState(DownloadService.isDownloadingFeeds())
    }

    /**
     * 展示列表
     */
    private fun displayList() {
        if (view == null) {
            Log.e(TAG, "Required root view is not yet created. Stop binding data to UI.")
            return
        }
        if (adapter == null) {
            recyclerView!!.adapter = null
            adapter = FeedItemListAdapter(activity as MainActivity?)
            adapter!!.setOnSelectModeListener(this)
            recyclerView!!.adapter = adapter
            swipeActions = SwipeActions(this, TAG).attachTo(recyclerView)
            adapter!!.setOnMenuItemClickListener(object :
                MultiSelectAdapter.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?) {
                    EpisodeMultiSelectActionHandler(
                        (activity as MainActivity?),
                        adapter!!.selectedItems
                    )
                        .handleAction(item!!.itemId)
                    adapter!!.endSelectMode()
                }
            })
            adapter!!.setonPrepareActionListener(object : OnPrepareActionModeListener {
                override fun onPrepareActionMode(mode: ActionMode?, item: Menu?) {
                    if (feed!!.isLocalFeed) {
                        item!!.findItem(R.id.download_batch).isVisible = false
                        item.findItem(R.id.delete_batch).isVisible = false
                    }
                }
            })
        }
        if (skeleton.isSkeleton()) {
            skeletonRecyclerDelay.showOriginal()
        }
        if (feed != null && feed!!.items != null) {
            adapter!!.updateItems(feed!!.items)
            swipeActions!!.setFilter(feed!!.itemFilter)
        }
        refreshToolbarState()
        updateSyncProgressBarVisibility()
    }

    /**
     * 刷新header feed标题图片等
     */
    private fun refreshHeaderView() {
        setupHeaderView()
        if (recyclerView == null || feed == null) {
            Log.e(TAG, "Unable to refresh header view")
            return
        }
        loadFeedImage()
        if (feed!!.hasLastUpdateFailed()) {
            txtvFailure.visibility = View.VISIBLE
        } else {
            txtvFailure.visibility = View.INVISIBLE
        }
        if (feed!!.preferences != null && !feed!!.preferences.keepUpdated) {
            txtvUpdatesDisabled.text =
                "{md-pause-circle-outline} " + this.getString(R.string.updates_disabled_label)
            Iconify.addIcons(txtvUpdatesDisabled)
            txtvUpdatesDisabled.visibility = View.VISIBLE
        } else {
            txtvUpdatesDisabled.visibility = View.GONE
        }
        txtvTitle.text = feed!!.title
        txtvAuthor.text = feed!!.author
        if (feed!!.itemFilter != null) {
            if (feed!!.itemFilter!!.values.isNotEmpty()) {
                filterImage.setImageResource(R.drawable.ic_filter_disable)
            } else {
                filterImage.setImageResource(R.drawable.ic_filter)
            }
        } else {
            filterImage.setImageResource(R.drawable.ic_filter)
        }
    }

    private fun setupHeaderView() {
        if (feed == null || headerCreated) {
            return
        }

        // https://github.com/bumptech/glide/issues/529
        imgvBackground!!.colorFilter = LightingColorFilter(-0x99999a, 0x000000)
        imgvCover!!.setOnClickListener({ v: View? -> openAbout() })
        headerCreated = true
    }

    private fun loadFeedImage() {
        Glide.with(this)
            .load(feed!!.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.color.image_readability_tint)
                    .error(R.color.image_readability_tint)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .transform(FastBlurTransformation())
                    .dontAnimate()
            )
            .into((imgvBackground)!!)
        Glide.with(this)
            .load(feed!!.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_podcast_background_round)
                    .error(R.drawable.ic_podcast_background_round)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .centerCrop()
                    .dontAnimate()
            )
            .into((imgvCover)!!)
    }

    /**
     * 加载items
     */
    private fun loadItems() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable({ loadData() })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Feed? ->
                    refreshHeaderView()
                    if (!isDownloadingFeed) {
                        //如果正在下载不需要处理这些界面逻辑
                        displayList()
                        detailInfoView.setEpisodesLoaded(true)
                        if (feed != null && feed!!.getId() > 0) {
                            subscribe_button.setVisibility(View.VISIBLE)
                        }
                    }
                }, { error: Throwable? ->
                    feed = null
                    refreshHeaderView()
                    displayList()
                    Log.e(TAG, Log.getStackTraceString(error))
                })
    }

    private fun initDetailView() {
        runOnUiThread({
            if (getContext() == null) {
                //从桌面快捷方式进入，因为会加载两个该item（一个最近，一个当前的）导致这里为空，而且后续rxjava两个分支都不走，原因未知
                Timber.e(" loadItems break getContext() == null")
                return@runOnUiThread
            }
            //要放在主线程中执行哦，后面如果本地数据库不存在需要从网络获取，就放在下载之前调用
            if (!feed!!.isLocalFeed()) {
                mInfoViewToggleButton!!.setVisibility(View.VISIBLE)
                mInfoViewToggleButton!!.setOnClickListener(View.OnClickListener { v: View? -> openAbout() })
                mInfoViewToggleButton!!.setBackground(
                    getColoredDrawable(
                        getContext(), R.drawable.shape_circle, resolveColor(
                            (getContext())!!, android.R.attr.windowBackground
                        )
                    )
                )
                if (!feed!!.isSubscribed()) {
                    detailInfoView!!.expandCollapseContent(false, false)
                } else {
                    detailInfoView!!.setVisibility(View.GONE)
                }
            } else {
                detailInfoView!!.setVisibility(View.GONE)
            }

            //订阅按钮状态
            subscribe_button!!.setCircleRingColor(
                resolveColor(
                    (getContext())!!,
                    android.R.attr.windowBackground
                )
            )
            subscribe_button!!.setSubscribedIconColor(accentColor((getContext())!!))
            subscribe_button!!.setSubscribed(feed!!.isSubscribed())
            var circleFillColor: Int = 0
            if (feed!!.isSubscribed()) {
                circleFillColor = subscribe_button!!.getCircleRingColor()
            }
            subscribe_button!!.setCircleFillColor(circleFillColor)
            detailInfoView!!.setData(feed, getActivity())
        })
    }

    private var finalGetFeedUrl = false
    private fun loadData(): Feed? {
        if (feedID > 0) {
            //有feedid说明在数据库中是存在的
            feed = DBReader.getFeed(feedID, true)
        }
        if (feed == null) {
            return null
        } else {
            initDetailView()
            if (feed!!.id == 0L) {
                var feedFromDb: Feed? = null
                if (!TextUtils.isEmpty(feed!!.itunesId)) {
                    //来自itunes
                    Timber.i("feedUrl from itunes ")
                    feedFromDb = DBReader.getFeedByItunesFeedId(feed!!.itunesId, true)
                } else {
                    feedFromDb = DBReader.getFeed(feed!!.download_url, true)
                }
                if (feedFromDb == null) {
                    //id为0，并且通过feedUrl也查询不到才进入，否则会有多条记录
                    PodcastSearcherRegistry.lookupUrl(feed!!.download_url)
                        .subscribeOn(Schedulers.trampoline())
                        .observeOn(Schedulers.trampoline())
                        .subscribe(
                            { feedUrl: String ->
                                Timber.i("get feedUrl from itunes " + feedUrl)
                                feed!!.setDownload_url(feedUrl)
                                finalGetFeedUrl = true
                            },
                            { error: Throwable? ->
                                if (error is FeedUrlNotFoundException) {
                                    finalGetFeedUrl = !TextUtils.isEmpty(
                                        tryToRetrieveFeedUrlBySearch(
                                            (error as FeedUrlNotFoundException?)!!
                                        )
                                    )
                                } else {
                                    feed!!.setLastUpdateFailed(true)
                                    runOnUiThread {
                                        //放在主线程中执行
                                        txtvFailure.setText(R.string.null_value_podcast_error)
                                    }
                                    Log.e(TAG, Log.getStackTraceString(error))
                                }
                            })
                    Log.i(TAG, "check isDownloadingFeed $isDownloadingFeed")
                    if (finalGetFeedUrl) {
                        if (!isDownloadingFeed) {
                            DownloadService.download(
                                context, false, DownloadRequestCreator.create(feed).build()
                            )
                            isDownloadingFeed = true
                        } else {
                            //下载失败后同样会重新loadData，这时我们不能再重新下载了(也有可能其他Event会进入这个分支，检查下是否确实没有在下载了)否则进入了死循环，但是也确实下载完成了
                            if (!DownloadService.isDownloadingFile(
                                    feed!!.download_url
                                )
                            ) {
                                Log.i(TAG, "not downloading setLastUpdateFailed ")
                                isDownloadingFeed = false
                                feed!!.setLastUpdateFailed(true)
                            }
                        }
                    }
                    return feed
                } else {
                    //从数据库中查到feed了所以用db的值
                    feed = feedFromDb
                    feedID = feedFromDb.id
                }
            }
        }
        DBReader.loadAdditionalFeedItemListData(feed!!.items)
        if (feed!!.sortOrder != null) {
            val feedItems = feed!!.items
            FeedItemPermutors.getPermutor(feed!!.sortOrder!!).reorder(feedItems)
            feed!!.items = feedItems
        }
        return feed
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onKeyUp(event: KeyEvent) {
        if (!isAdded || !isVisible || !isMenuVisible) {
            return
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_T -> recyclerView!!.smoothScrollToPosition(0)
            KeyEvent.KEYCODE_B -> recyclerView!!.smoothScrollToPosition(adapter!!.itemCount - 1)
            else -> {}
        }
    }

    private inner class FeedItemListAdapter(mainActivity: MainActivity?) : EpisodeItemListAdapter(
        (mainActivity)!!, R.menu.episodes_multi_menu
    ) {
        override fun beforeBindViewHolder(holder: EpisodeItemViewHolder?, pos: Int) {
            holder!!.coverHolder.visibility = if(Prefs.showEpisodeCoverInFeed) View.VISIBLE else View.GONE
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            super.onCreateContextMenu(menu, v, menuInfo)
            if (!inActionMode()) {
                menu.findItem(R.id.multi_select).isVisible = true
            }
            MenuItemUtils.setOnClickListeners(
                menu,
                { item: MenuItem -> this@FeedItemlistFragment.onContextItemSelected(item) })
        }
    }

    fun openAbout() {
        if (feed == null) {
            return
        }
        if (!feed!!.isLocalFeed && detailInfoView!!.isAllDataLoaded) {
            if (!feed!!.isSubscribed) {
                detailInfoView!!.toggleCollapseMode()
                return
            }
            if (detailInfoView!!.isCollapsed) {
                detailInfoView!!.expandCollapseContent(true, false)
            }
            animateSeriesInfoView("openAbout", null)
        }
    }

    /* synthetic */   fun `lambda$animateSeriesInfoView$2`(valueAnimator: ValueAnimator) {
        val view = mInfoViewToggleButton
        if (view != null) {
            view.rotation = (valueAnimator.animatedValue as Float).toFloat()
        }
    }

    private fun animateSeriesInfoView(str: String, animatorListener: Animator.AnimatorListener?) {
        if (feed == null) {
            return
        }
        if (!feed!!.isLocalFeed) {
            val z = detailInfoView!!.visibility == View.VISIBLE
            detailInfoView!!.clearDescriptionSelection()
            val fArr = FloatArray(2)
            var f = 180.0f
            fArr[0] = if (z) 180.0f else 0.0f
            if (z) {
                f = 0.0f
            }
            fArr[1] = f
            val ofFloat = ValueAnimator.ofFloat(*fArr)
            ofFloat.duration = 150L
            ofFloat.addUpdateListener(AnimatorUpdateListener { valueAnimator ->

                // from class: fm.player.ui.fragments.i
                // android.animation.ValueAnimator.AnimatorUpdateListener
                Timber.e("rote " + valueAnimator.animatedValue)
                `lambda$animateSeriesInfoView$2`(valueAnimator)
            })
            ofFloat.start()
            if (z) {
                val viewHeight = detailInfoView!!.getViewHeight(false)
                if (viewHeight > 0) {
                    detailInfoViewContainer!!.layoutParams.height = viewHeight
                    detailInfoViewContainer!!.pivotY = 0.0f
                    detailInfoViewContainer!!.pivotX = 0.0f
                    val ofInt = ValueAnimator.ofInt(viewHeight, 0)
                    ofInt.addUpdateListener(object : AnimatorUpdateListener {
                        // from class: fm.player.ui.fragments.FeedItemlistFragment.10
                        // android.animation.ValueAnimator.AnimatorUpdateListener
                        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
                            if (detailInfoViewContainer != null) {
                                detailInfoViewContainer!!.scaleY =
                                    (valueAnimator.animatedValue as Int).toFloat() / viewHeight
                                detailInfoView!!.layoutParams.height =
                                    (valueAnimator.animatedValue as Int)
                                detailInfoView!!.requestLayout()
                            }
                        }
                    })
                    ofInt.addListener(object : Animator.AnimatorListener {
                        // from class: fm.player.ui.fragments.FeedItemlistFragment.11
                        // android.animation.Animator.AnimatorListener
                        override fun onAnimationCancel(animator: Animator) {
                            val animatorListener2 = animatorListener
                            animatorListener2?.onAnimationCancel(animator)
                        }

                        // android.animation.Animator.AnimatorListener
                        override fun onAnimationEnd(animator: Animator) {
                            val seriesDetailInfoView = detailInfoView
                            if (seriesDetailInfoView != null) {
                                seriesDetailInfoView.visibility = View.GONE
                            }
                            val animatorListener2 = animatorListener
                            animatorListener2?.onAnimationEnd(animator)
                        }

                        // android.animation.Animator.AnimatorListener
                        override fun onAnimationRepeat(animator: Animator) {
                            val animatorListener2 = animatorListener
                            animatorListener2?.onAnimationRepeat(animator)
                        }

                        // android.animation.Animator.AnimatorListener
                        override fun onAnimationStart(animator: Animator) {
                            val animatorListener2 = animatorListener
                            animatorListener2?.onAnimationStart(animator)
                        }
                    })
                    ofInt.duration = 250L
                    ofInt.interpolator = AccelerateDecelerateInterpolator()
                    ofInt.start()
                    detailInfoView!!.animateFadeOverlayExpandButton(false, 250)
                    return
                }
                return
            }
            detailInfoView!!.measure(0, 0)
            val viewHeight2 = detailInfoView!!.getViewHeight(true)
            if (viewHeight2 > 0) {
                detailInfoViewContainer!!.layoutParams.height = viewHeight2
                detailInfoViewContainer!!.pivotY = 0.0f
                detailInfoViewContainer!!.pivotX = 0.0f
                detailInfoView!!.visibility = View.VISIBLE
                val ofInt2 = ValueAnimator.ofInt(0, viewHeight2)
                ofInt2.addUpdateListener(object : AnimatorUpdateListener {
                    // from class: fm.player.ui.fragments.FeedItemlistFragment.12
                    // android.animation.ValueAnimator.AnimatorUpdateListener
                    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
                        if (detailInfoViewContainer != null) {
                            detailInfoViewContainer!!.scaleY =
                                (valueAnimator.animatedValue as Int).toFloat() / viewHeight2
                            detailInfoView!!.layoutParams.height =
                                (valueAnimator.animatedValue as Int)
                            detailInfoView!!.requestLayout()
                        }
                    }
                })
                ofInt2.addListener(object : Animator.AnimatorListener {
                    // from class: fm.player.ui.fragments.FeedItemlistFragment.13
                    // android.animation.Animator.AnimatorListener
                    override fun onAnimationCancel(animator: Animator) {}

                    // android.animation.Animator.AnimatorListener
                    override fun onAnimationEnd(animator: Animator) {
                        val FeedItemlistFragment = this@FeedItemlistFragment
                        if (FeedItemlistFragment.detailInfoViewContainer != null) {
                            FeedItemlistFragment.detailInfoView!!.layoutParams.height =
                                if (detailInfoView!!.isCollapsed) detailInfoView!!.collapsedHeight else -2
                            detailInfoViewContainer!!.layoutParams.height = -2
                            detailInfoViewContainer!!.requestLayout()
                            detailInfoView!!.requestLayout()
                        }
                    }

                    // android.animation.Animator.AnimatorListener
                    override fun onAnimationRepeat(animator: Animator) {}

                    // android.animation.Animator.AnimatorListener
                    override fun onAnimationStart(animator: Animator) {}
                })
                ofInt2.duration = 250L
                ofInt2.interpolator = AccelerateDecelerateInterpolator()
                ofInt2.start()
                detailInfoView!!.animateFadeOverlayExpandButton(true, 250)
            }
        }
    }

    @OnClick(R.id.filter_items)
    fun filterFeedItems() {
        if (feed == null) {
            showSnack(activity, R.string.please_wait_for_data, Toast.LENGTH_LONG)
        }
        FeedMenuProcess.showFilterDialog(context, feed)
    }

    @OnClick(R.id.sort_items)
    fun sortFeedItems() {
        if (feed == null) {
            showSnack(activity, R.string.please_wait_for_data, Toast.LENGTH_LONG)
        }
        FeedMenuProcess.showSortDialog(context, feed)
    }

    @OnClick(R.id.action_search)
    fun searchFeedItems() {
        if (feed == null) {
            showSnack(activity, R.string.please_wait_for_data, Toast.LENGTH_LONG)
        }
        (activity as MainActivity?)!!.loadChildFragment(
            LocalSearchFragment.Companion.newInstance(
                feed!!.id, feed!!.title
            )
        )
    }

    companion object {
        @JvmField
        val TAG = "ItemlistFragment"
        private val ARGUMENT_FEED_ID = "argument.allen.town.podcast.feed_id"
        private val ARGUMENT_FEED = "argument.allen.town.podcast.feed"
        private val KEY_UP_ARROW = "up_arrow"

        /**
         * Creates new ItemlistFragment which shows the Feeditems of a specific
         * feed. Sets 'showFeedtitle' to false
         *
         * @param feedId The id of the feed to show
         * @return the newly created instance of an ItemlistFragment
         */
        @JvmStatic
        fun newInstance(feedId: Long): FeedItemlistFragment {
            val i = FeedItemlistFragment()
            val b = Bundle()
            b.putLong(ARGUMENT_FEED_ID, feedId)
            i.arguments = b
            return i
        }

        @JvmStatic
        fun newInstance(feed: Feed?): FeedItemlistFragment {
            val i = FeedItemlistFragment()
            val b = Bundle()
            b.putParcelable(ARGUMENT_FEED, feed)
            i.arguments = b
            return i
        }
    }

    override fun backToContentTop() {
        recyclerView.scrollToPosition(5)
        recyclerView.post { recyclerView.smoothScrollToPosition(0) }
        appBar.setExpanded(true)
    }
}