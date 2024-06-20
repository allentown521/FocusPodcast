package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.storage.NavDrawerData.*
import allen.town.podcast.databinding.EditTextDialogBinding
import allen.town.podcast.model.feed.Feed
import allen.town.podcast.model.feed.FeedPreferences
import android.app.Activity
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import java.lang.ref.WeakReference

class RenameItemDialog {
    private val activityRef: WeakReference<Activity>
    private var feed: Feed? = null
    private var drawerItem: DrawerItem? = null

    constructor(activity: Activity, feed: Feed?) {
        activityRef = WeakReference(activity)
        this.feed = feed
    }

    constructor(activity: Activity, drawerItem: DrawerItem?) {
        activityRef = WeakReference(activity)
        this.drawerItem = drawerItem
    }

    fun show() {
        val activity = activityRef.get() ?: return
        val content = View.inflate(activity, R.layout.edit_text_dialog, null)
        val alertViewBinding = EditTextDialogBinding.bind(content)
        val title = if (feed != null) feed!!.title else drawerItem!!.title
        alertViewBinding.urlEditText.setText(title)
        val dialog = AccentMaterialDialog(
            activity,
            R.style.MaterialAlertDialogTheme
        )
            .setView(content)
            .setTitle(if (feed != null) R.string.rename_feed_label else R.string.rename_tag_label)
            .setPositiveButton(android.R.string.ok) { d: DialogInterface?, input: Int ->
                val newTitle = alertViewBinding.urlEditText.text.toString()
                if (feed != null) {
                    feed!!.customTitle = newTitle
                    DBWriter.setFeedCustomTitle(feed)
                } else {
                    renameTag(newTitle)
                }
            }
            .setNeutralButton(allen.town.podcast.core.R.string.reset, null)
            .setNegativeButton(allen.town.podcast.core.R.string.cancel_label, null)
            .show()

        // To prevent cancelling the dialog on button click
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener { view: View? -> alertViewBinding.urlEditText.setText(title) }
    }

    private fun renameTag(title: String) {
        if (DrawerItem.Type.TAG == drawerItem!!.type) {
            val feedPreferences: MutableList<FeedPreferences> = ArrayList()
            for (item in (drawerItem as TagDrawerItem?)!!.children) {
                feedPreferences.add((item as FeedDrawerItem).feed.preferences)
            }
            for (preferences in feedPreferences) {
                preferences.tags.remove(drawerItem!!.title)
                preferences.tags.add(title)
                DBWriter.setFeedPreferences(preferences)
            }
        }
    }
}