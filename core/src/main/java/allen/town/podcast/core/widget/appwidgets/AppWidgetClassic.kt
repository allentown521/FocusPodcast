package allen.town.podcast.core.widget.appwidgets

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import allen.town.podcast.core.R
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.widget.WidgetUpdater
import allen.town.podcast.core.widget.base.BaseAppWidget
import java.util.concurrent.TimeUnit

class AppWidgetClassic : BaseAppWidget() {
    val TAG = "BaseAppWidget"

    override fun getLayout(): Int {
        return R.layout.app_widget_classic
    }

    override fun processRemoteViewIfNeeded(
        context: Context,
        remoteViews: RemoteViews,
        widgetState: WidgetUpdater.WidgetState,
        appWidgetIds: IntArray,
        isCreated: Boolean
    ) {
        if (widgetState.media != null) {
            var icon: Bitmap?
            val iconSize = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)

            try {
                icon = Glide.with(context)
                    .asBitmap()
                    .load(widgetState.media.imageLocation)
                    .apply(
                        RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .transforms(
                                RoundedCorners((8 * context.resources.displayMetrics.density).toInt())
                            )
                    )
                    .submit(iconSize, iconSize)[500, TimeUnit.MILLISECONDS]
                remoteViews.setImageViewBitmap(R.id.imgvCover, icon)
            } catch (tr1: Throwable) {
                try {
                    icon = Glide.with(context)
                        .asBitmap()
                        .load(ImageResourceUtils.getFallbackImageLocation(widgetState.media))
                        .apply(
                            RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                                .transforms(
                                    RoundedCorners((8 * context.resources.displayMetrics.density).toInt())
                                )
                        )
                        .submit(iconSize, iconSize)[500, TimeUnit.MILLISECONDS]
                    remoteViews.setImageViewBitmap(R.id.imgvCover, icon)
                } catch (tr2: Throwable) {
                    Log.e(TAG, "Error loading the media icon for the widget", tr2)
                    remoteViews.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_round)
                }
            }
        }
        pushUpdate(context, appWidgetIds, remoteViews)
    }

    companion object {

        const val NAME = "app_widget_classic"

        private var mInstance: AppWidgetClassic? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
        val instance: AppWidgetClassic
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetClassic()
                }
                return mInstance!!
            }
    }
}