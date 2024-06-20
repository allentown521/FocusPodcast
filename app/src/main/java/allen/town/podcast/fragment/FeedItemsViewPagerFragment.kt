package allen.town.podcast.fragment

import allen.town.focus_common.util.MenuIconUtil.showToolbarMenuIcon
import allen.town.podcast.fragment.FeedItemFragment.Companion.newInstance
import allen.town.podcast.model.feed.FeedItem
import io.reactivex.disposables.Disposable
import android.os.Bundle
import org.greenrobot.eventbus.ThreadMode
import allen.town.podcast.event.FeedItemEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.core.storage.DBReader
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Displays information about a list of FeedItems.
 */
class FeedItemsViewPagerFragment : Fragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var pager: ViewPager2
    private var feedItems: LongArray?= null
    private var item: FeedItem? = null
    private var disposable: Disposable? = null
    private lateinit var toolbar: Toolbar
    var extendedFloatingActionButton: ExtendedFloatingActionButton? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val layout = inflater.inflate(R.layout.feeditem_pager_fragment, container, false)
        toolbar = layout.findViewById(R.id.toolbar)
        toolbar.setTitle("")
        toolbar.inflateMenu(R.menu.feeditem_options)
        toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> parentFragmentManager.popBackStack() })
        toolbar.setOnMenuItemClickListener(this)
        showToolbarMenuIcon(toolbar)
        extendedFloatingActionButton = layout.findViewById(R.id.play_float_button)
        feedItems = requireArguments().getLongArray(ARG_FEEDITEMS)
        val feedItemPos = Math.max(0, requireArguments().getInt(ARG_FEEDITEM_POS))
        pager = layout.findViewById(R.id.pager)
        // FragmentStatePagerAdapter documentation:
        // > When using FragmentStatePagerAdapter the host ViewPager must have a valid ID set.
        // When opening multiple ItemPagerFragments by clicking "item" -> "visit podcast" -> "item" -> etc,
        // the ID is no longer unique and FragmentStatePagerAdapter does not display any pages.
        var newId = View.generateViewId()
        if (savedInstanceState != null && savedInstanceState.getInt(KEY_PAGER_ID, 0) != 0) {
            // Restore state by using the same ID as before. ID collisions are prevented in MainActivity.
            newId = savedInstanceState.getInt(KEY_PAGER_ID, 0)
        }
        pager.setId(newId)
        pager.setAdapter(ItemPagerAdapter(this))
        pager.setCurrentItem(feedItemPos, false)
        pager.setOffscreenPageLimit(1)
        loadItem(feedItems!![feedItemPos])
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                loadItem(feedItems!![position])
            }
        })
        EventBus.getDefault().register(this)
        return layout
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGER_ID, pager!!.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    private fun loadItem(itemId: Long) {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable { DBReader.getFeedItem(itemId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result: FeedItem? ->
                item = result
                refreshToolbarState()
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    fun refreshToolbarState() {
        if (item == null) {
            return
        }
        if (item!!.hasMedia()) {
            FeedItemMenuProcess.onPrepareMenu(toolbar!!.menu, item)
        } else {
            // these are already available via button1 and button2
            FeedItemMenuProcess.onPrepareMenu(
                toolbar!!.menu, item,
                R.id.mark_read_item, R.id.visit_website_item
            )
        }
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        return FeedItemMenuProcess.onMenuItemClicked(this, menuItem.itemId, item!!)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FeedItemEvent) {
        for (item in event.items) {
            if (this.item != null && this.item!!.id == item.id) {
                this.item = item
                refreshToolbarState()
                return
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        refreshToolbarState()
    }

    private fun openPodcast() {
        val fragment: Fragment = FeedItemlistFragment.Companion.newInstance(
            item!!.feedId
        )
        (activity as MainActivity?)!!.loadChildFragment(fragment)
    }

    private inner class ItemPagerAdapter internal constructor(fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment {
            return newInstance(feedItems!![position])
        }

        override fun getItemCount(): Int {
            return feedItems!!.size
        }
    }

    companion object {
        private const val ARG_FEEDITEMS = "feeditems"
        private const val ARG_FEEDITEM_POS = "feeditem_pos"
        private const val KEY_PAGER_ID = "pager_id"

        /**
         * Creates a new instance of an ItemPagerFragment.
         *
         * @param feeditems   The IDs of the FeedItems that belong to the same list
         * @param feedItemPos The position of the FeedItem that is currently shown
         * @return The ItemFragment instance
         */
        fun newInstance(feeditems: LongArray?, feedItemPos: Int): FeedItemsViewPagerFragment {
            val fragment = FeedItemsViewPagerFragment()
            val args = Bundle()
            args.putLongArray(ARG_FEEDITEMS, feeditems)
            args.putInt(ARG_FEEDITEM_POS, Math.max(0, feedItemPos))
            fragment.arguments = args
            return fragment
        }
    }
}