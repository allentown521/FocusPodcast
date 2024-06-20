package allen.town.podcast.dialog

import allen.town.podcast.MyApp.Companion.runOnUiThread
import allen.town.podcast.R
import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class LiveProgressDialog internal constructor(
    var progressBar: LinearLayout,
    var textView: TextView
) {
    fun dismiss() {
        runOnUiThread { progressBar.visibility = View.GONE }
    }

    companion object {
        fun show(context: Activity, charSequence: CharSequence?): LiveProgressDialog {
            val progressDialog = LiveProgressDialog(
                context.findViewById(R.id.llProgressBar),
                context.findViewById(R.id.pbText)
            )
            progressDialog.progressBar.visibility = View.VISIBLE
            progressDialog.textView.text = charSequence
            return progressDialog
        }
    }
}