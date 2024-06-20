package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.util.ShareUtils
import allen.town.podcast.model.feed.FeedItem
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment

class ShareDialog : DialogFragment() {
    private var ctx: Context? = null
    private var item: FeedItem? = null
    private var prefs: SharedPreferences? = null
    private var radioMediaFile: RadioButton? = null
    private var radioLinkToEpisode: RadioButton? = null
    private var checkBoxStartAt: SwitchCompat? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (arguments != null) {
            ctx = activity
            item = requireArguments().getSerializable(ARGUMENT_FEED_ITEM) as FeedItem?
            prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        val content = View.inflate(ctx, R.layout.share_episode_dialog, null)
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            ctx!!,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(R.string.share_label)
        builder.setView(content)
        val radioGroup = content.findViewById<RadioGroup>(R.id.share_dialog_radio_group)
        radioGroup.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            checkBoxStartAt!!.isEnabled = checkedId != R.id.share_media_file_radio
        }
        radioLinkToEpisode = content.findViewById(R.id.share_link_to_episode_radio)
        radioMediaFile = content.findViewById(R.id.share_media_file_radio)
        checkBoxStartAt = content.findViewById(R.id.share_start_at_timer_dialog)
        setupOptions()
        builder.setPositiveButton(R.string.share_label) { dialog: DialogInterface?, id: Int ->
            val includePlaybackPosition = checkBoxStartAt!!.isChecked()
            if (radioLinkToEpisode!!.isChecked()) {
                ShareUtils.shareFeedItemLinkWithDownloadLink(ctx, item, includePlaybackPosition)
            } else if (radioMediaFile!!.isChecked()) {
                ShareUtils.shareFeedItemFile(ctx, item!!.media)
            } else {
                throw IllegalStateException("Unknown share method")
            }
            prefs!!.edit().putBoolean(PREF_SHARE_EPISODE_START_AT, includePlaybackPosition).apply()
        }
            .setNegativeButton(R.string.cancel_label) { dialog: DialogInterface, id: Int -> dialog.dismiss() }
        return builder.create()
    }

    private fun setupOptions() {
        val hasMedia = item!!.media != null
        val downloaded = hasMedia && item!!.media!!.isDownloaded
        radioMediaFile!!.visibility = if (downloaded) View.VISIBLE else View.GONE
        val hasDownloadUrl = hasMedia && item!!.media!!.download_url != null
        if (!ShareUtils.hasLinkToShare(item) && !hasDownloadUrl) {
            radioLinkToEpisode!!.visibility = View.GONE
        }
        radioMediaFile!!.isChecked = false
        val switchIsChecked = prefs!!.getBoolean(PREF_SHARE_EPISODE_START_AT, false)
        checkBoxStartAt!!.isChecked = switchIsChecked
    }

    companion object {
        private const val ARGUMENT_FEED_ITEM = "feedItem"
        private const val PREF_NAME = "ShareDialog"
        private const val PREF_SHARE_EPISODE_START_AT = "prefShareEpisodeStartAt"
        @JvmStatic
        fun newInstance(item: FeedItem?): ShareDialog {
            val arguments = Bundle()
            arguments.putSerializable(ARGUMENT_FEED_ITEM, item)
            val dialog = ShareDialog()
            dialog.arguments = arguments
            return dialog
        }
    }
}