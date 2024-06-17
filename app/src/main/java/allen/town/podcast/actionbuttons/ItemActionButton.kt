package allen.town.podcast.actionbuttons

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.model.feed.FeedItem
import android.app.Activity

interface ItemActionButton  {
    @get:StringRes
    abstract val label: Int

    @get:DrawableRes
    abstract val drawable: Int
    @ColorInt
    fun getDrawableTintColor(context: Context?): Int {
        return -1
    }

    abstract fun onClick(context: Activity?)
    open val isVisibility: Int
        get() = View.VISIBLE

    fun configure(button: View, icon: ImageView?, context: Activity) {
        button.visibility = isVisibility
        button.contentDescription = context.getString(label)
        button.setOnClickListener { view: View? -> onClick(context) }
        icon?.setImageResource(drawable)
    }

    fun noMedia(item: FeedItem?):Boolean{
        return item == null || item.media == null
    }

    fun isLocalFeed(item: FeedItem):Boolean{
        return item.feed.isLocalFeed
    }

    companion object {
        @JvmStatic
        fun forItem(item: FeedItem): ItemActionButton {
            val media = item.media ?: return MarkAsPlayedActionButton(item)
            val isDownloadingMedia = DownloadService.isDownloadingFile(media.download_url)
            return if (FeedItemUtil.isCurrentlyPlaying(media)) {
                PauseActionButton(item)
            } else if (item.feed.isLocalFeed) {
                PlayLocalActionButton(item)
            } else if (media.isDownloaded) {
                PlayActionButton(item)
            } else if (isDownloadingMedia) {
                CancelDownloadActionButton(item)
            } else if (Prefs.isStreamOverDownload) {
                StreamActionButton(item)
            } else {
                DownloadActionButton(item)
            }
        }
    }
}