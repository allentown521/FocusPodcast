package allen.town.podcast.fragment

import allen.town.focus_common.util.DoubleClickBackToContentTopListener
import allen.town.focus_common.util.MenuIconUtil.showToolbarMenuIcon
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.ItemOffsetDecoration
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.MultiSelectAdapter
import allen.town.podcast.adapter.MultiSelectAdapter.OnSelectModeListener
import allen.town.podcast.adapter.PlaylistAdapter
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.feed.util.PlaybackSpeedUtils
import allen.town.podcast.core.pref.Prefs.isPlaylistKeepSorted
import allen.town.podcast.core.pref.Prefs.isPlaylistLocked
import allen.town.podcast.core.pref.Prefs.queueKeepSortedOrder
import allen.town.podcast.core.pref.Prefs.timeRespectsSpeed
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.Converter
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.download.AutoUpdateManager
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.core.util.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import allen.town.podcast.core.view.TopAppBarLayout
import allen.town.podcast.event.FeedItemEvent
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.QueueEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.fragment.actions.EpisodeMultiSelectActionHandler
import allen.town.podcast.fragment.swipeactions.SwipeActions
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedItemFilter
import allen.town.podcast.model.feed.SortOrder
import allen.town.podcast.util.SkeletonRecyclerDelay
import allen.town.podcast.view.EmptyViewHandler
import allen.town.podcast.view.StorePositionRecyclerView
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Shows all items in the queue.
 */
class PlaylistFragment : Fragment(), Toolbar.OnMenuItemClickListener, OnSelectModeListener, DoubleClickBackToContentTopListener.IBackToContentTopView {
    private lateinit var recyclerView: StorePositionRecyclerView
    private var recyclerAdapter: PlaylistAdapter? = null
    private lateinit var emptyView: EmptyViewHandler
    private lateinit var progLoading: ProgressBar
    private lateinit var toolbar: Toolbar
    private var displayUpArrow = false
    private var queue: MutableList<FeedItem>? = null
    private var isUpdatingFeeds = false
    private var disposable: Disposable? = null
    private lateinit var swipeActions: SwipeActions
    private var prefs: SharedPreferences? = null
    private lateinit var topAppBarLayout: TopAppBarLayout
    private lateinit var skeleton: Skeleton
    private lateinit var skeletonRecyclerDelay: SkeletonRecyclerDelay
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
        if (queue != null) {
            onFragmentLoaded(true)
        }
        loadItems(true)
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        recyclerView.saveScrollPosition(TAG)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: QueueEvent) {
        if (queue == null) {
            return
        } else if (recyclerAdapter == null) {
            loadItems(true)
            return
        }
        when (event.action) {
            QueueEvent.Action.ADDED -> {
                queue!!.add(event.position, event.item)
                recyclerAdapter!!.notifyItemInserted(event.position)
            }
            QueueEvent.Action.SET_QUEUE, QueueEvent.Action.SORTED -> {
                queue = event.items
                recyclerAdapter!!.notifyDataSetChanged()
            }
            QueueEvent.Action.REMOVED, QueueEvent.Action.IRREVERSIBLE_REMOVED -> {
                val position = FeedItemUtil.indexOfItemWithId(queue, event.item.id)
                queue!!.removeAt(position)
                recyclerAdapter!!.notifyItemRemoved(position)
            }
            QueueEvent.Action.CLEARED -> {
                queue!!.clear()
                recyclerAdapter!!.notifyDataSetChanged()
            }
            QueueEvent.Action.MOVED -> return
            else -> return
        }
        recyclerView!!.saveScrollPosition(TAG)
        onFragmentLoaded(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        if (queue == null) {
            return
        } else if (recyclerAdapter == null) {
            loadItems(true)
            return
        }
        var i = 0
        val size = event.items.size
        while (i < size) {
            val item = event.items[i]
            val pos = FeedItemUtil.indexOfItemWithId(queue, item.id)
            if (pos >= 0) {
                queue!!.removeAt(pos)
                queue!!.add(pos, item)
                recyclerAdapter!!.notifyItemChangedCompat(pos)
                refreshInfoBar()
            }
            i++
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        val update = event.update
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            refreshToolbarState()
        }
        if (recyclerAdapter != null && update.mediaIds.size > 0) {
            for (mediaId in update.mediaIds) {
                val pos = FeedItemUtil.indexOfItemWithMediaId(queue, mediaId)
                if (pos >= 0) {
                    recyclerAdapter!!.notifyItemChangedCompat(pos)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent?) {
        if (recyclerAdapter != null) {
            for (i in 0 until recyclerAdapter!!.itemCount) {
                val holder =
                    recyclerView!!.findViewHolderForAdapterPosition(i) as EpisodeItemViewHolder?
                if (holder != null && holder.isCurrentlyPlayingItem) {
                    holder.notifyPlaybackPositionUpdated(event!!)
                    break
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerStatusChanged(event: PlayerStatusEvent?) {
        loadItems(false)
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing) {
            refreshToolbarState()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        // Sent when playback position is reset
        loadItems(false)
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing) {
            refreshToolbarState()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onKeyUp(event: KeyEvent) {
        if (!isAdded || !isVisible || !isMenuVisible) {
            return
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_T -> recyclerView!!.smoothScrollToPosition(0)
            KeyEvent.KEYCODE_B -> recyclerView!!.smoothScrollToPosition(recyclerAdapter!!.itemCount - 1)
            else -> {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (recyclerAdapter != null) {
            recyclerAdapter!!.endSelectMode()
        }
        recyclerAdapter = null
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(null);
            toolbar.setOnLongClickListener(null);
        }
    }

    private val updateRefreshMenuItemChecker =
        UpdateRefreshMenuItemChecker { DownloadService.isRunning && DownloadService.isDownloadingFeeds() }

    private fun refreshToolbarState() {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(
            toolbar!!.menu,
            R.id.refresh_item, updateRefreshMenuItemChecker
        )
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext())
            return true
        } else if (itemId == R.id.clear_queue) {
            // make sure the user really wants to clear the queue
            val conDialog: ConfirmationDialog = object : ConfirmationDialog(
                activity,
                R.string.clear_queue_label,
                R.string.clear_queue_confirmation_msg
            ) {
                override fun onConfirmButtonPressed(
                    dialog: DialogInterface
                ) {
                    dialog.dismiss()
                    DBWriter.clearQueue()
                }
            }
            conDialog.createNewDialog().show()
            return true
        } else if (itemId == R.id.action_search) {
            (activity as MainActivity?)!!.loadChildFragment(LocalSearchFragment.Companion.newInstance())
            return true
        } else if (itemId == R.id.queue_sort) {
            EditSortDialog().openDialog()
            return true
        } else if (itemId == R.id.history) {
            (activity as MainActivity?)!!.loadChildFragment(PlaybackHistoryFragment())
            return true
        }
        return false
    }

    private fun setQueueLocked(locked: Boolean) {
        isPlaylistLocked = locked
        refreshToolbarState()
        if (recyclerAdapter != null) {
            recyclerAdapter!!.updateDragDropEnabled()
        }
        if (locked) {
            showSnack(activity, R.string.queue_locked, Toast.LENGTH_SHORT)
        } else {
            showSnack(activity, R.string.queue_unlocked, Toast.LENGTH_SHORT)
        }
    }

    /**
     * This method is called if the user clicks on a sort order menu item.
     *
     * @param sortOrder New sort order.
     */
    private fun setSortOrder(sortOrder: SortOrder) {
        queueKeepSortedOrder = sortOrder
        DBWriter.reorderQueue(sortOrder, true)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (!isVisible || recyclerAdapter == null) {
            return false
        }
        val selectedItem = recyclerAdapter!!.longPressedItem
        if (selectedItem == null) {
            Log.i(TAG, "Selected item was null, ignoring selection")
            return super.onContextItemSelected(item)
        }
        val position = FeedItemUtil.indexOfItemWithId(queue, selectedItem.id)
        if (position < 0) {
            Log.i(TAG, "Selected item no longer exist, ignoring selection")
            return super.onContextItemSelected(item)
        }
        if (recyclerAdapter!!.onContextItemSelected(item)) {
            return true
        }
        val itemId = item.itemId
        if (itemId == R.id.move_to_top_item) {
            queue!!.add(0, queue!!.removeAt(position))
            recyclerAdapter!!.notifyItemMoved(position, 0)
            DBWriter.moveQueueItemToTop(selectedItem.id, true)
            return true
        } else if (itemId == R.id.move_to_bottom_item) {
            queue!!.add(queue!!.size - 1, queue!!.removeAt(position))
            recyclerAdapter!!.notifyItemMoved(position, queue!!.size - 1)
            DBWriter.moveQueueItemToBottom(selectedItem.id, true)
            return true
        }
        return FeedItemMenuProcess.onMenuItemClicked(this, item.itemId, selectedItem)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.queue_fragment, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.setOnMenuItemClickListener(this)
        displayUpArrow = parentFragmentManager.backStackEntryCount != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (activity as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        toolbar.inflateMenu(R.menu.playlist_menu)
        toolbar.setOnClickListener(DoubleClickBackToContentTopListener(this) )
        showToolbarMenuIcon(toolbar)
        refreshToolbarState()
        recyclerView = root.findViewById(R.id.recyclerView)
        create(recyclerView)
        val animator = recyclerView.getItemAnimator()
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
        recyclerView.setRecycledViewPool((activity as MainActivity?)!!.recycledViewPool)
        registerForContextMenu(recyclerView)
        val swipeRefreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getInteger(R.integer.swipe_refresh_distance))
        swipeRefreshLayout.setOnRefreshListener {
            AutoUpdateManager.runImmediate(requireContext())
            Handler(Looper.getMainLooper()).postDelayed(
                { swipeRefreshLayout.isRefreshing = false },
                resources.getInteger(R.integer.swipe_to_refresh_duration_in_ms).toLong()
            )
        }
        swipeActions = QueueSwipeActions()
        swipeActions.setFilter(FeedItemFilter(FeedItemFilter.QUEUED))
        swipeActions.attachTo(recyclerView)
        emptyView = EmptyViewHandler(context)
        emptyView.attachToRecyclerView(recyclerView)
        emptyView.setIcon(R.drawable.ic_playlist)
        emptyView.setTitle(R.string.no_items_header_label)
        progLoading = root.findViewById(R.id.progLoading)
        //        progLoading.setVisibility(View.VISIBLE);
//        topAppBarLayout = root.findViewById(R.id.appBarLayout);
        toolbar.setTitle(getString(R.string.playlist_label))
        skeleton = recyclerView.applySkeleton(R.layout.item_small_recyclerview_skeleton, 15)
        skeletonRecyclerDelay = SkeletonRecyclerDelay(skeleton,recyclerView)
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    private fun onFragmentLoaded(restoreScrollPosition: Boolean) {
        if (queue != null) {
            if (recyclerAdapter == null) {
                val activity = activity as MainActivity?
                recyclerAdapter = object : PlaylistAdapter(activity!!, swipeActions) {
                    override fun onCreateContextMenu(
                        menu: ContextMenu,
                        v: View,
                        menuInfo: ContextMenuInfo?
                    ) {
                        super.onCreateContextMenu(menu, v, menuInfo)
                        MenuItemUtils.setOnClickListeners(menu) { item: MenuItem ->
                            this@PlaylistFragment.onContextItemSelected(
                                item
                            )
                        }
                    }
                }
                recyclerAdapter!!.setOnMenuItemClickListener(object :
                    MultiSelectAdapter.OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem?) {
                        EpisodeMultiSelectActionHandler(
                            getActivity() as MainActivity?,
                            recyclerAdapter!!.selectedItems
                        )
                            .handleAction(item!!.itemId)
                        recyclerAdapter!!.endSelectMode()
                    }
                })
                recyclerAdapter!!.setOnSelectModeListener(this)
                recyclerView.adapter = recyclerAdapter
                emptyView.updateAdapter(recyclerAdapter)
                skeleton =
                    recyclerView.applySkeleton(R.layout.item_small_recyclerview_skeleton, 15)
                skeletonRecyclerDelay = SkeletonRecyclerDelay(skeleton,recyclerView)
                skeletonRecyclerDelay.showSkeleton()
            }
            recyclerAdapter!!.updateItems(queue!!)
        } else {
            recyclerAdapter = null
            emptyView.updateAdapter(null)
        }
        if (restoreScrollPosition) {
            recyclerView.restoreScrollPosition(TAG)
        }

        // we need to refresh the options menu because it sometimes
        // needs data that may have just been loaded.
        refreshToolbarState()
        refreshInfoBar()
    }

    private fun refreshInfoBar() {
        var info: String? = String.format(
            Locale.getDefault(), "%d%s",
            queue!!.size, getString(R.string.episodes_suffix)
        )
        if (queue!!.size > 0) {
            var timeLeft: Long = 0
            for (item in queue!!) {
                var playbackSpeed = 1f
                if (timeRespectsSpeed()) {
                    playbackSpeed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(item.media)
                }
                if (item.media != null) {
                    val itemTimeLeft = (item.media!!
                        .duration - item.media!!.position).toLong()
                    timeLeft += (itemTimeLeft / playbackSpeed).toLong()
                }
            }
            info += " ("
            info += Converter.getDurationStringLocalized(activity, timeLeft)
            info += ")"
            toolbar!!.subtitle = info
        } else {
            //播放列表是空不显示子标题
            toolbar!!.subtitle = ""
        }
        toolbar!!.setSubtitleTextAppearance(
            context,
            R.style.FocusPodcast_TextView_ListItemSecondaryTitle
        )
    }

    private fun loadItems(restoreScrollPosition: Boolean) {
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (queue == null) {
            emptyView.hide()
            skeletonRecyclerDelay.showSkeleton()
            //            progLoading.setVisibility(View.VISIBLE);
        }
        disposable = Observable.fromCallable { DBReader.getQueue() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ items: MutableList<FeedItem>? ->
//                    progLoading.setVisibility(View.GONE);
                queue = items
                onFragmentLoaded(restoreScrollPosition)
                if (skeleton.isSkeleton()) {
                    skeletonRecyclerDelay.showOriginal()
                }
                if (recyclerAdapter != null) {
                    recyclerAdapter!!.notifyDataSetChanged()
                }
            }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    override fun onStartSelectMode() {
        swipeActions!!.detach()
        refreshToolbarState()
        toolbar!!.visibility = View.GONE
    }

    override fun onEndSelectMode() {
        swipeActions!!.attachTo(recyclerView)
        toolbar!!.visibility = View.VISIBLE
    }

    private inner class QueueSwipeActions :
        SwipeActions(ItemTouchHelper.UP or ItemTouchHelper.DOWN, this@PlaylistFragment, TAG) {
        // Position tracking whilst dragging
        var dragFrom = -1
        var dragTo = -1
        override fun onMove(
            recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition

            // Update tracked position
            if (dragFrom == -1) {
                dragFrom = fromPosition
            }
            dragTo = toPosition
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from >= queue!!.size || to >= queue!!.size || from < 0 || to < 0) {
                return false
            }
            queue!!.add(to, queue!!.removeAt(from))
            recyclerAdapter!!.notifyItemMoved(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (disposable != null) {
                disposable!!.dispose()
            }

            //SwipeActions
            super.onSwiped(viewHolder, direction)
        }

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            // Check if drag finished
            if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                reallyMoved(dragFrom, dragTo)
            }
            dragTo = -1
            dragFrom = dragTo
        }

        private fun reallyMoved(from: Int, to: Int) {
            // Write drag operation to database
            DBWriter.moveQueueItem(from, to, true)
        }
    }

    internal inner class EditSortDialog {
        var sortItems = arrayOf<String>()
        var sortValues = arrayOf<SortOrder>()
        var selectedIndex = 0
        var ASC_INDEX = 1
        var DESC_INDEX = 0
        var isDesc = true
        private var adapter: OrderSelectionAdapter? = null
        lateinit var orderGroup: MaterialButtonToggleGroup
        lateinit var keepSortS: SwitchCompat
        lateinit var lockS: SwitchCompat
        var lastIsLocked = false
        var lastIsQueueKeepSorted = false
        fun openDialog() {
            val lastSortOrder = queueKeepSortedOrder
            if (lastSortOrder != null) {
                selectedIndex = lastSortOrder.groupIndex
                isDesc = lastSortOrder.code - lastSortOrder.ascBaseCode == DESC_INDEX
            }
            lastIsQueueKeepSorted = isPlaylistKeepSorted
            lastIsLocked = isPlaylistLocked
            sortItems = context!!.resources.getStringArray(R.array.queue_sort_options)
            val commonSortStringValues =
                context!!.resources.getStringArray(R.array.queue_sort_values)
            sortValues = SortOrder.valuesOf(commonSortStringValues)
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.edit_feed_sort_dialog_layout, null, false)
            layout.findViewById<View>(R.id.keep_sort_l).visibility = View.VISIBLE
            keepSortS = layout.findViewById(R.id.keep_sort_s)
            lockS = layout.findViewById(R.id.lock_s)
            //保持排序
            keepSortS.setChecked(lastIsQueueKeepSorted)
            keepSortS.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                lockS.setEnabled(!isChecked)
                adapter!!.notifyDataSetChanged()
            })

            //锁定
            lockS.setChecked(lastIsLocked)
            lockS.setEnabled(!keepSortS.isChecked())
            lockS.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> })
            orderGroup = layout.findViewById(R.id.order_group)
            setOrderGroupVisibility()
            orderGroup.check(if (!isDesc) R.id.asc_button else R.id.desc_button)
            val recyclerView = layout.findViewById<RecyclerView>(R.id.orders)
            //https://github.com/BelooS/ChipsLayoutManager
            val chipsLayoutManager = ChipsLayoutManager.newBuilder(context).build()
            recyclerView.layoutManager = chipsLayoutManager
            recyclerView.addItemDecoration(ItemOffsetDecoration(context!!, 4))
            adapter = OrderSelectionAdapter()
            adapter!!.setHasStableIds(true)
            recyclerView.adapter = adapter
            val dialog: AlertDialog.Builder = AccentMaterialDialog(
                context!!,
                R.style.MaterialAlertDialogTheme
            )
            dialog.setView(layout)
            dialog.setTitle(R.string.sort)
            dialog.setPositiveButton(android.R.string.ok) { dialog, which ->
                updateKeepSort()
                toggleQueueLock()
                val baseIndex =
                    if (orderGroup.getCheckedButtonId() == R.id.asc_button) ASC_INDEX else DESC_INDEX
                setSortOrder(sortValues[selectedIndex * 2 + baseIndex])
                if (recyclerAdapter != null) {
                    recyclerAdapter!!.updateDragDropEnabled()
                }
            }
            dialog.setNegativeButton(R.string.cancel_label, null)
                .show()
        }

        private fun updateKeepSort() {
            if (lastIsQueueKeepSorted == keepSortS!!.isChecked) {
                //如果和上一次保存的结果相同那么不需要做处理
                return
            }
            val keepSortedNew = keepSortS!!.isChecked
            isPlaylistKeepSorted = keepSortedNew
            if (recyclerAdapter != null) {
                recyclerAdapter!!.updateDragDropEnabled()
            }
            if (keepSortedNew) {
                val sortOrder = queueKeepSortedOrder
                DBWriter.reorderQueue(sortOrder, true)
            }
        }

        private fun toggleQueueLock() {
            if (lastIsLocked == lockS!!.isChecked) {
                //如果和上一次保存的结果相同那么不需要做处理
                return
            }
            if (!lockS!!.isChecked) {
                setQueueLocked(false)
            } else {
                val shouldShowLockWarning = prefs!!.getBoolean(PREF_SHOW_LOCK_WARNING, true)
                if (!shouldShowLockWarning) {
                    setQueueLocked(true)
                } else {
                    val builder: AlertDialog.Builder = AccentMaterialDialog(
                        context!!,
                        R.style.MaterialAlertDialogTheme
                    )
                    builder.setTitle(R.string.lock_queue)
                    builder.setMessage(R.string.queue_lock_warning)
                    val view = View.inflate(context, R.layout.checkbox_do_not_show_again, null)
                    val checkDoNotShowAgain =
                        view.findViewById<CheckBox>(R.id.checkbox_do_not_show_again)
                    builder.setView(view)
                    builder.setPositiveButton(R.string.lock_queue) { dialog: DialogInterface?, which: Int ->
                        prefs!!.edit().putBoolean(
                            PREF_SHOW_LOCK_WARNING, !checkDoNotShowAgain.isChecked
                        ).apply()
                        setQueueLocked(true)
                    }
                    builder.setNegativeButton(R.string.cancel_label, null)
                    builder.show()
                }
            }
        }

        /**
         * 当前选中的是否是随机，如果是随机那么隐藏排序分组
         *
         * @return
         */
        private val isRandomSelected: Boolean
            private get() = selectedIndex == SortOrder.RANDOM.groupIndex

        fun setOrderGroupVisibility() {
            orderGroup!!.visibility =
                if (isRandomSelected) View.GONE else View.VISIBLE
        }

        internal inner class OrderSelectionAdapter :
            RecyclerView.Adapter<OrderSelectionAdapter.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val entryView = inflater.inflate(R.layout.single_tag_chip, parent, false)
                val chip = entryView.findViewById<Chip>(R.id.chip)
                return ViewHolder(entryView as Chip)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.chip.text = sortItems[holder.adapterPosition]
                holder.chip.isChecked = selectedIndex == holder.adapterPosition
                holder.chip.isCheckedIconVisible = holder.chip.isChecked
                holder.chip.setOnClickListener {
                    selectedIndex = holder.adapterPosition
                    setOrderGroupVisibility()
                    adapter!!.notifyDataSetChanged()
                }
            }

            override fun getItemCount(): Int {
                return sortItems.size
            }

            override fun getItemId(position: Int): Long {
                return sortItems[position].hashCode().toLong()
            }

            inner class ViewHolder internal constructor(var chip: Chip) : RecyclerView.ViewHolder(
                chip
            )
        }
    }

    companion object {
        const val TAG = "PlaylistFragment"
        private const val KEY_UP_ARROW = "up_arrow"
        private const val PREFS = "pref_playlist_fragment"
        private const val PREF_SHOW_LOCK_WARNING = "show_lock_warning"
    }

    override fun backToContentTop() {
        recyclerView.scrollToPosition(5)
        recyclerView.post { recyclerView.smoothScrollToPosition(0) }
    }
}