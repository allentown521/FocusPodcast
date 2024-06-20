package allen.town.podcast.adapter

import allen.town.focus_common.util.MenuIconUtil.showContextMenuIcon
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.fragment.FeedItemsViewPagerFragment
import allen.town.podcast.menuprocess.FeedItemMenuProcess
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.viewholder.EpisodeItemViewHolder
import android.app.Activity
import android.os.Build
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import androidx.annotation.MenuRes
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.lang3.ArrayUtils
import java.lang.ref.WeakReference

/**
 * List adapter for the list of new episodes.
 */
open class EpisodeItemListAdapter(mainActivity: MainActivity, @MenuRes menuResId: Int) :
    MultiSelectAdapter<EpisodeItemViewHolder?>(mainActivity, menuResId),
    OnCreateContextMenuListener {
    private val mainActivityRef: WeakReference<MainActivity>
    private var episodes: List<FeedItem> = ArrayList()
    var longPressedItem: FeedItem? = null
        private set
    var longPressedPosition = 0 // used to init actionMode
    fun updateItems(items: List<FeedItem>) {
        episodes = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return R.id.view_type_episode_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeItemViewHolder {
        return EpisodeItemViewHolder(mainActivityRef.get()!!, parent)
    }

    override fun onBindViewHolder(holder: EpisodeItemViewHolder, pos: Int) {
        // Reset state of recycled views
        holder.coverHolder.visibility = View.VISIBLE
        holder.dragHandle.visibility = View.GONE
        beforeBindViewHolder(holder, pos)
        val item = episodes[pos]
        holder.bind(item)
        holder.itemView.setOnClickListener { v: View? ->
            val activity = mainActivityRef.get()
            if (activity != null && !inActionMode()) {
                val ids = FeedItemUtil.getIds(episodes)
                val position = ArrayUtils.indexOf(ids, item.id)
                activity.loadChildFragment(FeedItemsViewPagerFragment.newInstance(ids, position))
            } else {
                toggleSelection(holder.bindingAdapterPosition)
            }
        }
        holder.itemView.setOnCreateContextMenuListener(this)
        holder.itemView.setOnLongClickListener { v: View? ->
            longPressedItem = getItem(holder.bindingAdapterPosition)
            longPressedPosition = holder.bindingAdapterPosition
            false
        }
        holder.itemView.setOnTouchListener { v: View?, e: MotionEvent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                    && e.buttonState == MotionEvent.BUTTON_SECONDARY
                ) {
                    longPressedItem = getItem(holder.bindingAdapterPosition)
                    longPressedPosition = holder.bindingAdapterPosition
                    return@setOnTouchListener false
                }
            }
            false
        }
        if (inActionMode()) {
            holder.playPauseProgressButton.visibility = View.GONE
            holder.selectCheckBox.setOnClickListener { v: View? -> toggleSelection(holder.bindingAdapterPosition) }
            holder.selectCheckBox.isChecked = isSelected(pos)
            holder.selectCheckBox.visibility = View.VISIBLE
        } else {
            holder.selectCheckBox.visibility = View.GONE
        }
        afterBindViewHolder(holder, pos)
        holder.hideSeparatorIfNecessary()
    }

    protected open fun beforeBindViewHolder(holder: EpisodeItemViewHolder?, pos: Int) {}
    protected open fun afterBindViewHolder(holder: EpisodeItemViewHolder, pos: Int) {}
    override fun onViewRecycled(holder: EpisodeItemViewHolder) {
        super.onViewRecycled(holder)
        // Set all listeners to null. This is required to prevent leaking fragments that have set a listener.
        // Activity -> recycledViewPool -> EpisodeItemViewHolder -> Listener -> Fragment (can not be garbage collected)
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnCreateContextMenuListener(null)
        holder.itemView.setOnLongClickListener(null)
        holder.itemView.setOnTouchListener(null)
        holder.playPauseProgressButton.setOnClickListener(null)
        holder.dragHandle.setOnTouchListener(null)
        holder.coverHolder.setOnTouchListener(null)
    }

    /**
     * [.notifyItemChanged] is final, so we can not override.
     * Calling [.notifyItemChanged] may bind the item to a new ViewHolder and execute a transition.
     * This causes flickering and breaks the download animation that stores the old progress in the View.
     * Instead, we tell the adapter to use partial binding by calling [.notifyItemChanged].
     * We actually ignore the payload and always do a full bind but calling the partial bind method ensures
     * that ViewHolders are always re-used.
     *
     * @param position Position of the item that has changed
     */
    fun notifyItemChangedCompat(position: Int) {
        notifyItemChanged(position, "foo")
    }

    override fun getItemId(position: Int): Long {
        val item = episodes[position]
        return item?.id ?: RecyclerView.NO_POSITION.toLong()
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    protected fun getItem(index: Int): FeedItem {
        return episodes[index]
    }

    protected val activity: Activity?
        protected get() = mainActivityRef.get()

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        val inflater = mainActivityRef.get()!!.menuInflater
        if (inActionMode()) {
            inflater.inflate(R.menu.multi_select_context_popup, menu)
        } else {
            if (longPressedItem == null) {
                return
            }
            inflater.inflate(R.menu.feeditemlist_context, menu)
            showContextMenuIcon(menu)
            menu.setHeaderTitle(longPressedItem!!.title)
            FeedItemMenuProcess.onPrepareMenu(menu, longPressedItem, R.id.skip_episode_item)
        }
    }

    fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.multi_select) {
            startSelectMode(longPressedPosition)
            return true
        } else if (item.itemId == R.id.select_all_above) {
            setSelected(0, longPressedPosition, true)
            return true
        } else if (item.itemId == R.id.select_all_below) {
            setSelected(longPressedPosition + 1, itemCount, true)
            return true
        }
        return false
    }

    val selectedItems: List<FeedItem>
        get() {
            val items: MutableList<FeedItem> = ArrayList()
            for (i in 0 until itemCount) {
                if (isSelected(i)) {
                    items.add(getItem(i))
                }
            }
            return items
        }

    init {
        mainActivityRef = WeakReference(mainActivity)
        setHasStableIds(true)
    }
}