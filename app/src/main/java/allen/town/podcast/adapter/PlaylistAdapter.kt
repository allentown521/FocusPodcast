package allen.town.podcast.adapter

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.fragment.swipeactions.SwipeActions
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.annotation.SuppressLint
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MotionEvent
import android.view.View

/**
 * List adapter for the queue.
 */
open class PlaylistAdapter(
    mainActivity: MainActivity,
    private val swipeActions: SwipeActions
) : EpisodeItemListAdapter(mainActivity, R.menu.playlist_action_menus) {
    private var dragDropEnabled: Boolean
    fun updateDragDropEnabled() {
        dragDropEnabled = !(Prefs.isPlaylistKeepSorted || Prefs.isPlaylistLocked)
        notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun afterBindViewHolder(holder: EpisodeItemViewHolder, pos: Int) {
        if (!dragDropEnabled || inActionMode()) {
            holder.dragHandle.visibility = View.GONE
            holder.dragHandle.setOnTouchListener(null)
            holder.coverHolder.setOnTouchListener(null)
        } else {
            holder.dragHandle.visibility = View.VISIBLE
            holder.dragHandle.setOnTouchListener { v1: View?, event: MotionEvent ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    swipeActions.startDrag(holder)
                }
                false
            }
            holder.coverHolder.setOnTouchListener { v1: View, event: MotionEvent ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val isLtr = holder.itemView.layoutDirection == View.LAYOUT_DIRECTION_LTR
                    val factor: Float = if (isLtr) 1F else -1.toFloat()
                    if (factor * event.x < factor * 0.5 * v1.width) {
                        swipeActions.startDrag(holder)
                    } else {
                        Log.d(TAG, "ignore drag in right half of the image")
                    }
                }
                false
            }
        }
        holder.isInQueue.visibility = View.GONE
        holder.size.visibility = View.GONE
        holder.separatorSize.visibility = View.GONE
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        val inflater = activity!!.menuInflater
        inflater.inflate(R.menu.queue_context, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
        if (!inActionMode()) {
            menu.findItem(R.id.multi_select).isVisible = true
            val keepSorted = Prefs.isPlaylistKeepSorted
            if (getItem(0).id == longPressedItem!!.getId() || keepSorted) {
                menu.findItem(R.id.move_to_top_item).isVisible = false
            }
            if (getItem(itemCount - 1).id == longPressedItem!!.getId() || keepSorted) {
                menu.findItem(R.id.move_to_bottom_item).isVisible = false
            }
        } else {
            menu.findItem(R.id.move_to_top_item).isVisible = false
            menu.findItem(R.id.move_to_bottom_item).isVisible = false
        }
    }

    companion object {
        private const val TAG = "QueueRecyclerAdapter"
    }

    init {
        dragDropEnabled = !(Prefs.isPlaylistKeepSorted || Prefs.isPlaylistLocked)
    }
}