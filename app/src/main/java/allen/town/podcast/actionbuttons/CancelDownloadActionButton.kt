package allen.town.podcast.actionbuttons

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBWriter
import android.app.Activity

class CancelDownloadActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.cancel_download_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_cancel

    override fun onClick(context: Activity?) {
        val media = item.media
        DownloadService.cancel(context, media!!.download_url)
        if (Prefs.isEnableAutodownload) {
            item.disableAutoDownload()
            DBWriter.setFeedItem(item)
        }
    }
}