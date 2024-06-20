/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package allen.town.podcast.core.widget.appwidgets

import allen.town.focus_common.util.RetroUtil
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import code.name.monkey.appthemehelper.util.ImageUtil
import com.bumptech.glide.request.target.Target
import allen.town.podcast.core.R
import allen.town.podcast.core.glide.palette.BitmapPaletteWrapper
import allen.town.podcast.core.widget.WidgetUpdater
import allen.town.podcast.core.widget.base.BaseAppWidget
import allen.town.podcast.playback.base.PlayerStatus

open class AppWidgetText : BaseAppWidget() {
    private var target: Target<BitmapPaletteWrapper>? = null // for cancellation

    private val TAG = "AppWidgetText"

    /**
     * Link up various button actions using [PendingIntent].
     */

    companion object {

        const val NAME = "app_widget_text"

        private var mInstance: AppWidgetText? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
        val instance: AppWidgetText
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetText()
                }
                return mInstance!!
            }
    }

    override fun getLayout(): Int {
        return R.layout.app_widget_text
    }

    override fun getPauseBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_pause,
                ContextCompat.getColor(
                    context, R.color.md_white_1000
                )
            ), 1f
        )
    }

    override fun getPlayBitmap(context: Context): Bitmap {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_play,
                ContextCompat.getColor(
                    context, R.color.md_white_1000
                )
            ), 1f
        )
    }

    override fun getNextBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_skip,
                ContextCompat.getColor(
                    context, R.color.md_white_1000
                )
            ), 1f
        )
    }

    override fun getForwardBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_fast_forward,
                ContextCompat.getColor(
                    context, R.color.md_white_1000
                )
            ), 1f
        )
    }

    override fun getRewindBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_fast_rewind,
                ContextCompat.getColor(
                    context, R.color.md_white_1000
                )
            ), 1f
        )
    }

    override fun processRemoteViewIfNeeded(
        context: Context,
        remoteViews: RemoteViews,
        widgetState: WidgetUpdater.WidgetState,
        appWidgetIds: IntArray,
        isCreated: Boolean,
    ) {
        remoteViews.setImageViewResource(R.id.imgvCover, R.drawable.default_audio_art)

        if (imageSize == 0) {
            imageSize =
                context.resources.getDimensionPixelSize(R.dimen.app_widget_classic_image_size)
        }
        if (cardRadius == 0f) {
            cardRadius = context.resources.getDimension(R.dimen.app_widget_card_radius)
        }

        // Load the album cover async and push the update on completion
        val appContext = context.applicationContext

        // Set correct drawable for pause state
        val playPauseRes =
            if (widgetState.status == PlayerStatus.PLAYING) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        remoteViews.setImageViewBitmap(
            R.id.butPlayExtended,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    context,
                    playPauseRes,
                    ContextCompat.getColor(
                        context, R.color.md_white_1000
                    )
                )
            )
        )

        pushUpdate(context, appWidgetIds, remoteViews)
    }
}
