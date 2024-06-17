package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.databinding.FeedRefreshDialogBinding
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.TimeUnit

class FeedRefreshPrefDialog(private val context: Context) {
    private var viewBinding: FeedRefreshDialogBinding? = null
    fun show() {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(R.string.feed_refresh_title)
        viewBinding = FeedRefreshDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
        builder.setView(viewBinding!!.root)
        val spinnerArrayAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item, buildSpinnerEntries()
        )
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewBinding!!.spinner.adapter = spinnerArrayAdapter
        viewBinding!!.timePicker.setIs24HourView(DateFormat.is24HourFormat(context))
        viewBinding!!.spinner.setSelection(ArrayUtils.indexOf(INTERVAL_VALUES_HOURS, 24))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewBinding!!.timePicker.hour = 8
            viewBinding!!.timePicker.minute = 0
        } else {
            viewBinding!!.timePicker.currentHour = 8
            viewBinding!!.timePicker.currentMinute = 0
        }
        val currInterval = Prefs.updateInterval
        val updateTime = Prefs.updateTimeOfDay
        if (currInterval > 0) {
            viewBinding!!.spinner.setSelection(
                ArrayUtils.indexOf(
                    INTERVAL_VALUES_HOURS,
                    TimeUnit.MILLISECONDS.toHours(currInterval).toInt()
                )
            )
            viewBinding!!.intervalRadioButton.isChecked = true
        } else if (updateTime.size == 2 && updateTime[0] >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewBinding!!.timePicker.hour = updateTime[0]
                viewBinding!!.timePicker.minute = updateTime[1]
            } else {
                viewBinding!!.timePicker.currentHour = updateTime[0]
                viewBinding!!.timePicker.currentMinute = updateTime[1]
            }
            viewBinding!!.timeRadioButton.isChecked = true
        } else {
            viewBinding!!.disableRadioButton.isChecked = true
        }
        updateVisibility()
        viewBinding!!.radioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup?, i: Int -> updateVisibility() }
        builder.setPositiveButton(R.string.confirm_label) { dialog: DialogInterface?, which: Int ->
            if (viewBinding!!.intervalRadioButton.isChecked) {
                Prefs.updateInterval =
                    INTERVAL_VALUES_HOURS[viewBinding!!.spinner.selectedItemPosition].toLong()
            } else if (viewBinding!!.timeRadioButton.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Prefs.setUpdateTimeOfDay(
                        viewBinding!!.timePicker.hour,
                        viewBinding!!.timePicker.minute
                    )
                } else {
                    Prefs.setUpdateTimeOfDay(
                        viewBinding!!.timePicker.currentHour,
                        viewBinding!!.timePicker.currentMinute
                    )
                }
            } else if (viewBinding!!.disableRadioButton.isChecked) {
                Prefs.disableAutoUpdate(context)
            } else {
                throw IllegalStateException("Unexpected error.")
            }
        }
        builder.setNegativeButton(R.string.cancel_label, null)
        builder.show()
    }

    private fun buildSpinnerEntries(): Array<String?> {
        val res = context.resources
        val entries = arrayOfNulls<String>(INTERVAL_VALUES_HOURS.size)
        for (i in INTERVAL_VALUES_HOURS.indices) {
            val hours = INTERVAL_VALUES_HOURS[i]
            entries[i] = res.getQuantityString(R.plurals.feed_refresh_every_x_hours, hours, hours)
        }
        return entries
    }

    private fun updateVisibility() {
        viewBinding!!.spinner.visibility =
            if (viewBinding!!.intervalRadioButton.isChecked) View.VISIBLE else View.GONE
        viewBinding!!.timePicker.visibility =
            if (viewBinding!!.timeRadioButton.isChecked) View.VISIBLE else View.GONE
    }

    companion object {
        private val INTERVAL_VALUES_HOURS = intArrayOf(1, 2, 4, 8, 12, 24, 72)
    }
}