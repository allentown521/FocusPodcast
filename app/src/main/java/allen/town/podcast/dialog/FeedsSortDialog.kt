package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import android.app.Dialog
import allen.town.podcast.R
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.google.android.material.chip.Chip
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.pref.Prefs.FEED_ORDER_COUNTER
import allen.town.podcast.databinding.EditFeedSortDialogLayoutBinding
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.focus_common.views.ItemOffsetDecoration
import org.greenrobot.eventbus.EventBus
import java.util.*

class FeedsSortDialog : DialogFragment() {

    protected var orderValues: ArrayList<String> = ArrayList()
    protected var orderNames: ArrayList<String> = ArrayList()
    protected var feedOrder: Int = FEED_ORDER_COUNTER
    protected lateinit var feedOrderMethod: String
    var selectedIndex: Int = 0

    private var viewBinding: EditFeedSortDialogLayoutBinding? = null
    private var adapter: OrderSelectionAdapter? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        feedOrder = Prefs.feedOrder
        feedOrderMethod = Prefs.feedOrderMethod
        orderValues =
            requireContext().resources.getStringArray(R.array.nav_drawer_feed_order_values).toList()  as ArrayList<String>
        orderNames =
            requireContext().resources.getStringArray(R.array.nav_drawer_feed_order_options).toList() as ArrayList<String>
        selectedIndex = orderValues.indexOf("$feedOrder")

        viewBinding = EditFeedSortDialogLayoutBinding.inflate(
            layoutInflater
        )

        viewBinding!!.orderGroup.check(if (Prefs.ORDER_ASC.equals(feedOrderMethod)) R.id.asc_button else R.id.desc_button)

        //https://github.com/BelooS/ChipsLayoutManager
        val chipsLayoutManager = ChipsLayoutManager.newBuilder(context).build()
        viewBinding!!.orders.layoutManager = chipsLayoutManager
        viewBinding!!.orders.addItemDecoration(
            ItemOffsetDecoration(
                requireContext(),
                4
            )
        )
        adapter = OrderSelectionAdapter()
        adapter!!.setHasStableIds(true)
        viewBinding!!.orders.adapter = adapter
        val dialog: AlertDialog.Builder = AccentMaterialDialog(
            requireContext(),
            R.style.MaterialAlertDialogTheme
        )
        dialog.setView(viewBinding!!.root)
        dialog.setTitle(R.string.pref_nav_drawer_feed_order_title)
        dialog.setPositiveButton(android.R.string.ok) { d: DialogInterface?, input: Int ->
            updatePreferences()
        }
        dialog.setNegativeButton(R.string.cancel_label, null)
        return dialog.create()
    }


    private fun updatePreferences(
    ) {
        Prefs.setFeedOrder(orderValues[selectedIndex])
        Prefs.feedOrderMethod = (if (viewBinding!!.orderGroup.checkedButtonId == R.id.asc_button) Prefs.ORDER_ASC else Prefs.ORDER_DESC)
        //Update subscriptions
        EventBus.getDefault().post(UnreadItemsUpdateEvent())
    }

    inner class OrderSelectionAdapter : RecyclerView.Adapter<OrderSelectionAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val entryView = inflater.inflate(R.layout.single_tag_chip, parent, false)
            val chip: Chip = entryView.findViewById(R.id.chip)
            return ViewHolder(entryView as Chip)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.chip.text = orderNames[position]
            holder.chip.isChecked = selectedIndex == position
            holder.chip.isCheckedIconVisible = holder.chip.isChecked
            holder.chip.setOnClickListener {
                selectedIndex = position
                adapter!!.notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return orderNames.size
        }

        override fun getItemId(position: Int): Long {
            return orderNames[position].hashCode().toLong()
        }

        inner class ViewHolder internal constructor(var chip: Chip) : RecyclerView.ViewHolder(
            chip
        )
    }

    companion object {
        const val TAG = "FeedsSortDialog"

        @JvmStatic
        fun newInstance(): FeedsSortDialog {
            val fragment = FeedsSortDialog()
            return fragment
        }

    }

}