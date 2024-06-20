package allen.town.podcast.actionbuttons

import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.core.util.IntentUtils.openInBrowser
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.R
import android.app.Activity

class VisitWebsiteActionButton(val item: FeedItem) : ItemActionButton {
    @get:StringRes
    override val label: Int
        get() = R.string.visit_website_label

    @get:DrawableRes
    override val drawable: Int
        get() = R.drawable.ic_web

    override fun onClick(context: Activity?) {
        openInBrowser(context!!, item.getLink())
    }

    override val isVisibility: Int
        get() = View.GONE
}