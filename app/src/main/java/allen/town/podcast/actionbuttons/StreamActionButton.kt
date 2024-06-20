package allen.town.podcast.actionbuttons

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.pref.UsageStatistics
import allen.town.podcast.core.util.NetworkUtils
import allen.town.podcast.dialog.UseStreamConfirmDialog
import allen.town.podcast.core.util.playback.PlaybackServiceStarter
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.model.playback.MediaType
import android.app.Activity

class StreamActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.stream_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_play_48dp

    override fun onClick(context: Activity?) {
        val media: FeedMedia = item.getMedia() ?: return
        UsageStatistics.logAction(UsageStatistics.ACTION_STREAM)
        if (!NetworkUtils.isStreamingAllowed()) {
            UseStreamConfirmDialog(context!!, media).show()
            return
        }
        PlaybackServiceStarter(context, media)
            .callEvenIfRunning(true)
            .start()
        if (media.mediaType == MediaType.VIDEO) {
            context!!.startActivity(PlaybackService.getPlayerActivityIntent(context, media))
        }
    }
}