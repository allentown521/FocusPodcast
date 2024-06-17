package allen.town.podcast.actionbuttons

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.storage.DBTasks
import allen.town.podcast.core.util.playback.PlaybackServiceStarter
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.model.playback.MediaType
import android.app.Activity

class PlayActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.play_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_play_48dp

    override fun onClick(context: Activity?) {
        val media: FeedMedia = item.getMedia() ?: return
        if (!media.fileExists()) {
            DBTasks.notifyMissingFeedMediaFile(context, media)
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