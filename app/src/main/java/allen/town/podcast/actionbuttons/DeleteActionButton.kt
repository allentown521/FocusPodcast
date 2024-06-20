package allen.town.podcast.actionbuttons

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.storage.DBWriter
import android.app.Activity

class DeleteActionButton(val item: FeedItem) : ItemActionButton {
    override fun getDrawableTintColor(context: Context?): Int {
        return accentColor(context!!)
    }

    @get:StringRes
    override val label: Int
        get() = R.string.delete_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_round_check_circle_outline_24

    override fun onClick(context: Activity?) {
        val media: FeedMedia = item.getMedia() ?: return
        val dialog: ConfirmationDialog = object : ConfirmationDialog(
            context,
            R.string.delete_label,
            R.string.confirm_delete_download_file
        ) {
            override fun onConfirmButtonPressed(clickedDialog: DialogInterface) {
                clickedDialog.dismiss()
                DBWriter.deleteFeedMediaOfItem(context!!, media.id)
            }
        }
        dialog.createNewDialog().show()
    }

    override val isVisibility: Int
        get() = if (item.getMedia() != null && item.getMedia()!!
                .isDownloaded()
        ) View.VISIBLE else View.INVISIBLE
}