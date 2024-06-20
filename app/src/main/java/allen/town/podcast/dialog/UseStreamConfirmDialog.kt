package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.util.playback.PlaybackServiceStarter
import allen.town.podcast.model.playback.Playable
import android.content.Context
import android.content.DialogInterface

class UseStreamConfirmDialog(private val context: Context, private val playable: Playable) {
    fun show() {
        AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
            .setTitle(R.string.stream_label)
            .setMessage(R.string.confirm_mobile_streaming_notification_message)
            .setPositiveButton(R.string.confirm_mobile_streaming_button_once) { dialog: DialogInterface?, which: Int -> stream() }
            .setNegativeButton(R.string.confirm_mobile_streaming_button_always) { dialog: DialogInterface?, which: Int ->
                Prefs.isAllowMobileStreaming = true
                stream()
            }
            .setNeutralButton(R.string.cancel_label, null)
            .show()
    }

    private fun stream() {
        PlaybackServiceStarter(context, playable)
            .callEvenIfRunning(true)
            .shouldStreamThisTime(true)
            .start()
    }
}