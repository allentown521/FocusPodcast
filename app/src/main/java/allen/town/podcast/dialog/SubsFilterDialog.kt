package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.feed.SubscriptionsFilter
import allen.town.podcast.core.feed.SubscriptionsFilterGroup
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.event.UnreadItemsUpdateEvent
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.greenrobot.eventbus.EventBus
import java.util.*

object SubsFilterDialog {
    @JvmStatic
    fun showDialog(context: Context) {
        val subscriptionsFilter = Prefs.subscriptionsFilter
        val filterValues: MutableSet<String> = HashSet(Arrays.asList(*subscriptionsFilter.values))
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(context.getString(R.string.pref_filter_feed_title))
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.filter_dialog, null, false)
        val rows = layout.findViewById<LinearLayout>(R.id.filter_rows)
        builder.setView(layout)
        for (item in SubscriptionsFilterGroup.values()) {
            val row = inflater.inflate(R.layout.filter_dialog_row, null) as LinearLayout
            val filter1 = row.findViewById<MaterialButton>(R.id.filter_dialog_radioButton1)
            val filter2 = row.findViewById<MaterialButton>(R.id.filter_dialog_radioButton2)
            filter1.setText(item.values[0].displayName)
            filter1.tag = item.values[0].filterId
            if (item.values.size == 2) {
                filter2.setText(item.values[1].displayName)
                filter2.tag = item.values[1].filterId
            } else {
                filter2.visibility = View.GONE
            }
            rows.addView(row)
        }
        for (filterId in filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                (layout.findViewWithTag<View>(filterId) as MaterialButton).isChecked = true
            }
        }
        builder.setPositiveButton(R.string.confirm_label) { dialog: DialogInterface?, which: Int ->
            filterValues.clear()
            for (i in 0 until rows.childCount) {
                if (rows.getChildAt(i) !is MaterialButtonToggleGroup) {
                    continue
                }
                val group = rows.getChildAt(i) as MaterialButtonToggleGroup
                if (group.checkedButtonId != View.NO_ID) {
                    val tag = group.findViewById<View>(group.checkedButtonId).tag as String
                    if (tag != null) { // Clear buttons use no tag
                        filterValues.add(tag)
                    }
                }
            }
            updateFilter(filterValues)
        }
        builder.setNegativeButton(R.string.cancel_label, null)
        builder.show()
    }

    private fun updateFilter(filterValues: Set<String>) {
        val subscriptionsFilter = SubscriptionsFilter(filterValues.toTypedArray())
        Prefs.subscriptionsFilter = subscriptionsFilter
        EventBus.getDefault().post(UnreadItemsUpdateEvent())
    }
}