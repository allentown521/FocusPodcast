package allen.town.podcast.actionbuttons

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.util.playback.PlaybackServiceStarter
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.model.playback.MediaType
import android.app.Activity

class PlayLocalActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.play_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_play_24dp

    override fun onClick(context: Activity?) {
        val media: FeedMedia = item.getMedia() ?: return
        PlaybackServiceStarter(context, media)
            .callEvenIfRunning(true)
            .start()
        if (media.mediaType == MediaType.VIDEO) {
            context!!.startActivity(PlaybackService.getPlayerActivityIntent(context, media))
        }
    }
}