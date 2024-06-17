package allen.town.podcast.fragment

import allen.town.focus_common.util.DoubleClickBackToContentTopListener
import allen.town.focus_common.util.MenuIconUtil.showToolbarMenuIcon
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.BuildConfig
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.EpisodeItemListAdapter
import allen.town.podcast.adapter.MultiSelectAdapter
import allen.town.podcast.adapter.MultiSelectAdapter.OnSelectModeListener
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.download.AutoUpdateManager
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.core.util.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import allen.town.podcast.event.FeedItemEvent
import allen.town.podcast.event.FeedListUpdateEvent
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.fragment.LocalSearchFragment.Companion.newInstance
import allen.town.podcast.fragment.actions.EpisodeMultiSelectActionHandler
import allen.town.podcast.fragment.swipeactions.SwipeActions
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedItemFilter
import allen.town.podcast.util.SkeletonRecyclerDelay
import allen.town.podcast.view.EmptyViewHandler
import allen.town.podcast.view.StorePositionRecyclerView
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Shows unread or recently published episodes
 */
abstract class EpisodesListFragment : Fragment(), OnSelectModeListener, DoubleClickBackToContentTopListener.IBackToContentTopView {
    @JvmField
    protected var page = 1
    protected var isLoadingMore = false
    protected var hasMoreItems = true
    lateinit var recyclerView: StorePositionRecyclerView
    var listAdapter: EpisodeItemListAdapter? = null
    var progLoading: ProgressBar? = null
    var loadingMoreView: View? = null
    lateinit var emptyView: EmptyViewHandler
    private var swipeActions: SwipeActions? = null
    protected lateinit var toolbar: Toolbar
    private var displayUpArrow = false
    private var skeleton: Skeleton? = null
    private lateinit var skeletonRecyclerDelay: SkeletonRecyclerDelay
    var episodes: MutableList<FeedItem> = ArrayList()

    @Volatile
    private var isUpdatingFeeds = false
    protected var disposable: Disposable? = null
    @JvmField
    protected var txtvInformation: TextView? = null
    open val prefName: String
        get() = PREF_NAME
    abstract val swipeTag: String
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        loadItems()
    }

    override fun onResume() {
        super.onResume()
        registerForContextMenu(recyclerView)
    }

    override fun onPause() {
        super.onPause()
        recyclerView.saveScrollPosition(prefName)
        unregisterForContextMenu(recyclerView)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    private val updateRefreshMenuItemChecker =
        UpdateRefreshMenuItemChecker { DownloadService.isRunning && DownloadService.isDownloadingFeeds() }

    override fun onPrepareOptionsMenu(menu: Menu) {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(
            menu,
            R.id.refresh_item,
            updateRefreshMenuItemChecker
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) {
            return true
        }
        val itemId = item.itemId
        if (itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext())
            return true
        } else if (itemId == R.id.mark_all_read_item) {
            val markAllReadConfirmationDialog: ConfirmationDialog = object : ConfirmationDialog(
                activity,
                R.string.mark_all_read_label,
                R.string.mark_all_read_confirmation_msg
            ) {
                override fun onConfirmButtonPressed(dialog: DialogInterface) {
                    dialog.dismiss()
                    DBWriter.markAllItemsRead()
                    showSnack(activity, R.string.mark_all_read_msg, Toast.LENGTH_LONG)
                }
            }
            markAllReadConfirmationDialog.createNewDialog().show()
            return true
        }  else if (itemId == R.id.action_search) {
            (activity as MainActivity?)!!.loadChildFragment(newInstance())
            return true
        }
        return false
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (!userVisibleHint || !isVisible || !isMenuVisible) {
            // The method is called on all fragments in a ViewPager, so this needs to be ignored in invisible ones.
            // Apparently, none of the visibility check method works reliably on its own, so we just use all.
            return false
        } else if (listAdapter!!.longPressedItem == null) {
            Log.i(TAG, "selected item was null")
            return super.onContextItemSelected(item)
        } else if (listAdapter!!.onContextItemSelected(item)) {
            return true
        }
        val selectedItem = listAdapter!!.longPressedItem
        return FeedItemMenuProcess.onMenuItemClicked(this, item.itemId, selectedItem!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.all_episodes_fragment, container, false)
        txtvInformation = root.findViewById(R.id.txtvInformation)
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.episodes)
        showToolbarMenuIcon(toolbar)
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item: MenuItem ->
            onOptionsItemSelected(
                item
            )
        })
        toolbar.setOnClickListener(DoubleClickBackToContentTopListener(this))
        onPrepareOptionsMenu(toolbar.getMenu())
        displayUpArrow = parentFragmentManager.backStackEntryCount != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (activity as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        recyclerView = root.findViewById(android.R.id.list)
        recyclerView.setRecycledViewPool((activity as MainActivity?)!!.recycledViewPool)
        create(recyclerView)
        setupLoadMoreScrollListener()
        val animator = recyclerView.getItemAnimator()
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
        val swipeRefreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getInteger(R.integer.swipe_refresh_distance))
        swipeRefreshLayout.setOnRefreshListener {
            AutoUpdateManager.runImmediate(requireContext())
            Handler(Looper.getMainLooper()).postDelayed(
                { swipeRefreshLayout.isRefreshing = false },
                resources.getInteger(R.integer.swipe_to_refresh_duration_in_ms).toLong()
            )
        }
        progLoading = root.findViewById(R.id.progLoading)
        //        progLoading.setVisibility(View.VISIBLE);
        loadingMoreView = root.findViewById(R.id.loadingMore)
        emptyView = EmptyViewHandler(context)
        emptyView.attachToRecyclerView(recyclerView)
        emptyView.setIcon(R.drawable.ic_episodes)
        emptyView.setTitle(R.string.no_all_episodes_head_label)
        createRecycleAdapter(recyclerView, emptyView)
        emptyView.hide()
        skeleton = recyclerView.applySkeleton(R.layout.item_small_recyclerview_skeleton, 15)
        skeletonRecyclerDelay = SkeletonRecyclerDelay(skeleton!!, recyclerView)
        skeletonRecyclerDelay.showSkeleton()
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    private fun setupLoadMoreScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, deltaX: Int, deltaY: Int) {
                super.onScrolled(view, deltaX, deltaY)
                if (!isLoadingMore && hasMoreItems && recyclerView.isScrolledToBottom) {
                    /* The end of the list has been reached. Load more data. */
                    page++
                    loadMoreItems()
                    isLoadingMore = true
                }
            }
        })
    }

    private fun loadMoreItems() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        isLoadingMore = true
        loadingMoreView!!.visibility = View.VISIBLE
        disposable = Observable.fromCallable { loadMoreData() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ data: List<FeedItem> ->
                if (data.size < EPISODES_PER_PAGE) {
                    hasMoreItems = false
                }
                episodes.addAll(data)
                onFragmentLoaded(episodes)
            }, { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
            ) {
                recyclerView.post {
                    isLoadingMore = false
                } // Make sure to not always load 2 pages at once
                //                        progLoading.setVisibility(View.GONE);
                if (skeleton!!.isSkeleton()) {
                    skeletonRecyclerDelay.showOriginal()
                }
                loadingMoreView!!.visibility = View.GONE
            }
    }

    protected open fun onFragmentLoaded(episodes: List<FeedItem>) {
        val restoreScrollPosition = listAdapter!!.itemCount == 0
        if (episodes.size == 0) {
            createRecycleAdapter(recyclerView, emptyView)
        } else {
            listAdapter!!.updateItems(episodes)
        }
        if (restoreScrollPosition) {
            recyclerView.restoreScrollPosition(prefName!!)
        }
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing) {
            onPrepareOptionsMenu(toolbar.menu)
        }
    }

    /**
     * 如果希望有不同的多选菜单覆盖它
     */
    open fun getMultiMenu():Int{
        return R.menu.episodes_multi_menu
    }
    /**
     * Currently, we need to recreate the list adapter in order to be able to undo last item via the
     * snackbar. See #3084 for details.
     */
    private fun createRecycleAdapter(
        recyclerView: RecyclerView?,
        emptyViewHandler: EmptyViewHandler?
    ) {
        val mainActivity = activity as MainActivity?
        listAdapter = object :
            EpisodeItemListAdapter(mainActivity!!, getMultiMenu()) {
            override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenuInfo?
            ) {
                super.onCreateContextMenu(menu, v, menuInfo)
                if (!inActionMode()) {
                    menu.findItem(R.id.multi_select).isVisible = true
                }
                MenuItemUtils.setOnClickListeners(menu) { item: MenuItem ->
                    this@EpisodesListFragment.onContextItemSelected(
                        item
                    )
                }
            }
        }
        listAdapter!!.setOnSelectModeListener(this)
        listAdapter!!.setOnMenuItemClickListener(object : MultiSelectAdapter.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?) {
                EpisodeMultiSelectActionHandler(
                    activity as MainActivity?,
                    listAdapter!!.selectedItems
                )
                    .handleAction(item!!.itemId)
                listAdapter!!.endSelectMode()
            }
        })
        listAdapter!!.updateItems(episodes)
        recyclerView!!.adapter = listAdapter
        emptyViewHandler!!.updateAdapter(listAdapter)
        swipeActions = SwipeActions(this, swipeTag).attachTo(recyclerView)
        swipeActions!!.setFilter(FeedItemFilter.unfiltered())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (listAdapter != null) {
            listAdapter!!.endSelectMode()
        }
        listAdapter = null
    }

    override fun onStartSelectMode() {
        toolbar.visibility = View.GONE
        swipeActions!!.detach()
    }

    override fun onEndSelectMode() {
        toolbar.visibility = View.VISIBLE
        swipeActions!!.attachTo(recyclerView)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        for (item in event.items) {
            val pos = FeedItemUtil.indexOfItemWithId(episodes, item.id)
            if (pos >= 0) {
                episodes.removeAt(pos)
                if (shouldUpdatedItemRemainInList(item)) {
                    episodes.add(pos, item)
                    listAdapter!!.notifyItemChangedCompat(pos)
                } else {
                    listAdapter!!.notifyItemRemoved(pos)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent) {
        if (listAdapter != null) {
            for (i in 0 until listAdapter!!.itemCount) {
                val holder =
                    recyclerView.findViewHolderForAdapterPosition(i) as EpisodeItemViewHolder?
                if (holder != null && holder.isCurrentlyPlayingItem) {
                    holder.notifyPlaybackPositionUpdated(event)
                    break
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onKeyUp(event: KeyEvent) {
        if (!isAdded || !isVisible || !isMenuVisible) {
            return
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_T -> recyclerView.smoothScrollToPosition(0)
            KeyEvent.KEYCODE_B -> recyclerView.smoothScrollToPosition(listAdapter!!.itemCount - 1)
            else -> {}
        }
    }

    protected open fun shouldUpdatedItemRemainInList(item: FeedItem): Boolean {
        return true
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        val update = event.update
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            onPrepareOptionsMenu(toolbar.menu)
        }
        if (update.mediaIds.size > 0) {
            for (mediaId in update.mediaIds) {
                val pos = FeedItemUtil.indexOfItemWithMediaId(episodes, mediaId)
                if (pos >= 0) {
                    listAdapter!!.notifyItemChangedCompat(pos)
                }
            }
        }
    }

    private fun updateUi() {
        loadItems()
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing) {
            onPrepareOptionsMenu(toolbar.menu)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onPlayerStatusChanged(event: PlayerStatusEvent) {
        updateUi()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent) {
        updateUi()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFeedListChanged(event: FeedListUpdateEvent) {
        updateUi()
    }

    fun loadItems() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable(
            { loadData() })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ data: MutableList<FeedItem> ->
                if (skeleton!!.isSkeleton()) {
                    skeletonRecyclerDelay.showOriginal()
                }
                loadingMoreView!!.visibility = View.GONE
                hasMoreItems = true
                episodes = data
                onFragmentLoaded(episodes)
                onPrepareOptionsMenu(toolbar.menu)
            }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    override fun backToContentTop() {
        recyclerView.scrollToPosition(5)
        recyclerView.post { recyclerView.smoothScrollToPosition(0) }
    }

    protected abstract fun loadData(): MutableList<FeedItem>

    /**
     * Load a new page of data as defined by [.page] and [.EPISODES_PER_PAGE].
     * If the number of items returned is less than [.EPISODES_PER_PAGE],
     * it will be assumed that the underlying data is exhausted
     * and this method will not be called again.
     *
     * @return The items from the next page of data
     */
    protected abstract fun loadMoreData(): MutableList<FeedItem>

    companion object {
        const val TAG = "EpisodesListFragment"
        const val PREF_NAME = "pref_episodes_listFragment"
        @JvmStatic
        protected val EPISODES_PER_PAGE = if (BuildConfig.DEBUG) 20 else 150
        private const val PREF_LAST_TAB_POSITION = "tab_position"
        private const val KEY_UP_ARROW = "up_arrow"
    }
}