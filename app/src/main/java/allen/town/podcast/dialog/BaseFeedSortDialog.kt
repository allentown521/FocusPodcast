package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.ItemOffsetDecoration
import allen.town.podcast.R
import allen.town.podcast.model.feed.SortOrder
import allen.town.podcast.model.feed.SortOrder.ASC_INDEX
import allen.town.podcast.model.feed.SortOrder.DESC_INDEX
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip

abstract class BaseFeedSortDialog(
    protected var context: Context,
    protected var lastSortOrder: SortOrder?,
) {
    private var sortItems: Array<String> = arrayOf()
    private var sortValues: Array<SortOrder> = arrayOf()
    var selectedIndex: Int = 0
    var isDesc = true
    private var adapter: OrderSelectionAdapter? = null
    fun openDialog() {

        sortItems = context.resources.getStringArray(R.array.feed_episodes_sort_options)
        val commonSortStringValues =
            context.resources.getStringArray(R.array.feed_episodes_sort_values)
        sortValues = SortOrder.valuesOf(commonSortStringValues)

        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.edit_feed_sort_dialog_layout, null, false)


        val orderGroup = layout.findViewById<MaterialButtonToggleGroup>(R.id.order_group)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.orders)

        lastSortOrder?.run {
            selectedIndex = groupIndex
            isDesc = (code - ascBaseCode) == DESC_INDEX
        }
        orderGroup.check(if (!isDesc) R.id.asc_button else R.id.desc_button)

        //https://github.com/BelooS/ChipsLayoutManager
        val chipsLayoutManager = ChipsLayoutManager.newBuilder(context).build()
        recyclerView.layoutManager = chipsLayoutManager
        recyclerView.addItemDecoration(
            ItemOffsetDecoration(
                context,
                4
            )
        )
        adapter = OrderSelectionAdapter()
        adapter!!.setHasStableIds(true)
        recyclerView.adapter = adapter
        val dialog: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        dialog.setView(layout)
        dialog.setTitle(R.string.sort)
        dialog.setPositiveButton(android.R.string.ok) { d: DialogInterface?, input: Int ->
            val baseIndex =
                if (orderGroup.checkedButtonId == R.id.asc_button) ASC_INDEX else DESC_INDEX
            updateSort(sortValues[selectedIndex * 2 + baseIndex])
        }
        dialog.setNegativeButton(R.string.cancel_label, null)
            .show()

    }


    protected abstract fun updateSort(sortOrder: SortOrder)

    inner class OrderSelectionAdapter : RecyclerView.Adapter<OrderSelectionAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val entryView = inflater.inflate(R.layout.single_tag_chip, parent, false)
            val chip: Chip = entryView.findViewById(R.id.chip)
            return ViewHolder(entryView as Chip)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.chip.text = sortItems[position]
            holder.chip.isChecked = selectedIndex == position
            holder.chip.isCheckedIconVisible = holder.chip.isChecked
            holder.chip.setOnClickListener {
                selectedIndex = position
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

    companion object {
        const val TAG = "FeedsSortDialog"

    }
}