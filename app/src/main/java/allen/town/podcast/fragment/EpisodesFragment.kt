package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.dialog.FilterDialog
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedItemFilter
import android.content.Context
import android.os.Bundle
import android.view.*
import com.joanzapata.iconify.Iconify
import org.apache.commons.lang3.StringUtils

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
class EpisodesFragment : EpisodesListFragment() {
    private var feedItemFilter = FeedItemFilter("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        feedItemFilter = FeedItemFilter(prefs.getString(PREF_FILTER, ""))
    }

    override val swipeTag
        get() = TAG
    override val prefName
        get() = PREF_NAME

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        emptyView.setIcon(R.drawable.ic_star)
        toolbar.setTitle(R.string.episodes_label)
        return root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!super.onOptionsItemSelected(item)) {
            if (item.itemId == R.id.filter_items) {
                showFilterDialog()
                return true
            }
            false
        } else {
            true
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.filter_items).isVisible = true
        menu.findItem(R.id.mark_all_read_item).isVisible = true
    }

    override fun onFragmentLoaded(episodes: List<FeedItem>) {
        super.onFragmentLoaded(episodes)
        val filterItem = toolbar.menu.findItem(R.id.filter_items)
        if (feedItemFilter.values.size > 0) {
            filterItem.setTitle(R.string.filtered_label)
            filterItem.setIcon(R.drawable.ic_filter_disable)
        } else {
            filterItem.setTitle(R.string.filter)
            filterItem.setIcon(R.drawable.ic_filter)
        }
    }

    private fun showFilterDialog() {
        val filterDialog: FilterDialog = object : FilterDialog(requireContext(), feedItemFilter) {
            override fun updateFilter(filterValues: Set<String>?) {
                feedItemFilter = FeedItemFilter(filterValues!!.toTypedArray())
                val prefs =
                    activity!!.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(PREF_FILTER, StringUtils.join(filterValues, ",")).apply()
                loadItems()
            }
        }
        filterDialog.openDialog()
    }

    override fun shouldUpdatedItemRemainInList(item: FeedItem): Boolean {
        val prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val feedItemFilter = FeedItemFilter(prefs.getString(PREF_FILTER, ""))
        return if (feedItemFilter.isShowDownloaded && (!item.hasMedia() || !item.media!!.isDownloaded)) {
            false
        } else true
    }

    override fun loadData(): MutableList<FeedItem> {
        return DBReader.getRecentlyPublishedEpisodes(0, page * EPISODES_PER_PAGE, feedItemFilter)
    }

    override fun loadMoreData(): MutableList<FeedItem> {
        return DBReader.getRecentlyPublishedEpisodes(
            (page - 1) * EPISODES_PER_PAGE,
            EPISODES_PER_PAGE,
            feedItemFilter
        )
    }

    companion object {
        const val PREF_NAME = "pref_all_episodes_fragment"
        const val TAG = "EpisodesFragment"
        private const val PREF_FILTER = "filter"
    }
}