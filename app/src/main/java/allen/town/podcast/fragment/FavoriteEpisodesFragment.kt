package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.event.FavoritesEvent
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.Subscribe

/**
 * Like 'EpisodesFragment' except that it only shows favorite episodes and
 * supports swiping to remove from favorites.
 */
class FavoriteEpisodesFragment constructor() : EpisodesListFragment() {
    override val prefName: String get() = PREF_NAME

    override val swipeTag: String
        get() = TAG

    @Subscribe
    fun onEvent(event: FavoritesEvent?) {
        loadItems()
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.filter_items).setVisible(false)
        menu.findItem(R.id.mark_all_read_item).setVisible(false)
        menu.findItem(R.id.refresh_item).setVisible(false)
    }

    override fun getMultiMenu(): Int {
        return R.menu.fav_episodes_multi_menu
    }

    public override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = super.onCreateView(inflater, container, savedInstanceState)
        emptyView.setIcon(R.drawable.ic_star)
        emptyView.setTitle(R.string.no_fav_episodes_head_label)
        toolbar.setTitle(R.string.favorite_episodes_label)
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback =
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
            ) {
                public override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                public override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val holder: EpisodeItemViewHolder = viewHolder as EpisodeItemViewHolder
                    if (disposable != null) {
                        disposable!!.dispose()
                    }
                    val item: FeedItem? = holder.feedItem
                    if (item != null) {
                        DBWriter.removeFavoriteItem(item)
                        (getActivity() as MainActivity?)!!.showSnackbarAbovePlayer(
                            R.string.removed_item,
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(
                                getString(R.string.undo),
                                View.OnClickListener({ v: View? -> DBWriter.addFavoriteItem(item) })
                            )
                    }
                }
            }
        val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        return root
    }

    override fun loadData(): MutableList<FeedItem> {
        return DBReader.getFavoriteItemsList(0, page * EPISODES_PER_PAGE)
    }

    override fun loadMoreData(): MutableList<FeedItem> {
        return DBReader.getFavoriteItemsList((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE)
    }

    companion object {
        const val TAG: String = "FavoriteEpisodesFragment"
        private val PREF_NAME: String = "pref_favorite_episodes_fragment"
    }
}