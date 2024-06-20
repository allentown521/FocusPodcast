package allen.town.podcast.adapter

import allen.town.focus_common.util.MenuIconUtil.showMenuIcon
import allen.town.podcast.R
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView

/**
 * Used by Recyclerviews that need to provide ability to select items.
 * 只有使用了系统默认actionbar主题，并且使用startSupportActionMode(使用这个menu可以显示超过2个，否则只能显示2个，其他的折叠了)才能使toolbar正常被覆盖
 */
abstract class MultiSelectAdapter<T : RecyclerView.ViewHolder?>(
    private val activity: AppCompatActivity, //0代表没有
    @param:MenuRes private val menuResId: Int
) : RecyclerView.Adapter<T>() {
    private var actionMode: ActionMode? = null
    private val selectedIds = HashSet<Long>()
    private var onSelectModeListener: OnSelectModeListener? = null
    private var onPrepareActionModeListener: OnPrepareActionModeListener? = null
    private var onMenuItemClickListener: OnMenuItemClickListener? = null
    fun startSelectMode(pos: Int) {
        if (inActionMode()) {
            endSelectMode()
        }
        if (onSelectModeListener != null) {
            onSelectModeListener!!.onStartSelectMode()
        }
        selectedIds.clear()
        selectedIds.add(getItemId(pos))
        notifyDataSetChanged()
        actionMode = activity.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val inflater = mode.menuInflater
                //                inflater.inflate(R.menu.multi_select_options, menu);
                if (menuResId > 0) {
                    inflater.inflate(menuResId, menu)
                }
                showMenuIcon(menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                updateTitle()
                toggleSelectAllIcon(
                    menu.findItem(R.id.select_toggle),
                    selectedIds.size == itemCount
                )
                if (onPrepareActionModeListener != null) {
                    onPrepareActionModeListener!!.onPrepareActionMode(mode, menu)
                }
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId == R.id.select_toggle) {
                    val allSelected = selectedIds.size == itemCount
                    setSelected(0, itemCount, !allSelected)
                    toggleSelectAllIcon(item, !allSelected)
                    updateTitle()
                    return true
                } else {
                    if (onMenuItemClickListener != null) {
                        onMenuItemClickListener!!.onMenuItemClick(item)
                    }
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                callOnEndSelectMode()
                actionMode = null
                selectedIds.clear()
                notifyDataSetChanged()
            }
        })
        updateTitle()
    }

    /**
     * End action mode if currently in select mode, otherwise do nothing
     */
    fun endSelectMode() {
        if (inActionMode()) {
            callOnEndSelectMode()
            actionMode!!.finish()
        }
    }

    fun isSelected(pos: Int): Boolean {
        return selectedIds.contains(getItemId(pos))
    }

    /**
     * Set the selected state of item at given position
     *
     * @param pos      the position to select
     * @param selected true for selected state and false for unselected
     */
    open fun setSelected(pos: Int, selected: Boolean) {
        if (selected) {
            selectedIds.add(getItemId(pos))
        } else {
            selectedIds.remove(getItemId(pos))
        }
        updateTitle()
    }

    /**
     * Set the selected state of item for a given range
     *
     * @param startPos start position of range, inclusive
     * @param endPos   end position of range, inclusive
     * @param selected indicates the selection state
     * @throws IllegalArgumentException if start and end positions are not valid
     */
    @Throws(IllegalArgumentException::class)
    fun setSelected(startPos: Int, endPos: Int, selected: Boolean) {
        var i = startPos
        while (i < endPos && i < itemCount) {
            setSelected(i, selected)
            i++
        }
        notifyItemRangeChanged(startPos, endPos - startPos)
    }

    protected fun toggleSelection(pos: Int) {
        setSelected(pos, !isSelected(pos))
        notifyItemChanged(pos)
        if (selectedIds.size == 0) {
            endSelectMode()
        }
    }

    fun inActionMode(): Boolean {
        return actionMode != null
    }

    val selectedCount: Int
        get() = selectedIds.size

    private fun toggleSelectAllIcon(selectAllItem: MenuItem?, allSelected: Boolean) {
        if (selectAllItem == null) {
            return
        }
        if (allSelected) {
            selectAllItem.setIcon(R.drawable.ic_select_none)
            selectAllItem.setTitle(R.string.deselect_all_label)
        } else {
            selectAllItem.setIcon(R.drawable.ic_select_all)
            selectAllItem.setTitle(R.string.select_all_label)
        }
    }

    private fun updateTitle() {
        if (actionMode == null) {
            return
        }
        actionMode!!.title = selectedIds.size.toString() + ""
    }

    fun setOnSelectModeListener(onSelectModeListener: OnSelectModeListener?) {
        this.onSelectModeListener = onSelectModeListener
    }

    fun setonPrepareActionListener(onPrepareActionModeListener: OnPrepareActionModeListener?) {
        this.onPrepareActionModeListener = onPrepareActionModeListener
    }

    private fun callOnEndSelectMode() {
        if (onSelectModeListener != null) {
            onSelectModeListener!!.onEndSelectMode()
        }
    }

    interface OnSelectModeListener {
        fun onStartSelectMode()
        fun onEndSelectMode()
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(item: MenuItem?)
    }

    interface OnPrepareActionModeListener {
        fun onPrepareActionMode(mode: ActionMode?, item: Menu?)
    }

    fun setOnMenuItemClickListener(onMenuItemClickListener: OnMenuItemClickListener?) {
        this.onMenuItemClickListener = onMenuItemClickListener
    }
}