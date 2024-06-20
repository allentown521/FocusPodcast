package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.EpisodeItemListAdapter
import allen.town.podcast.adapter.FeedSearchResultAdapter
import allen.town.podcast.adapter.SubFeedsAdapter.GridDividerItemDecorator
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.event.DownloaderUpdate
import allen.town.podcast.core.storage.FeedSearcher
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.event.FeedItemEvent
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.model.feed.Feed
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.view.EmptyViewHandler
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.EditTextUtil
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.faltenreich.skeletonlayout.Skeleton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Callable

/**
 * Performs a search operation on all feeds or one specific feed and displays the search result.
 */
class LocalSearchFragment constructor() : Fragment() {
    private var adapter: EpisodeItemListAdapter? = null
    private var adapterFeeds: FeedSearchResultAdapter? = null
    private var disposable: Disposable? = null
    private lateinit var emptyViewHandler: EmptyViewHandler
    private lateinit var recyclerView: RecyclerView
    private var results: MutableList<FeedItem>? = null
    private lateinit var searchView: SearchView
    private var automaticSearchDebouncer: Handler? = null
    private var lastQueryChange: Long = 0
    private val feedSkeleton: Skeleton? = null
    private val itemSkeleton: Skeleton? = null
    private lateinit var prefs: SharedPreferences
    private var searchFeeds: Boolean = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        automaticSearchDebouncer = Handler(Looper.getMainLooper())
    }

    public override fun onStop() {
        super.onStop()
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout: View = inflater.inflate(R.layout.search_fragment, container, false)
        setupToolbar(layout.findViewById(R.id.toolbar))
        prefs = requireActivity().getSharedPreferences(SubFeedsFragment.PREFS, Context.MODE_PRIVATE)
        recyclerView = layout.findViewById(R.id.recyclerView)
        recyclerView.setLayoutManager(LinearLayoutManager(getActivity()))
        recyclerView.setRecycledViewPool((getActivity() as MainActivity?)!!.recycledViewPool)
        adapter = object : EpisodeItemListAdapter((getActivity() as MainActivity?)!!, 0) {
            public override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenuInfo?
            ) {
                super.onCreateContextMenu(menu, v, menuInfo)
                MenuItemUtils.setOnClickListeners(
                    menu,
                    MenuItem.OnMenuItemClickListener({ item: MenuItem ->
                        this@LocalSearchFragment.onContextItemSelected(item)
                    })
                )
            }
        }
        recyclerView.setAdapter(adapter)
        //        itemSkeleton = SkeletonLayoutUtils.applySkeleton(recyclerView, R.layout.item_small_recyclerview_skeleton, 15);
        val recyclerViewFeeds: RecyclerView = layout.findViewById(R.id.recyclerViewFeeds)
        val gridLayoutManager: GridLayoutManager = GridLayoutManager(
            getContext(),
            prefs.getInt(
                SubFeedsFragment.PREF_NUM_COLUMNS,
                getResources().getInteger(R.integer.subscriptions_default_num_of_columns)
            ), RecyclerView.VERTICAL, false
        )
        recyclerViewFeeds.addItemDecoration(GridDividerItemDecorator())
        recyclerViewFeeds.setLayoutManager(gridLayoutManager)
        adapterFeeds = FeedSearchResultAdapter((getActivity() as MainActivity?)!!)
        recyclerViewFeeds.setAdapter(adapterFeeds)
        //        recyclerViewFeeds.setNestedScrollingEnabled(false);
//        feedSkeleton = SkeletonLayoutUtils.applySkeleton(recyclerViewFeeds, R.layout.item_grid_recyclerview_skeleton, 30);
//        NestedScrollView nestedScrollView = layout.findViewById(R.id.nested_scroll);
//        ScrollView nestedScrollView = layout.findViewById(R.id.nested_scroll);
//        nestedScrollView.setNestedScrollingEnabled(true);
        create(recyclerView)
        create(recyclerViewFeeds)
        emptyViewHandler = EmptyViewHandler(getContext())
        emptyViewHandler!!.setIcon(R.drawable.ic_search)
        emptyViewHandler!!.setTitle(R.string.search_status_no_results)
        EventBus.getDefault().register(this)
        if (requireArguments().getString(ARG_QUERY, null) != null) {
            searchView!!.setQuery(requireArguments().getString(ARG_QUERY, null), false)
            searchWithProgressBar()
        }
        searchView!!.setOnQueryTextFocusChangeListener(OnFocusChangeListener({ view: View, hasFocus: Boolean ->
            if (hasFocus) {
                showInputMethod(view.findFocus())
            }
        }))
        EditTextUtil.setCursorDrawableForSearchView(searchView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            public override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val imm: InputMethodManager =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0)
                }
            }
        })
        searchFeeds = requireArguments().getBoolean(ARG_SEARCH_FEED, false)
        if (searchFeeds) {
            recyclerView.setVisibility(View.GONE)
            recyclerViewFeeds.setVisibility(View.VISIBLE)
            emptyViewHandler.attachToRecyclerView(recyclerViewFeeds)
        } else {
            recyclerView.setVisibility(View.VISIBLE)
            recyclerViewFeeds.setVisibility(View.GONE)
            emptyViewHandler.attachToRecyclerView(recyclerView)
        }
        return layout
    }

    public override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.setTitle(R.string.search_label)
        toolbar.setNavigationOnClickListener(View.OnClickListener({ v: View? -> getParentFragmentManager().popBackStack() }))
        toolbar.inflateMenu(R.menu.search)
        val item: MenuItem = toolbar.getMenu().findItem(R.id.action_search)
        item.expandActionView()
        searchView = (item.getActionView() as SearchView?)!!
        searchView.setQueryHint(getString(R.string.search_label))
        searchView.requestFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            public override fun onQueryTextSubmit(s: String): Boolean {
                searchView.clearFocus()
                searchWithProgressBar()
                return true
            }

            public override fun onQueryTextChange(s: String): Boolean {
                automaticSearchDebouncer!!.removeCallbacksAndMessages(null)
                if (s.isEmpty() || s.endsWith(" ") || ((lastQueryChange != 0L && System.currentTimeMillis() > lastQueryChange + SEARCH_DEBOUNCE_INTERVAL))) {
                    search()
                } else {
                    automaticSearchDebouncer!!.postDelayed(Runnable({
                        search()
                        lastQueryChange =
                            0 // Don't search instantly with first symbol after some pause
                    }), (SEARCH_DEBOUNCE_INTERVAL / 2).toLong())
                }
                lastQueryChange = System.currentTimeMillis()
                return false
            }
        })
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            public override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            public override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                getParentFragmentManager().popBackStack()
                return true
            }
        })
    }

    public override fun onContextItemSelected(item: MenuItem): Boolean {
        val selectedItem: FeedItem? = adapter!!.longPressedItem
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection")
            return super.onContextItemSelected(item)
        }
        return FeedItemMenuProcess.onMenuItemClicked(this, item.getItemId(), selectedItem)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        search()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        if (results == null) {
            return
        } else if (adapter == null) {
            search()
            return
        }
        var i: Int = 0
        val size: Int = event.items.size
        while (i < size) {
            val item: FeedItem = event.items.get(i)
            val pos: Int = FeedItemUtil.indexOfItemWithId(results, item.getId())
            if (pos >= 0) {
                results!!.removeAt(pos)
                results!!.add(pos, item)
                adapter!!.notifyItemChangedCompat(pos)
            }
            i++
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        val update: DownloaderUpdate = event.update
        if (adapter != null && update.mediaIds.size > 0) {
            for (mediaId: Long in update.mediaIds) {
                val pos: Int = FeedItemUtil.indexOfItemWithMediaId(results, mediaId)
                if (pos >= 0) {
                    adapter!!.notifyItemChangedCompat(pos)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent?) {
        if (adapter != null) {
            for (i in 0 until adapter!!.getItemCount()) {
                val holder: EpisodeItemViewHolder? =
                    recyclerView!!.findViewHolderForAdapterPosition(i) as EpisodeItemViewHolder?
                if (holder != null && holder.isCurrentlyPlayingItem) {
                    holder.notifyPlaybackPositionUpdated((event)!!)
                    break
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerStatusChanged(event: PlayerStatusEvent?) {
        search()
    }

    private fun searchWithProgressBar() {
//        progressBar.setVisibility(View.VISIBLE);
        emptyViewHandler!!.hide()
        if (requireArguments().getLong(ARG_FEED, 0) == 0L) {
            //订阅源下面搜索不显示
//            feedSkeleton.showSkeleton();
        }
        //        itemSkeleton.showSkeleton();
        search()
    }

    private fun search() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable(
            Callable({ performSearch() })
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ results: Pair<List<FeedItem>?, List<Feed>?> ->
//                    progressBar.setVisibility(View.GONE);
//                    if (feedSkeleton.isSkeleton()) {
//                        feedSkeleton.showOriginal();
//                    }
//                    if (itemSkeleton.isSkeleton()) {
//                        itemSkeleton.showOriginal();
//                    }
                this.results = results.first as MutableList<FeedItem>?
                adapter!!.updateItems((results.first)!!)
                if (requireArguments().getLong(ARG_FEED, 0) == 0L) {
                    adapterFeeds!!.updateData(results.second)
                } else {
                    adapterFeeds!!.updateData(emptyList())
                }
                if (searchView.getQuery().toString().isEmpty()) {
                    emptyViewHandler.setMessage(R.string.type_to_search)
                } else {
                    emptyViewHandler.setMessage(
                        getString(
                            R.string.no_results_for_query,
                            searchView.getQuery()
                        )
                    )
                }
            }, { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) })
    }

    private fun performSearch(): Pair<List<FeedItem>?, List<Feed>?> {
        val query: String = searchView.getQuery().toString()
        if (query.isEmpty()) {
            return Pair(emptyList(), emptyList<Feed>())
        }
        val feed: Long = requireArguments().getLong(ARG_FEED)
        var items: List<FeedItem>? = ArrayList()
        var feeds: List<Feed>? = ArrayList()
        if (searchFeeds) {
            feeds = FeedSearcher.searchFeeds(query)
        } else {
            items = FeedSearcher.searchFeedItems(query, feed)
        }
        return Pair(items, feeds)
    }

    private fun showInputMethod(view: View) {
        val imm: InputMethodManager? =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imm != null) {
            imm.showSoftInput(view, 0)
        }
    }

    companion object {
        private val TAG: String = "LocalSearchFragment"
        private val ARG_QUERY: String = "query"
        private val ARG_FEED: String = "feed"
        private val ARG_FEED_NAME: String = "feedName"
        private val ARG_SEARCH_FEED: String = "search_feed"
        private val SEARCH_DEBOUNCE_INTERVAL: Int = 1500

        /**
         * Create a new SearchFragment that searches all feeds.
         */
        @JvmStatic
        fun newInstance(): LocalSearchFragment {
            val fragment: LocalSearchFragment = LocalSearchFragment()
            val args: Bundle = Bundle()
            args.putLong(ARG_FEED, 0)
            fragment.setArguments(args)
            return fragment
        }

        /**
         * Create a new SearchFragment that searches all feeds with pre-defined query.
         */
        @JvmStatic
        fun newInstance(query: String?): LocalSearchFragment {
            val fragment: LocalSearchFragment = newInstance()
            fragment.requireArguments().putString(ARG_QUERY, query)
            return fragment
        }

        /**
         * Create a new SearchFragment that searches one specific feed.
         */
        @JvmStatic
        fun newInstance(feed: Long, feedTitle: String?): LocalSearchFragment {
            val fragment: LocalSearchFragment = newInstance()
            fragment.requireArguments().putLong(ARG_FEED, feed)
            fragment.requireArguments()
                .putString(ARG_FEED_NAME, feedTitle)
            return fragment
        }

        @JvmStatic
        fun newFeedSearchInstance(): LocalSearchFragment {
            val fragment: LocalSearchFragment = newInstance()
            fragment.requireArguments().putBoolean(ARG_SEARCH_FEED, true)
            return fragment
        }
    }
}