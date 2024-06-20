package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import android.content.Context
import android.content.DialogInterface
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import java.util.*

/**
 * Shows the dialog that allows setting the skip time.
 */
object SkipPrefDialog {
    @JvmStatic
    fun showSkipPreference(context: Context, direction: SkipDirection, textView: TextView?) {
        var checked = 0
        val skipSecs: Int
        skipSecs = if (direction == SkipDirection.SKIP_FORWARD) {
            Prefs.fastForwardSecs
        } else {
            Prefs.rewindSecs
        }
        val values = context.resources.getIntArray(R.array.seek_delta_values)
        val choices = arrayOfNulls<String>(values.size)
        for (i in values.indices) {
            if (skipSecs == values[i]) {
                checked = i
            }
            choices[i] = String.format(
                Locale.getDefault(),
                "%d %s", values[i], context.getString(R.string.time_seconds)
            )
        }
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(if (direction == SkipDirection.SKIP_FORWARD) R.string.pref_fast_forward else R.string.pref_rewind)
        builder.setSingleChoiceItems(choices, checked, null)
        builder.setNegativeButton(R.string.cancel_label, null)
        builder.setPositiveButton(R.string.confirm_label) { dialog: DialogInterface, which: Int ->
            val choice = (dialog as AlertDialog).listView.checkedItemPosition
            if (choice < 0 || choice >= values.size) {
                System.err.printf("Choice in showSkipPreference is out of bounds %d", choice)
            } else {
                val seconds = values[choice]
                if (direction == SkipDirection.SKIP_FORWARD) {
                    Prefs.fastForwardSecs = seconds
                } else {
                    Prefs.rewindSecs = seconds
                }
                if (textView != null) {
                    textView.text = NumberFormat.getInstance().format(seconds.toLong())
                }
            }
        }
        builder.create().show()
    }

    enum class SkipDirection {
        SKIP_FORWARD, SKIP_REWIND
    }
}