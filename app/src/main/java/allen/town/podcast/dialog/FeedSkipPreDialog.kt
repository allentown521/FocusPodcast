package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.EditText

/**
 * Displays a dialog with a username and password text field and an optional checkbox to save username and preferences.
 */
abstract class FeedSkipPreDialog(
    context: Context?, skipIntroInitialValue: Int,
    skipEndInitialValue: Int
) : AccentMaterialDialog(
    context!!, R.style.MaterialAlertDialogTheme
) {
    protected abstract fun onConfirmed(skipIntro: Int, skipEndig: Int)

    init {
        setTitle(R.string.pref_feed_skip)
        val rootView = View.inflate(context, R.layout.feed_pref_skip_dialog, null)
        setView(rootView)
        val etxtSkipIntro = rootView.findViewById<EditText>(R.id.etxtSkipIntro)
        val etxtSkipEnd = rootView.findViewById<EditText>(R.id.etxtSkipEnd)
        etxtSkipIntro.setText(skipIntroInitialValue.toString())
        etxtSkipEnd.setText(skipEndInitialValue.toString())
        setNegativeButton(R.string.cancel_label, null)
        setPositiveButton(R.string.confirm_label) { dialog: DialogInterface?, which: Int ->
            val skipIntro: Int
            val skipEnding: Int
            skipIntro = try {
                etxtSkipIntro.text.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            skipEnding = try {
                etxtSkipEnd.text.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            onConfirmed(skipIntro, skipEnding)
        }
    }
}