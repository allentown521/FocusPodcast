package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.feed.FeedItemFilterGroup
import allen.town.podcast.model.feed.FeedItemFilter
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.*

abstract class FilterDialog(protected var context: Context, protected var filter: FeedItemFilter) {
    fun openDialog() {
        val filterValues: MutableSet<String> = HashSet(Arrays.asList(*filter.values))
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(R.string.filter)
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.filter_dialog, null, false)
        val rows = layout.findViewById<LinearLayout>(R.id.filter_rows)
        builder.setView(layout)
        for (item in FeedItemFilterGroup.values()) {
            val row = inflater.inflate(R.layout.filter_dialog_row, null, false) as LinearLayout
            val filter1 = row.findViewById<MaterialButton>(R.id.filter_dialog_radioButton1)
            val filter2 = row.findViewById<MaterialButton>(R.id.filter_dialog_radioButton2)
            filter1.setText(item.values[0].displayName)
            filter1.tag = item.values[0].filterId
            filter2.setText(item.values[1].displayName)
            filter2.tag = item.values[1].filterId
            rows.addView(row)
        }
        for (filterId in filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                val button = layout.findViewWithTag<MaterialButton>(filterId)
                if (button != null) {
                    button.isChecked = true
                }
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
        builder.create().show()
    }

    protected abstract fun updateFilter(filterValues: Set<String>?)
}