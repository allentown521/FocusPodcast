package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.event.playback.PlaybackHistoryEvent
import allen.town.podcast.model.feed.FeedItem
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlaybackHistoryFragment constructor() : EpisodesListFragment(),
    Toolbar.OnMenuItemClickListener {
    private var displayUpArrow: Boolean = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = super.onCreateView(inflater, container, savedInstanceState)
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.playback_history_label)
        toolbar.setOnMenuItemClickListener(this)
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (getActivity() as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        toolbar.getMenu().clear()
        toolbar.inflateMenu(R.menu.playback_history)
        refreshToolbarState()
        emptyView.setIcon(R.drawable.ic_history)
        emptyView.setTitle(R.string.no_history_head_label)
        return root
    }

    override val swipeTag: String
        get() = TAG

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    fun refreshToolbarState() {
        val hasHistory: Boolean = episodes != null && !episodes.isEmpty()
        toolbar.getMenu().findItem(R.id.clear_history_item).setVisible(hasHistory)
    }

    public override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.clear_history_item) {
            val conDialog: ConfirmationDialog = object : ConfirmationDialog(
                activity,
                R.string.clear_history_label,
                R.string.clear_playback_history_msg
            ) {
                override fun onConfirmButtonPressed(dialog: DialogInterface) {
                    dialog.dismiss()
                    DBWriter.clearPlaybackHistory()
                }
            }
            conDialog.createNewDialog().show()
            return true
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryUpdated(event: PlaybackHistoryEvent?) {
        loadItems()
        refreshToolbarState()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public override fun onPlayerStatusChanged(event: PlayerStatusEvent) {
        loadItems()
        refreshToolbarState()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public override fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent) {
        loadItems()
        refreshToolbarState()
    }

    override fun onFragmentLoaded(episodes: List<FeedItem>) {
        super.onFragmentLoaded(episodes)
        listAdapter!!.notifyDataSetChanged()
        refreshToolbarState()
    }

    override fun loadData(): MutableList<FeedItem> {
        return DBReader.getPlaybackHistory(0, page * EPISODES_PER_PAGE)
    }

    override fun loadMoreData(): MutableList<FeedItem> {
        return DBReader.getPlaybackHistory((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE)
    }

    companion object {
        val TAG: String = "PlaybackHistoryFragment"
        private val KEY_UP_ARROW: String = "up_arrow"
    }
}