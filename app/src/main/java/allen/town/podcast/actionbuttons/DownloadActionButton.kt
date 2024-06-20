package allen.town.podcast.actionbuttons

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.pref.UsageStatistics
import allen.town.podcast.core.util.NetworkUtils
import allen.town.podcast.core.service.download.DownloadRequestCreator
import allen.town.podcast.core.storage.DBWriter
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.core.service.download.DownloadService
import android.app.Activity

class DownloadActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.download_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_round_arrow_circle_down_24
    override val isVisibility: Int
        get() = if (item.getFeed().isLocalFeed()) View.GONE else View.VISIBLE

    override fun onClick(context: Activity?) {
        val media: FeedMedia? = item.getMedia()
        if (media == null || shouldNotDownload(media)) {
            return
        }
        UsageStatistics.logAction(UsageStatistics.ACTION_DOWNLOAD)
        if (NetworkUtils.isEpisodeDownloadAllowed() || MobileDownloadHelper.userAllowedMobileDownloads()) {
            DownloadService.download(
                context,
                false,
                DownloadRequestCreator.create(item.getMedia()).build()
            )
        } else if (MobileDownloadHelper.userChoseAddToQueue() && !item.isTagged(FeedItem.TAG_QUEUE)) {
            DBWriter.addQueueItem(context, item)
            showSnack(context, R.string.added_to_queue_label, Toast.LENGTH_SHORT)
        } else {
            MobileDownloadHelper.confirmMobileDownload(context, item)
        }
    }

    private fun shouldNotDownload(media: FeedMedia): Boolean {
        val isDownloading = DownloadService.isDownloadingFile(media.download_url)
        return isDownloading || media.isDownloaded
    }
}