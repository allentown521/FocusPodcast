package allen.town.podcast.actionbuttons

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.core.util.IntentUtils.sendLocalBroadcast
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.service.playback.PlaybackService
import android.app.Activity

class PauseActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.pause_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_pause

    override fun onClick(context: Activity?) {
        val media: FeedMedia = item.getMedia() ?: return
        if (FeedItemUtil.isCurrentlyPlaying(media)) {
            sendLocalBroadcast(context!!, PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE)
        }
    }
}