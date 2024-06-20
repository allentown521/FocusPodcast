package allen.town.podcast.fragment

import allen.town.focus_common.util.DoubleClickBackToContentTopListener
import allen.town.focus_common.util.MenuIconUtil.showToolbarMenuIcon
import allen.town.focus_common.util.Timber
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.MultiSelectAdapter
import allen.town.podcast.adapter.MultiSelectAdapter.OnSelectModeListener
import allen.town.podcast.adapter.SubFeedsAdapter
import allen.town.podcast.adapter.SubFeedsAdapter.GridDividerItemDecorator
import allen.town.podcast.appshortcuts.SubscriptionActivityStarter
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.pref.Prefs.subscriptionsFilter
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.NavDrawerData.*
import allen.town.podcast.core.util.download.AutoUpdateManager
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.core.util.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import allen.town.podcast.dialog.FeedsSortDialog.Companion.newInstance
import allen.town.podcast.dialog.RemoveFeedDialog.show
import allen.town.podcast.dialog.RenameItemDialog
import allen.town.podcast.dialog.SubsFilterDialog.showDialog
import allen.town.podcast.dialog.TagEditDialog
import allen.town.podcast.dialog.TagEditDialog.Companion.newInstance
import allen.town.podcast.event.FeedListUpdateEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.fragment.actions.FeedMultiSelectActionHandler
import allen.town.podcast.statistics.StatisticsFragment
import allen.town.podcast.util.SkeletonRecyclerDelay
import allen.town.podcast.view.EmptyViewHandler
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.animation.AnimationUtils
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.Callable

/**
 * Fragment for displaying feed subscriptions
 */
class SubFeedsFragment : Fragment(), Toolbar.OnMenuItemClickListener, OnSelectModeListener,
    DoubleClickBackToContentTopListener.IBackToContentTopView {
    private lateinit var subscriptionRecycler: RecyclerView
    private var subscriptionAdapter: SubFeedsAdapter? = null
    private lateinit var subscriptionAddButton: FloatingActionButton
    private lateinit var emptyView: EmptyViewHandler
    private lateinit var toolbar: Toolbar
    private var displayedFolder: String? = null
    private var isUpdatingFeeds = false
    private var displayUpArrow = false
    private var disposable: Disposable? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var skeleton: Skeleton
    private var listItems: List<DrawerItem>? = null
    private lateinit var skeletonRecyclerDelay: SkeletonRecyclerDelay
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.v("subfeeds oncreate view")
        val root = inflater.inflate(R.layout.fragment_subscriptions, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.setOnMenuItemClickListener(this)
        displayUpArrow = parentFragmentManager.backStackEntryCount != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (activity as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        toolbar.inflateMenu(R.menu.subscriptions)
        showToolbarMenuIcon(toolbar)
        for (i in COLUMN_CHECKBOX_IDS.indices) {
            // Do this in Java to localize numbers
            toolbar.getMenu().findItem(COLUMN_CHECKBOX_IDS[i]).title =
                String.format(Locale.getDefault(), "%d", i + MIN_NUM_COLUMNS)
        }
        refreshToolbarState()
        if (arguments != null) {
            displayedFolder = requireArguments().getString(ARGUMENT_FOLDER, null)
            if (displayedFolder != null) {
                toolbar.setTitle(displayedFolder)
            }
        }
        subscriptionRecycler = root.findViewById(R.id.subscriptions_grid)
        setColumnNumber(prefs.getInt(PREF_NUM_COLUMNS, defaultNumOfColumns))
        subscriptionRecycler.addItemDecoration(GridDividerItemDecorator())

        toolbar.setOnClickListener(DoubleClickBackToContentTopListener(this))
        registerForContextMenu(subscriptionRecycler)
        subscriptionAddButton = root.findViewById(R.id.subscriptions_add)
        val swipeRefreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getInteger(R.integer.swipe_refresh_distance))
        swipeRefreshLayout.setOnRefreshListener {
            AutoUpdateManager.runImmediate(requireContext())
            Handler(Looper.getMainLooper()).postDelayed(
                { swipeRefreshLayout.isRefreshing = false },
                resources.getInteger(R.integer.swipe_to_refresh_duration_in_ms).toLong()
            )
        }
        toolbar.setTitle(getString(R.string.subscriptions_label))
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    private fun refreshToolbarState() {
        val columns = prefs.getInt(PREF_NUM_COLUMNS, defaultNumOfColumns)
        toolbar.menu.findItem(COLUMN_CHECKBOX_IDS[columns - MIN_NUM_COLUMNS]).isChecked =
            true
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(
            toolbar.menu,
            R.id.refresh_item, updateRefreshMenuItemChecker
        )
        val filterItem = toolbar.menu.findItem(R.id.subscriptions_filter)
        if (subscriptionsFilter.isEnabled) {
            filterItem.setTitle(R.string.filtered_label)
            filterItem.setIcon(R.drawable.ic_filter_disable)
        } else {
            filterItem.setTitle(R.string.filter)
            filterItem.setIcon(R.drawable.ic_filter)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext())
            return true
        } else if (itemId == R.id.subscriptions_filter) {
            showDialog(requireContext())
            return true
        } else if (itemId == R.id.subscriptions_sort) {
            newInstance().show(childFragmentManager, null)
            return true
        } else if (itemId == R.id.subscription_num_columns_3) {
            setColumnNumber(3)
            return true
        } else if (itemId == R.id.subscription_num_columns_4) {
            setColumnNumber(4)
            return true
        } else if (itemId == R.id.subscription_num_columns_5) {
            setColumnNumber(5)
            return true
        } else if (itemId == R.id.action_search) {
            (activity as MainActivity?)!!.loadChildFragment(LocalSearchFragment.newFeedSearchInstance())
            return true
        } else if (itemId == R.id.action_statistics) {
            (activity as MainActivity?)!!.loadChildFragment(StatisticsFragment())
            return true
        }
        return false
    }

    private fun setColumnNumber(columns: Int) {
        val gridLayoutManager = GridLayoutManager(
            context,
            columns, RecyclerView.VERTICAL, false
        )
        subscriptionRecycler.layoutManager = gridLayoutManager
        prefs.edit().putInt(PREF_NUM_COLUMNS, columns).apply()
        refreshToolbarState()
    }

    private fun setupEmptyView() {
        emptyView = EmptyViewHandler(context)
        emptyView.setIcon(R.drawable.ic_add_subscription)
        emptyView.setTitle(R.string.no_subscriptions_head_label)
        emptyView.setMessage(R.string.no_subscriptions_label)
        emptyView.attachToRecyclerView(subscriptionRecycler)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        //gird 如何加载动画 https://proandroiddev.com/enter-animation-using-recyclerview-and-layoutanimation-part-2-grids-688829b1d29b
        val loadLayoutAnimation =
            AnimationUtils.loadLayoutAnimation(context, R.anim.grid_layout_animation_from_bottom)
        subscriptionRecycler.layoutAnimation = loadLayoutAnimation
//        subscriptionRecycler.scheduleLayoutAnimation()

        subscriptionAdapter = object : SubFeedsAdapter((activity as MainActivity?)!!) {
            override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenuInfo?
            ) {
                super.onCreateContextMenu(menu, v, menuInfo)
                MenuItemUtils.setOnClickListeners(menu) { item: MenuItem ->
                    this@SubFeedsFragment.onContextItemSelected(
                        item
                    )
                }
            }
        }
        subscriptionAdapter!!.setOnSelectModeListener(this)
        subscriptionRecycler.adapter = subscriptionAdapter
//        subscriptionRecycler.adapter = ScaleInAnimationAdapter(subscriptionAdapter!!).apply {
//            setFirstOnly(true)
//            setDuration(1500)
//            setInterpolator(OvershootInterpolator(.5f))
//        }

        subscriptionAdapter!!.setOnMenuItemClickListener(object :
            MultiSelectAdapter.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?) {
                FeedMultiSelectActionHandler(
                    activity as MainActivity?,
                    subscriptionAdapter!!.selectedItems
                )
                    .handleAction(item!!.itemId)
            }
        })
        setupEmptyView()
        subscriptionAddButton.setOnClickListener { view: View? ->
            if (activity is MainActivity) {
                (activity as MainActivity?)!!.loadChildFragment(DiscoverFragment())
            }
        }
        skeleton =
            subscriptionRecycler.applySkeleton(R.layout.item_grid_recyclerview_skeleton, 30)
        skeletonRecyclerDelay = SkeletonRecyclerDelay(skeleton, subscriptionRecycler)
        skeletonRecyclerDelay.showSkeleton()
        Timber.v("subfeeds onview create ")
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        loadSubscriptions()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (subscriptionAdapter != null) {
            subscriptionAdapter!!.endSelectMode()
        }
    }

    private fun loadSubscriptions() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        emptyView.hide()
        disposable = Observable.fromCallable {
            val data = DBReader.getNavDrawerData(true)
            val items = data.items
            val feedItems: MutableList<DrawerItem> = ArrayList()
            for (item in items) {
                if (item.type == DrawerItem.Type.TAG
                    && item.title == displayedFolder
                ) {
                    return@fromCallable (item as TagDrawerItem).children
                } else {
                    if (item.type != DrawerItem.Type.TAG) {
                        feedItems.add(item)
                    }
                }
            }
            feedItems
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: List<DrawerItem> ->
                    if (listItems != null && listItems!!.size > result.size) {
                        // We have fewer items. This can result in items being selected that are no longer visible.
                        subscriptionAdapter!!.endSelectMode()
                    }
                    listItems = result
                    subscriptionAdapter!!.setItems(result)
                    subscriptionAdapter!!.notifyDataSetChanged()
                    if (skeleton.isSkeleton()) {
                        skeletonRecyclerDelay.showOriginal()
                    }
                    emptyView.updateVisibility()
                }) { error: Throwable? ->
                Log.e(TAG, Log.getStackTraceString(error))
                skeletonRecyclerDelay.showOriginal()
            }

        refreshToolbarState()
    }

    private val defaultNumOfColumns: Int
        private get() = resources.getInteger(R.integer.subscriptions_default_num_of_columns)

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val drawerItem = subscriptionAdapter!!.selectedItem ?: return false
        val itemId = item.itemId
        if (drawerItem.type == DrawerItem.Type.TAG && itemId == R.id.rename_folder_item) {
            RenameItemDialog(requireActivity(), drawerItem).show()
            return true
        }
        val feed = (drawerItem as FeedDrawerItem).feed
        if (itemId == R.id.edit_tags) {
            newInstance(listOf(feed.preferences))
                .show(childFragmentManager, TagEditDialog.TAG)
            return true
        } else if (itemId == R.id.rename_item) {
            RenameItemDialog(requireActivity(), feed).show()
            return true
        } else if (itemId == R.id.shortcut_item) {
            SubscriptionActivityStarter.createShortcut(context, feed)
            return true
        } else if (itemId == R.id.remove_feed) {
            show(requireContext(), feed)
            return true
        } else if (itemId == R.id.multi_select) {
            return subscriptionAdapter!!.onContextItemSelected(item)
        }
        return super.onContextItemSelected(item)
    }

    private fun <T> displayConfirmationDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        task: Callable<out T?>
    ) {
        val dialog: ConfirmationDialog = object : ConfirmationDialog(activity, title, message) {
            @SuppressLint("CheckResult")
            override fun onConfirmButtonPressed(clickedDialog: DialogInterface) {
                clickedDialog.dismiss()
                Observable.fromCallable(task)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result: T? -> loadSubscriptions() }
                    ) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
            }
        }
        dialog.createNewDialog().show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFeedListChanged(event: FeedListUpdateEvent?) {
        loadSubscriptions()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        loadSubscriptions()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            refreshToolbarState()
        }
    }

    private val updateRefreshMenuItemChecker =
        UpdateRefreshMenuItemChecker { DownloadService.isRunning && DownloadService.isDownloadingFeeds() }

    override fun onEndSelectMode() {
        subscriptionAdapter!!.setItems(listItems!!)
        subscriptionAdapter!!.notifyDataSetChanged()
        toolbar.visibility = View.VISIBLE
    }

    override fun onStartSelectMode() {
        toolbar.visibility = View.GONE
        val feedsOnly: MutableList<DrawerItem> = ArrayList()
        for (item in listItems!!) {
            if (item.type == DrawerItem.Type.FEED) {
                feedsOnly.add(item)
            }
        }
        subscriptionAdapter!!.setItems(feedsOnly)
        subscriptionAdapter!!.notifyDataSetChanged()
    }

    companion object {
        const val TAG = "SubFeedsFragment"
        const val PREFS = "SubFeedsFragment"
        const val PREF_NUM_COLUMNS = "columns"
        private const val KEY_UP_ARROW = "up_arrow"
        private const val ARGUMENT_FOLDER = "folder"
        private const val MIN_NUM_COLUMNS = 3
        private val COLUMN_CHECKBOX_IDS = intArrayOf(
            R.id.subscription_num_columns_3,
            R.id.subscription_num_columns_4,
            R.id.subscription_num_columns_5
        )

        fun newInstance(folderTitle: String?): SubFeedsFragment {
            val fragment = SubFeedsFragment()
            val args = Bundle()
            args.putString(ARGUMENT_FOLDER, folderTitle)
            fragment.arguments = args
            return fragment
        }
    }

    override fun backToContentTop() {
        subscriptionRecycler.scrollToPosition(5)
        subscriptionRecycler.post { subscriptionRecycler.smoothScrollToPosition(0) }
    }
}